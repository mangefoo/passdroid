package com.kodholken.passdroid;

import android.app.ListActivity;

public class TimeoutListActivity extends ListActivity {
	@Override
	protected void onPause() {
		Utils.debug("TimeoutListActivity::onPause()");
		super.onPause();
		TimeoutHandler.gotPause();
	}
	
	@Override
	protected void onResume() {
		Utils.debug("TimeoutListActivity::onResume()");
		super.onResume();
		TimeoutHandler.gotResume();
		if (TimeoutHandler.hasTimedOut()) {
			Session.getInstance().setLoggedIn(false);
			finish();
		}
	}
}
