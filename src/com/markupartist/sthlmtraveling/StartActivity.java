/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.utils.ErrorReporter;

public class StartActivity extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start);

        ActionBar ab = (ActionBar) findViewById(R.id.actionbar);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayUseLogoEnabled(true);
        ab.setHomeLogo(R.drawable.logo);
        ab.setTitle(R.string.app_name);

        final ErrorReporter reporter = ErrorReporter.getInstance();
        reporter.checkErrorAndReport(this);

        final TabHost tabHost = getTabHost();
        tabHost.setup();
        //tabHost.setBackgroundColor(Color.WHITE);
        //tabHost.getTabWidget().setBackgroundColor(Color.BLACK);

        tabHost.addTab(tabHost.newTabSpec("search")
               .setIndicator(buildIndicator(R.string.search_label)/*, 
                       getResources().getDrawable(R.drawable.tab_planner)*/)
               .setContent(new Intent(this, PlannerActivity.class)
               .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        tabHost.addTab(tabHost.newTabSpec("favorites")
                .setIndicator(buildIndicator(R.string.favorites_label)/*,
                        getResources().getDrawable(R.drawable.tab_favorites)*/)
                .setContent(new Intent(this, FavoritesFragmentActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        tabHost.addTab(tabHost.newTabSpec("departures")
                .setIndicator(buildIndicator(R.string.departures)/*, 
                        getResources().getDrawable(R.drawable.tab_departures)*/)
                .setContent(new Intent(this, SearchDeparturesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        tabHost.addTab(tabHost.newTabSpec("deviations")
                .setIndicator(buildIndicator(R.string.deviations_label)/*, 
                        getResources().getDrawable(R.drawable.tab_deviations)*/)
                .setContent(new Intent(this, TrafficStatusActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        // Start background service.
        DeviationService.startAsRepeating(getApplicationContext());
    }
    
    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested string resource as
     * its label.
     *
     * @param textRes
     * @return View
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater()
                .inflate(R.layout.tab_indicator,
                        (ViewGroup) findViewById(android.R.id.tabs), false);
        indicator.setText(getString(textRes).toUpperCase());
        return indicator;
    }
}
