package com.markupartist.sthlmtraveling;

import com.flurry.android.FlurryAgent;
import com.google.android.maps.MapActivity;

public abstract class BaseMapActivity extends MapActivity {
    public void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, MyApplication.ANALYTICS_KEY);
     }

     public void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
     }
}
