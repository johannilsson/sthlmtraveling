package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;

import java.util.Map;

public class BaseFragmentActivity extends SherlockFragmentActivity {

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

    protected ActionBar initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        return actionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            final Intent startIntent = new Intent(this, StartActivity.class);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(startIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
