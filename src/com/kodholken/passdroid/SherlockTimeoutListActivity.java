package com.kodholken.passdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;

public class SherlockTimeoutListActivity extends SherlockListActivity {
    private BroadcastReceiver receiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Session.TIMEOUT_ACTION);
        
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("Got broadcast");
                Session.getInstance().setLoggedIn(false);
                finish();
            }
        };

        registerReceiver(receiver, filter);
        System.out.println("Receiver registered");

        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onPause() {
        System.out.println("onPause()");
        Session.setTimeoutTimer(this);
        super.onPause();
    };
    
    @Override
    protected void onResume() {
        System.out.println("onResume()");
        Session.clearTimeoutTimer(this);
        super.onResume();
    }
    
    @Override
    protected void onDestroy() {
        System.out.println("onDestroy()");
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
