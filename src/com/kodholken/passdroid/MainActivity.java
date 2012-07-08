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

package com.kodholken.passdroid;

import com.kodholken.passdroid.db.DbMigration;
import com.kodholken.passdroid.db.SystemData;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Main activity of the application. This is started upon application startup
 * and will determine which parts to launch depending on the stored application
 * state.
 */
public class MainActivity extends Activity {	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
	}
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// Other activities use SessiongetInstance().setExitMain() to indicate
    	// whether we should exit the the application or spawn a new activity
    	// when control returns to this activity.
    	if (Session.getInstance().getExitMain()) {
    		Session.getInstance().setExitMain(false);
    		finish();
    		return;
    	}
    	
    	startup();
    }
    
    /*
     * This method launch different activities depending on the application
     * state. If the user is flagged as logged in we proceed to the password
     * list activity. If we detect that the application has been updated to a
     * newer version we perform migration steps. if we detect that this is the
     * first time the application is run we prompt the user for a master
     * password.
     */
    private void startup() {
    	String appVersion = Utils.getVersion(this);

    	if (Session.getInstance().isLoggedIn()) {
    		Utils.startPasswordActivity(this);
    		return;
    	}

    	SystemData system = new SystemData(this);
    	system.verifyTable();
    	
    	if (!system.hasVersion()) {
    		system.setVersion(appVersion);
    	} else {
        	String dbVersion = system.getVersion();

        	// Check for application update. If we detect an update we call
        	// the pre-login migration function. The post-login function is 
        	// called when the user has logged in.
        	// @see LoginActivity#handleVersionChange
        	if (!dbVersion.equals(appVersion)) {
        		DbMigration.preLoginMigration(this, dbVersion, appVersion);
        	}
    	}

    	// First time logins is detected by the absence of a master password
    	if (system.hasKey()) {
        	Intent i = new Intent(this, LoginActivity.class);
        	startActivity(i);
    	} else {
        	Intent i = new Intent(this, InitializeActivity.class);
        	startActivity(i);
    	}
    }
}