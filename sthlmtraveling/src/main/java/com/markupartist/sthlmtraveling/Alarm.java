/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling;

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
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import static android.content.Context.POWER_SERVICE;

/**
 * @author Blenda Fröjdh & Filip Appelgren
 */

public class Alarm extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
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
        mBuilder.setDeleteIntent(pendingDismissIntent);



        mNotificationManager.notify(mNotificationInt, mBuilder.build());
        Vibrator mVibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        mVibrator.vibrate(DEFAULT_VIBRATE_PATTERN, 0);
        mWakeLock.release();


    }
}


