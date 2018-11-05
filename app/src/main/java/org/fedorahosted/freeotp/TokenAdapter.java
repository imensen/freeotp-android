/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.apache.commons.codec.binary.Hex;
import org.fedorahosted.freeotp.data.Encrypter;
import org.fedorahosted.freeotp.data.TargetData;
import org.fedorahosted.freeotp.edit.DeleteActivity;
import org.fedorahosted.freeotp.edit.EditActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import static java.security.AccessController.getContext;

public class TokenAdapter extends BaseReorderableAdapter {
    private final TokenPersistence mTokenPersistence;
    private final LayoutInflater mLayoutInflater;
    private final ClipboardManager mClipMan;
    private final Map<String, TokenCode> mTokenCodes;
    private final ConnectivityManager mConnectivityManager;

    public TokenAdapter(Context ctx) {
        mTokenPersistence = new TokenPersistence(ctx);
        mLayoutInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mClipMan = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        mTokenCodes = new HashMap<>();
        mConnectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                mTokenCodes.clear();
            }

            @Override
            public void onInvalidated() {
                mTokenCodes.clear();
            }
        });
    }

    @Override
    public int getCount() {
        return mTokenPersistence.length();
    }

    @Override
    public Token getItem(int position) {
        return mTokenPersistence.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    protected void move(int fromPosition, int toPosition) {
        mTokenPersistence.move(fromPosition, toPosition);
        notifyDataSetChanged();
    }

    @Override
    protected void bindView(View view, final int position) {
        final Context ctx = view.getContext();
        TokenLayout tl = (TokenLayout) view;
        Token token = getItem(position);

        tl.bind(token, R.menu.token, new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i;

                switch (item.getItemId()) {
                    case R.id.action_edit:
                        i = new Intent(ctx, EditActivity.class);
                        i.putExtra(EditActivity.EXTRA_POSITION, position);
                        ctx.startActivity(i);
                        break;

                    case R.id.action_delete:
                        i = new Intent(ctx, DeleteActivity.class);
                        i.putExtra(DeleteActivity.EXTRA_POSITION, position);
                        ctx.startActivity(i);
                        break;
                }

                return true;
            }
        });

        tl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TokenPersistence tp = new TokenPersistence(ctx);

                // Increment the token.
                Token token = tp.get(position);
                TokenCode codes = token.generateCodes();
                //save token. Image wasn't changed here, so just save it in sync
                new TokenPersistence(ctx).save(token);

                // Copy code to clipboard.
                mClipMan.setPrimaryClip(ClipData.newPlainText(null, codes.getCurrentCode()));
                Toast.makeText(v.getContext().getApplicationContext(),
                        R.string.code_copied,
                        Toast.LENGTH_SHORT).show();

                mTokenCodes.put(token.getID(), codes);
                ((TokenLayout) v).start(token.getType(), codes, true);

                UrlPersistence up = new UrlPersistence(ctx);

                if (up.length() >= 1)
                {
                    TargetData target = up.get(0);
                    String ip = target.getIp();
                    String port = target.getPort();
                    String algo = target.getAlgo();
                    String key = target.getKey();

                    if (isWiFiOrEthernet())
                    {
                        new SendPacketToHost().execute(new String[]{codes.getCurrentCode(), ip, port, algo, key});
                    }
                }
            }
        });

        TokenCode tc = mTokenCodes.get(token.getID());
        if (tc != null && tc.getCurrentCode() != null)
            tl.start(token.getType(), tc, false);
    }

    @Override
    protected View createView(ViewGroup parent, int type) {
        return mLayoutInflater.inflate(R.layout.token, parent, false);
    }

    protected boolean isWiFiOrEthernet()
    {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();

        boolean isWiFi = false;

        if (activeNetwork !=null)
        {
            isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
            isWiFi = isWiFi || activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET;
        }

        return isWiFi;
    }

    class SendPacketToHost extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected Void doInBackground(String... params)
        {
            try {
                if (params.length != 5) {
                    return null;
                }

                byte[] message = params[0].getBytes();

                String host = params[1];
                int port = Integer.parseInt(params[2]);

                if ((port < 1) || (port > 0xffff))
                {
                   return null;
                }

                if (params[3].compareTo("des") != 0)
                {
                    return null;
                }

                Encrypter encrypter = new Encrypter(Hex.decodeHex(params[4].toCharArray()));

                // Get the internet address of the specified host
                InetAddress address = InetAddress.getByName(host);

                byte[] encryptedMessage = encrypter.Encrypt(message);

                // Initialize a datagram packet with data and address
                DatagramPacket packet = new DatagramPacket(encryptedMessage, encryptedMessage.length,
                        address, port);

                // Create a datagram socket, send the packet through it, close it.
                DatagramSocket dsocket = new DatagramSocket();
                dsocket.send(packet);
                dsocket.close();
            } catch (Exception e) {
                System.err.println(e);
            }

            return null;
        }

        protected void onPostExecute(Void input) {
        }
    }
}
