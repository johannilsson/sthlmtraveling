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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.text.BidiFormatter;
import android.support.v4.view.ViewCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.utils.RtlUtils;
import com.markupartist.sthlmtraveling.utils.ViewHelper;
import com.markupartist.sthlmtraveling.utils.text.RoundedBackgroundSpan;

import java.util.Locale;

/**
 * Represent a Route
 */
public class TripView extends LinearLayout {
    private Planner.Trip2 trip;
    private boolean mShowDivider = true;

    public TripView(Context context) {
        super(context);
    }

    public TripView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTrip(final Planner.Trip2 trip) {
        this.trip = trip;
        removeAllViews();
        updateViews();
    }

    public void showDivider(boolean show) {
        mShowDivider = show;
    }

    public void updateViews() {
        this.setOrientation(VERTICAL);

        float scale = getResources().getDisplayMetrics().density;

        this.setPadding(
                getResources().getDimensionPixelSize(R.dimen.list_horizontal_padding),
                getResources().getDimensionPixelSize(R.dimen.list_vertical_padding),
                getResources().getDimensionPixelSize(R.dimen.list_horizontal_padding),
                getResources().getDimensionPixelSize(R.dimen.list_vertical_padding));


        LinearLayout timeStartEndLayout = new LinearLayout(getContext());
        TextView timeStartEndText = new TextView(getContext());
        timeStartEndText.setText(trip.toTimeDisplay(getContext()));
        timeStartEndText.setTextColor(getResources().getColor(R.color.body_text_1));
        timeStartEndText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        ViewCompat.setPaddingRelative(timeStartEndText, 0, 0, 0, (int) (2 * scale));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (RtlUtils.isRtl(Locale.getDefault())) {
                timeStartEndText.setTextDirection(View.TEXT_DIRECTION_RTL);
            }
        }
        timeStartEndLayout.addView(timeStartEndText);

        LinearLayout startAndEndPointLayout = new LinearLayout(getContext());
        TextView startAndEndPoint = new TextView(getContext());
        BidiFormatter bidiFormatter = BidiFormatter.getInstance(RtlUtils.isRtl(Locale.getDefault()));
        startAndEndPoint.setText(String.format("%s – %s",
                bidiFormatter.unicodeWrap(trip.origin.getName()),
                bidiFormatter.unicodeWrap(trip.destination.getName())));

        startAndEndPoint.setTextColor(getResources().getColor(R.color.body_text_1)); // Dark gray
        startAndEndPoint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (RtlUtils.isRtl(Locale.getDefault())) {
                startAndEndPoint.setTextDirection(View.TEXT_DIRECTION_RTL);
            }
        }
        ViewCompat.setPaddingRelative(startAndEndPoint, 0, (int) (4 * scale), 0, (int) (4 * scale));
        startAndEndPointLayout.addView(startAndEndPoint);

        RelativeLayout timeLayout = new RelativeLayout(getContext());

        LinearLayout routeChanges = new LinearLayout(getContext());
        routeChanges.setGravity(Gravity.CENTER_VERTICAL);

        int currentTransportCount = 1;

        LinearLayout.LayoutParams changesLayoutParams =
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        changesLayoutParams.gravity = Gravity.CENTER_VERTICAL;
        int transportCount = trip.subTrips.size();
        for (Planner.SubTrip subTrip : trip.subTrips) {
            ImageView changeImageView = new ImageView(getContext());
            Drawable transportDrawable = subTrip.transport.getDrawable(getContext());
            changeImageView.setImageDrawable(transportDrawable);
            if (RtlUtils.isRtl(Locale.getDefault())) {
                ViewCompat.setScaleX(changeImageView, -1f);
            }
            ViewCompat.setPaddingRelative(changeImageView, 0, 0, 0, 0);
            changeImageView.setLayoutParams(changesLayoutParams);
            routeChanges.addView(changeImageView);

            if (currentTransportCount <= 3) {
                String lineName = subTrip.transport.getLineName();
                if (!TextUtils.isEmpty(lineName)) {
                    TextView lineNumberView = new TextView(getContext());
                    lineNumberView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    RoundedBackgroundSpan roundedBackgroundSpan = new RoundedBackgroundSpan(
                            subTrip.transport.getColor(getContext()),
                            Color.WHITE,
                            ViewHelper.dipsToPix(getContext().getResources(), 4));
                    SpannableStringBuilder sb = new SpannableStringBuilder();
                    sb.append(lineName);
                    sb.append(' ');
                    sb.setSpan(roundedBackgroundSpan, 0, lineName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    lineNumberView.setText(sb);

                    ViewCompat.setPaddingRelative(lineNumberView,
                            (int) (5 * scale), (int) (1 * scale), (int) (2 * scale), 0);
                    lineNumberView.setLayoutParams(changesLayoutParams);
                    routeChanges.addView(lineNumberView);
                }
            }

            if (transportCount > currentTransportCount) {
                ImageView separator = new ImageView(getContext());
                separator.setImageResource(R.drawable.transport_separator);
                ViewCompat.setPaddingRelative(separator, (int) (2 * scale), 0, (int) (5 * scale), 0);
                separator.setLayoutParams(changesLayoutParams);
                routeChanges.addView(separator);
                if (RtlUtils.isRtl(Locale.getDefault())) {
                    ViewCompat.setScaleX(separator, -1f);
                }
            }

            currentTransportCount++;
        }

        TextView durationText = new TextView(getContext());
        durationText.setText(trip.getDurationText(getResources()));
        durationText.setTextColor(getResources().getColor(R.color.body_text_1));
        durationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        durationText.setTypeface(Typeface.DEFAULT_BOLD);

        timeLayout.addView(routeChanges);
        timeLayout.addView(durationText);

        RelativeLayout.LayoutParams durationTextParams = (RelativeLayout.LayoutParams) durationText.getLayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            durationTextParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        } else {
            durationTextParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        durationText.setLayoutParams(durationTextParams);

        if (trip.mt6MessageExist || trip.remarksMessageExist || trip.rtuMessageExist) {
            ImageView warning = new ImageView(getContext());
            warning.setImageResource(R.drawable.ic_trip_deviation);
            ViewCompat.setPaddingRelative(warning, (int) (8 * scale), (int) (16 * scale), 0, 0);
            timeLayout.addView(warning);
        }

        View divider = new View(getContext());
        ViewGroup.LayoutParams dividerParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(dividerParams);

        this.addView(timeLayout);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) routeChanges.getLayoutParams();
        params.height = LayoutParams.MATCH_PARENT;
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        routeChanges.setLayoutParams(params);

        this.addView(startAndEndPointLayout);
        this.addView(timeStartEndLayout);
        if (mShowDivider) {
            this.addView(divider);
        }
    }
}
