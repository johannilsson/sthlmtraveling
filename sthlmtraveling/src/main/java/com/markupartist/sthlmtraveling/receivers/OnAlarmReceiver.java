package com.markupartist.sthlmtraveling.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.service.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "OnAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "recieved alarm");

        DeviationService.startService(context);
    }
  }
