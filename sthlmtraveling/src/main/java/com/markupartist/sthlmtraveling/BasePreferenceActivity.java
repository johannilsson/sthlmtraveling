package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.markupartist.sthlmtraveling.utils.Analytics;

public class BasePreferenceActivity extends PreferenceActivity {
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
     }

    protected void registerScreen(String event) {
        Analytics.getInstance(this).registerScreen(event);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
