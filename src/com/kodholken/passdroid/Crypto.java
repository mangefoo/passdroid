/*    
    This file is part of the Passdroid password management software.
    
    Copyright (C) 2009-2012  Magnus Eriksson <eriksson.mag@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.kodholken.passdroid;

import java.util.Random;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    /**
     * Calculate SHA256 hash from a password string.
     * 
     * @param password password to calculate hash from.
     * @return byte array representing the hash or null if there was an error.
     */
    public static byte [] hmacFromPassword(String password) {
        byte [] key = null;

        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init (new SecretKeySpec ("notverysecretiv".getBytes ("UTF-8"),
                                          "RAW"));
            hmac.update(password.getBytes("UTF-8"));
            key = hmac.doFinal();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return key;
    }

    /**
     * Verify password against CRC value after transformation.
     * 
     * @param password password to verify.
     * @param dbString string to use as verfication after transformation.
     * 
     * @return true if the password is verified. False if not.
     */
    public static boolean verifyPassword(String password, String dbString) {
        byte [] xor;

        try {
            xor = Base64.decode(dbString);
        } catch (Base64Exception ex) {
            ex.printStackTrace();
            return false;
        }

        byte [] hmac = hmacFromPassword(password);
        byte [] key = new byte[xor.length];

        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (xor[i] ^ hmac[i]);
        }

        CRC32 crc = new CRC32();
        crc.update(key, 0, 28);
        long crcValue = crc.getValue();

        return ((key[28] == (byte) ((crcValue >> 24) & 0xff)) &&
                (key[29] == (byte) ((crcValue >> 16) & 0xff)) &&
                (key[30] == (byte) ((crcValue >> 8) & 0xff)) &&
                (key[31] == (byte) (crcValue & 0xff)));
    }

    /**
     * Method to verify that the AES implementation is indeed using the 256 bit
     * key version when given a 256 bit key. The reference key and cipher text
     * is taken from the AES test vectors available at
     * http://csrc.nist.gov/groups/STM/cavp/documents/aes/KAT_AES.zip
     * (last one in CBCVarKey256.rsp):
     * 
     * COUNT = 255
     * KEY = ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
     * IV = 00000000000000000000000000000000
     * CIPHERTEXT = 4bf85f1b5d54adbc307b0a048389adcb
     * PLAINTEXT = 00000000000000000000000000000000 
     * 
     * @return true if 256 bit AES is implemented. Else false.
     */
    public static boolean verifyAes256Implementation() {
        final byte [] key = {
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };

        final byte [] cipher = {
                (byte) 0x4b, (byte) 0xf8, (byte) 0x5f, (byte) 0x1b,
                (byte) 0x5d, (byte) 0x54, (byte) 0xad, (byte) 0xbc,
                (byte) 0x30, (byte) 0x7b, (byte) 0x0a, (byte) 0x04,
                (byte) 0x83, (byte) 0x89, (byte) 0xad, (byte) 0xcb };
        
        final byte [] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        final byte [] plain = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher c = Cipher.getInstance("AES/CBC/NoPadding", "BC");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            c.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            byte[] res = c.doFinal(plain);

            if (res.length != cipher.length) {
                return false;
            }

            for (int i = 0; i < res.length; i++) {
                if (res[i] != cipher[i]) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }
    
    /**
     * Encrypt a string using AES CBC mode from the Bouncy Castle crypto
     * provider. The result is PKCS7 padded.
     * 
     * @param key    AES key to use. 16, 24 or 32 bytes for 128, 192 or 256 bit
     *               encryption.
     * @param clear Clear text string to encrypt.
     * @return      PKCS7 padded byte array with the encrypted data. null on 
     *              error.
     */
    public static byte[] encryptAesCbc(byte[] key, String clear) {
        byte[] encrypted;
        byte[] salt = new byte[2];

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            Random rnd = new Random();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            rnd.nextBytes(salt);
            cipher.update(salt);
            encrypted = cipher.doFinal(clear.getBytes());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return encrypted;
    }
    
    /**
     * Encrypt and base64 encode a string with AES in CBC mode.
     * @see #encryptAesCbc(byte[], String).
     * 
     * @param key   AES key.
     * @param clear Clear text to encrypt.
     * @return Base64 encoded enrypted string. null on error.
     */
    public static String encryptAesCbcAsBase64(byte [] key, String clear) {
        byte[] encrypted = encryptAesCbc(key, clear);
        
        if (encrypted == null) {
            return null;
        }

        return Base64.encode(encrypted);
    }

    /**
     * Decrypt PKCS7 padded data using AES in CBC mode.
     * 
     * @param key       AES key.
     * @param encrypted encrypted data as byte array.
     * @return Decrypted data as a byte array or null if there was an error.
     */
    public static byte[] decryptAesCbc(byte[] key, byte[] encrypted) {
        byte[] decBytes;

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            decBytes = cipher.doFinal(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return decBytes;
    }

    /**
     * Decrypt PKCS7 padded data using AES in CBC mode.
     * 
     * @param key       AES key.
     * @param encrypted encrypted data as string.
     * @return Decrypted data as a byte array or null if there was an error.
     */
    public static byte[] decryptAesCbc(byte[] key, String encrypted) {
        try {
            return decryptAesCbc(key, Base64.decode(encrypted));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Decrypt PKCS7 encoded data using AES in CBC mode and convert the data
     * to an UTF8 encoded string. @see decryptAesCbc(byte [], encrypted).
     * 
     * @param key  AES key.
     * @param encrypted 
     * @return
     */
    public static String decryptAesCbcToUtf8(byte[] key, String encrypted) {
        
        try {
            byte[] decrypted = decryptAesCbc(key, encrypted);
            return new String(decrypted, 2, decrypted.length - 2, "UTF8");
        } catch(Exception ex) {}
        
        return null;
    }
}
