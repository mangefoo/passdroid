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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Utils {
    public static void alertDialog(Context context, String title, 
            String message) {
        AlertDialog alertDialog;

        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            } }); 
        alertDialog.show();
    }

    public static void startPasswordActivity(Context context) {
        Intent i = new Intent(context, PasswordListActivity.class);
        context.startActivity(i);
    }

    public static void logoutConfirmDialog(Context context) {
    }

    public static void debug(String message) {
        Log.d("Passdroid", message);
    }

    public static void error(String message) {
        Log.e("Passdroid", message);
    }

    public static void notice(String message) {
        Log.i("Passdroid", message);
    }

    public static String getVersion(Context context) throws RuntimeException {
        try {
            return context.getPackageManager()
            .getPackageInfo(context.getPackageName(), 0)
            .versionName;
        } catch(NameNotFoundException ex) {
            throw new RuntimeException("Failed to get app version");
        }
    }

    public static String generateKey(String masterPassword) {
        Random rnd = new Random();
        byte[] key = new byte[32];
        rnd.nextBytes(key);

        CRC32 crc = new CRC32();
        crc.update(key, 0, 28);
        long crcValue = crc.getValue();
        key[28] = (byte) ((crcValue >> 24) & 0xff);
        key[29] = (byte) ((crcValue >> 16) & 0xff);
        key[30] = (byte) ((crcValue >> 8) & 0xff);
        key[31] = (byte) (crcValue & 0xff);

        byte[] pwdHmac = Crypto.hmacFromPassword(masterPassword);

        assert (key.length != pwdHmac.length);

        byte[] xor = new byte[key.length];
        for (int i = 0; i < key.length; i++) {
            xor[i] = (byte) (key[i] ^ pwdHmac[i]);
        }

        return Base64.encode(xor);
    }

    public static String escapeXMLChars(String s) {
        return s.replaceAll("&",  "&amp;")
        .replaceAll("'",  "&apos;")
        .replaceAll("\"", "&quot;")
        .replaceAll("<",  "&lt;")
        .replaceAll(">",  "&gt;");
    }

    public static String unescapeXMLChars(String s) {
        return s.replaceAll("&amp;",  "&")
        .replaceAll("&apos;", "'")
        .replaceAll("&quot;", "\"")
        .replaceAll("&lt;",   "<")
        .replaceAll("&gt;",   ">");
    }
}
