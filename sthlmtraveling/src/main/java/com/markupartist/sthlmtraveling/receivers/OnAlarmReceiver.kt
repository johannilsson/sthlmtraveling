package com.markupartist.sthlmtraveling.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.markupartist.sthlmtraveling.service.DeviationService.Companion.startService

class OnAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Received alarm")
        startService(context)
    }

    companion object {
        private const val TAG = "OnAlarmReceiver"
    }
}
