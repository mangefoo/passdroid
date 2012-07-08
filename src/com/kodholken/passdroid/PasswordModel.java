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

import java.util.ArrayList;
import java.util.Arrays;

import com.kodholken.passdroid.db.PasswordData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class PasswordModel {
    private static PasswordModel singleton = null;

    private boolean loaded;
    private PasswordData passwordData;
    private PasswordEntry [] passwords;
    private ArrayList<PasswordModelListener> listeners;

    public PasswordModel(Context context) {
        passwordData = new PasswordData(context.getApplicationContext());
        passwordData.verifyTable(); // Make sure data table exist
        loaded = false;
        listeners = new ArrayList<PasswordModelListener>();
    }

    public static PasswordModel getInstance(Context context) {
        if (singleton == null) {
            singleton = new PasswordModel(context);
        }

        return singleton;
    }

    public PasswordEntry[] getPasswords() {
        load();
        return passwords;
    }

    public PasswordEntry getAt(int index) {
        load();
        return passwords[index];
    }

    public boolean setPasswords(PasswordEntry [] passwords) {
        boolean res = false;
        SQLiteDatabase db = passwordData.getWritableDatabase();

        db.beginTransaction();
        try {
            SQLiteStatement stmt = db.compileStatement("DELETE FROM data");
            stmt.execute();
            for (PasswordEntry entry : passwords) {
                ContentValues values = new ContentValues();

                entry.encryptAll(Session.getInstance().getKey());
                values.put("system",   entry.getEncSystem());
                values.put("username", entry.getEncUsername());
                values.put("password", entry.getEncPassword());

                long id = db.insertOrThrow("data", null, values);
                entry.setId(id);
            }
            db.setTransactionSuccessful();
            this.passwords = passwords;
            res = true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();

        if (res) {
            notifyListeners();
        }

        return res;
    }

    public void addListener(PasswordModelListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onPasswordModelChange(this);
        }
    }

    private final void load() {
        if (!loaded || Session.getInstance().needReload()) {
            Session.getInstance().setNeedReload(false);
            loadPasswords();
            loaded = true;
            notifyListeners();
        }
    }

    private void loadPasswords() {
        final String [] columns = { "id", "system", "username", "password" };
        SQLiteDatabase db = passwordData.getReadableDatabase();
        Cursor cur = db.query("data", columns, null, null, null, null, "id DESC");

        int i = 0;
        passwords = new PasswordEntry[cur.getCount()];
        while (cur.moveToNext()) {
            passwords[i] = new PasswordEntry();
            passwords[i].setId(cur.getInt(0));
            passwords[i].setEncSystem(cur.getString(1));
            passwords[i].setEncUsername(cur.getString(2));
            passwords[i].setEncPassword(cur.getString(3));

            passwords[i].decryptAll(Session.getInstance().getKey());

            i++;
        }
        cur.close();
        db.close();

        Arrays.sort(passwords);
    }
}
