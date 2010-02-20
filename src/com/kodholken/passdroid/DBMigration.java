package com.kodholken.passdroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class DBMigration {
	public static void migrate(String oldVersion, String newVersion) {
		
	}
	
	public static boolean changePassword(Context context, String oldPassword, String newPassword) {
		SQLiteDatabase db = null;
		boolean result = false;
		
		byte [] oldKey = Crypto.hmacFromPassword(oldPassword);
		byte [] newKey = Crypto.hmacFromPassword(newPassword);
		
		try {
			final String [] columns = { "id", "system", "username", "password" };
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
