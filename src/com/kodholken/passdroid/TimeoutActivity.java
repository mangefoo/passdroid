package com.kodholken.passdroid;

import android.app.Activity;

public class TimeoutActivity extends Activity {
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
