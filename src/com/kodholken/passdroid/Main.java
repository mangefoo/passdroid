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

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;

public class Main extends Activity {
	Button passwordButton;
	Button clearDbButton;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	if (Constants.CLEARDB) {
    		clearDatabase();
    		finish();
    	}
	}
    
    public void onResume() {
    	super.onResume();
    	
    	if (Session.getInstance().getExitMain()) {
    		Session.getInstance().setExitMain(false);
    		finish();
    		return;
    	}
    	
    	startup();
    }
    
    private void startup() {
    	String appVersion = Utils.getVersion(this);

    	if (Session.getInstance().isLoggedIn()) {
    		Utils.startPasswordsView(this);
    		return;
    	}

    	SystemData system = new SystemData(this);
    	system.verifyTable();
    	
    	if (!system.hasVersion()) {
    		system.setVersion(appVersion);
    	} else {
        	String dbVersion = system.getVersion();

        	if (!dbVersion.equals(appVersion)) {
        		DBMigration.migrate(dbVersion, appVersion);
        		// Upgrade database
        	}
    	}
 
    	if (system.hasKey()) {
    		startLogin();
    	} else {
    		startInitialize();
    	}
    }
    
    public void startPassword() {
		  Intent i = new Intent(this, Passwords.class);
		  startActivity(i);
    }
    
    public void startLogin() {
    	Intent i = new Intent(this, Login.class);
    	startActivity(i);
    }
    
    public void startInitialize() {
    	Intent i = new Intent(this, Initialize.class);
    	startActivity(i);
    }
    
    public void clearDatabase() {
    	PasswordData passwordData = new PasswordData(this);
		SQLiteDatabase db = passwordData.getReadableDatabase();
		passwordData.onUpgrade(db, 1, 1);
		db.close();
		SystemData systemData = new SystemData(this);
		db = systemData.getReadableDatabase();
		systemData.onUpgrade(db, 1, 1);
		db.close();
    }
}