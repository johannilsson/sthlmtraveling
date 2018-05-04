package com.markupartist.sthlmtraveling;

/**
 * Blenda Fröjdh & Filip Appelgren
 */

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Random;

import static android.content.Context.POWER_SERVICE;

public class Alarm extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
      //  int mRequestPending = 0;
        final int mNotificationInt = new Random().nextInt();
        final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
        PowerManager mPowerManager;
        PowerManager.WakeLock mWakeLock;

        mPowerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
           mWakeLock.acquire();

        Bundle bundle = intent.getExtras();
        String mTitle = bundle.getString("NOTIFICATION_TITLE");
        String mMessage = bundle.getString("NOTIFICATION_TEXT");

        Intent mDismissIntent = new Intent(context, AlarmFragment.class);
        Log.v("mNotificationInt", "value" + mNotificationInt);
        mDismissIntent.putExtra("intId", mNotificationInt);
        PendingIntent pendingDismissIntent = PendingIntent.getBroadcast(context, 1, mDismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);

        mBuilder.addAction(R.drawable.ic_action_done, "Okay", pendingDismissIntent);
        mBuilder.setContentTitle(mTitle);
        mBuilder.setContentText(mMessage);
        mBuilder.setSmallIcon(R.drawable.logo);
        mBuilder.setAutoCancel(true);

        mNotificationManager.notify(mNotificationInt, mBuilder.build());
        Vibrator mVibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        mVibrator.vibrate(DEFAULT_VIBRATE_PATTERN, 0);
        mWakeLock.release();


    }
}


