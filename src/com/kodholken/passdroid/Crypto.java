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

import java.util.zip.CRC32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
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
}
