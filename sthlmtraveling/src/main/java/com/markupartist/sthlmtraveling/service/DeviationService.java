package com.markupartist.sthlmtraveling.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.markupartist.sthlmtraveling.DeviationsActivity;
import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.deviation.Deviation;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationNotificationDbAdapter;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore;
import com.markupartist.sthlmtraveling.receivers.OnAlarmReceiver;

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
            ArrayList<Deviation> deviations = deviationStore.getDeviations();
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
        // Set the icon, scrolling text and timestamp
        Notification notification =
            new Notification(android.R.drawable.stat_notify_error, deviation.getDetails(),
                System.currentTimeMillis());

        notification.flags = Notification.FLAG_AUTO_CANCEL;

        // The PendingIntent to launch our activity if the user selects this notification
        Intent i = new Intent(this, DeviationsActivity.class);
        i.setAction(DeviationsActivity.DEVIATION_FILTER_ACTION);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.deviations_label),
                getText(R.string.new_deviations), contentIntent);

        // Send the notification.
        mNotificationManager.notify(R.string.deviations_label, notification);
    }

    public static void startAsRepeating(Context context) {
        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context);

        boolean enabled =
            sharedPreferences.getBoolean("notification_deviations_enabled", false);
        Log.d(TAG, "notification_deviations_enabled: " + enabled);

        AlarmManager mgr =
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        if (enabled) {
            Log.d(TAG, "Starting DeviationService in the background");
            // Default one hour.
            long updateInterval = Long.parseLong(
                    sharedPreferences.getString("notification_deviations_update_interval", "3600000"));
            mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 60000,
                    updateInterval,
                    pi);        
        } else {
            mgr.cancel(pi);
        }
    }

    public static void startService(Context context) {
        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context);

        boolean enabled =
            sharedPreferences.getBoolean("notification_deviations_enabled", false);

        if (enabled) {
            WakefulIntentService.acquireStaticLock(context);
            context.startService(new Intent(context, DeviationService.class));
        } else {
            Log.d(TAG, "Stopping DeviationService");
            AlarmManager mgr =
                (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(context, OnAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
            mgr.cancel(pi);
        }
    }
}
