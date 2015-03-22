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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore;
import com.markupartist.sthlmtraveling.provider.planner.Planner;

import java.util.ArrayList;

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

        LinearLayout timeLayout = new LinearLayout(getContext());

        TextView routeDetail = new TextView(getContext());
        routeDetail.setText(trip.toText());
        routeDetail.setTextColor(Color.BLACK);
        routeDetail.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        routeDetail.setPadding(0, 0, 0, (int)(2 * scale));
        //routeDetail.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        timeLayout.addView(routeDetail);

        if (trip.mt6MessageExist || trip.remarksMessageExist || trip.rtuMessageExist) {
            ImageView warning = new ImageView(getContext());
            warning.setImageResource(R.drawable.ic_trip_deviation);
            warning.setPadding((int)(8 * scale), (int)(16 * scale), 0, 0);

            timeLayout.addView(warning);
        }

        TextView startAndEndPoint = new TextView(getContext());
        startAndEndPoint.setText(trip.origin.name + " — " + trip.destination.name);
        startAndEndPoint.setTextColor(0xFF444444); // Dark gray
        startAndEndPoint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        startAndEndPoint.setPadding(0, (int)(2 * scale), 0, (int)(2 * scale));

        LinearLayout routeChanges = new LinearLayout(getContext());
        routeChanges.setPadding(0, (int) (10 * scale), (int) (5 * scale), 0);
        //routeChanges.setGravity(Gravity.CENTER_VERTICAL);

        int currentTransportCount = 1;

        int transportCount = trip.subTrips.size();
        for (Planner.SubTrip subTrip : trip.subTrips) {
            ImageView change = new ImageView(getContext());
            change.setImageResource(subTrip.transport.getImageResource());
            change.setPadding(0, 0, 0, 0);
            routeChanges.addView(change);

                /*
                RoundRectShape rr = new RoundRectShape(new float[]{6, 6, 6, 6, 6, 6, 6, 6}, null, null);
                ShapeDrawable ds = new ShapeDrawable();
                ds.setShape(rr);
                ds.setColorFilter(transport.getColor(), Mode.SCREEN);
                */

            // Okey, this is _not_ okey!!
            ArrayList<Integer> lineNumbers = new ArrayList<Integer>();
            lineNumbers = DeviationStore.extractLineNumbers(subTrip.transport.name, lineNumbers);
            if (!lineNumbers.isEmpty()) {
                TextView lineNumberView = new TextView(getContext());
                lineNumberView.setTextColor(Color.BLACK);
                lineNumberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                //lineNumberView.setBackgroundDrawable(ds);
                //lineNumberView.setText(transport.getShortName());
                lineNumberView.setText(Integer.toString(lineNumbers.get(0)));
                //lineNumberView.setPadding(7, 2, 7, 2);
                lineNumberView.setPadding((int)(5 * scale), (int)(0 * scale), (int)(2 * scale), (int)(4 * scale));
                routeChanges.addView(lineNumberView);
            } else {

            }

            if (transportCount > currentTransportCount) {
                ImageView separator = new ImageView(getContext());
                separator.setImageResource(R.drawable.transport_separator);
                //separator.setPadding(9, 7, 9, 0);
                separator.setPadding((int)(5 * scale), (int)(5 * scale), (int)(5 * scale), 0);
                routeChanges.addView(separator);
            }

            currentTransportCount++;
        }


        View divider = new View(getContext());
        ViewGroup.LayoutParams dividerParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(dividerParams);
        //divider.setBackgroundResource(R.drawable.abs__list_divider_holo_light);

        this.addView(timeLayout);
        this.addView(startAndEndPoint);
        this.addView(routeChanges);
        if (mShowDivider) {
            this.addView(divider);
        }
    }
}
