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

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.sthlmtraveling.service.DeviationService;
import com.markupartist.sthlmtraveling.utils.ErrorReporter;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

public class StartActivity extends BaseFragmentActivity {
    private PageFragmentAdapter mPageAdapter;
    private ViewPager mPager;
    private TabPageIndicator mIndicator;

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

        mPageAdapter = new PageFragmentAdapter(this, getSupportFragmentManager());

        mPageAdapter.addPage(new PageInfo(
                getString(R.string.search_label), PlannerFragment.class, null));
        mPageAdapter.addPage(new PageInfo(
                getString(R.string.favorites_label), FavoritesFragment.class, null));
        mPageAdapter.addPage(new PageInfo(
                getString(R.string.departures), SearchDeparturesFragment.class, null));
        mPageAdapter.addPage(new PageInfo(
                getString(R.string.deviations_label), TrafficStatusFragment.class, null));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPageAdapter);

        mIndicator = (TabPageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        // Start background service.
        DeviationService.startAsRepeating(getApplicationContext());
    }

    public class PageFragmentAdapter extends FragmentPagerAdapter implements TitleProvider {

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
        public String getTitle(int position) {
            return mPages.get(position).getTextResource().toUpperCase();
        }

    }

    public class PageInfo {
        private String mTextResource;
        private Class<?> mFragmentClass;
        private Bundle mArgs;

        public PageInfo(String textResource, Class<?> fragmentClass,
                Bundle args) {
            mTextResource = textResource;
            mFragmentClass = fragmentClass;
            mArgs = args;
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
