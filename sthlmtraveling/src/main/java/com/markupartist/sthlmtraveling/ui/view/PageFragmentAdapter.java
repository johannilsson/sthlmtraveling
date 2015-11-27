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

package com.markupartist.sthlmtraveling.ui.view;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by johan on 18/11/14.
 */
public class PageFragmentAdapter extends FragmentPagerAdapter implements SlidingTabLayout.TabIconProvider {

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

    @Override
    @DrawableRes
    public int getIcon(int position) {
        return mPages.get(position).getIcon();
    }

    public void updatePageArgs(int position, Bundle args) {
        mPages.get(position).mArgs = args;
    }

    public Bundle getPageArgs(int position) {
        return mPages.get(position).mArgs;
    }

    public void setLayoutDirection(boolean isRtl) {
        if (isRtl) {
            Collections.reverse(mPages);
        }
    }

    public static class PageInfo {
        private String mTextResource;
        private Class<?> mFragmentClass;
        private Bundle mArgs;
        private int mIcon;

        public PageInfo(final String textResource, final Class<?> fragmentClass,
                        final Bundle args, final int icon) {
            mTextResource = textResource;
            mFragmentClass = fragmentClass;
            mArgs = args;
            mIcon = icon;
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
            return mTextResource;
        }

        public int getIcon() {
            return mIcon;
        }
    }
}