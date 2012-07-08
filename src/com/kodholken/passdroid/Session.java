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

/**
 * Singleton class for keeping application state accessible to all classes of
 * the application.
 */
public class Session {
    private byte [] key;         // Key derived from the master password. It is
    // used for all encryption and decryption of 
    // the user stored data.
    private boolean isLoggedIn;  // Indicates whether the user is currently
    // logged in.

    private boolean needReload;

    private boolean exitMain;  // Indicates whether the MainActivity should 
    // exit when it receives control (onResume)

    private static Session session;  // Singleton instance of this class

    private Session() {
        needReload = false;
        isLoggedIn = false;
        exitMain = false;
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
}
