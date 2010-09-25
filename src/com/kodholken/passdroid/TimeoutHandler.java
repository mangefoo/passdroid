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
