package com.markupartist.sthlmtraveling;

/**
 * Blenda och Filip
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class Alarm extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        String mMessage = intent.getStringExtra("notificationExtra");

        mBuilder.setContentText(mMessage);
        mBuilder.setContentTitle(mMessage);
        mBuilder.setSmallIcon(R.drawable.icon);
        mNotificationManager.notify(0, mBuilder.build());


        Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        vibrator.vibrate(5000);

    }


}
