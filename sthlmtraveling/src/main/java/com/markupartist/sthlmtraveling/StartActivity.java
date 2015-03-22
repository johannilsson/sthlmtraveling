/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;

import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.ui.view.PageFragmentAdapter;
import com.markupartist.sthlmtraveling.ui.view.SlidingTabLayout;
import com.markupartist.sthlmtraveling.utils.BeaconManager;
import com.markupartist.sthlmtraveling.utils.ErrorReporter;

public class StartActivity extends BaseFragmentActivity {
    private ViewPager mPager;
    private SlidingTabLayout mSlidingTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start);

        registerScreen("Start");

        final PageFragmentAdapter pageAdapter = new PageFragmentAdapter(this, getSupportFragmentManager());

        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.search_label),
                PlannerFragment.class, null, R.drawable.ic_action_location_directions_active));
        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.favorites_label),
                FavoritesFragment.class, null, R.drawable.ic_action_star_on_active));
        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.departures),
                SearchDeparturesFragment.class, null, R.drawable.ic_action_time_active));
        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.deviations_label),
                TrafficStatusFragment.class, null, R.drawable.ic_action_deviations_active));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMarginDrawable(R.color.light_grey);
        mPager.setPageMargin(25);  // TODO: Compensate with denisity to get it right on all screens
        mPager.setAdapter(pageAdapter);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        Resources res = getResources();
        mSlidingTabLayout.setSelectedIndicatorColors(res.getColor(R.color.tab_selected_strip));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mPager);

        //TabPageIndicator tabPageIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        //tabPageIndicator.setViewPager(mPager);

        ActionBar ab = getSupportActionBar();
        ab.hide();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        final ErrorReporter reporter = ErrorReporter.getInstance();
        reporter.checkErrorAndReport(this);

        // Start background service.
        DeviationService.startAsRepeating(getApplicationContext());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putParcelable("page_adapter", mPageAdapter.saveState());
    }

    @Override
    public boolean onSearchRequested() {
        if (mPager.getCurrentItem() != 0) {
            mPager.setCurrentItem(0); // Search.
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        BeaconManager.getInstance(this).start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        BeaconManager.getInstance(this).stop();
    }

}
