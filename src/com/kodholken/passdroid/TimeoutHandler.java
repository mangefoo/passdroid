package com.kodholken.passdroid;

import android.content.Context;
import android.preference.PreferenceManager;

public class TimeoutHandler {
	private static final int LIMIT_DEFAULT = 1000;
	
	private static long paused = 0;
	private static long limit = LIMIT_DEFAULT;
	private static boolean timeout = false;
	
	public synchronized static void gotResume() {
		if (paused > 0) {
			long gap = System.currentTimeMillis() - paused;
			if (gap > limit) {
				Utils.notice("Focus timeout on " + gap + " ms");
				timeout = true;
			}
		}
	}
	
	public synchronized static void gotPause() {
		paused = System.currentTimeMillis();
	}

	public synchronized static boolean hasTimedOut(Context context) {
		boolean doLogout = PreferenceManager.
		                                  getDefaultSharedPreferences(context).
										  getBoolean("close_logout", true);
		
		if (!doLogout) {
			return false;
		}
		
		return timeout;
	}

	public synchronized static void setTimeout(boolean timeout) {
		TimeoutHandler.timeout = timeout;
		TimeoutHandler.paused = 0;
	}
}
