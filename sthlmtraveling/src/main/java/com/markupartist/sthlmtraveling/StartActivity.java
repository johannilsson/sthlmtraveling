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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.ErrorReporter;

import java.util.ArrayList;
//import com.viewpagerindicator.TitleProvider;

public class StartActivity extends BaseFragmentActivity {
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start);

        registerScreen("Start");

        final PageFragmentAdapter pageAdapter = new PageFragmentAdapter(this, getSupportFragmentManager());

        pageAdapter.addPage(new PageInfo(getString(R.string.search_label), PlannerFragment.class, null, "Planner"));
        pageAdapter.addPage(new PageInfo(getString(R.string.favorites_label), FavoritesFragment.class, null, "Favorites"));
        pageAdapter.addPage(new PageInfo(getString(R.string.departures), SearchDeparturesFragment.class, null, "Search departures"));
        pageAdapter.addPage(new PageInfo(getString(R.string.deviations_label), TrafficStatusFragment.class, null, "Traffic status"));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMarginDrawable(R.color.light_grey);
        mPager.setPageMargin(25);  // TODO: Compensate with denisity to get it right on all screens
        mPager.setAdapter(pageAdapter);
        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        getSupportActionBar().setSelectedNavigationItem(position);
                    }
                }
        );

        //TabPageIndicator tabPageIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        //tabPageIndicator.setViewPager(mPager);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                mPager.setCurrentItem(tab.getPosition());
                Analytics.getInstance(StartActivity.this).registerScreen(
                        pageAdapter.getName(tab.getPosition()));
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }
        };

        ab.addTab(ab.newTab().setIcon(R.drawable.tab_planner).setTabListener(tabListener));
        ab.addTab(ab.newTab().setIcon(R.drawable.tab_favorites).setTabListener(tabListener));
        ab.addTab(ab.newTab().setIcon(R.drawable.tab_departures).setTabListener(tabListener));
        ab.addTab(ab.newTab().setIcon(R.drawable.tab_deviations).setTabListener(tabListener));

        final ErrorReporter reporter = ErrorReporter.getInstance();
        reporter.checkErrorAndReport(this);

        // Start background service.
        DeviationService.startAsRepeating(getApplicationContext());
    }

    public class PageFragmentAdapter extends FragmentPagerAdapter /*implements TitleProvider*/ {

        private ArrayList<PageInfo> mPages = new ArrayList<PageInfo>();
        private FragmentActivity mContext;

        public PageFragmentAdapter(FragmentActivity activity, FragmentManager fm) {
            super(fm);
            mContext = activity;
        }

        public void addPage(PageInfo page) {
            mPages.add(page);
        }

        @Override
        public Fragment getItem(int position) {
            PageInfo page = mPages.get(position);
            return Fragment.instantiate(mContext,
                    page.getFragmentClass().getName(), page.getArgs());
        }

        @Override
        public int getCount() {
            return mPages.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPages.get(position).getTextResource().toUpperCase();
        }

        public String getName(final int position) {
            return mPages.get(position).getName();
        }
    }

    public class PageInfo {
        private String mTextResource;
        private Class<?> mFragmentClass;
        private Bundle mArgs;
        private String mName;

        public PageInfo(final String textResource, final Class<?> fragmentClass,
                        final Bundle args, final String name) {
            mTextResource = textResource;
            mFragmentClass = fragmentClass;
            mArgs = args;
            mName = name;
        }

        public String getTextResource() {
            return mTextResource;
        }

        public Class<?> getFragmentClass() {
            return mFragmentClass;
        }

        public Bundle getArgs() {
            return mArgs;
        }

        public String getName() {
            return mName;
        }
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
}
