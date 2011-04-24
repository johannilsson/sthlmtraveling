package com.markupartist.sthlmtraveling;

import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.flurry.android.FlurryAgent;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.actionbar.R;

public class BaseListActivity extends ListActivity {

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

    protected void registerEvent(String event) {
        FlurryAgent.onEvent(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
        FlurryAgent.onEvent(event, parameters);
    }

    protected ActionBar initActionBar() {
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        
        /*actionBar.setHomeAction(new IntentAction(this,
                new Intent(this, StartActivity.class),
                R.drawable.ic_actionbar_home_default));*/
        final Intent startIntent = new Intent(this, StartActivity.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        actionBar.setOnTitleClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(startIntent);
            }
        });

        return actionBar;
    }
}
