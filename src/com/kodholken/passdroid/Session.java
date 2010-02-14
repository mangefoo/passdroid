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

import java.util.Timer;
import java.util.TimerTask;

public class Session {
	private byte [] key;
    private boolean isLoggedIn;
	private Timer   logoutTimer;
	private boolean idleLogout;
	private int     idleLogoutTime;
	private IdleLogoutCallback idleLogoutCallback;
	private int countdown;
	private boolean needReload;
	private boolean exitMain;

	private static Session session;

	private Session() {
		needReload = false;
		isLoggedIn = false;
		logoutTimer = null;
		idleLogout = false;
		exitMain = false;
		idleLogoutTime = 0;
		idleLogoutCallback = null;
	}
	
	public static Session getInstance() {
		if (session == null) {
			session = new Session();
		}
			
		return session;
	}
	
	public void setKey(byte [] key) {
		this.key = key;
	}

	public byte[] getKey() {
		return key;
	}
	
	public boolean isLoggedIn() {
		return isLoggedIn;
	}
	
	public void setLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}
	
	public void setLoggedIn() {
		setLoggedIn(true);
	}
	
	public void logout() {
		cancelLogoutTimer();
		
		if (!isLoggedIn) {
			return ;
		}
		
		for (int i = 0; i < key.length; i++) {
			key[i] = 0;
		}
		
		key = null;
		setLoggedIn(false);
	}
	
	public void bumpLogoutTimer() {
		Utils.debug("Session::bumpLogoutTimer() called");
		if (logoutTimer != null) {
			logoutTimer.cancel();
		}
		
		if (idleLogoutCallback != null) {
			idleLogoutCallback.idleLogoutCancel();
		}
		
		if (!idleLogout) {
			return ;
		}
		
		logoutTimer = new Timer();
		logoutTimer.schedule(new TimerTask() { 
			public void run() {
				Utils.debug("Idle logout triggered");
				startCountdown();
				//logout();
				//idleLogoutCallback.idleLogoutCallback();
			} 
		}, (idleLogoutTime - 10) * 1000);
		Utils.debug("Logout timer scheduled");
	}

	private void startCountdown() {
		countdown = 10;
		if (idleLogoutCallback != null) {
			idleLogoutCallback.idleLogoutCountdown(countdown);
		}
		logoutTimer.schedule(new TimerTask() {
			public void run() {
				stepCountdown();
			}
		}, 1000);
	}
	
	private void stepCountdown() {
		countdown--;
		Utils.debug("Countdown: " + countdown);
		if (countdown > 0) {
			if (idleLogoutCallback != null) {
				idleLogoutCallback.idleLogoutCountdown(countdown);
			}
			Utils.debug("Scheduling new countdown");
			logoutTimer.schedule(new TimerTask() {
				public void run() {
					stepCountdown();
				}
			}, 1000);
			Utils.debug("Scheduled new countdown");
		} else {
			logout();
			if (idleLogoutCallback != null) {
				idleLogoutCallback.idleLogoutCallback();
			}
		}
	}
	
	public void cancelLogoutTimer() {
		Utils.debug("Session::cancelLogoutTimer() called");
		if (logoutTimer != null) {
			logoutTimer.cancel();
			logoutTimer.purge();
			Utils.debug("Logout timer cancelled");
		} else {
			Utils.debug("Session::cancelLogoutTimer(): No timer to cancel");
		}
	}

	public void setIdleLogout(boolean idleLogout) {
		System.out.println("Setting idle logout to " + idleLogout);

		if (!idleLogout) {
			cancelLogoutTimer();
		}
		this.idleLogout = idleLogout;
	}
	
	public void setIdleLogoutTime(int idleLogoutTime) {
		System.out.println("Setting idle logout time to " + idleLogoutTime);
		
		this.idleLogoutTime = idleLogoutTime;
		if (idleLogout) {
			bumpLogoutTimer();
		}
	}
	
	public void setIdleLogoutCallback(IdleLogoutCallback callback) {
		this.idleLogoutCallback = callback;
	}
	
	public void setNeedReload(boolean needReload) {
		this.needReload = needReload;
	}
	
	public boolean needReload() {
		return needReload;
	}
	
	public void setExitMain(boolean exitMain) {
		this.exitMain = exitMain;
	}
	
	public boolean getExitMain() {
		return exitMain;
	}
}
