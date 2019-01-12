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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PasswordEntry implements Comparable<PasswordEntry> {
    private long id;

    private String encSystem;
    private String encUsername;
    private String encPassword;
    private String encNote;
    private String encUrl;

    private String decSystem;
    private String decUsername;
    private String decPassword;
    private String decNote;
    private String decUrl;

    public void decryptAll(byte [] key) {
        decSystem   = decrypt(key, encSystem);
        decUsername = decrypt(key, encUsername);
        decPassword = decrypt(key, encPassword);
        if (encNote != null) {
            decNote = decrypt(key, encNote);
        } else {
            decNote = null;
        }
        
        if (encUrl != null) {
            decUrl = decrypt(key, encUrl);
        } else {
            decUrl = null;
        }
    }

    public void encryptAll(byte [] key) {
        encSystem   = encrypt(key, decSystem);
        encUsername = encrypt(key, decUsername);
        encPassword = encrypt(key, decPassword);
        
        if (decNote != null) {
            encNote = encrypt(key, decNote);
        } else {
            encNote = null;
        }
        
        if (decUrl != null) {
            encUrl      = encrypt(key, decUrl);
        } else {
            encUrl = null;
        }
    }

    /*
     * Since this method should only be used when converting really old
     * databases that does not have any note or URL columns we don't bother
     * converting these values.
     */
    public void convertEcbToCbc(byte [] key) {
        encSystem   = ecbToCbc(key, encSystem);
        encUsername = ecbToCbc(key, encUsername);
        encPassword = ecbToCbc(key, encPassword);
    }
    
    /*
     * This method is used to convert entries with zeroed out IVs and a salt
     * to entries with random IVs and no salt. The conversion takes place in
     * the same version where notes and URLs were introduced, hence we do not
     * need to convert the note and URL fields.
     */
    public void convertZeroIvToIv(byte [] key) {
        encSystem   = zeroIvToIv(key, encSystem);
        encUsername = zeroIvToIv(key, encUsername);
        encPassword = zeroIvToIv(key, encPassword);
    }

    private String ecbToCbc(byte [] key, String encrypted) {
        String decrypted = decryptEcb(key, encrypted);
        return encrypt(key, decrypted);
    }
    
    public String zeroIvToIv(byte [] key, String encrypted) {
        String decrypted = decryptZeroIv(key, encrypted);
        return encrypt(key, decrypted);
    }

    private String encrypt(byte [] key, String clear) {
        byte [] encrypted;
        byte [] complete;

        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            Random rnd = new Random();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");

            byte [] iv = new byte[16];
            rnd.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            encrypted = cipher.doFinal(clear.getBytes());
            
            complete = new byte[encrypted.length + iv.length];
            System.arraycopy(iv, 0, complete, 0, iv.length);
            System.arraycopy(encrypted, 0, complete, iv.length, encrypted.length);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return Base64.encode(complete);
    }

    private String decrypt(byte [] key, String encrypted) {
        String decrypted;
        byte [] decBytes;

        try {
            byte [] encBytes = Base64.decode(encrypted);

            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");  
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            
            byte [] iv = new byte[16];
            System.arraycopy(encBytes, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            decBytes = cipher.doFinal(encBytes, iv.length, encBytes.length - iv.length);
            decrypted = new String(decBytes, 0, decBytes.length, "UTF8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return decrypted;
    }

    private String decryptZeroIv(byte [] key, String encrypted) {
        String decrypted;
        byte [] decBytes;

        try {
            byte [] encBytes = Base64.decode(encrypted);

            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");  

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            byte [] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            decBytes = cipher.doFinal(encBytes);
            decrypted = new String(decBytes, 2, decBytes.length - 2, "UTF8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return decrypted;
    }

    private String decryptEcb(byte [] key, String encrypted) {
        String decrypted;
        byte [] decBytes;

        try {
            byte [] encBytes = Base64.decode(encrypted);

            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");  
            //Cipher cipher = Cipher.getInstance("AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC"); 
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);  
            decBytes = cipher.doFinal(encBytes);
            decrypted = new String(decBytes, 2, decBytes.length - 2, "UTF8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return decrypted;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getEncSystem() {
        return encSystem;
    }
    public void setEncSystem(String encSystem) {
        this.encSystem = encSystem;
    }
    public String getEncUsername() {
        return encUsername;
    }
    public void setEncUsername(String encUsername) {
        this.encUsername = encUsername;
    }
    public String getEncPassword() {
        return encPassword;
    }
    public void setEncPassword(String encPassword) {
        this.encPassword = encPassword;
    }
    public String getDecSystem() {
        return decSystem;
    }
    public void setDecSystem(String decSystem) {
        this.decSystem = decSystem;
    }
    public String getDecUsername() {
        return decUsername;
    }
    public void setDecUsername(String decUsername) {
        this.decUsername = decUsername;
    }
    public String getDecPassword() {
        return decPassword;
    }
    public void setDecPassword(String decPassword) {
        this.decPassword = decPassword;
    }
    public String getEncNote() {
        return encNote;
    }
    public void setEncNote(String encNote) {
        this.encNote = encNote;
    }
    public String getEncUrl() {
        return encUrl;
    }
    public void setEncUrl(String encUrl) {
        this.encUrl = encUrl;
    }
    public String getDecNote() {
        return decNote;
    }
    public void setDecNote(String decNote) {
        this.decNote = decNote;
    }
    public String getDecUrl() {
        return decUrl;
    }
    public void setDecUrl(String decUrl) {
        this.decUrl = decUrl;
    }

    @Override
    public int compareTo(PasswordEntry another) {
        if (getDecSystem().equalsIgnoreCase(another.getDecSystem())) {
            return this.getDecUsername().compareTo(another.getDecUsername());
        }

        return getDecSystem().toLowerCase().compareTo(another.getDecSystem().toLowerCase());
    }
}
