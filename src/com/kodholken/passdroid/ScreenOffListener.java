package com.kodholken.passdroid;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;

public class ScreenOffListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean clear = PreferenceManager.getDefaultSharedPreferences(context).
                                          getBoolean("clear_clipboard", true);
        
        if (clear) {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            
            String clipboardPassword = Session.getInstance().getClipboardPassword();
            // We only clear the clipboard when the content equals the last
            // copied password.
            if (clipboardPassword != null &&
                cm.hasText() &&
                cm.getText().equals(clipboardPassword)) {

                cm.setText("");
                Session.getInstance().setClipboardPassword(null);
            }
        }
    }
}
