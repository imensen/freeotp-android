package org.fedorahosted.freeotp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.fedorahosted.freeotp.data.TargetData;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlPersistence {
    private static final String NAME  = "urls";
    private static final String ORDER = "urlOrder";
    private final SharedPreferences prefs;
    private final Gson gson;

    private List<TargetData> getUrlOrder() {
        Type type = new TypeToken<List<TargetData>>(){}.getType();
        String str = prefs.getString(ORDER, "[]");
        List<TargetData> order = gson.fromJson(str, type);
        return order == null ? new LinkedList<TargetData>() : order;
    }

    private SharedPreferences.Editor setTokenOrder(List<TargetData> order) {
        return prefs.edit().putString(ORDER, gson.toJson(order));
    }

    private static final Pattern IPV4_TARGET
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])):([0-9]+)\\?algo=([^&]+)&key=([^&]+)");

    public static boolean addWithToast(Context ctx, String uri) {
        if (uri.startsWith("myotp://"))
        {
            Toast.makeText(ctx, "freeotp server recognized", Toast.LENGTH_SHORT).show();
            try {
                String ip = uri.substring(8);

                Matcher matcher = IPV4_TARGET.matcher(ip);
                if (matcher.find())
                {
                    new UrlPersistence(ctx).add(new TargetData(matcher.group(1), matcher.group(6), matcher.group(7), matcher.group(8)));
                    Toast.makeText(ctx, "Key recognized", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(ctx, "Bad IP", Toast.LENGTH_SHORT).show();
                    return true;
                }
            } catch (UriInvalidException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        return false;
    }

    public UrlPersistence(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public int length() {
        return getUrlOrder().size();
    }

    public TargetData get(int position) {
        TargetData target = getUrlOrder().get(position);
        String key = target.getIp();
        String str = prefs.getString(key, null);

        try {
            return gson.fromJson(str, TargetData.class);
        } catch (JsonSyntaxException jse) {
            return null;
        }
    }

    public void add(TargetData token) throws UriInvalidException {
        String ip = token.getIp();

        List<TargetData> order = getUrlOrder();
        order.clear();
        setTokenOrder(order).clear().apply();

        order.add(0, token);
        setTokenOrder(order).putString(ip, gson.toJson(token)).apply();
    }

    public void move(int fromPosition, int toPosition) {
        if (fromPosition == toPosition)
            return;

        List<TargetData> order = getUrlOrder();
        if (fromPosition < 0 || fromPosition > order.size())
            return;
        if (toPosition < 0 || toPosition > order.size())
            return;

        order.add(toPosition, order.remove(fromPosition));
        setTokenOrder(order).apply();
    }

    public void delete(int position) {
        List<TargetData> order = getUrlOrder();
        String key = order.remove(position).getIp();
        setTokenOrder(order).remove(key).apply();
    }

    public void save(String token) {
        prefs.edit().putString(token, gson.toJson(token)).apply();
    }

    private class UriInvalidException extends Exception {
    }
}
