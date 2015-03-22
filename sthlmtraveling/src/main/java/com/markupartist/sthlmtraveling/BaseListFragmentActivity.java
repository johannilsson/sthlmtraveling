package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.markupartist.sthlmtraveling.utils.Analytics;

import java.util.Map;

public class BaseListFragmentActivity extends ActionBarActivity implements OnItemClickListener {

    private ListView mListView;

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

    protected void registerScreen(String event) {
        Analytics.getInstance(this).registerScreen(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
    }

    protected ActionBar initActionBar(int menuResource) {
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

    /**
     * Sets the list view to the default resource value android.R.id.list. Call this after setContentView
     */
    protected void setDefaultListView() {
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
    }

    public ListView getListView() {
        return mListView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Implement in subclass
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
