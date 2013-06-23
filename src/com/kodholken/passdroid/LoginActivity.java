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

import com.kodholken.passdroid.R;
import com.kodholken.passdroid.db.DbMigration;
import com.kodholken.passdroid.db.SystemData;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Activity that displays the login screen and verifies the master password.
 */
public class LoginActivity extends Activity {
    private Button loginButton;
    private EditText passwordView;
    private boolean hasBackKeyDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If we are already logged in we are done. This will return control
        // to the MainActivity which is responsible for creating and launching
        // a PasswordActivity instance.
        if (Session.getInstance().isLoggedIn()) {
            finish();
        }

        setContentView(R.layout.login);

        passwordView = (EditText) findViewById(R.id.login_password);
        passwordView.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        handleLogin();
                        return true;
                    }
                }
                return false;
            }
        });

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
            return ;
        }

        ((EditText) this.findViewById(R.id.login_password)).setText("");
    }

    /**
     * Handles events when the user release the back button. This is used in
     * conjunction with onKeyDown() to make sure that we only react to back
     * events initialized in this view. 
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (hasBackKeyDown) {
                // A back key on the login menu should exit the application
                // so we need to indicate to MainActivity that it should exit
                // in onResume()
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

    /**
     * Handler for when the user press the login button after entering the
     * master password. If the correct password is entered we flag the user as
     * logged in and return control to the MainActivity which will launch the
     * 
     */
    private void handleLogin() {
        EditText t = (EditText) this.findViewById(R.id.login_password);
        if (t != null) {
            String password = t.getText().toString();
            if (verifyPassword(password)) {
                Session.getInstance().setKey(Crypto.hmacFromPassword(password));
                Session.getInstance().setLoggedIn();

                handleVersionChange();
                Utils.startPasswordActivity(this);
            } else {
                Utils.alertDialog(this, "Login Failure",
                "The supplied password was incorrect.");
                ((EditText) findViewById(R.id.login_password)).selectAll();
            }
        }	
    }

    /**
     * Handle the post-login version change processing. This is where the app
     * version will be updated in the database.
     */
    private void handleVersionChange() {
        SystemData system = new SystemData(this);
        String dbVersion = system.getVersion();
        String appVersion = Utils.getVersion(this);

        if (!dbVersion.equals(appVersion)) {
            if (DbMigration.postLoginMigration(this, dbVersion, appVersion)) {
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
