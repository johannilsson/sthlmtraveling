package com.markupartist.sthlmtraveling.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.markupartist.sthlmtraveling.service.DeviationService.Companion.startAsRepeating

class OnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "OnBootReceiver")
        startAsRepeating(context)
    }

    companion object {
        private const val TAG = "OnBootReceiver"
    }
}
