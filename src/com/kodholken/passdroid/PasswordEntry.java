/*    
    This file is part of the Passdroid password management software.
    
    Copyright (C) 2009-2010  Magnus Eriksson <eriksson.mag@gmail.com>

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
	
	private String decSystem;
	private String decUsername;
	private String decPassword;
	
	public void decryptAll(byte [] key) {
		decSystem   = decrypt(key, encSystem);
		decUsername = decrypt(key, encUsername);
		decPassword = decrypt(key, encPassword);
	}
	
	public void encryptAll(byte [] key) {
		encSystem   = encrypt(key, decSystem);
		encUsername = encrypt(key, decUsername);
		encPassword = encrypt(key, decPassword);
	}
	
	public void convertEcbToCbc(byte [] key) {
		encSystem   = ecbToCbc(key, encSystem);
		encUsername = ecbToCbc(key, encUsername);
		encPassword = ecbToCbc(key, encPassword);
	}
	
	private String ecbToCbc(byte [] key, String encrypted) {
		String decrypted = decryptEcb(key, encrypted);
		return encrypt(key, decrypted);
	}
	
	private String encrypt(byte [] key, String clear) {
		byte [] encrypted;
		byte [] salt = new byte[2];
		
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		try {
			Random rnd = new Random();
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
			byte [] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
			rnd.nextBytes(salt);
			cipher.update(salt);
			encrypted = cipher.doFinal(clear.getBytes());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		
		return Base64.encode(encrypted);
	}
	
	private String decrypt(byte [] key, String encrypted) {
		String decrypted;
		byte [] decBytes;
		
		try {
			byte [] encBytes = Base64.decode(encrypted);

			SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");  
			//Cipher cipher = Cipher.getInstance("AES");
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

	@Override
	public int compareTo(PasswordEntry another) {
		if (getDecSystem().equalsIgnoreCase(another.getDecSystem())) {
			return this.getDecUsername().compareTo(another.getDecUsername());
		}
		
		return getDecSystem().toLowerCase().compareTo(another.getDecSystem().toLowerCase());
	}
}
