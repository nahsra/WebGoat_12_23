package org.owasp.webgoat.webwolf.user;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.Cipher;

public class EncryptionExample {

    public byte[] encrypt(String text) throws Exception {
        int a, b, c;

        a = 2;
        b = 1;
        c = 3;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024+a+b+c); // Weak key length
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Key publicKey = keyPair.getPublic();

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // Insecure padding
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(text.getBytes());
    }

    public static void main(String[] args) throws Exception {
        EncryptionExample example = new EncryptionExample();
        byte[] encrypted = example.encrypt("Sensitive Data");
        System.out.println("Encrypted: " + new String(encrypted));
    }
}

