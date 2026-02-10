package com.markupartist.sthlmtraveling.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.markupartist.sthlmtraveling.R

/**
 * Helper class for managing notification channels and permissions.
 */
object NotificationHelper {
    /**
     * Notification channel ID for service deviation alerts.
     */
    const val CHANNEL_ID_DEVIATIONS = "deviations"

    /**
     * Request code for POST_NOTIFICATIONS permission.
     */
    const val REQUEST_POST_NOTIFICATIONS = 1001

    /**
     * Creates notification channels for the app.
     * This must be called before posting any notifications on Android 8.0+.
     *
     * @param context The application context
     */
    @JvmStatic
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            notificationManager?.let {
                // Create the deviations channel
                val channel = NotificationChannel(
                    CHANNEL_ID_DEVIATIONS,
                    context.getString(R.string.notification_channel_deviations_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = context.getString(R.string.notification_channel_deviations_description)
                it.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Checks if the app has permission to post notifications.
     *
     * @param context The application context
     * @return true if permission is granted or not required, false otherwise
     */
    @JvmStatic
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On Android 12 and below, notification permission is granted by default
            true
        }
    }

    /**
     * Requests the POST_NOTIFICATIONS permission from the user.
     *
     * @param activity The activity to request permission from
     * @param requestCode The request code to use for the permission callback
     */
    @JvmStatic
    fun requestNotificationPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
        }
    }
}
