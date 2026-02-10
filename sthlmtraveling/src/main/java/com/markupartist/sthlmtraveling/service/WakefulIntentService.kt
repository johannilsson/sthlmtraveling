package com.markupartist.sthlmtraveling.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log

abstract class WakefulIntentService(name: String?) : IntentService(name) {
    abstract fun doWakefulWork(intent: Intent?)

    override fun onHandleIntent(intent: Intent?) {
        try {
            doWakefulWork(intent)
        } finally {
            val lock = getLock(applicationContext)
            if (lock.isHeld) {
                try {
                    lock.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception when releasing wakelock")
                }
            }
        }
    }

    companion object {
        private const val TAG = "WakefulIntentService"
        const val LOCK_NAME = "com.markupartist.sthlmtraveling.service.WakefulIntentService.Static"
        private var lockStatic: WakeLock? = null

        @Synchronized
        private fun getLock(context: Context): WakeLock {
            if (lockStatic == null) {
                val mgr = context.getSystemService(POWER_SERVICE) as PowerManager
                lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME)
                lockStatic?.setReferenceCounted(true)
            }
            return lockStatic!!
        }

        fun acquireStaticLock(context: Context) {
            Log.d(TAG, "About to acquire static lock")
            getLock(context).acquire()
        }
    }
}
