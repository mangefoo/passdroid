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
import com.kodholken.passdroid.db.SystemData;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class InitializeActivity extends Activity {
    Button okButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initialize);

        okButton = (Button) this.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOK();
            }
        });
    }

    private void handleOK() {
        String p1;
        EditText p2;


        p1 = ((EditText) findViewById(R.id.master_password_1)).getText().toString();
        p2 = (EditText) findViewById(R.id.master_password_2);

        if (p1.equals(p2.getText().toString())) {
            initialize(p1);
            Session.getInstance().setKey(Crypto.hmacFromPassword(p1));
            Session.getInstance().setLoggedIn();
            Utils.startPasswordActivity(this);
            finish();
        } else {
            Utils.alertDialog(this, getString(R.string.initialize_mismatch_title), getString(R.string.initialize_mismatch_text));
        }
    }

    private void initialize(String masterPassword) {
        String xorString = Utils.generateKey(masterPassword);

        SystemData system = new SystemData(this);
        system.setAttribute("key", xorString);
    }
}
