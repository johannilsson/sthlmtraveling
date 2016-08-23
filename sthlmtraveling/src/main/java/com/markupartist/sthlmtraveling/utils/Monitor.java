/*
 * Copyright (C) 2009-2016 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.markupartist.sthlmtraveling.utils;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

/**
 *
 */
public class Monitor {
    private static final String TAG = "Monitor";
    private Handler handler = new Handler();
    private boolean isStarted;


    public void onStart() {
        isStarted = true;
        handler.removeCallbacks(ticker);
        ticker.run();
    }

    public void onStop() {
        handler.removeCallbacks(ticker);
        isStarted = false;
    }

    public void handleUpdate() {
        Log.i(TAG, "update.");
    }

    protected void onUpdate() {
        if (this.isStarted) {
            handleUpdate();
        } else {
            onStop();
            Log.e(TAG, "updated called but manager is not running.");
        }
    }

    private final Runnable ticker = new Runnable() {
        public void run() {
            onUpdate();

            long now = SystemClock.uptimeMillis();
            long next = now + 60 * 1000;

            if (isStarted) {
                handler.postAtTime(ticker, next);
            }
        }
    };

}
