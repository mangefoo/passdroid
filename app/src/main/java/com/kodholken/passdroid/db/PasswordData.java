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

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PasswordData extends SQLiteOpenHelper {	
    public PasswordData(Context ctx) {
        super(ctx, Constants.DBNAME, null, Constants.DBVERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE data (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   "system TEXT NOT NULL, username TEXT, password TEXT, note TEXT, url TEXT)");
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
            db.rawQuery("SELECT * FROM data", null);
        } catch (SQLException ex) {
            onCreate(db);
        }
        db.close();
    }
}
