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
package com.markupartist.sthlmtraveling.ui.view

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.markupartist.sthlmtraveling.ui.view.SlidingTabLayout.TabIconProvider
import java.util.Collections
import java.util.Locale

/**
 * Created by johan on 18/11/14.
 */
class PageFragmentAdapter(private val mContext: FragmentActivity, fm: FragmentManager) :
    FragmentPagerAdapter(fm), TabIconProvider {
    private val mPages = ArrayList<PageInfo>()

    fun addPage(page: PageInfo?) {
        mPages.add(page!!)
    }

    override fun getItem(position: Int): Fragment {
        val page = mPages.get(position)
        return Fragment.instantiate(
            mContext,
            page.fragmentClass!!.getName(), page.args
        )
    }

    override fun getCount(): Int {
        return mPages.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mPages.get(position).name!!.uppercase(Locale.getDefault())
    }

    fun getName(position: Int): String? {
        return mPages.get(position).name
    }

    @DrawableRes
    override fun getIcon(position: Int): Int {
        return mPages.get(position).icon
    }

    fun updatePageArgs(position: Int, args: Bundle?) {
        mPages.get(position).args = args
    }

    fun getPageArgs(position: Int): Bundle? {
        return mPages.get(position).args
    }

    fun setLayoutDirection(isRtl: Boolean) {
        if (isRtl) {
            Collections.reverse(mPages)
        }
    }

    class PageInfo(
        val name: String?, val fragmentClass: Class<*>?,
        var args: Bundle?, val icon: Int
    )
}