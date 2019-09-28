package com.markupartist.sthlmtraveling;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.TaskStackBuilder;

public class DeparturesShortcutProxyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        Intent startIntent = new Intent(getApplicationContext(), StartActivity.class);
        if (!intent.hasExtra(DeparturesActivity.EXTRA_SITE_NAME)) {
            startActivity(startIntent);
        }

        Intent departuresIntent = new Intent(getApplicationContext(), DeparturesActivity.class);
        departuresIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME,
                intent.getStringExtra(DeparturesActivity.EXTRA_SITE_NAME));

        Intent[] pendingIntents =
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(startIntent)
                        .addNextIntentWithParentStack(departuresIntent)
                        .getIntents();
        startActivities(pendingIntents);
    }
}
