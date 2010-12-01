package com.markupartist.sthlmtraveling;

import java.util.Map;

import com.flurry.android.FlurryAgent;

import android.app.ListActivity;
import android.os.Bundle;

public class BaseListActivity extends ListActivity {

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, MyApplication.ANALYTICS_KEY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlurryAgent.onPageView();
    }

    @Override
    protected void onStop() {
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
