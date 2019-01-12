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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity that displays the user passwords.
 */
public class PasswordListActivity extends TimeoutListActivity
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
    private TextView     passwordCountTextView;

    private boolean passwordCounterIsVisible = false;

    private PasswordAdapter passwordAdapter = null;
    
    MenuItem searchMenuItem;

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

        emptyListHelp = (TextView) findViewById(R.id.empty_list_help);
        
        passwordCountTextView = (TextView) findViewById(R.id.password_count);
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
    public boolean onSearchRequested() {
        searchMenuItem.expandActionView();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        searchMenuItem = menu.add(Menu.NONE, OPTION_MENU_SEARCH, Menu.NONE, getString(R.string.options_search));
        
        searchMenuItem
        .setActionView(R.layout.collapsable_search)
        .setIcon(R.drawable.ic_menu_search)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        final EditText searchField = (EditText) searchMenuItem.getActionView().findViewById(R.id.search_field);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (passwordAdapter != null) {
                    passwordAdapter.setFilterString(arg0.toString());
                    passwordAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                    int arg2, int arg3) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                    int arg3) {
                // TODO Auto-generated method stub
                
            }    
        });

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                passwordCountTextView.setVisibility(View.GONE);
                searchField.post(new Runnable() {
                    @Override
                    public void run() {
                        searchField.setText("");
                        searchField.setFocusable(true);
                        searchField.setFocusableInTouchMode(true);
                        searchField.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
                    }
                });

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchField.setFocusable(false);
                searchField.setFocusableInTouchMode(false);

                if (passwordCounterIsVisible) {
                    passwordCountTextView.setVisibility(View.VISIBLE);
                }
                passwordAdapter.setFilterString(null);
                passwordAdapter.notifyDataSetChanged();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchField.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

                return true;
            }
        });
        
        menu.add(Menu.NONE, OPTION_MENU_ADD, Menu.NONE, getString(R.string.options_add))
        .setIcon(R.drawable.ic_menu_add);
        
        searchMenuItem = menu.add(Menu.NONE, OPTION_MENU_GENERATE, Menu.NONE,
                getString(R.string.options_generate_password));
        searchMenuItem.setIcon(R.drawable.ic_menu_rotate);

        searchMenuItem = menu.add(Menu.NONE, OPTION_MENU_SETTINGS, Menu.NONE,
                getString(R.string.options_settings));
        searchMenuItem.setIcon(R.drawable.ic_menu_preferences);

        searchMenuItem = menu.add(Menu.NONE, OPTION_MENU_ABOUT, Menu.NONE,
                getString(R.string.options_about));
        searchMenuItem.setIcon(R.drawable.ic_menu_info_details);

        searchMenuItem = menu.add(Menu.NONE, OPTION_MENU_LOGOUT, Menu.NONE,
                getString(R.string.options_logout));
        searchMenuItem.setIcon(R.drawable.ic_menu_close_clear_cancel);

        if (Constants.DEBUG) {
            searchMenuItem = menu.add(Menu.NONE,OPTION_MENU_DROPDB, Menu.NONE, "Drop DB");
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
        default:
            return super.onOptionsItemSelected(item);
        }
        
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        //PasswordModel model = PasswordModel.getInstance(this);
        PasswordEntry entry = (PasswordEntry) passwordAdapter.getItem(position);

        Intent i = new Intent(this, ShowActivity.class);
        i.putExtra("id",       entry.getId());
        i.putExtra("system",   entry.getDecSystem());
        i.putExtra("username", entry.getDecUsername());
        i.putExtra("password", entry.getDecPassword());
        i.putExtra("note",     entry.getDecNote());
        i.putExtra("url",      entry.getDecUrl());

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
        
        passwordAdapter = new PasswordAdapter(this, entries,
                                              PreferenceManager.getDefaultSharedPreferences(this).
                                              getBoolean("display_username", true)); 
        setListAdapter(passwordAdapter); 

        updateTitlebar();

        if (entries.length == 0) {
            emptyListHelp.setVisibility(View.VISIBLE);
            passwordCounterIsVisible = false;
            passwordCountTextView.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
        } else {
            emptyListHelp.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            passwordCounterIsVisible = true;
            passwordCountTextView.setVisibility(View.VISIBLE);
        }
    }

    private void updateTitlebar() {		
        int count = PasswordModel.getInstance(this).getPasswords().length;
        String quantifier = count == 1 ? "" : "s";

        passwordCountTextView.setText(count + " password" + quantifier);
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
