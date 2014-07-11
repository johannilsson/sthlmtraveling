package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.flurry.android.FlurryAgent;
import com.markupartist.sthlmtraveling.utils.Analytics;

import java.util.Map;

public class BasePreferenceActivity extends SherlockPreferenceActivity {
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

    protected void registerScreen(String event) {
        FlurryAgent.onEvent(event);
        Analytics.getInstance(this).registerScreen(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        if (parameters == null) {
            FlurryAgent.onEvent(event);
        } else {
            FlurryAgent.onEvent(event, parameters);
        }
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
