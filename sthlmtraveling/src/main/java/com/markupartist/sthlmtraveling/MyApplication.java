package com.markupartist.sthlmtraveling;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.misc.ApiServiceProvider;
import com.markupartist.sthlmtraveling.data.misc.HttpHelper;
import com.markupartist.sthlmtraveling.service.DataMigrationService;
import com.markupartist.sthlmtraveling.utils.ThemeHelper;
import com.squareup.okhttp.OkHttpClient;

import java.util.Locale;

public class MyApplication extends Application {
    static String TAG = "StApplication";
    private ApiService mApiService;
    private OkHttpClient mHttpClient;

    public static MyApplication get(Context context) {
        return (MyApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setNightMode();

        reloadLocaleForApplication();

        DataMigrationService.startService(this);
    }

    public OkHttpClient getHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = HttpHelper.getInstance(this).getClient();
        }
        return mHttpClient;
    }

    public ApiService getApiService() {
        if (mApiService == null) {
            mApiService = ApiServiceProvider.getApiService(getHttpClient());
        }
        return mApiService;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadLocaleForApplication();
    }

    private void setNightMode() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String mode = sharedPreferences.getString("dark_mode_preference", ThemeHelper.DEFAULT_DARK_MODE);
        ThemeHelper.applyDarkMode(mode);
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
            case "ru":
                locale = new Locale("ru", "RU");
                break;
            case "bs":
                locale = new Locale("bs", "BA");
                break;
            case "iw":
                locale = new Locale("iw", "");
                break;
            case "mn":
                locale = new Locale("mn", "MN");
                break;
            case "pt_BR":
                locale = new Locale("pt", "BR");
                break;
        }

        if (locale != null) {
            Log.d(TAG, "setting locale " + locale);
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.setLocale(locale);
            Locale.setDefault(locale);
            res.updateConfiguration(conf, dm);
        }
    }
}
