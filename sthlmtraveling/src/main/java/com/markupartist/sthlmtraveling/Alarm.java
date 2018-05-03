package com.markupartist.sthlmtraveling;

/**
 * Blenda Fröjdh & Filip Appelgren
 */

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import static android.content.Context.POWER_SERVICE;

public class Alarm extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        PowerManager mPowerManager;
        PowerManager.WakeLock mWakeLock;

        mPowerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
           mWakeLock.acquire();

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String mTitle = intent.getStringExtra("NOTIFICATION_TITLE");
        String mMessage = intent.getStringExtra("NOTIFICATION_MESSAGE");

        Intent mDismissIntent = new Intent(context, AlarmFragment.class);
        PendingIntent pendingDismissIntent = PendingIntent.getBroadcast(context, 1, mDismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.addAction(R.drawable.ic_action_done, "Okay", pendingDismissIntent);
        mBuilder.setContentTitle(mTitle);
        mBuilder.setContentText(mMessage);
        mBuilder.setSmallIcon(R.drawable.logo);
        mBuilder.setAutoCancel(true);
        mBuilder.setContentIntent(pendingIntent);

        mNotificationManager.notify(0, mBuilder.build());

        Vibrator mVibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);

        final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250, 250};
        mVibrator.vibrate(DEFAULT_VIBRATE_PATTERN, 0);
        mWakeLock.release();


    }
}


