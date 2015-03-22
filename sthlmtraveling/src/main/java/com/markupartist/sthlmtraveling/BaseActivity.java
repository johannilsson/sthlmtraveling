package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.markupartist.sthlmtraveling.utils.Analytics;

import java.util.Map;

public class BaseActivity extends ActionBarActivity {

    @Override
    protected void onStart() {
       super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
       super.onStop();
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

    protected void registerScreen(String event) {
        Analytics.getInstance(this).registerScreen(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
    }

}
