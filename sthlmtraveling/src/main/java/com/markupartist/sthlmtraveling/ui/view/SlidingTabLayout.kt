/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.markupartist.sthlmtraveling.utils.ViewHelper.getDrawableColorInt

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 *
 *
 * To use the component, simply add it to your view hierarchy. Then in your
 * [android.app.Activity] or [Fragment] call
 * [.setViewPager] providing it the ViewPager this layout is being used for.
 *
 *
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via [.setSelectedIndicatorColors]. The
 * alternative is via the [TabColorizer] interface which provides you complete control over
 * which color is used for any individual position.
 *
 *
 * The views used as tabs can be customized by calling [.setCustomTabView],
 * providing the layout ID of your custom layout.
 */
class SlidingTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : HorizontalScrollView(context, attrs, defStyle) {
    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * [.setCustomTabColorizer].
     */
    interface TabColorizer {
        /**
         * @return return the color of the indicator used when `position` is selected.
         */
        fun getIndicatorColor(position: Int): Int
    }

    private val mTitleOffset: Int

    private var mTabViewLayoutId = 0

    @IdRes
    private var mTabViewTextViewId = 0
    private var mDistributeEvenly = false

    private var mViewPager: ViewPager? = null
    private val mContentDescriptions = SparseArray<String?>()
    private var mViewPagerPageChangeListener: OnPageChangeListener? = null

    private val mTabStrip: SlidingTabStrip

    init {
        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false)
        // Make sure that the Tab Strips fills this View
        setFillViewport(true)

        mTitleOffset = (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density).toInt()

        mTabStrip = SlidingTabStrip(context)
        addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    /**
     * Set the custom [TabColorizer] to be used.
     *
     *
     * If you only require simple custmisation then you can use
     * [.setSelectedIndicatorColors] to achieve
     * similar effects.
     */
    fun setCustomTabColorizer(tabColorizer: TabColorizer?) {
        mTabStrip.setCustomTabColorizer(tabColorizer)
    }

    fun setDistributeEvenly(distributeEvenly: Boolean) {
        mDistributeEvenly = distributeEvenly
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same color.
     */
    fun setSelectedIndicatorColors(vararg colors: Int) {
        mTabStrip.setSelectedIndicatorColors(*colors)
    }

    /**
     * Set the [ViewPager.OnPageChangeListener]. When using [SlidingTabLayout] you are
     * required to set any [ViewPager.OnPageChangeListener] through this method. This is so
     * that the layout can update it's scroll position correctly.
     *
     * @see ViewPager.setOnPageChangeListener
     */
    fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        mViewPagerPageChangeListener = listener
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId  id of the [TextView] in the inflated view
     */
    fun setCustomTabView(layoutResId: Int, textViewId: Int) {
        mTabViewLayoutId = layoutResId
        mTabViewTextViewId = textViewId
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    fun setViewPager(viewPager: ViewPager?) {
        mTabStrip.removeAllViews()

        mViewPager = viewPager
        if (viewPager != null) {
            viewPager.setOnPageChangeListener(InternalViewPagerListener())
            populateTabStrip()
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * [.setCustomTabView].
     */
    protected fun createDefaultTabView(context: Context?): TextView {
        val textView = TextView(context)
        textView.setGravity(Gravity.CENTER)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP.toFloat())
        textView.setTypeface(Typeface.DEFAULT_BOLD)
        textView.setLayoutParams(
            LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            )
        )

        val outValue = TypedValue()
        getContext().getTheme().resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue, true
        )
        textView.setBackgroundResource(outValue.resourceId)

        val padding = (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density).toInt()
        textView.setPadding(padding, padding, padding, padding)

        return textView
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * [.setCustomTabView].
     */
    protected fun createImageTabView(context: Context?): ImageView {
        val v = ImageView(context)
        v.setLayoutParams(
            LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            )
        )

        val outValue = TypedValue()
        getContext().getTheme().resolveAttribute(
            android.R.attr.selectableItemBackground,
            outValue, true
        )
        v.setBackgroundResource(outValue.resourceId)

        val padding = (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density).toInt()
        v.setPadding(padding, padding, padding, padding)

        return v
    }

    private fun populateTabStrip() {
        val adapter = mViewPager!!.getAdapter()
        val tabClickListener: OnClickListener = TabClickListener()

        for (i in 0..<adapter!!.getCount()) {
            var tabView: View? = null
            var tabTitleView: TextView? = null
            var tabIconView: ImageView? = null

            if (mTabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView =
                    LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip, false)
                tabTitleView = tabView.findViewById<View?>(mTabViewTextViewId) as TextView?
            }

            if (tabView == null) {
                if (adapter is TabIconProvider) {
                    tabView = createImageTabView(getContext())
                    tabIconView = tabView as ImageView?
                } else {
                    tabView = createDefaultTabView(getContext())
                    tabTitleView = tabView as TextView?
                }
            }

            if (tabTitleView != null) {
                tabTitleView.setText(adapter.getPageTitle(i))
            } else if (tabIconView != null) {
                tabIconView.setImageDrawable(
                    getDrawableColorInt(
                        getContext(), (adapter as TabIconProvider).getIcon(i), Color.WHITE
                    )
                )
            }

            if (mDistributeEvenly) {
                val lp = tabView.getLayoutParams() as LinearLayout.LayoutParams
                lp.width = 0
                lp.weight = 1f
            }

            tabView.setOnClickListener(tabClickListener)
            val desc = mContentDescriptions.get(i, null)
            if (desc != null) {
                tabView.setContentDescription(desc)
            }

            mTabStrip.addView(tabView)
            if (i == mViewPager!!.getCurrentItem()) {
                tabView.setSelected(true)
                //                if (tabView instanceof ImageView) {
//                    //noinspection deprecation
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                        ((ImageView) tabView).setImageAlpha(255);
//                    }
//                }
            }
        }
    }

