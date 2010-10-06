package com.markupartist.sthlmtraveling;

import java.util.Map;

import android.app.Activity;
import android.os.Bundle;

import com.flurry.android.FlurryAgent;

public class BaseActivity extends Activity {

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
}
