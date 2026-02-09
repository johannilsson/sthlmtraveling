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

package com.markupartist.sthlmtraveling.utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 *
 */
open class Monitor {
    private val handler = Handler(Looper.getMainLooper())
    private var isStarted = false

    open fun onStart() {
        isStarted = true
        handler.removeCallbacks(ticker)
        ticker.run()
    }

    open fun onStop() {
        handler.removeCallbacks(ticker)
        isStarted = false
    }

    open fun handleUpdate() {
        // Override in subclasses
    }

    protected open fun onUpdate() {
        if (isStarted) {
            handleUpdate()
        } else {
            onStop()
            Log.e(TAG, "updated called but manager is not running.")
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            onUpdate()

            val now = SystemClock.uptimeMillis()
            val next = now + 60 * 1000

            if (isStarted) {
                handler.postAtTime(this, next)
            }
        }
    }

    companion object {
        private const val TAG = "Monitor"
    }
}