    fun setContentDescription(i: Int, desc: String?) {
        mContentDescriptions.put(i, desc)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (mViewPager != null) {
            scrollToTab(mViewPager!!.getCurrentItem(), 0)
        }
    }

    private fun scrollToTab(tabIndex: Int, positionOffset: Int) {
        val tabStripChildCount = mTabStrip.getChildCount()
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return
        }

        val selectedChild = mTabStrip.getChildAt(tabIndex)
        if (selectedChild != null) {
            var targetScrollX = selectedChild.getLeft() + positionOffset

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= mTitleOffset
            }

            scrollTo(targetScrollX, 0)
        }
    }

    private inner class InternalViewPagerListener : OnPageChangeListener {
        private var mScrollState = 0

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val tabStripChildCount = mTabStrip.getChildCount()
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return
            }

            mTabStrip.onViewPagerPageChanged(position, positionOffset)

            val selectedTitle = mTabStrip.getChildAt(position)
            val extraOffset =
                if (selectedTitle != null) (positionOffset * selectedTitle.getWidth()).toInt() else
                    0
            scrollToTab(position, extraOffset)

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener!!.onPageScrolled(
                    position, positionOffset,
                    positionOffsetPixels
                )
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            mScrollState = state

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener!!.onPageScrollStateChanged(state)
            }
        }

        override fun onPageSelected(position: Int) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                mTabStrip.onViewPagerPageChanged(position, 0f)
                scrollToTab(position, 0)
            }
            for (i in 0..<mTabStrip.getChildCount()) {
                val v = mTabStrip.getChildAt(i)
                v.setSelected(position == i)

                //                if (v instanceof ImageView) {
//                    //noinspection deprecation
//                    int alpha = position == i ? 255 : IMAGE_ALPHA_NOT_SELECTED;
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                        ((ImageView) v).setImageAlpha(alpha);
//                    }
//                }
            }
            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener!!.onPageSelected(position)
            }
        }
    }

    private inner class TabClickListener : OnClickListener {
        override fun onClick(v: View) {
            for (i in 0..<mTabStrip.getChildCount()) {
                if (v === mTabStrip.getChildAt(i)) {
                    mViewPager!!.setCurrentItem(i)
                    return
                }
            }
        }
    }

    interface TabIconProvider {
        @DrawableRes
        fun getIcon(position: Int): Int
    }

    companion object {
        private const val TITLE_OFFSET_DIPS = 24
        private const val TAB_VIEW_PADDING_DIPS = 12
        private const val TAB_VIEW_TEXT_SIZE_SP = 12
        private const val IMAGE_ALPHA_NOT_SELECTED = 170
    }
}
