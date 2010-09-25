package com.kodholken.passdroid;

public class TimeoutHandler {
	private static final int LIMIT_DEFAULT = 5000;
	
	private static long paused = 0;
	private static long limit = LIMIT_DEFAULT;
	private static boolean timeout = false;
	
	public synchronized static void gotResume() {
		if (paused > 0 && (System.currentTimeMillis() - paused) > limit) {
			timeout = true;
		}		
	}
	
	public synchronized static void gotPause() {
		paused = System.currentTimeMillis();
	}

	public synchronized static boolean hasTimedOut() {
		return timeout;
	}

	public synchronized static void setTimeout(boolean timeout) {
		TimeoutHandler.timeout = timeout;
		TimeoutHandler.paused = 0;
	}
}
