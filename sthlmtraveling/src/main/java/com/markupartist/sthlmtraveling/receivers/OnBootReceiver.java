package com.markupartist.sthlmtraveling.receivers;

import com.markupartist.sthlmtraveling.service.DeviationService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
    //private static final int PERIOD = 300000; // 5 minutes
    private static final int PERIOD = 300000; // 5 minutes
    private static final String TAG = "OnBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "OnBootReceiver");

        DeviationService.startAsRepeating(context);
    }
}
