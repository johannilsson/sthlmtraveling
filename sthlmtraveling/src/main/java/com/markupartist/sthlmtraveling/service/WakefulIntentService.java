package com.markupartist.sthlmtraveling.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

abstract public class WakefulIntentService extends IntentService {
    private static String TAG = "WakefulIntentService";
    public static final String LOCK_NAME =
            "com.markupartist.sthlmtraveling.service.WakefulIntentService.Static";
    private static PowerManager.WakeLock lockStatic = null;

    public WakefulIntentService(String name) {
        super(name);
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME);
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }

    abstract void doWakefulWork(Intent intent);

    public static void acquireStaticLock(Context context) {
        Log.d(TAG, "About acquire static lock");
        getLock(context).acquire();
    }

    @Override
    final protected void onHandleIntent(Intent intent) {
        try {
            doWakefulWork(intent);
        } finally {
            PowerManager.WakeLock lock = getLock(getApplicationContext());
            if (lock.isHeld()) {
                try {
                    lock.release();
                } catch (Exception e) {
                    Log.e(TAG, "Exception when releasing wakelock");
                }
            }
        }
    }
}
