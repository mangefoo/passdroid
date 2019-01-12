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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Singleton class for keeping application state accessible to all classes of
 * the application.
 */
public class Session {
    public static final String TIMEOUT_ACTION = "com.kodholken.passdroid.TIMEOUT";

    private byte [] key;         // Key derived from the master password. It is
    // used for all encryption and decryption of 
    // the user stored data.
    private boolean isLoggedIn;  // Indicates whether the user is currently
    // logged in.

    private boolean needReload;

    private boolean exitMain;  // Indicates whether the MainActivity should 
    // exit when it receives control (onResume)

    private static Session session;  // Singleton instance of this class
    
    private PendingIntent timeoutIntent;
    
    private int resumes = 0;
    
    private String clipboardPassword;

    private Session() {
        needReload = false;
        isLoggedIn = false;
        exitMain = false;
        timeoutIntent = null;
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
        if (!isLoggedIn) {
            return ;
        }

        for (int i = 0; i < key.length; i++) {
            key[i] = 0;
        }

        key = null;
        setLoggedIn(false);
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
    
    public int incResume() {
        return ++resumes;
    }
    
    public int decResume() {
        if (resumes < 1) {
            throw new RuntimeException("Invalid resume state");
        }
        
        return --resumes;
    }
    
    public int getResumes() {
        return resumes;
    }

    public static void setTimeoutTimer(Context context) {
        Session session = getInstance();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (session.timeoutIntent != null) {
            am.cancel(session.timeoutIntent);
            session.timeoutIntent = null;
        }
        
        session.timeoutIntent = PendingIntent.getBroadcast(context, 0, new Intent(TIMEOUT_ACTION), PendingIntent.FLAG_ONE_SHOT);
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 5000, session.timeoutIntent);
    }

    public static void clearTimeoutTimer(Context context) {
        Session session = getInstance();
        
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (session.timeoutIntent != null) {
            am.cancel(session.timeoutIntent);
            session.timeoutIntent = null;
        }
    }
    
    public String getClipboardPassword() {
        return clipboardPassword;
    }
    
    public void setClipboardPassword(String clipboardPassword) {
        this.clipboardPassword = clipboardPassword;
    }
}
