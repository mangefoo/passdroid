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
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Activity {
	private Button loginButton;
	private boolean hasBackKeyDown;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Session.getInstance().isLoggedIn()) {
			finish();
		}

		setContentView(R.layout.login);

		loginButton = (Button) this.findViewById(R.id.login_button);
		
		loginButton.setOnClickListener(new OnClickListener() {
		  @Override
		  public void onClick(View v) {
			  handleLogin();
		  }
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (Session.getInstance().isLoggedIn()) {
			finish();
		}
		
		((EditText) this.findViewById(R.id.login_password)).setText("");
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (hasBackKeyDown) {
				Session.getInstance().setExitMain(true);
				hasBackKeyDown = false;
				finish();
				return true;
			}
			hasBackKeyDown = false;
		}
		
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			hasBackKeyDown = true;
		}
		
		return false;
	}
	
	private void handleLogin() {
		EditText t = (EditText) this.findViewById(R.id.login_password);
		if (t != null) {
			String password = t.getText().toString();
			if (verifyPassword(password)) {
				Session.getInstance().setKey(Crypto.hmacFromPassword(password));
				Session.getInstance().setLoggedIn();
				
				handleVersionChange();
				Utils.startPasswordsView(this);
			} else {
				Utils.alertDialog(this, "Login Failure", "The supplied password was incorrect.");
				((EditText) findViewById(R.id.login_password)).selectAll();
			}
		}	
	}
	
	private void handleVersionChange() {
    	SystemData system = new SystemData(this);
		String dbVersion = system.getVersion();
    	String appVersion = Utils.getVersion(this);
    	
    	if (!dbVersion.equals(appVersion)) {
    		if (DBMigration.postLoginMigration(this, dbVersion, appVersion)) {
    			Utils.debug("Setting version to " + appVersion);
    			system.updateVersion(appVersion);
    		}
    	}
	}
		
	private boolean verifyPassword(String password) {
		SystemData systemData = new SystemData(this);
		return Crypto.verifyPassword(password, systemData.getKey());
	}
}
