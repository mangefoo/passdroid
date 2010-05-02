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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class ShowPassword extends Activity {
	public static final int OPTION_MENU_EDIT     = 0;
	public static final int OPTION_MENU_DELETE   = 1;
	public static final int OPTION_MENU_SETTINGS = 2;
	public static final int OPTION_MENU_ABOUT    = 3;
	public static final int OPTION_MENU_LOGOUT   = 4;
	
	private long passwordId;
	private String system;
	private String username;
	private String password;
	
	private Button editButton;
	private Button deleteButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Bundle extras = getIntent().getExtras();
		
		passwordId = extras.getLong("id");
		system = extras.getString("system");
		username = extras.getString("username");
		password = extras.getString("password");

		this.setContentView(R.layout.show);

		((TextView) findViewById(R.id.system)).setText(system);
		TextView usernameView = (TextView) findViewById(R.id.username);
		TextView usernameHeaderView = (TextView) findViewById(R.id.username_header);
		if (username.length() > 0) {
			usernameView.setText(username);
		} else {
			usernameView.setVisibility(View.GONE);
			usernameHeaderView.setVisibility(View.GONE);
		}
		((TextView) findViewById(R.id.password)).setText(password);
		
		editButton = (Button) this.findViewById(R.id.edit_button);
		editButton.setOnClickListener(new OnClickListener() {
		  @Override
		  public void onClick(View v) {
			  Utils.debug("Edit clicked");
			  editPassword();
		  }
		});
		
		deleteButton = (Button) this.findViewById(R.id.delete_button);
		deleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Utils.debug("Delete clicked");
				confirmDelete();
			}
		});
	}
	
	private void editPassword() {
		Intent i = new Intent(this, Edit.class);
		i.putExtra("id", passwordId);
		i.putExtra("system", system);
		i.putExtra("username", username);
		i.putExtra("password", password);
		
		startActivity(i);
		finish();
	}
	
	private void confirmDelete() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Confirm delete");
		alertDialog.setMessage("Are your sure you want to delete " + system + "?");
		
		alertDialog.setButton(AlertDialog.BUTTON1, "Yes", new DialogInterface.OnClickListener() { 
			public void onClick(DialogInterface dialog, int which) {
				Utils.debug("User choosed yes");
				deletePassword();
				Session.getInstance().setNeedReload(true);
			}
		});
		
		alertDialog.setButton(AlertDialog.BUTTON2, "No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Utils.debug("User choosed no");
			}
		});
		
		alertDialog.show();
	}
	
	private void deletePassword() {
		PasswordData passwordData = new PasswordData(this);
		SQLiteDatabase db = passwordData.getWritableDatabase();
		
		try {
			db.delete("data", "id=" + passwordId, null);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		db.close();
		
		finish();
	}
}
