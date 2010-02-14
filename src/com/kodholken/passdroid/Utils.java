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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

public class Utils {
	public static void alertDialog(Context context, String title, String message) {
		AlertDialog alertDialog;
		
	    alertDialog = new AlertDialog.Builder(context).create();
	    alertDialog.setTitle(title);
	    alertDialog.setMessage(message);
	    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialog, int which) {
	        return;
	      } }); 
	    alertDialog.show();
	}
	
	public static void startPasswordsView(Context context) {
		Intent i = new Intent(context, Passwords.class);
		context.startActivity(i);
	}
	
	public static void logoutConfirmDialog(Context context) {
	}
	
	public static void debug(String message) {
		Log.d("Passdroid", message);
	}
}
