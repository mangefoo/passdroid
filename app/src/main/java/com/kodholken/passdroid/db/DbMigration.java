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

package com.kodholken.passdroid.db;

import com.kodholken.passdroid.Crypto;
import com.kodholken.passdroid.PasswordEntry;
import com.kodholken.passdroid.Session;
import com.kodholken.passdroid.Utils;
import com.kodholken.passdroid.Version;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class DbMigration {
    public static boolean preLoginMigration(Context context, String oldVersion, String newVersion) {
        return true;
    }

    public static boolean postLoginMigration(Context context, String oldVersion, String newVersion) {
        boolean result = false;

        Version _oldVersion = Version.parse(oldVersion);
        Version _newVersion = Version.parse(newVersion);
        Version _2_0_Version = new Version(2, 0);

        /*
         * Version 0.95 changed the AES mode from ECB to CBC so we
         * convert the database entries from version 0.9
         */
        if (oldVersion.equals("0.9") && newVersion.equals("0.95")) {
            Utils.debug("Version 0.9 to 0.95 conversion: ECB -> CBC");
            result = ecb2cbcConversion(context);
            if (result) {
                Utils.debug("Database converted successfully");
            } else {
                Utils.error("Database conversion failed");
            }
        } else if ((_oldVersion.compareTo(_2_0_Version) < 0) &&
                   (_newVersion.compareTo(_2_0_Version)) >= 0) {
            result = zeroIvToIvConversion(context);
            if (result) {
                Utils.debug("Database successfully converted from zero IV to IV");
            } else {
                Utils.debug("Database convertion from zero IV to IV failed");
            }
        } else {
            result = true;
        }

        return result;
    }
    
    /*
     * Since we have two SQLiteOpenHelper derived classes (PasswordData and
     * SystemData) that work on the same database table (Constants.DBNAME) all
     * update processing needs to be done in a common function since we do not 
     * know which class will trigger the update callback. I.e. the onUpgrade()
     * implementations should call this, and only this, method.
     */
    public static void handleDatabaseUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Utils.debug("handleDatabaseUpgrade()");
        if (oldVersion == 1 && newVersion == 2) {
            Utils.debug("Adding table comlums 'note' and 'url' due to version " +
                        "upgrade from 1 to 2");
            db.execSQL("ALTER TABLE data ADD COLUMN note TEXT");
            db.execSQL("ALTER TABLE data ADD COLUMN url TEXT");
        }
    }

    private static boolean zeroIvToIvConversion(Context context) {
        boolean result = false;

        final String [] columns = { "id", "system", "username", "password" };
        PasswordData passwordData = new PasswordData(context);
        SQLiteDatabase db = passwordData.getReadableDatabase();
        try {
            db.beginTransaction();
            Cursor cur = db.query("data", columns, null, null, null, null,
                                  "id DESC");

            PasswordEntry password;
            ContentValues values = new ContentValues();
            while (cur.moveToNext()) {
                password = new PasswordEntry();
                password.setId(cur.getInt(0));
                password.setEncSystem(cur.getString(1));
                password.setEncUsername(cur.getString(2));
                password.setEncPassword(cur.getString(3));

                password.convertZeroIvToIv(Session.getInstance().getKey());

                values.put("system", password.getEncSystem());
                values.put("username", password.getEncUsername());
                values.put("password", password.getEncPassword());

                db.update("data", values, "id=" + password.getId(), null);
            }
            cur.close();
            db.setTransactionSuccessful();
            result = true;
        } catch (SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            db.endTransaction();
            db.close();
        }

        return result;
    }

    public static boolean ecb2cbcConversion(Context context) {
        boolean result = false;

        final String [] columns = { "id", "system", "username", "password" };
        PasswordData passwordData = new PasswordData(context);
        SQLiteDatabase db = passwordData.getReadableDatabase();
        try {
            db.beginTransaction();
            Cursor cur = db.query("data", columns, null, null, null, null,
            "id DESC");

            PasswordEntry password;
            ContentValues values = new ContentValues();
            while (cur.moveToNext()) {
                password = new PasswordEntry();
                password.setId(cur.getInt(0));
                password.setEncSystem(cur.getString(1));
                password.setEncUsername(cur.getString(2));
                password.setEncPassword(cur.getString(3));

                password.convertEcbToCbc(Session.getInstance().getKey());

                values.put("system", password.getEncSystem());
                values.put("username", password.getEncUsername());
                values.put("password", password.getEncPassword());

                db.update("data", values, "id=" + password.getId(), null);
            }
            cur.close();
            db.setTransactionSuccessful();
            result = true;
        } catch (SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            db.endTransaction();
            db.close();
        }

        return result;
    }

    public static boolean changePassword(Context context, String oldPassword, String newPassword) {
        SQLiteDatabase db = null;
        boolean result = false;

        byte [] oldKey = Crypto.hmacFromPassword(oldPassword);
        byte [] newKey = Crypto.hmacFromPassword(newPassword);

        try {
            final String [] columns = { "id", "system", "username", "password", "note", "url" };
            PasswordData passwordData = new PasswordData(context);

            db = passwordData.getReadableDatabase();
            db.beginTransaction();
            Cursor cur = db.query("data", columns, null, null, null, null, "id DESC");

            int i = 0;
            PasswordEntry [] passwords = new PasswordEntry[cur.getCount()];
            while (cur.moveToNext()) {
                passwords[i] = new PasswordEntry();
                passwords[i].setId(cur.getInt(0));
                passwords[i].setEncSystem(cur.getString(1));
                passwords[i].setEncUsername(cur.getString(2));
                passwords[i].setEncPassword(cur.getString(3));
                passwords[i].setEncNote(cur.getString(4));
                passwords[i].setEncUrl(cur.getString(5));

                passwords[i].decryptAll(oldKey);

                i++;
            }
            cur.close();

            ContentValues values = new ContentValues();
            for (i = 0; i < passwords.length; i++) {
                passwords[i].encryptAll(newKey);
                values.put("system", passwords[i].getEncSystem());
                values.put("username", passwords[i].getEncUsername());
                values.put("password", passwords[i].getEncPassword());
                values.put("note", passwords[i].getEncNote());
                values.put("url", passwords[i].getEncUrl());
                db.update("data", values, "id=" + passwords[i].getId(), null);
            }

            String dbKey = Utils.generateKey(newPassword);
            values.clear();
            values.put("value", dbKey);
            String [] attrs = { "key" };
            db.update("system", values, "attribute=?", attrs);

            db.setTransactionSuccessful();
            result = true;
        } catch (SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            db.endTransaction();
            db.close();
        }

        return result;
    }
}
