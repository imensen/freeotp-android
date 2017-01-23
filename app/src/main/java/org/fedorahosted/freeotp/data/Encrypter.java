package org.fedorahosted.freeotp.data;

import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class Encrypter
{
    private byte[] key;

    private Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding"); // cipher is not thread safe

    public Encrypter(byte[] key) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException {
        this.key = key;

        DESKeySpec keySpec = new DESKeySpec(this.key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey specifiedKey = keyFactory.generateSecret(keySpec);

        this.cipher.init(Cipher.ENCRYPT_MODE, specifiedKey);
    }

    public byte[] Encrypt(byte[] message) throws BadPaddingException, IllegalBlockSizeException {
        byte[] encryptedMessage = this.cipher.doFinal(message);
        return encryptedMessage;
    }
}
