package com.markupartist.sthlmtraveling.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.markupartist.sthlmtraveling.DeviationsActivity
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.provider.deviation.Deviation
import com.markupartist.sthlmtraveling.provider.deviation.DeviationNotificationDbAdapter
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore.Companion.extractLineNumbers
import com.markupartist.sthlmtraveling.receivers.OnAlarmReceiver
import com.markupartist.sthlmtraveling.utils.NotificationHelper
import com.markupartist.sthlmtraveling.utils.NotificationHelper.createNotificationChannels
import java.io.IOException

class DeviationService : WakefulIntentService("DeviationService") {
    private lateinit var notificationManager: NotificationManager
    private lateinit var db: DeviationNotificationDbAdapter

    override fun doWakefulWork(intent: Intent?) {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        db = DeviationNotificationDbAdapter(applicationContext)
        db.open()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val filterString = sharedPreferences.getString("notification_deviations_lines_csv", "") ?: ""
        val triggerFor = extractLineNumbers(filterString, null)

        val deviationStore = DeviationStore()

        try {
            var deviations = deviationStore.getDeviations(this)
            deviations = DeviationStore.filterByLineNumbers(deviations, triggerFor)

            for (deviation in deviations) {
                if (!db.containsReference(deviation.reference, deviation.messageVersion)) {
                    db.create(deviation.reference, deviation.messageVersion)
                    Log.d(TAG, "Notification triggered for ${deviation.reference}")
                    showNotification(deviation)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        db.close()
    }

    /**
     * Show a notification while this service is running.
     */
    private fun showNotification(deviation: Deviation) {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Cannot send notification - permission not granted")
                return
            }
        }

        // Ensure notification channels are created
        createNotificationChannels(this)

        // The PendingIntent to launch our activity if the user selects this notification
        val i = Intent(this, DeviationsActivity::class.java)
        i.setAction(DeviationsActivity.DEVIATION_FILTER_ACTION)
        val contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)

        val notification =
            NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_DEVIATIONS)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setTicker(deviation.details)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setContentTitle(getText(R.string.deviations_label))
                .setContentText(getText(R.string.new_deviations))
                .build()
        notification.flags = Notification.FLAG_AUTO_CANCEL
        // Send the notification.
        notificationManager.notify(R.string.deviations_label, notification)
    }

    companion object {
        private const val TAG = "DeviationService"
        @JvmStatic
        fun startAsRepeating(context: Context) {
            stopService(context)
        }

        @JvmStatic
        fun startService(context: Context) {
            stopService(context)
        }

        fun stopService(context: Context) {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            val mgr =
                context.getSystemService(ALARM_SERVICE) as AlarmManager
            val i = Intent(context, OnAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE)
            mgr.cancel(pi)
            sharedPreferences.edit().putBoolean("notification_deviations_enabled", false).apply()
            Log.d(TAG, "Deviation service stopped")
        }
    }
}
