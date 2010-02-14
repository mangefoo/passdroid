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
import android.widget.TextView;

public class Edit extends Activity {
	private Button saveButton;
	private Button cancelButton;
	
	private long passwordId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit);
		
		Bundle extras = getIntent().getExtras();

		passwordId = extras.getLong("id");
		
		((TextView) findViewById(R.id.system)).setText(extras.getString("system"));
		((TextView) findViewById(R.id.username)).setText(extras.getString("username"));
		((TextView) findViewById(R.id.password)).setText(extras.getString("password"));		

		saveButton = (Button) this.findViewById(R.id.save_button);
		saveButton.setOnClickListener(new OnClickListener() {
		  @Override
		  public void onClick(View v) {
			  updatePassword();
			  Session.getInstance().setNeedReload(true);
			  Utils.debug("Save Clicked");
		  }
		});
		
		cancelButton = (Button) this.findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void updatePassword() {
		String system   = ((TextView) findViewById(R.id.system)).getText().toString();
		String username = ((TextView) findViewById(R.id.username)).getText().toString();
		String password = ((TextView) findViewById(R.id.password)).getText().toString();
		
		ContentValues values = new ContentValues();
		
		PasswordEntry entry = new PasswordEntry();
		entry.setDecSystem(system);
		entry.setDecUsername(username);
		entry.setDecPassword(password);
		entry.encryptAll(Session.getInstance().getKey());
		
		values.put("system",   entry.getEncSystem());	
		values.put("username", entry.getEncUsername());
		values.put("password", entry.getEncPassword());
		
		PasswordData passwordData = new PasswordData(this);
		SQLiteDatabase db = passwordData.getWritableDatabase();
		
		try {
			db.update("data", values, "id=" + passwordId, null);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		db.close();
		
		finish();
	}
}
