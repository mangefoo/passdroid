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

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kodholken.passdroid.R;
import com.kodholken.passdroid.db.PasswordData;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Activity that displays a password entry and let the user choose to edit or
 * delete the entry.
 */
public class ShowActivity extends SherlockActivity {	
    private long passwordId;
    private String system;
    private String username;
    private String password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        passwordId = extras.getLong("id");
        system = extras.getString("system");
        username = extras.getString("username");
        password = extras.getString("password");

        this.setContentView(R.layout.show);

        TextView usernameView = (TextView) findViewById(R.id.username);
        TextView usernameHeaderView = (TextView) findViewById(R.id.username);

        // Do not show the username entry if it does not exist
        if (username.length() > 0) {
            usernameView.setText(username);
        } else {
            usernameView.setVisibility(View.GONE);
            usernameHeaderView.setVisibility(View.GONE);
        }
        ((TextView) findViewById(R.id.password)).setText(password);

        setupClipboardAction();
        
        getSupportActionBar().setTitle(system);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Edit")
            .setIcon(android.R.drawable.ic_menu_edit)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add("Delete")
            .setIcon(android.R.drawable.ic_menu_delete) 
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String option = item.getTitle().toString();
        
        if (option.equals("Edit")) {
            editPassword();
            return true;
        } else if (option.equals("Delete")) {
            confirmDelete();
            return true;
        }
        
        return false;
    }

    /**
     * Set up a clickable password. When clicked the user will be prompted to
     * choose if the password should be copied to the clipboard.
     */
    private void setupClipboardAction() {
        final TextView passwordView = (TextView) findViewById(R.id.password);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Copy to clipboard");
        alertDialog.setMessage("Copy the password to clipboard?");

        alertDialog.setButton(AlertDialog.BUTTON1, "Yes",
                new DialogInterface.OnClickListener() { 
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = 
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(passwordView.getText());
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON2, "No",
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        });

        passwordView.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.show();
                    }
                }
        );
    }

    private void editPassword() {
        Intent i = new Intent(this, EditActivity.class);
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
        alertDialog.setMessage("Are your sure you want to delete "+system+"?");

        alertDialog.setButton(AlertDialog.BUTTON1, "Yes",
                new DialogInterface.OnClickListener() { 
            public void onClick(DialogInterface dialog, int which) {
                deletePassword();
                Session.getInstance().setNeedReload(true);
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON2, "No", 
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
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
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}
