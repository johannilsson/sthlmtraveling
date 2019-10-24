package com.markupartist.sthlmtraveling.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentStatus;
import com.markupartist.sthlmtraveling.MyApplication;

import java.net.MalformedURLException;
import java.net.URL;

public class UserConsentForm {
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private ConsentForm form;

    public UserConsentForm(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
    }

    void showIfConsentIsUnknown() {
        if (sharedPreferences.getBoolean("personalized_ads_consent_is_unknown", false)) {
            show();
        }
    }

    void init() {
        URL privacyUrl = null;
        try {
            privacyUrl = new URL("http://sthlmtraveling.se/privacy.html");
        } catch (MalformedURLException e) {
            // ..
        }
        form = new ConsentForm.Builder(context, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        form.show();
                    }

                    @Override
                    public void onConsentFormOpened() {
                        // Consent form was displayed.
                    }

                    @Override
                    public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        // Consent form was closed.
                        updateUserConsent(consentStatus);
                    }

                    @Override
                    public void onConsentFormError(String errorDescription) {
                        // Consent form error.
                        Log.e("UserConstentForm", "Consent form error " + errorDescription);
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build();

        form.load();
    }

    public void updateUserConsent(ConsentStatus consentStatus) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        switch (consentStatus) {
            case UNKNOWN:
                editor.putBoolean("personalized_ads_consent_is_unknown", true);
                break;
            case NON_PERSONALIZED:
                editor.putBoolean("has_consent_to_serve_personalized_ads", false);
                editor.putBoolean("personalized_ads_consent_is_unknown", false);
                break;
            case PERSONALIZED:
                editor.putBoolean("has_consent_to_serve_personalized_ads", true);
                editor.putBoolean("personalized_ads_consent_is_unknown", false);
                break;
        }
        editor.apply();
    }

    public void show() {
        if (form == null) {
            init();
        } else {
            form.show();
        }
    }
}
