package com.markupartist.sthlmtraveling;

import com.markupartist.sthlmtraveling.service.DeviationService;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity
        implements OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals("prefered_language_preference")) {
            MyApplication application = (MyApplication) getApplication();
            application.reloadLocaleForApplication();
            Toast.makeText(this, R.string.restart_app_for_full_effect,
                    Toast.LENGTH_LONG).show();
        } else if (key.equals("notification_deviations_enabled")) {
            DeviationService.startAsRepeating(SettingsActivity.this);
        }

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);    
    }
}
