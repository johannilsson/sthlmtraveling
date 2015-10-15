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
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;

import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.ui.view.PageFragmentAdapter;
import com.markupartist.sthlmtraveling.ui.view.SlidingTabLayout;
import com.markupartist.sthlmtraveling.utils.RtlUtils;

import java.util.Locale;

public class StartActivity extends BaseFragmentActivity {
    private static final int PAGE_SEARCH_POS = 0;
    private static final int PAGE_FAVORITES_POS = 1;
    private static final int PAGE_DEPARTURES_POS = 2;
    private static final int PAGE_DEVIATIONS_POS = 3;

    private static final int PAGE_RTL_SEARCH_POS = 3;
    private static final int PAGE_RTL_FAVORITES_POS = 2;
    private static final int PAGE_RTL_DEPARTURES_POS = 1;
    private static final int PAGE_RTL_DEVIATIONS_POS = 0;

    private ViewPager mPager;
    private SlidingTabLayout mSlidingTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start);

        registerScreen("Start");
        registerScreen("Planner"); // Planner is the first tab so register it.

        final PageFragmentAdapter pageAdapter = new PageFragmentAdapter(this, getSupportFragmentManager());

        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.search_label),
                PlannerFragment.class, null, R.drawable.ic_action_location_directions_active));
        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.favorites_label),
                FavoritesFragment.class, null, R.drawable.ic_action_star_on_active));
        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.departures),
                SearchDeparturesFragment.class, null, R.drawable.ic_action_time_active));
        pageAdapter.addPage(new PageFragmentAdapter.PageInfo(getString(R.string.deviations_label),
                TrafficStatusFragment.class, null, R.drawable.ic_action_deviations_active));

        pageAdapter.setLayoutDirection(RtlUtils.isRtl(Locale.getDefault()));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMarginDrawable(R.color.light_grey);
        mPager.setPageMargin(25);  // TODO: Compensate with denisity to get it right on all screens
        mPager.setAdapter(pageAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                int pos = getPagePos(position);
                switch (pos) {
                    case PAGE_SEARCH_POS:
                        registerScreen("Planner");
                        break;
                    case PAGE_FAVORITES_POS:
                        registerScreen("Favorites");
                        break;
                    case PAGE_DEPARTURES_POS:
                        registerScreen("Search departures");
                        break;
                    case PAGE_DEVIATIONS_POS:
                        registerScreen("Traffic status");
                        break;
                }
            }
        });

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        Resources res = getResources();
        mSlidingTabLayout.setSelectedIndicatorColors(res.getColor(R.color.tab_selected_strip));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mPager);

        // We force the tab layout to LTR and instead reverse the order for RTL languages.
        ViewCompat.setLayoutDirection(mSlidingTabLayout, ViewCompat.LAYOUT_DIRECTION_LTR);

        mPager.setCurrentItem(getPagePos(PAGE_SEARCH_POS));

        ActionBar ab = getSupportActionBar();
        ab.hide();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        // Start background service.
        DeviationService.startAsRepeating(getApplicationContext());
    }

    public int getPagePos(int pos) {
        if (RtlUtils.isRtl(Locale.getDefault())) {
            switch (pos) {
                case PAGE_SEARCH_POS:
                    return PAGE_RTL_SEARCH_POS;
                case PAGE_FAVORITES_POS:
                    return PAGE_RTL_FAVORITES_POS;
                case PAGE_DEPARTURES_POS:
                    return PAGE_RTL_DEPARTURES_POS;
                case PAGE_DEVIATIONS_POS:
                    return PAGE_RTL_DEVIATIONS_POS;
            }
        }
        return pos;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putParcelable("page_adapter", mPageAdapter.saveState());
    }

    @Override
    public boolean onSearchRequested() {
        int currentPos = mPager.getCurrentItem();
        if (currentPos != getPagePos(PAGE_SEARCH_POS)) {
            mPager.setCurrentItem(getPagePos(PAGE_SEARCH_POS));
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
