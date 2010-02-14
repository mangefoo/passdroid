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

import com.kodholken.passdroid.R;

import android.app.Activity;
import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Add extends Activity {
	Button addButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add);
		
		addButton = (Button) this.findViewById(R.id.add_button);
		
		addButton.setOnClickListener(new OnClickListener() {
		  @Override
		  public void onClick(View v) {
			  addPassword();
			  System.out.println("Clicked");
		  }
		});
	}
	
	private void addPassword() {
		long id = -1;
		EditText v;
		
		if (!Session.getInstance().isLoggedIn()) {
			Utils.alertDialog(getParent(), "Failure", "Adding new entry failed due to session timeout.");
			return;
		}
		
		ContentValues values = new ContentValues();
		PasswordEntry entry = new PasswordEntry();
		
		v = (EditText) this.findViewById(R.id.system);
		entry.setDecSystem(v.getText().toString());
		v = (EditText) this.findViewById(R.id.username);
		entry.setDecUsername(v.getText().toString());
		v = (EditText) this.findViewById(R.id.password);
		entry.setDecPassword(v.getText().toString());

		entry.encryptAll(Session.getInstance().getKey());
		
		values.put("system",   entry.getEncSystem());	
		values.put("username", entry.getEncUsername());
		values.put("password", entry.getEncPassword());
		
		PasswordData passwordData = new PasswordData(this);
		SQLiteDatabase db = passwordData.getWritableDatabase();
		
		try {
			id = db.insertOrThrow("data", null, values);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		db.close();
		
		if (id == -1) {
			Utils.alertDialog(this, "Failure", "Adding new entry failed.");
		} else {
			Session.getInstance().setNeedReload(true);
			finish();
		}
	}
}
