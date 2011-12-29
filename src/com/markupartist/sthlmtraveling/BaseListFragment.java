package com.markupartist.sthlmtraveling;

import java.util.Map;

import android.support.v4.app.ListFragment;

import com.flurry.android.FlurryAgent;

public class BaseListFragment extends ListFragment {


    protected void registerEvent(String event) {
        FlurryAgent.onEvent(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

}
