package com.markupartist.sthlmtraveling;

import java.util.Map;

import com.actionbarsherlock.app.SherlockListFragment;
import com.flurry.android.FlurryAgent;

public class BaseListFragment extends SherlockListFragment {


    protected void registerEvent(String event) {
        FlurryAgent.onEvent(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

}
