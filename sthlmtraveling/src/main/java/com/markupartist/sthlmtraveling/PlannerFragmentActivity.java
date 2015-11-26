package com.markupartist.sthlmtraveling;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class PlannerFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.planner_list);

        if (null == savedInstanceState) {
            PlannerFragment plannerFragment = new PlannerFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, plannerFragment)
                    .commit();
        }
    }
}
