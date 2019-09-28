package com.markupartist.sthlmtraveling;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import androidx.core.app.NavUtils;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.UserConsentForm;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsActivity";
    private static final int DIALOG_CLEAR_SEARCH_HISTORY = 0;
    private static final int DIALOG_CLEAR_FAVORITES = 1;
    private static final int DIALOG_WHY_ADS = 2;

    private HistoryDbAdapter mHistoryDbAdapter;
    private int mClickCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.preferences);

        Analytics.getInstance(this).registerScreen("Settings");

        mHistoryDbAdapter = new HistoryDbAdapter(this).open();

        Preference customPref = findPreference("about_version");
        customPref.setSummary("Version " + AppConfig.APP_VERSION);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasConsent = sharedPreferences.getBoolean("has_consent_to_serve_personalized_ads", false);
        findPreference("preference_see_relevant_ads")
                .setTitle(hasConsent ? R.string.relevant_ads_enabled : R.string.relevant_ads_disabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHistoryDbAdapter.close();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("prefered_language_preference")) {
            MyApplication application = (MyApplication) getApplication();
            application.reloadLocaleForApplication();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                recreate();
            }
            Toast.makeText(this, R.string.restart_app_for_full_effect,
                    Toast.LENGTH_LONG).show();
        } else if (key.equals("notification_deviations_enabled")) {
            boolean enabled = sharedPreferences.getBoolean("notification_deviations_enabled", false);
            if (enabled) {
                Analytics.getInstance(this).event("Settings", "Deviation Service", "start");
            } else {
                Analytics.getInstance(this).event("Settings", "Deviation Service", "stop");
            }
            DeviationService.startAsRepeating(this);
        } else if (key.equals("has_consent_to_serve_personalized_ads")) {
            boolean hasConsent = sharedPreferences.getBoolean("has_consent_to_serve_personalized_ads", false);
            findPreference("preference_see_relevant_ads")
                    .setTitle(hasConsent ? R.string.relevant_ads_enabled : R.string.relevant_ads_disabled);
        }

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("clear_search_history")) {
            onCreateDialog(DIALOG_CLEAR_SEARCH_HISTORY).show();
            return true;
        } else if (preference.getKey().equals("clear_favorites")) {
            onCreateDialog(DIALOG_CLEAR_FAVORITES).show();
            return true;
        } else if (preference.getKey().equals("about_legal")) {
            Intent i = new Intent(SettingsActivity.this, AboutActivity.class);
            startActivity(i);
        } else if (preference.getKey().equals("help_support")) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://kundo.se/org/sthlm-traveling/"));
            startActivity(i);
        } else if (preference.getKey().equals("about_donate")) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.donate_url)));
            startActivity(i);
        } else if (preference.getKey().equals("about_contact")) {
            final Intent emailIntent = new Intent(
                    Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.putExtra(
                    android.content.Intent.EXTRA_EMAIL,
                    new String[]{getString(R.string.send_feedback_email_emailaddress)});
            emailIntent.putExtra(
                    android.content.Intent.EXTRA_SUBJECT,
                    getText(R.string.send_feedback_email_title));
            startActivity(Intent.createChooser(emailIntent,
                    getText(R.string.send_email)));
        } else if (preference.getKey().equals("about_version")) {
            mClickCount++;
            if (mClickCount == 20) {
                ((String) null).trim();
            }
        } else if (preference.getKey().equals("about_ads")) {
            onCreateDialog(DIALOG_WHY_ADS).show();
        }  else if (preference.getKey().equals("preference_see_relevant_ads")) {
            UserConsentForm form = new UserConsentForm(this);
            form.show();
        } else if (preference.getKey().equals("about_privacy_policy")) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.sthlmtraveling.se/privacy.html"));
            startActivity(i);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CLEAR_SEARCH_HISTORY:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.search_clear_history_preference)
                        .setMessage(R.string.search_clear_history_confirm)
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getContentResolver().delete(Journeys.CONTENT_URI,
                                        Journeys.STARRED + " = 0 OR " +
                                                Journeys.STARRED + " IS NULL",
                                        null
                                );

                                mHistoryDbAdapter.deleteAll();
                                Toast.makeText(SettingsActivity.this,
                                        R.string.search_history_cleared,
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getContentResolver().delete(Journeys.CONTENT_URI,
                                        Journeys.STARRED + " = 1", null);
                                Toast.makeText(SettingsActivity.this,
                                        R.string.search_favorites_cleared,
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create();
            case DIALOG_WHY_ADS:
                View adDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ads, null);
                final SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                boolean isDisabled = sharedPreferences.getBoolean("is_ads_disabled", false);
                SwitchCompat switchAds = (SwitchCompat) adDialogView.findViewById(R.id.switch_ads);
                switchAds.setChecked(isDisabled);
                switchAds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            Analytics.getInstance(SettingsActivity.this).event("Ads", "Disabled");
                            sharedPreferences.edit().putBoolean("is_ads_disabled", true).apply();
                        } else {
                            Analytics.getInstance(SettingsActivity.this).event("Ads", "Enabled");
                            sharedPreferences.edit().putBoolean("is_ads_disabled", false).apply();
                        }
                    }
                });

                return new AlertDialog.Builder(this)
                        .setTitle(R.string.ads_title)
                        .setCancelable(true)
                        .setView(adDialogView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .create();
        }

        return null;
    }
}
