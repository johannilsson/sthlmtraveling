package com.markupartist.sthlmtraveling;

import android.app.Activity;

import com.flurry.android.FlurryAgent;

public class BaseActivity extends Activity {
    public void onStart() {
       super.onStart();
       FlurryAgent.onStartSession(this, MyApplication.ANALYTICS_KEY);
    }

    public void onStop() {
       super.onStop();
       FlurryAgent.onEndSession(this);
    }
}
