package com.markupartist.sthlmtraveling;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.markupartist.sthlmtraveling.service.DataMigrationService;

import java.util.Locale;

import io.fabric.sdk.android.Fabric;

public class MyApplication extends Application {
    static String TAG = "StApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        reloadLocaleForApplication();

        DataMigrationService.startService(this);
    }

    /* (non-Javadoc)
     * @see android.app.Application#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadLocaleForApplication();
    }

    public void reloadLocaleForApplication() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String language = sharedPreferences
                .getString("prefered_language_preference", "system");
        Log.d(TAG, "Preferred language: " + language);

        Locale locale = null;
        switch (language) {
            case "sv":
                locale = new Locale("sv", "SE");
                break;
            case "en":
                locale = Locale.ENGLISH;
                break;
            case "fr":
                locale = Locale.FRENCH;
                break;
            case "es":
                locale = new Locale("es", "");
                break;
            case "de":
                locale = new Locale("de", "");
                break;
            case "nl":
                locale = new Locale("nl", "");
                break;
            case "zh_CN":
                locale = new Locale("zh", "CN");
                break;
            case "zh_TW":
                locale = new Locale("zh", "TW");
                break;
            case "it":
                locale = new Locale("it", "IT");
                break;
            case "ar":
                locale = new Locale("ar", "");
                break;
        }

        if (locale != null) {
            Log.d(TAG, "setting locale " + locale);
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = locale;
            res.updateConfiguration(conf, dm);
        }
    }

    /**
     * Get analytics tracker.
     *
     * @return A Tracker
     */
    public synchronized Tracker getTracker() {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.setDryRun(BuildConfig.DEBUG);
        if (BuildConfig.DEBUG) {
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        }
        return analytics.newTracker(AppConfig.ANALYTICS_PROPERTY_KEY);
    }
}
