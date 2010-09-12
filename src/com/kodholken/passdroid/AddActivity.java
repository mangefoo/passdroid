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

/**
 * Activity for adding new passwords.
 */
public class AddActivity extends Activity {
	private Button addButton;
	private Button generateButton;
	private TextView title;
	private EditText password;
	private Intent generateIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.add);

		password = (EditText) findViewById(R.id.password);
		title = (TextView) findViewById(R.id.header);
		title.setText(R.string.add_title);
		
		addButton = (Button) this.findViewById(R.id.add_button);
		
		addButton.setOnClickListener(new OnClickListener() {
		  @Override
		  public void onClick(View v) {
			  addPassword();
		  }
		});
		
		setupGenerateButton();
	}
	
	private void addPassword() {
		long id = -1;
		EditText v;
		
		if (!Session.getInstance().isLoggedIn()) {
			Utils.alertDialog(getParent(), "Failure",
					"Adding new entry failed due to session timeout.");
			return;
		}
		
		ContentValues values = new ContentValues();
		PasswordEntry entry = new PasswordEntry();
		
		v = (EditText) this.findViewById(R.id.system);
		// We require that the user has entered a valid system name
		if (v.getText().toString().matches("^\\s*$")) {
			Utils.alertDialog(this, "Empty system name",
					 "Please enter a system name before adding the password.");
			return ;
		}
		
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
	protected void onActivityResult(int requestCode, int resultCode,
			                        Intent data) {
		if (resultCode == 1) {
			this.password.setText(GeneratePasswordActivity.
					              getGeneratedPassword());
		}
	}
}
