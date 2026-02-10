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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import com.markupartist.sthlmtraveling.ui.view.SlidingTabLayout.TabColorizer

internal class SlidingTabStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val mBottomBorderThickness: Int
    private val mBottomBorderPaint: Paint

    private val mSelectedIndicatorThickness: Int
    private val mSelectedIndicatorPaint: Paint

    private val mDefaultBottomBorderColor: Int

    private var mSelectedPosition = 0
    private var mSelectionOffset = 0f

    private var mCustomTabColorizer: TabColorizer? = null
    private val mDefaultTabColorizer: SimpleTabColorizer

    init {
        setWillNotDraw(false)

        val density = getResources().getDisplayMetrics().density

        val outValue = TypedValue()
        context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true)
        val themeForegroundColor = outValue.data

        mDefaultBottomBorderColor = setColorAlpha(
            themeForegroundColor,
            DEFAULT_BOTTOM_BORDER_COLOR_ALPHA
        )

        mDefaultTabColorizer = SimpleTabColorizer()
        mDefaultTabColorizer.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR)

        mBottomBorderThickness = (DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density).toInt()
        mBottomBorderPaint = Paint()
        mBottomBorderPaint.setColor(mDefaultBottomBorderColor)

        mSelectedIndicatorThickness = (SELECTED_INDICATOR_THICKNESS_DIPS * density).toInt()
        mSelectedIndicatorPaint = Paint()
    }

    fun setCustomTabColorizer(customTabColorizer: TabColorizer?) {
        mCustomTabColorizer = customTabColorizer
        invalidate()
    }

    fun setSelectedIndicatorColors(vararg colors: Int) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null
        mDefaultTabColorizer.setIndicatorColors(*colors)
        invalidate()
    }

    fun onViewPagerPageChanged(position: Int, positionOffset: Float) {
        mSelectedPosition = position
        mSelectionOffset = positionOffset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val height = getHeight()
        val childCount = getChildCount()
        val tabColorizer = (if (mCustomTabColorizer != null)
            mCustomTabColorizer
        else
            mDefaultTabColorizer)!!

        // Thick colored underline below the current selection
        if (childCount > 0) {
            val selectedTitle = getChildAt(mSelectedPosition)
            var left = selectedTitle.getLeft()
            var right = selectedTitle.getRight()
            var color = tabColorizer.getIndicatorColor(mSelectedPosition)

            if (mSelectionOffset > 0f && mSelectedPosition < (getChildCount() - 1)) {
                val nextColor = tabColorizer.getIndicatorColor(mSelectedPosition + 1)
                if (color != nextColor) {
                    color = blendColors(nextColor, color, mSelectionOffset)
                }

                // Draw the selection partway between the tabs
                val nextTitle = getChildAt(mSelectedPosition + 1)
                left = (mSelectionOffset * nextTitle.getLeft() +
                        (1.0f - mSelectionOffset) * left).toInt()
                right = (mSelectionOffset * nextTitle.getRight() +
                        (1.0f - mSelectionOffset) * right).toInt()
            }

            mSelectedIndicatorPaint.setColor(color)

            canvas.drawRect(
                left.toFloat(), (height - mSelectedIndicatorThickness).toFloat(), right.toFloat(),
                height.toFloat(), mSelectedIndicatorPaint
            )
        }

        // Thin underline along the entire bottom edge
        canvas.drawRect(
            0f,
            (height - mBottomBorderThickness).toFloat(),
            getWidth().toFloat(),
            height.toFloat(),
            mBottomBorderPaint
        )
    }

    private class SimpleTabColorizer : TabColorizer {
        private var mIndicatorColors: IntArray = intArrayOf()

        override fun getIndicatorColor(position: Int): Int {
            return mIndicatorColors[position % mIndicatorColors.size]
        }

        fun setIndicatorColors(vararg colors: Int) {
            mIndicatorColors = colors
        }
    }

    companion object {
        private const val DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 0
        private const val DEFAULT_BOTTOM_BORDER_COLOR_ALPHA: Byte = 0x26
        private const val SELECTED_INDICATOR_THICKNESS_DIPS = 3
        private const val DEFAULT_SELECTED_INDICATOR_COLOR = -0xcc4a1b

        /**
         * Set the alpha value of the `color` to be the given `alpha` value.
         */
        private fun setColorAlpha(color: Int, alpha: Byte): Int {
            return Color.argb(
                alpha.toInt(),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }

        /**
         * Blend `color1` and `color2` using the given ratio.
         *
         * @param ratio of which to blend. 1.0 will return `color1`, 0.5 will give an even blend,
         * 0.0 will return `color2`.
         */
        private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRation = 1f - ratio
            val r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation)
            val g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation)
            val b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation)
            return Color.rgb(r.toInt(), g.toInt(), b.toInt())
        }
    }
}
