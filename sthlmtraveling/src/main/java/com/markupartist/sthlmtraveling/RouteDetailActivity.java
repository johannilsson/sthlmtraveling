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

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.models.Alert;
import com.markupartist.sthlmtraveling.data.models.IntermediateStop;
import com.markupartist.sthlmtraveling.data.models.IntermediateResponse;
import com.markupartist.sthlmtraveling.data.models.Leg;
import com.markupartist.sthlmtraveling.data.models.Place;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.data.models.TravelMode;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.SmsTicketDialog;
import com.markupartist.sthlmtraveling.utils.AdProxy;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.LegUtil;
import com.markupartist.sthlmtraveling.utils.RtlUtils;
import com.markupartist.sthlmtraveling.utils.ViewHelper;
import com.markupartist.sthlmtraveling.utils.text.RoundedBackgroundSpan;
import com.markupartist.sthlmtraveling.utils.text.SpanUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class RouteDetailActivity extends BaseListActivity {
    public static final String TAG = "RouteDetailActivity";

    public static final String EXTRA_ROUTE = "sthlmtraveling.intent.extra.ROUTE";
    public static final String EXTRA_JOURNEY_QUERY = "sthlmtraveling.intent.action.JOURNEY_QUERY";

    private static final int DIALOG_BUY_SMS_TICKET = 1;

    private Route mRoute;
    private JourneyQuery mJourneyQuery;
    private SubTripAdapter mSubTripAdapter;

    private ImageButton mFavoriteButton;

    private ActionBar mActionBar;
    private AdProxy mAdProxy;
    private ApiService mApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        registerScreen("Route details");

        Bundle extras = getIntent().getExtras();

        mRoute = (Route) getLastNonConfigurationInstance();
        if (mRoute == null) {
            mRoute = extras.getParcelable(EXTRA_ROUTE);
        }

        mApiService = MyApplication.get(RouteDetailActivity.this).getApiService();

        mJourneyQuery = extras.getParcelable(EXTRA_JOURNEY_QUERY);

        mActionBar = initActionBar();

        updateStartAndEndPointViews(mJourneyQuery);

        View headerView = getLayoutInflater().inflate(R.layout.route_header_details, null);

        if (shouldShowAds(mJourneyQuery.hasPromotions)) {
            Pair<AdProxy.Provider, String> adConf = AppConfig.getAdConfForRouteDetails(mJourneyQuery.promotionNetwork);
            mAdProxy = new AdProxy(this, adConf.first, adConf.second);
        }

        ViewGroup adContainer = null;
        if (mAdProxy != null && AdProxy.Provider.WIDESPACE == mAdProxy.getProvider()) {
            mAdProxy.load();
            adContainer = mAdProxy.getAdWithContainer(getListView(), false);
        }

        TextView timeView = (TextView) headerView.findViewById(R.id.route_date_time);

        BidiFormatter bidiFormatter = BidiFormatter.getInstance(Locale.getDefault());
        String timeStr = getString(R.string.time_to,
                bidiFormatter.unicodeWrap(String.valueOf(DateTimeUtil.formatDetailedDuration(getResources(), mRoute.getDuration() * 1000))),
                bidiFormatter.unicodeWrap(String.valueOf(getLocationName(mJourneyQuery.destination.asPlace()))));
        timeView.setText(timeStr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            timeView.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        }
        if (mRoute.canBuyTicket()) {

            View buySmsTicketView = headerView.findViewById(R.id.route_buy_ticket);
            buySmsTicketView.setVisibility(View.VISIBLE);
            buySmsTicketView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Analytics.getInstance(RouteDetailActivity.this).event("Ticket", "Click on zone");
                    showDialog(DIALOG_BUY_SMS_TICKET);
                }
            });

            TextView zoneView = (TextView) headerView.findViewById(R.id.route_zones);
            zoneView.setText(mRoute.getFare().getZones());
        }

        if (adContainer != null) {
            getListView().addHeaderView(adContainer, null, false);
        }
        getListView().addHeaderView(headerView, null, false);

        onRouteDetailsResult(mRoute);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAdProxy != null) {
            mAdProxy.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAdProxy != null) {
            mAdProxy.onPause();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_route_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem starItem = menu.findItem(R.id.actionbar_item_star);
        if (isStarredJourney(mJourneyQuery)) {
            starItem.setIcon(R.drawable.ic_action_star_on);
        } else {
            starItem.setIcon(R.drawable.ic_action_star_off);
        }
        // Disable the menu action for SMS tickets for now.
        if (!mRoute.canBuyTicket()) {
            menu.removeItem(R.id.actionbar_item_sms);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_item_time:
            Intent departuresIntent = new Intent(this, DeparturesActivity.class);
            Site s = Site.toSite(mRoute.fromStop());
            departuresIntent.putExtra(DeparturesActivity.EXTRA_SITE, s);
            startActivity(departuresIntent);
            return true;
        case R.id.actionbar_item_sms:
            if (mRoute.canBuyTicket()) {
                Analytics.getInstance(this).event("Ticket", "Click on ab");
                showDialog(DIALOG_BUY_SMS_TICKET);
            } else {
                Toast.makeText(this, R.string.sms_ticket_notice_disabled, Toast.LENGTH_LONG).show();
            }
            return true;
        case R.id.actionbar_item_star:
            handleStarAction();
            supportInvalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper that returns the my location text representation. If the {@link Location}
     * is set the accuracy will also be appended.
     * @param location the stop
     * @return a text representation of my location
     */
    private CharSequence getLocationName(Place location) {
        if (location == null) {
            return "Unknown";
        }
        if (location.isMyLocation()) {
            return getText(R.string.my_location);
        }
        return location.getName();
    }

    /**
     * Called before this activity is destroyed, returns the previous details.
     * This data is used if the screen is rotated. Then we don't need to ask for
     * the data again.
     * 
     * @return route details
     */
//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        return mRoute;
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
    }

    /**
     * Restores any local state, if any.
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
    }

    @Override
    protected void onDestroy() {
        if (mAdProxy != null) {
            mAdProxy.onDestroy();
        }
        super.onDestroy();
    }

    /**
     * Called when there is results to display.
     * @param route the route details
     */
    public void onRouteDetailsResult(Route route) {
        getListView().addFooterView(createFooterView(route));
        // Add attributions if dealing with a Google result.
        if (mJourneyQuery.destination.getSource() == Site.SOURCE_GOOGLE_PLACES) {
            View attributionView = getLayoutInflater().inflate(
                    R.layout.trip_row_attribution, null, false);
            getListView().addFooterView(attributionView);
        }

        setListAdapter(mSubTripAdapter);

        mRoute = route;
    }

    private View createFooterView(final Route route) {
        mSubTripAdapter = new SubTripAdapter(this, route.getLegs());

        int numSubTrips = route.getLegs().size();
        final Leg leg = route.getLegs().get(numSubTrips - 1);

        ViewGroup convertView = (ViewGroup) getLayoutInflater().inflate(R.layout.trip_row_stop_layout, null);
        Button nameView = (Button) convertView.findViewById(R.id.trip_stop_title);
        if (TravelMode.FOOT.equals(leg.getTravelMode())) {
            nameView.setText(getLocationName(mJourneyQuery.destination.asPlace()));
        } else {
            nameView.setText(leg.getTo().getName());
        }
        nameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(ViewOnMapActivity.createIntent(RouteDetailActivity.this,
                        mRoute, mJourneyQuery, Site.toSite(leg.getTo())));
            }
        });

        View endSegment = convertView.findViewById(R.id.trip_line_segment_end);
        endSegment.setVisibility(View.VISIBLE);

        convertView.findViewById(R.id.trip_layout_intermediate_stop).setVisibility(View.GONE);

        TextView departureTimeView = (TextView) convertView.findViewById(R.id.trip_departure_time);
        TextView expectedDepartureTimeView = (TextView) convertView.findViewById(R.id.trip_expected_departure_time);
        departureTimeView.setText(DateFormat.getTimeFormat(this).format(leg.getEndTime()));
        if (leg.getEndTimeRt() != null && leg.hasDepartureDelay()) {
            departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            expectedDepartureTimeView.setVisibility(View.VISIBLE);
            expectedDepartureTimeView.setText(DateFormat.getTimeFormat(this).format(leg.getEndTimeRt()));
        } else {
            departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            expectedDepartureTimeView.setVisibility(View.GONE);
        }
        convertView.findViewById(R.id.trip_intermediate_stops_layout).setVisibility(View.GONE);

        return convertView;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_BUY_SMS_TICKET:
            return SmsTicketDialog.createDialog(this, mRoute.getFare().getZones());
        }
        return null;
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    private boolean isStarredJourney(JourneyQuery journeyQuery) {
        String json;
        try {
            json = mJourneyQuery.toJson(false).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to convert journey to a json document.");
            return false;
        }

        String[] projection = new String[] { Journeys.JOURNEY_DATA, };
        Uri uri = Journeys.CONTENT_URI;
        String selection = Journeys.STARRED + " = ? AND " + Journeys.JOURNEY_DATA + " = ?";
        String[] selectionArgs = new String[] { "1", json };
        Cursor cursor = managedQuery(uri, projection, selection, selectionArgs, null);

        return cursor.getCount() > 0;
    }

    private View.OnClickListener mLocationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Site l = (Site) v.getTag();
            if (l.hasLocation()) {
                startActivity(ViewOnMapActivity.createIntent(
                        RouteDetailActivity.this, mRoute, mJourneyQuery, l));
            } else {
                Toast.makeText(RouteDetailActivity.this,
                        "Missing geo data", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void handleStarAction() {
        String json;
        try {
            json = mJourneyQuery.toJson(false).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to convert journey to a json document.");
            return;
        }

        ContentValues values = new ContentValues();
        values.put(Journeys.JOURNEY_DATA, json);
        Uri uri = Journeys.CONTENT_URI;
        String where = Journeys.JOURNEY_DATA + "= ?";
        String[] selectionArgs = new String[] { json };
        if (isStarredJourney(mJourneyQuery)) {
            values.put(Journeys.STARRED, "0");
            getContentResolver().update(
                    uri, values, where, selectionArgs);
        } else {
            values.put(Journeys.STARRED, "1");
            int affectedRows = getContentResolver().update(
                    uri, values, where, selectionArgs);
            if (affectedRows <= 0) {
                values.put(Journeys.STARRED, "1");
                getContentResolver().insert(
                        Journeys.CONTENT_URI, values);
            }
        }
    }

    /**
     * A not at all optimized adapter for showing a route based on a list of sub trips.
     */
    private class SubTripAdapter extends ArrayAdapter<Leg> {
        private LayoutInflater mInflater;
        private boolean mIsFetchingSubTrips;

        public SubTripAdapter(Context context, List<Leg> objects) {
            super(context, R.layout.route_details_row, objects);

            mInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final Leg leg = getItem(position);

            convertView = mInflater.inflate(R.layout.trip_row_stop_layout, parent, false);
            boolean isFirst = false;

            if (position > 0) {
                final Leg previousLeg = getItem(position - 1);
                convertView.findViewById(R.id.trip_intermediate_departure_time).setVisibility(View.GONE);
                TextView descView = (TextView) convertView.findViewById(R.id.trip_intermediate_stop_title);
                descView.setTextSize(12);
                descView.setText(getLocationName(previousLeg.getTo()));
                TextView arrivalView = (TextView) convertView.findViewById(R.id.trip_intermediate_arrival_time);
                arrivalView.setText(DateFormat.getTimeFormat(getContext()).format(previousLeg.getEndTime()));
                TextView expectedArrivalView = (TextView) convertView.findViewById(R.id.trip_intermediate_expected_arrival_time);
                if (previousLeg.getEndTimeRt() != null && previousLeg.hasArrivalDelay()) {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    expectedArrivalView.setVisibility(View.VISIBLE);
                    expectedArrivalView.setText(DateFormat.getTimeFormat(getContext()).format(previousLeg.getEndTimeRt()));
                } else {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                    expectedArrivalView.setVisibility(View.GONE);
                }
            } else {
                convertView.findViewById(R.id.trip_layout_intermediate_stop).setVisibility(View.GONE);
                convertView.findViewById(R.id.trip_line_segment_start).setVisibility(View.VISIBLE);
                isFirst = true;
            }

            Button nameView = (Button) convertView.findViewById(R.id.trip_stop_title);
            boolean shouldUseOriginName = isFirst && TravelMode.FOOT.equals(leg.getTravelMode());
            nameView.setText(shouldUseOriginName ?
                    getLocationName(mJourneyQuery.origin.asPlace()) : getLocationName(leg.getFrom()));
            nameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (leg.getFrom().hasLocation()) {
                        startActivity(ViewOnMapActivity.createIntent(getContext(),
                                mRoute, mJourneyQuery, Site.toSite(leg.getFrom())));
                    } else {
                        Toast.makeText(getContext(), "Missing geo data", Toast.LENGTH_LONG).show();
                    }
                }
            });

            TextView departureTimeView = (TextView) convertView.findViewById(R.id.trip_departure_time);
            TextView expectedDepartureTimeView = (TextView) convertView.findViewById(R.id.trip_expected_departure_time);

            departureTimeView.setText(DateFormat.getTimeFormat(getContext()).format(leg.getStartTime()));
            if (leg.getStartTimeRt() != null && leg.hasDepartureDelay()) {
                departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                expectedDepartureTimeView.setVisibility(View.VISIBLE);
                expectedDepartureTimeView.setText(DateFormat.getTimeFormat(getContext()).format(leg.getStartTimeRt()));
            } else {
                departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                expectedDepartureTimeView.setVisibility(View.GONE);
            }

            // Add description data
            ViewStub descriptionStub = (ViewStub) convertView.findViewById(R.id.trip_description_stub);
            View descriptionLayout = descriptionStub.inflate();
            TextView descriptionView = (TextView) descriptionLayout.findViewById(R.id.trip_description);
            descriptionView.setText(createDescription(leg));
            ImageView descriptionIcon = (ImageView) descriptionLayout.findViewById(R.id.trip_description_icon);
            descriptionIcon.setImageDrawable(LegUtil.getTransportDrawable(getContext(), leg));
            if (RtlUtils.isRtl(Locale.getDefault())) {
                ViewCompat.setScaleX(descriptionIcon, -1f);
            }
            //descriptionView.setCompoundDrawablesWithIntrinsicBounds(subTrip.transport.getImageResource(), 0, 0, 0);

            inflateMessages(leg, convertView);
            inflateIntermediate(leg, position, convertView);

            return convertView;
        }

        private void inflateIntermediate(final Leg leg, final int position, final View convertView) {
            final ToggleButton btnIntermediateStops = (ToggleButton) convertView.findViewById(R.id.trip_btn_intermediate_stops);

            CharSequence durationText = DateTimeUtil.formatDetailedDuration(getResources(), leg.getDuration() * 1000);
            btnIntermediateStops.setText(durationText);
            btnIntermediateStops.setTextOn(durationText);
            btnIntermediateStops.setTextOff(durationText);
            if (TravelMode.FOOT.equals(leg.getTravelMode())) {
                btnIntermediateStops.setVisibility(View.GONE);
            }

            final LinearLayout stopsLayout = (LinearLayout) convertView.findViewById(R.id.trip_intermediate_stops);

            btnIntermediateStops.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        List<IntermediateStop> intermediateStops = leg.getIntermediateStops();
                        if (stopsLayout.getChildCount() == 0
                                && intermediateStops.isEmpty() && !mIsFetchingSubTrips) {
                            mIsFetchingSubTrips = true;

                            List<String> references = new ArrayList<String>();
                            references.add(leg.getDetailRef());
                            mApiService.getIntermediateStops(references, new Callback<IntermediateResponse>() {
                                @Override
                                public void success(IntermediateResponse intermediateResponse, Response response) {
                                    List<IntermediateStop> intermediateStops = intermediateResponse.getStops(leg.getDetailRef());
                                    if (intermediateStops.isEmpty()) {
                                        stopsLayout.addView(inflateText(getText(R.string.no_intermediate_stops), stopsLayout));
                                    } else {
                                        leg.setIntermediateStops(intermediateStops);
                                        for (IntermediateStop is : intermediateStops) {
                                            if (leg.hasStopIndex(is.getLocation().getStopIndex())) {
                                                stopsLayout.addView(inflateIntermediateStop(is, stopsLayout));
                                            }
                                        }
                                    }
                                    mIsFetchingSubTrips = false;
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    mIsFetchingSubTrips = false;
                                }
                            });
                        } else if (stopsLayout.getChildCount() == 0
                                && !intermediateStops.isEmpty()) {
                            for (IntermediateStop is : intermediateStops) {
                                if (leg.hasStopIndex(is.getLocation().getStopIndex())) {
                                    stopsLayout.addView(inflateIntermediateStop(is, stopsLayout));
                                }
                            }
                        }
                        stopsLayout.setVisibility(View.VISIBLE);
                    } else {
                        stopsLayout.setVisibility(View.GONE);
                    }
                }
            });
        }

        private View inflateText(CharSequence text, LinearLayout stopsLayout) {
            View view = mInflater.inflate(R.layout.trip_row_intermediate_stop, stopsLayout, false);
            TextView descView = (TextView) view.findViewById(R.id.trip_intermediate_stop_title);
            descView.setTextSize(12);
            descView.setText(text);
            view.findViewById(R.id.trip_intermediate_line_segment_stop).setVisibility(View.GONE);
            return view;
        }

        private View inflateIntermediateStop(IntermediateStop stop, LinearLayout stopsLayout) {
            View view = mInflater.inflate(R.layout.trip_row_intermediate_stop, stopsLayout, false);
            view.findViewById(R.id.trip_intermediate_departure_time).setVisibility(View.GONE);
            TextView descView = (TextView) view.findViewById(R.id.trip_intermediate_stop_title);
            descView.setTextSize(12);
            descView.setText(stop.getLocation().getName());
            TextView arrivalView = (TextView) view.findViewById(R.id.trip_intermediate_arrival_time);
            TextView expectedArrivalView = (TextView) view.findViewById(R.id.trip_intermediate_expected_arrival_time);

            Date arrivalTime = stop.getTime();
            if (arrivalTime != null) {
                arrivalView.setText(DateFormat.getTimeFormat(getContext()).format(arrivalTime));
                if (stop.getTimeRt() != null && stop.hasDelay()) {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    expectedArrivalView.setVisibility(View.VISIBLE);
                    expectedArrivalView.setText(DateFormat.getTimeFormat(getContext()).format(
                            stop.getTimeRt()));
                } else {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    expectedArrivalView.setVisibility(View.GONE);
                }
            }

            return view;
        }

        private void inflateMessages(final Leg leg, final View convertView) {
            LinearLayout messagesLayout = (LinearLayout) convertView.findViewById(R.id.trip_messages);

            if (leg.hasAlerts()) {
                for (Alert alert : leg.getAlerts()) {
                    messagesLayout.addView(inflateMessage("alert", alert.getHeader(), messagesLayout));
                }
            }
            if (leg.hasNotes()) {
                for (Alert note : leg.getNotes()) {
                    messagesLayout.addView(inflateMessage("rtu", note.getHeader(), messagesLayout));
                }
            }
        }

        private View inflateMessage(String type, String message, LinearLayout messagesLayout) {
            View view = mInflater.inflate(R.layout.trip_row_message, messagesLayout, false);
            TextView messageView = (TextView) view.findViewById(R.id.trip_message);
            messageView.setText(message);
            return view;
        }

        private CharSequence createDescription(final Leg leg) {
            CharSequence description;
            if (TravelMode.FOOT.equals(leg.getTravelMode())) {
                description = getString(R.string.trip_description_walk);
            } else if (TravelMode.BOAT.equals(leg.getTravelMode())) {
                description = getString(R.string.trip_description_normal,
                        leg.getRouteName(),
                        leg.getHeadsing().getName());
                RoundedBackgroundSpan roundedBackgroundSpan = new RoundedBackgroundSpan(
                        LegUtil.getColor(getContext(), leg),
                        Color.WHITE,
                        ViewHelper.dipsToPix(getContext().getResources(), 4));
                Pattern pattern = Pattern.compile(leg.getRouteShortName());
                description = SpanUtils.createSpannable(description, pattern, roundedBackgroundSpan);
            } else {
                String routeName = LegUtil.getRouteName(leg, true);
                description = getString(R.string.trip_description_normal,
                        routeName,
                        leg.getHeadsing().getName());
                RoundedBackgroundSpan roundedBackgroundSpan = new RoundedBackgroundSpan(
                        LegUtil.getColor(getContext(), leg),
                        Color.WHITE,
                        ViewHelper.dipsToPix(getContext().getResources(), 4));
                Pattern pattern = Pattern.compile(routeName);
                description = SpanUtils.createSpannable(description, pattern, roundedBackgroundSpan);
            }
            return description;
        }
    }
}
