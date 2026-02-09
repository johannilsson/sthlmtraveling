package com.markupartist.sthlmtraveling.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.markupartist.sthlmtraveling.R;

/**
 * Helper class for managing notification channels and permissions.
 */
public class NotificationHelper {

    /**
     * Notification channel ID for service deviation alerts.
     */
    public static final String CHANNEL_ID_DEVIATIONS = "deviations";

    /**
     * Request code for POST_NOTIFICATIONS permission.
     */
    public static final int REQUEST_POST_NOTIFICATIONS = 1001;

    /**
     * Creates notification channels for the app.
     * This must be called before posting any notifications on Android 8.0+.
     *
     * @param context The application context
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                // Create the deviations channel
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID_DEVIATIONS,
                        context.getString(R.string.notification_channel_deviations_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription(
                        context.getString(R.string.notification_channel_deviations_description)
                );
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Checks if the app has permission to post notifications.
     *
     * @param context The application context
     * @return true if permission is granted or not required, false otherwise
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        // On Android 12 and below, notification permission is granted by default
        return true;
    }

    /**
     * Requests the POST_NOTIFICATIONS permission from the user.
     *
     * @param activity The activity to request permission from
     * @param requestCode The request code to use for the permission callback
     */
    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    requestCode
            );
        }
    }
}
