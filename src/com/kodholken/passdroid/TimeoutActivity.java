package com.kodholken.passdroid;

import android.app.Activity;

public class TimeoutActivity extends Activity {
	@Override
	protected void onResume() {
		Utils.debug("TimeoutActivity::onResume()");
		TimeoutHandler.gotResume();
		super.onResume();

		if (TimeoutHandler.hasTimedOut()) {
			Session.getInstance().setLoggedIn(false);
			finish();
		}
	}
	
	@Override
	protected void onPause() {
		Utils.debug("TimeoutActivity::onPause()");
		TimeoutHandler.gotPause();
		super.onPause();
	}
}
