package com.markupartist.sthlmtraveling;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.service.DeviationService;

public class SettingsActivity extends BasePreferenceActivity
        implements OnSharedPreferenceChangeListener {
    private static final String TAG = "SettingsActivity";
    private static final int DIALOG_CLEAR_SEARCH_HISTORY = 0;
    private static final int DIALOG_CLEAR_FAVORITES = 1;

    private HistoryDbAdapter mHistoryDbAdapter;
    private FavoritesDbAdapter mFavoritesDbAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        registerEvent("Settings");

        mHistoryDbAdapter = new HistoryDbAdapter(this).open();
        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHistoryDbAdapter.close();
        mFavoritesDbAdapter.close();
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
            boolean enabled = sharedPreferences.getBoolean("notification_deviations_enabled", false);
            if (enabled) {
                registerEvent("Starting deviation service");
            } else {
                registerEvent("Disabled deviation service");
            }
            DeviationService.startAsRepeating(SettingsActivity.this);
        }

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("clear_search_history")) {
            showDialog(DIALOG_CLEAR_SEARCH_HISTORY);
            return true;
        } else if (preference.getKey().equals("clear_favorites")) {
            showDialog(DIALOG_CLEAR_FAVORITES);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_CLEAR_SEARCH_HISTORY:
            return new AlertDialog.Builder(this)
                .setTitle(R.string.search_clear_history_preference)
                .setMessage(R.string.search_clear_history_confirm)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mHistoryDbAdapter.deleteAll();
                        Toast.makeText(SettingsActivity.this,
                                R.string.search_history_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, new OnClickListener() {                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        case DIALOG_CLEAR_FAVORITES:
            return new AlertDialog.Builder(this)
                .setTitle(R.string.search_clear_favorites_preference)
                .setMessage(R.string.search_clear_favorites_confirm)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mFavoritesDbAdapter.deleteAll();
                        Toast.makeText(SettingsActivity.this,
                                R.string.search_favorites_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.no, new OnClickListener() {                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        }

        return null;
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }
}
