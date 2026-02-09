package com.markupartist.sthlmtraveling.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.markupartist.sthlmtraveling.DeviationsActivity;
import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.deviation.Deviation;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationNotificationDbAdapter;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore;
import com.markupartist.sthlmtraveling.receivers.OnAlarmReceiver;
import com.markupartist.sthlmtraveling.utils.NotificationHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class DeviationService extends WakefulIntentService {
    private static String TAG = "DeviationService";
    private static String LINE_PATTERN = "[A-Za-zåäöÅÄÖ ]?([\\d]+)[ A-Z]?";
    private static Pattern sLinePattern = Pattern.compile(LINE_PATTERN);
    private NotificationManager mNotificationManager;
    //private Intent mInvokeIntent;
    private DeviationNotificationDbAdapter mDb;

    public DeviationService() {
        super("DeviationService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        mNotificationManager =
            (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //mInvokeIntent = new Intent(this, DeviationsActivity.class);

        mDb = new DeviationNotificationDbAdapter(getApplicationContext());
        mDb.open();

        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        String filterString = sharedPreferences.getString("notification_deviations_lines_csv", "");
        ArrayList<Integer> triggerFor = DeviationStore.extractLineNumbers(filterString, null);

        DeviationStore deviationStore = new DeviationStore();

        try {
            ArrayList<Deviation> deviations = deviationStore.getDeviations(this);
            deviations = DeviationStore.filterByLineNumbers(deviations, triggerFor);

            for (Deviation deviation : deviations) {
                if (!mDb.containsReference(deviation.getReference(),
                        deviation.getMessageVersion())) {
                    mDb.create(deviation.getReference(), deviation.getMessageVersion());
                    Log.d(TAG, "Notification triggered for " + deviation.getReference());
                    showNotification(deviation);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mDb.close();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(Deviation deviation) {
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot send notification - permission not granted");
                return;
            }
        }

        // Ensure notification channels are created
        NotificationHelper.createNotificationChannels(this);

        // The PendingIntent to launch our activity if the user selects this notification
        Intent i = new Intent(this, DeviationsActivity.class);
        i.setAction(DeviationsActivity.DEVIATION_FILTER_ACTION);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_DEVIATIONS)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setTicker(deviation.getDetails())
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setContentTitle(getText(R.string.deviations_label))
                .setContentText(getText(R.string.new_deviations))
                .build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        // Send the notification.
        mNotificationManager.notify(R.string.deviations_label, notification);
    }

    public static void startAsRepeating(Context context) {
        stopService(context);
    }

    public static void startService(Context context) {
        stopService(context);
    }

    public static void stopService(Context context) {
        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context);
        AlarmManager mgr =
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        mgr.cancel(pi);
        sharedPreferences.edit().putBoolean("notification_deviations_enabled", false).apply();
        Log.d(TAG, "Deviation service stopped");
    }
}
