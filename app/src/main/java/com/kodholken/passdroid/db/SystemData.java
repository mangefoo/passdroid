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

import com.kodholken.passdroid.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SystemData extends SQLiteOpenHelper {

    public SystemData(Context ctx) {
        super(ctx, Constants.DBNAME, null, Constants.DBVERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE system (id INTEGER PRIMARY KEY AUTOINCREMENT, attribute TEXT NOT NULL, value TEXT)");
    }

    /**
     * Do not perform any operations in this method except
     * DbMigration.handleDatabaseUpgrade(). @see DbMigration.handleDatabaseUpgrade()
     * for explanation.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DbMigration.handleDatabaseUpgrade(db, oldVersion, newVersion);
    }

    public void verifyTable() {
        SQLiteDatabase db = getReadableDatabase();
        try {
            db.rawQuery("SELECT * FROM system", null);
        } catch (SQLException ex) {
            onCreate(db);
        }
        db.close();
    }

    public boolean hasAttribute(String attr) {
        boolean result = false;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT count(*) FROM system WHERE attribute = ?", new String[] { attr });
        if (cur.moveToNext()) {
            result = cur.getInt(0) > 0;
        }
        cur.close();
        db.close();

        return result;
    }

    public boolean hasKey() {
        return hasAttribute("key");
    }

    public boolean hasVersion() {
        return hasAttribute("version");
    }

    public String getAttribute(String attr) {
        String result = null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT value FROM system WHERE attribute=?", new String[] { attr });
        if (cur.moveToNext()) {
            result = cur.getString(0);
        }
        cur.close();
        db.close();

        return result;
    }

    public String getKey() {
        return getAttribute("key");
    }

    public String getVersion() {
        return getAttribute("version");
    }

    public void setAttribute(String attr, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("attribute", attr);
        values.put("value", value);
        db.insertOrThrow("system", null, values);
        db.close();
    }

    public void updateAttribute(String attr, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("value", value);
        String [] whereArgs = { attr };
        db.update("system", values, "attribute=?", whereArgs);
        db.close();		
    }

    public void setKey(String value) {
        setAttribute("key", value);
    }

    public void setVersion(String value) {
        setAttribute("version", value);	
    }

    public void updateVersion(String value) {
        updateAttribute("version", value);
    }
}
