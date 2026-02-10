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

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.BidiFormatter
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.data.models.RealTimeState
import com.markupartist.sthlmtraveling.data.models.Route
import com.markupartist.sthlmtraveling.utils.DateTimeUtil
import com.markupartist.sthlmtraveling.utils.DateTimeUtil.formatDetailedDuration
import com.markupartist.sthlmtraveling.utils.LegUtil.getColor
import com.markupartist.sthlmtraveling.utils.LegUtil.getTransportDrawable
import com.markupartist.sthlmtraveling.utils.RtlUtils.isRtl
import com.markupartist.sthlmtraveling.utils.ViewHelper.dipsToPix
import com.markupartist.sthlmtraveling.utils.ViewHelper.tint
import com.markupartist.sthlmtraveling.utils.text.RoundedBackgroundSpan
import java.util.Date
import java.util.Locale

/**
 * Represent a Route
 */
class TripView : LinearLayout {
    private var trip: Route? = null
    private var mShowDivider = true

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setTrip(trip: Route) {
        this.trip = trip
        removeAllViews()
        updateViews()
    }

    fun showDivider(show: Boolean) {
        mShowDivider = show
    }

    fun updateViews() {
        this.setOrientation(VERTICAL)

        val scale = getResources().getDisplayMetrics().density

        this.setPadding(
            getResources().getDimensionPixelSize(R.dimen.list_horizontal_padding),
            getResources().getDimensionPixelSize(R.dimen.list_vertical_padding),
            getResources().getDimensionPixelSize(R.dimen.list_horizontal_padding),
            getResources().getDimensionPixelSize(R.dimen.list_vertical_padding)
        )

        val timeStartEndLayout = LinearLayout(getContext())
        val timeStartEndText = TextView(getContext())
        timeStartEndText.setText(DateTimeUtil.routeToTimeDisplay(getContext(), trip!!))
        timeStartEndText.setTextColor(ContextCompat.getColor(getContext(), R.color.body_text_1))
        timeStartEndText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        ViewCompat.setPaddingRelative(timeStartEndText, 0, 0, 0, (2 * scale).toInt())
        if (isRtl(Locale.getDefault())) {
            timeStartEndText.setTextDirection(TEXT_DIRECTION_RTL)
        }

        // Check if we have Realtime for start and or end.
        var hasRealtime = false
        var transitTime: Pair<Date, RealTimeState> = trip!!.departsAt(true)
        if (transitTime.second != RealTimeState.NOT_SET) {
            hasRealtime = true
        } else {
            transitTime = trip!!.arrivesAt(true)
            if (transitTime.second != RealTimeState.NOT_SET) {
                hasRealtime = true
            }
        }

        //        if (hasRealtime) {
//            ImageView liveDrawable = new ImageView(getContext());
//            liveDrawable.setImageResource(R.drawable.ic_live);
//            ViewCompat.setPaddingRelative(liveDrawable, 0, (int) (2 * scale), (int) (4 * scale), 0);
//            timeStartEndLayout.addView(liveDrawable);
//
//            AlphaAnimation animation1 = new AlphaAnimation(0.4f, 1.0f);
//            animation1.setDuration(600);
//            animation1.setRepeatMode(Animation.REVERSE);
//            animation1.setRepeatCount(Animation.INFINITE);
//
//            liveDrawable.startAnimation(animation1);
//        }
        timeStartEndLayout.addView(timeStartEndText)

        val startAndEndPointLayout = LinearLayout(getContext())
        val startAndEndPoint = TextView(getContext())
        val bidiFormatter = BidiFormatter.getInstance(isRtl(Locale.getDefault()))
        startAndEndPoint.setText(
            String.format(
                "%s – %s",
                bidiFormatter.unicodeWrap(trip!!.fromStop()!!.name),
                bidiFormatter.unicodeWrap(trip!!.toStop()!!.name)
            )
        )

        startAndEndPoint.setTextColor(getResources().getColor(R.color.body_text_1)) // Dark gray
        startAndEndPoint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        if (isRtl(Locale.getDefault())) {
            startAndEndPoint.setTextDirection(TEXT_DIRECTION_RTL)
        }
        ViewCompat.setPaddingRelative(
            startAndEndPoint,
            0,
            (4 * scale).toInt(),
            0,
            (4 * scale).toInt()
        )
        startAndEndPointLayout.addView(startAndEndPoint)

        val timeLayout = RelativeLayout(getContext())

        val routeChanges = LinearLayout(getContext())
        routeChanges.setGravity(Gravity.CENTER_VERTICAL)


        val changesLayoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        changesLayoutParams.gravity = Gravity.CENTER_VERTICAL

        if (trip!!.hasAlertsOrNotes()) {
            val warning = ImageView(getContext())
            warning.setImageResource(R.drawable.ic_warning_black_24dp)
            tint(warning, ContextCompat.getColor(getContext(), R.color.deviation))
            ViewCompat.setPaddingRelative(warning, 0, (4 * scale).toInt(), (4 * scale).toInt(), 0)
            routeChanges.addView(warning)
        }

        var currentTransportCount = 1
        val transportCount = trip!!.legs.size
        for (leg in trip!!.legs) {
            if (!leg.isTransit && transportCount > 3) {
                if (leg.distance < 150) {
                    continue
                }
            }
            if (currentTransportCount > 1 && transportCount >= currentTransportCount) {
                val separator = ImageView(getContext())
                separator.setImageResource(R.drawable.transport_separator)
                tint(separator, ContextCompat.getColor(getContext(), R.color.icon_default))
                ViewCompat.setPaddingRelative(separator, 0, 0, (4 * scale).toInt(), 0)
                separator.setLayoutParams(changesLayoutParams)
                routeChanges.addView(separator)
                if (isRtl(Locale.getDefault())) {
                    ViewCompat.setScaleX(separator, -1f)
                }
            }

            val changeImageView = ImageView(getContext())
            val transportDrawable = getTransportDrawable(getContext(), leg)
            changeImageView.setImageDrawable(transportDrawable)
            if (isRtl(Locale.getDefault())) {
                ViewCompat.setScaleX(changeImageView, -1f)
            }
            ViewCompat.setPaddingRelative(changeImageView, 0, 0, 0, 0)
            changeImageView.setLayoutParams(changesLayoutParams)
            routeChanges.addView(changeImageView)

            if (currentTransportCount <= 3) {
                val lineName = leg.routeShortName
                if (!TextUtils.isEmpty(lineName)) {
                    val lineNumberView = TextView(getContext())
                    lineNumberView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    val roundedBackgroundSpan = RoundedBackgroundSpan(
                        getColor(getContext(), leg),
                        Color.WHITE,
                        dipsToPix(getContext().getResources(), 4f)
                    )
                    val sb = SpannableStringBuilder()
                    sb.append(lineName)
                    sb.append(' ')
                    sb.setSpan(
                        roundedBackgroundSpan,
                        0,
                        lineName!!.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    lineNumberView.setText(sb)

                    ViewCompat.setPaddingRelative(
                        lineNumberView,
                        (5 * scale).toInt(), (1 * scale).toInt(), (4 * scale).toInt(), 0
                    )
                    lineNumberView.setLayoutParams(changesLayoutParams)
                    routeChanges.addView(lineNumberView)
                }
            }

            currentTransportCount++
        }

        val durationText = TextView(getContext())
        durationText.setText(
            formatDetailedDuration(
                getResources(),
                (trip!!.duration * 1000).toLong()
            )
        )
        durationText.setTextColor(ContextCompat.getColor(getContext(), R.color.body_text_1))
        durationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        durationText.setTypeface(Typeface.DEFAULT_BOLD)

        timeLayout.addView(routeChanges)
        timeLayout.addView(durationText)

        val durationTextParams = durationText.getLayoutParams() as RelativeLayout.LayoutParams
        durationTextParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        durationText.setLayoutParams(durationTextParams)

        val divider = View(getContext())
        val dividerParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, 1)
        divider.setLayoutParams(dividerParams)

        this.addView(timeLayout)

        val params = routeChanges.getLayoutParams() as RelativeLayout.LayoutParams
        params.height = LayoutParams.MATCH_PARENT
        params.addRule(RelativeLayout.CENTER_VERTICAL)
        routeChanges.setLayoutParams(params)

        this.addView(startAndEndPointLayout)
        this.addView(timeStartEndLayout)

        if (mShowDivider) {
            this.addView(divider)
        }
    }
}
