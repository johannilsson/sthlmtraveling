package com.markupartist.sthlmtraveling;

import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.MenuItem;

import com.flurry.android.FlurryAgent;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.actionbar.R;

public class BaseListFragment extends ListFragment {

    @Override
	public void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(getActivity(), MyApplication.ANALYTICS_KEY);
    }
   

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlurryAgent.onPageView();
    }

    @Override
	public void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(getActivity());
     }

    protected void registerEvent(String event) {
        FlurryAgent.onEvent(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

    protected ActionBar initActionBar(int menuResource) {
        ActionBar actionBar = (ActionBar) getActivity().findViewById(R.id.actionbar);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        getActivity().getMenuInflater().inflate(menuResource, actionBar.asMenu());
        return actionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_item_home:
            final Intent startIntent = new Intent(getActivity(), StartActivity.class);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(startIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
