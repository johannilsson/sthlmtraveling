package com.markupartist.sthlmtraveling;

import com.flurry.android.FlurryAgent;

import android.preference.PreferenceActivity;

public class BasePreferenceActivity extends PreferenceActivity {
    public void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, MyApplication.ANALYTICS_KEY);
     }

     public void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
     }
}
