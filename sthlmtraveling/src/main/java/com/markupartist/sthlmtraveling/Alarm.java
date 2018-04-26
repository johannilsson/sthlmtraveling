package com.markupartist.sthlmtraveling;

/**
 * Blenda och Filip
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.POWER_SERVICE;

public class Alarm extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm;
        PowerManager.WakeLock wl;

        pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
           wl.acquire();



        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        String mMessage = intent.getStringExtra("notificationMessage");
        Log.v("ossnaa", mMessage);

        mBuilder.setContentTitle(mMessage);
        mBuilder.setAutoCancel(true);
        mBuilder.setSmallIcon(R.drawable.icon);
        mBuilder.setAutoCancel(true);

        mNotificationManager.notify(0, mBuilder.build());

        Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        wl.release();

        final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};

        vibrator.vibrate(DEFAULT_VIBRATE_PATTERN, 0);
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vibrator.cancel();
    }


}
