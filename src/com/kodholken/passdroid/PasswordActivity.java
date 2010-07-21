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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class PasswordActivity extends ListActivity implements IdleLogoutCallback, PasswordModelListener {
	private static final int OPTION_MENU_ADD      = 1;
	private static final int OPTION_MENU_SEARCH   = 2;
	private static final int OPTION_MENU_SETTINGS = 3;
	private static final int OPTION_MENU_ABOUT    = 4;
	private static final int OPTION_MENU_DROPDB   = 5;
	private static final int OPTION_MENU_LOGOUT   = 6;
	
	private boolean hasBackKeyDown;
	private boolean loadSettingsOnResume;
	private ListView list;
	private int listPosition;
	
	private TextView     emptyListHelp;
	private LinearLayout countdownLayout;
	private TextView     countdownTextView;
	private int          countdownValue;
	private Handler      countdownHandler;
	
	public PasswordActivity() {
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
		
		loadSettingsOnResume = true; // Make sure settings are loaded on first run
		Session.getInstance().setNeedReload(true);
		Session.getInstance().setIdleLogoutCallback(this);
		
		emptyListHelp = (TextView) findViewById(com.kodholken.passdroid.R.id.empty_list_help);
		
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
		
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		getWindow().addContentView(countdownLayout, params);
		
		countdownHandler = new Handler();
		
		PasswordModel.getInstance(this).addListener(this);
	}
	
	@Override
	public void onResume() {
		Utils.debug("Passwords:onResume()");
		super.onResume();
		if (!Session.getInstance().isLoggedIn()) {
			finish();
			return ;
		}
		
		if (Session.getInstance().needReload()) {
			loadPasswords();
			Session.getInstance().setNeedReload(false);
			Utils.debug("Setting position to " + listPosition);
			list.setSelection(listPosition);
		} else {
			Utils.debug("Skipping loadPassword()");
		}
		
		if (loadSettingsOnResume) {
			loadSettingsOnResume = false;
			loadSettings();
		}
		
		Session.getInstance().bumpLogoutTimer();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem item = menu.add(Menu.NONE, OPTION_MENU_ADD, Menu.NONE, getString(R.string.options_add));
		item.setIcon(android.R.drawable.ic_menu_add);
		
		item = menu.add(Menu.NONE, OPTION_MENU_SETTINGS, Menu.NONE, getString(R.string.options_settings));
		item.setIcon(android.R.drawable.ic_menu_preferences);
		
		item = menu.add(Menu.NONE, OPTION_MENU_ABOUT, Menu.NONE, getString(R.string.options_about));
		item.setIcon(android.R.drawable.ic_menu_info_details);
		
		item = menu.add(Menu.NONE, OPTION_MENU_LOGOUT, Menu.NONE, getString(R.string.options_logout));
		item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		if (Constants.DEBUG) {
			item = menu.add(Menu.NONE, OPTION_MENU_DROPDB, Menu.NONE, "Drop DB");
		}
			
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;

		switch (item.getItemId()) {
		case OPTION_MENU_ABOUT:
			i = new Intent(this, About.class);
			startActivity(i);
			break;
		case OPTION_MENU_SEARCH:
			findViewById(R.id.search).setVisibility(View.VISIBLE);
			break;
		case OPTION_MENU_SETTINGS:
			startActivity(new Intent(this, Settings.class));
			loadSettingsOnResume = true;
			listPosition = list.getFirstVisiblePosition();
			break;
		case OPTION_MENU_ADD:
			Session.getInstance().setIdleLogout(false);
			Session.getInstance().cancelLogoutTimer();
			loadSettingsOnResume = true;
			startActivity(new Intent(this, Add.class));
			listPosition = list.getFirstVisiblePosition();
			break;
		case OPTION_MENU_DROPDB:
			PasswordData passwordData = new PasswordData(this);
			SQLiteDatabase db = passwordData.getWritableDatabase();
			passwordData.onUpgrade(db, 0, 0);
			db.close();
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
		
		Intent i = new Intent(this, ShowPassword.class);
		i.putExtra("id",       model.getAt(position).getId());
		i.putExtra("system",   model.getAt(position).getDecSystem());
		i.putExtra("username", model.getAt(position).getDecUsername());
		i.putExtra("password", model.getAt(position).getDecPassword());
		
		listPosition = list.getFirstVisiblePosition();
		
		startActivity(i);
	}
	
	@Override
	public void onUserInteraction() {
		Session.getInstance().bumpLogoutTimer();
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
	
	private void confirmLogout() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Confirm logout");
		alertDialog.setMessage("Do you really want to log out?");
		
		alertDialog.setButton(AlertDialog.BUTTON1, "Yes", new DialogInterface.OnClickListener() { 
			public void onClick(DialogInterface dialog, int which) {
				Session.getInstance().logout();
				finish();
			}
		});
		
		alertDialog.setButton(AlertDialog.BUTTON2, "No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		
		alertDialog.show();
	}
		
	private void loadPasswords() {
		PasswordEntry [] entries = PasswordModel.getInstance(this).getPasswords();
		setListAdapter(new PasswordAdapter(this, entries,
				PreferenceManager.getDefaultSharedPreferences(this).getBoolean("display_username", true))); 
		
		if (entries.length == 0) {
			emptyListHelp.setVisibility(View.VISIBLE);
			list.setVisibility(View.GONE);
		} else {
			emptyListHelp.setVisibility(View.GONE);
			list.setVisibility(View.VISIBLE);
		}
	}
	
	private void loadSettings() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Session.getInstance().setIdleLogout(pref.getBoolean("idle_logout", true));
		Session.getInstance().setIdleLogoutTime(Integer.parseInt(pref.getString("idle_logout_time", "60")));
		Session.getInstance().bumpLogoutTimer();
	}

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
