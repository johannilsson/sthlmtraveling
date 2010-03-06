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
import android.widget.TabHost;

import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.utils.ErrorReporter;

public class StartActivity extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ErrorReporter reporter = ErrorReporter.getInstance();
        reporter.checkErrorAndReport(this);

        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("search")
               .setIndicator(getText(R.string.search_label), 
                       getResources().getDrawable(R.drawable.tab_planner))
               .setContent(new Intent(this, PlannerActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("favorites")
                .setIndicator(getText(R.string.favorites_label), 
                        getResources().getDrawable(R.drawable.tab_favorites))
                .setContent(new Intent(this, FavoritesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        tabHost.addTab(tabHost.newTabSpec("departures")
                .setIndicator(getText(R.string.departures), 
                        getResources().getDrawable(R.drawable.tab_departures))
                .setContent(new Intent(this, SearchDeparturesActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("deviations")
                .setIndicator(getText(R.string.deviations_label), 
                        getResources().getDrawable(R.drawable.tab_deviations))
                .setContent(new Intent(this, DeviationsActivity.class)));

        // Start background service.
        DeviationService.startAsRepeating(getApplicationContext());
    }

}
