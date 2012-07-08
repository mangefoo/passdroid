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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kodholken.passdroid.db.PasswordData;

import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity that displays the user passwords.
 */
public class PasswordListActivity extends SherlockListActivity
implements IdleLogoutCallback,
PasswordModelListener {
    private static final int OPTION_MENU_ADD      = 1;
    private static final int OPTION_MENU_SETTINGS = 3;
    private static final int OPTION_MENU_ABOUT    = 4;
    private static final int OPTION_MENU_DROPDB   = 5;
    private static final int OPTION_MENU_LOGOUT   = 6;
    private static final int OPTION_MENU_GENERATE = 7;
    private static final int OPTION_MENU_SEARCH   = 8;

    private boolean hasBackKeyDown;
    // Used to keep the activity from reloading the settings on resumes where
    // the settings has not changed.
    private boolean loadSettingsOnResume;
    private ListView list;
    private int listPosition;

    private TextView     emptyListHelp;
    private LinearLayout countdownLayout;
    private TextView     countdownTextView;
    private int          countdownValue;
    private Handler      countdownHandler;
    //private TextView     titleBarCountTextView;

    public PasswordListActivity() {
        loadSettingsOnResume = false;
        hasBackKeyDown = false;
        countdownLayout = null;
        listPosition = 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Session.getInstance().isLoggedIn()) {
            finish();
            return ;
        }

        setContentView(R.layout.password);

        list = (ListView) findViewById(android.R.id.list);
        initLongClickListener();

        // Make sure settings are loaded on first run.
        loadSettingsOnResume = true;
        // Make sure the password entries is loaded from the database
        Session.getInstance().setNeedReload(true);

        emptyListHelp = (TextView) findViewById(
                com.kodholken.passdroid.R.id.empty_list_help);
        /*
		titleBarCountTextView = (TextView) findViewById
		                         (com.kodholken.passdroid.R.id.password_count);
         */
        getListView().setTextFilterEnabled(true);

        countdownLayout = new LinearLayout(this);
        countdownLayout.setGravity(Gravity.CENTER_VERTICAL);
        countdownLayout.setOrientation(LinearLayout.VERTICAL);
        countdownLayout.setVisibility(View.INVISIBLE);

        countdownTextView = new TextView(this);
        countdownTextView.setText("10");
        countdownTextView.setShadowLayer(5.0f, 5.0f, 5.0f, 0xff606060);
        countdownTextView.setTextSize(128);
        countdownTextView.setGravity(Gravity.CENTER);

        countdownLayout.addView(countdownTextView);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        getWindow().addContentView(countdownLayout, params);

        countdownHandler = new Handler();

        PasswordModel.getInstance(this).addListener(this);

        ActionBar actionBar = getSupportActionBar();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!Session.getInstance().isLoggedIn()) {
            finish();
            return ;
        }

        if (Session.getInstance().needReload()) {
            loadPasswords();
            Session.getInstance().setNeedReload(false);
            list.setSelection(listPosition);
        } else {
            Utils.debug("Skipping loadPassword()");
        }

        if (loadSettingsOnResume) {
            loadSettingsOnResume = false;
            loadSettings();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, OPTION_MENU_SEARCH, Menu.NONE, getString(R.string.options_search))
        .setIcon(android.R.drawable.ic_menu_search)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(Menu.NONE, OPTION_MENU_ADD, Menu.NONE, getString(R.string.options_add))
        .setIcon(android.R.drawable.ic_menu_add);


        MenuItem item;

        item = menu.add(Menu.NONE, OPTION_MENU_GENERATE, Menu.NONE,
                getString(R.string.options_generate_password));
        item.setIcon(android.R.drawable.ic_menu_rotate);

        item = menu.add(Menu.NONE, OPTION_MENU_SETTINGS, Menu.NONE,
                getString(R.string.options_settings));
        item.setIcon(android.R.drawable.ic_menu_preferences);

        item = menu.add(Menu.NONE, OPTION_MENU_ABOUT, Menu.NONE,
                getString(R.string.options_about));
        item.setIcon(android.R.drawable.ic_menu_info_details);

        item = menu.add(Menu.NONE, OPTION_MENU_LOGOUT, Menu.NONE,
                getString(R.string.options_logout));
        item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        if (Constants.DEBUG) {
            item = menu.add(Menu.NONE,OPTION_MENU_DROPDB, Menu.NONE, "Drop DB");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;

        switch (item.getItemId()) {
        case OPTION_MENU_ABOUT:
            i = new Intent(this, AboutActivity.class);
            startActivity(i);
            break;
        case OPTION_MENU_SETTINGS:
            startActivity(new Intent(this, SettingsActivity.class));
            loadSettingsOnResume = true;
            listPosition = list.getFirstVisiblePosition();
            break;
        case OPTION_MENU_ADD:
            loadSettingsOnResume = true;
            startActivity(new Intent(this, AddActivity.class));
            listPosition = list.getFirstVisiblePosition();
            break;
        case OPTION_MENU_DROPDB:
            PasswordData passwordData = new PasswordData(this);
            SQLiteDatabase db = passwordData.getWritableDatabase();
            passwordData.onUpgrade(db, 0, 0);
            db.close();
            break;
        case OPTION_MENU_GENERATE:
            Intent generateIntent = new Intent(this,
                    GeneratePasswordActivity.class);
            generateIntent.putExtra("displayPassword", 1);
            startActivity(generateIntent);
            break;
        case OPTION_MENU_LOGOUT:
            confirmLogout();
            break;
        }

        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        PasswordModel model = PasswordModel.getInstance(this);

        Intent i = new Intent(this, ShowActivity.class);
        i.putExtra("id",       model.getAt(position).getId());
        i.putExtra("system",   model.getAt(position).getDecSystem());
        i.putExtra("username", model.getAt(position).getDecUsername());
        i.putExtra("password", model.getAt(position).getDecPassword());

        listPosition = list.getFirstVisiblePosition();

        i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (hasBackKeyDown) {
                confirmLogout();
                hasBackKeyDown = false;
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
     * Set up a long click handler for the list view. The user will be presented
     * with an option to copy the password to clipboard.
     */
    private void initLongClickListener() {
        final Context context = this;

        list.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View v,
                    int position, long id) {

                final String password = PasswordModel.getInstance(context)
                .getPasswords()[position]
                                .getDecPassword();

                final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                alertDialog.setTitle("Copy to clipboard");
                alertDialog.setMessage("Copy the password to clipboard?");

                alertDialog.setButton(AlertDialog.BUTTON1, "Yes",
                        new DialogInterface.OnClickListener() { 
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = 
                            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(password);
                    }
                });

                alertDialog.setButton(AlertDialog.BUTTON2, "No",
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                });

                alertDialog.show();

                return true;
            }
        });
    }

    private void confirmLogout() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Confirm logout");
        alertDialog.setMessage("Do you really want to log out?");

        alertDialog.setButton(AlertDialog.BUTTON1, "Yes",
                new DialogInterface.OnClickListener() { 
            public void onClick(DialogInterface dialog, int which) {
                Session.getInstance().logout();
                finish();
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON2, "No",
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        });

        alertDialog.show();
    }

    private void loadPasswords() {
        PasswordEntry [] entries =
            PasswordModel.getInstance(this).getPasswords();
        setListAdapter(new PasswordAdapter(this, entries,
                PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean("display_username", true))); 

        updateTitlebar();

        if (entries.length == 0) {
            emptyListHelp.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else {
            emptyListHelp.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
    }

    private void updateTitlebar() {		
        int count = PasswordModel.getInstance(this).getPasswords().length;
        String quantifier = count == 1 ? "" : "s";

        //titleBarCountTextView.setText(count + " password" + quantifier);
    }

    private void loadSettings() {}

    @Override
    public void idleLogoutCallback() {
        finish();
    }

    @Override
    public void idleLogoutCountdown(int left) {
        Utils.debug("Idle logout countdown: " + left);
        countdownValue = left;
        countdownHandler.post(new Runnable() {
            public void run() {
                Utils.debug("Running in handler");
                countdownTextView.setText(Integer.toString(countdownValue));
                countdownLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void idleLogoutCancel() {
        countdownHandler.post(new Runnable() {
            public void run() {
                Utils.debug("Cancelling idle logout");
                countdownLayout.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onPasswordModelChange(PasswordModel model) {
        loadPasswords();
    }
}
