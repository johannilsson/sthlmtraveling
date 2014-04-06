package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.flurry.android.FlurryAgent;

import java.util.Map;

public class BasePreferenceActivity extends PreferenceActivity {
    @Override
    public void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, MyApplication.ANALYTICS_KEY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlurryAgent.onPageView();
    }

    @Override
    public void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
     }

    protected void registerEvent(String event) {
        FlurryAgent.onEvent(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Need to know if we are on the top level, then we should not apply this.
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
