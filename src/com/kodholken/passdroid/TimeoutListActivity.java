package com.kodholken.passdroid;

import android.app.ListActivity;

public class TimeoutListActivity extends ListActivity {
	@Override
	protected void onResume() {
		TimeoutHandler.gotResume();
		super.onResume();
		
		if (TimeoutHandler.hasTimedOut(this)) {
			Session.getInstance().setLoggedIn(false);
			finish();
		}
	}
	
	@Override
	protected void onPause() {
		TimeoutHandler.gotPause();
		super.onPause();
	}
}
