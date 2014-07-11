package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.StringUtils;

import java.util.Map;

public class BaseListActivity extends SherlockListActivity {

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

    protected void registerScreen(String event) {
        FlurryAgent.onEvent(event);
        Analytics.getInstance(this).registerScreen(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

    protected ActionBar initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        return actionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /**
     * Update the action bar with start and end points.
     * @param journeyQuery the journey query
     */
    protected void updateStartAndEndPointViews(final JourneyQuery journeyQuery) {
        ActionBar ab = getSupportActionBar();
        if (journeyQuery.origin.isMyLocation()) {
            ab.setTitle(StringUtils.getStyledMyLocationString(this));
        } else {
            ab.setTitle(journeyQuery.origin.getCleanName());
        }

        CharSequence via = null;
        if (journeyQuery.hasVia()) {
            via = journeyQuery.via.getCleanName();
        }
        if (journeyQuery.destination.isMyLocation()) {
            if (via != null) {
                ab.setSubtitle(TextUtils.join(" • ", new CharSequence[]{via, StringUtils.getStyledMyLocationString(this)}));
            } else {
                ab.setSubtitle(StringUtils.getStyledMyLocationString(this));
            }
        } else {
            if (via != null) {
                ab.setSubtitle(TextUtils.join(" • ", new CharSequence[]{via, journeyQuery.destination.getCleanName()}));
            } else {
                ab.setSubtitle(journeyQuery.destination.name);
            }
        }
    }
}
