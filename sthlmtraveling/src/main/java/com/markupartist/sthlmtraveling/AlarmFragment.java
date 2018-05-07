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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;


/**
 * @author Blenda Fröjdh & Filip Appelgren
 */

public class AlarmFragment extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};

        int notificationId = intent.getIntExtra("intId", 0);
        NotificationManager dismisser = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Log.v("intId", "value" + notificationId);
        dismisser.cancel(notificationId);
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.cancel();



    }
}
