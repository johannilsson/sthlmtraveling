package com.markupartist.sthlmtraveling;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

public class AlarmFragment extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        int mNotificationId = intent.getIntExtra("intId", 0);
        NotificationManager dismisser = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Log.v("intId", "value" + mNotificationId);
        dismisser.cancel(mNotificationId);
        
    }
}
