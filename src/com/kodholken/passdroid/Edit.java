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
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Edit extends Activity {
	private Button saveButton;
	private Button cancelButton;
	private Button generateButton;
	private EditText password;
	
	private Intent generateIntent;
	private long passwordId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.edit);
		
		TextView title = (TextView) this.findViewById(R.id.header);
		title.setText(R.string.edit_title);
		
		Bundle extras = getIntent().getExtras();

		passwordId = extras.getLong("id");
		
		((TextView) findViewById(R.id.system)).setText(extras.getString("system"));
		((TextView) findViewById(R.id.username)).setText(extras.getString("username"));
		
		password = (EditText) findViewById(R.id.password);
		password.setText(extras.getString("password"));

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
		
		setupGenerateButton();
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
	
	private void setupGenerateButton() {
		generateButton = (Button) findViewById(R.id.generate_button);
		generateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				createGenerateActivity();
			}
		});
	}
	
	private void createGenerateActivity() {
		generateIntent = new Intent(this, GeneratePasswordActivity.class);
		startActivityForResult(generateIntent, 0x42);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 1) {
			this.password.setText(GeneratePasswordActivity.getGeneratedPassword());
		}
	}
}
