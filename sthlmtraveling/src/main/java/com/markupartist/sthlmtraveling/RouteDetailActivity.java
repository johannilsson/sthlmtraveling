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
import android.support.design.widget.TabLayout;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v4.text.BidiFormatter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.TextUtils;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.models.Alert;
import com.markupartist.sthlmtraveling.data.models.IntermediateResponse;
import com.markupartist.sthlmtraveling.data.models.IntermediateStop;
import com.markupartist.sthlmtraveling.data.models.Leg;
import com.markupartist.sthlmtraveling.data.models.Place;
import com.markupartist.sthlmtraveling.data.models.RealTimeState;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.data.models.TravelMode;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.models.LegViewModel;
import com.markupartist.sthlmtraveling.ui.view.TicketDialogFragment;
import com.markupartist.sthlmtraveling.utils.AdProxy;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.LegUtil;
import com.markupartist.sthlmtraveling.utils.Monitor;
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
    private static final int TAB_CAP = 10;
    private static final String STATE_LEGS = "sthlmtraveling.intent.state.LEGS";
    private static TabDetails mTabDetails[] = new TabDetails[TAB_CAP];

    private Route mRoute;
    private JourneyQuery mJourneyQuery;
    private SubTripAdapter mSubTripAdapter;
    private ImageButton mFavoriteButton;
    private ActionBar mActionBar;
    private AdProxy mAdProxy;
    private ApiService mApiService;
    private Monitor mMonitor;
    private View mFooterView;
    private TabLayout mTabLayout;
    private TextView mTimeView;
    private Button mNameView;
    private Menu mMenuAbove;
    private String mTimeDestination;


//Tab functionality by Oskar Hahr
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        registerScreen("Route details");

        Bundle extras = getIntent().getExtras();

        mRoute = extras.getParcelable(EXTRA_ROUTE);

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
        mTimeView = timeView;
        /*changed into a separate function to be able to call it when changing tabs
        //this is now located in the function tripTimeDestinationUpdater
        //Oskar Hahr

        BidiFormatter bidiFormatter = BidiFormatter.getInstance(Locale.getDefault());

        String timeStr = getString(R.string.time_to,
                bidiFormatter.unicodeWrap(String.valueOf(DateTimeUtil.formatDetailedDuration(getResources(), mRoute.getDuration() * 1000))),
                bidiFormatter.unicodeWrap(String.valueOf(getLocationName(mJourneyQuery.destination.asPlace()))));

        timeView.setText(timeStr);
        */
        //tripTimeDestinationUpdater(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            timeView.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        }
        //Unnecessary check, since SMS TICKETS don't exist
        /*
        if (mRoute.canBuyTicket()) {

            View buySmsTicketView = headerView.findViewById(R.id.route_buy_ticket);
            buySmsTicketView.setVisibility(View.VISIBLE);
            buySmsTicketView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Analytics.getInstance(RouteDetailActivity.this).event("Ticket", "Click on zone");
                    TicketDialogFragment fragment = TicketDialogFragment.create(
                            mRoute.getFare());
                    fragment.show(getSupportFragmentManager(), null);
                }
            });

            TextView zoneView = (TextView) headerView.findViewById(R.id.route_zones);
            zoneView.setText(mRoute.getFare().getZones());
        }
        */

        if (adContainer != null) {
            getListView().addHeaderView(adContainer, null, false);
        }
        getListView().addHeaderView(headerView, null, false);

        mSubTripAdapter = new SubTripAdapter(this);
        if (savedInstanceState == null) {
            onRouteDetailsResult(mRoute);
        }



        mTabLayout = (this.findViewById(R.id.rDetails_Tab));
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                changeTabColor(tab,true);
                swapTabDetails();

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                changeTabColor(tab,false);
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        if(savedInstanceState == null)
            createTabs(true);
        else
            createTabs(false);

        mMonitor = new Monitor() {
            @Override
            public void handleUpdate() {
                super.handleUpdate();
                List<String> references = new ArrayList<>();

                long nowMillis = System.currentTimeMillis();
                for (Leg leg : mRoute.getLegs()) {
                    if (leg.shouldRefresh(nowMillis)) {
                        references.add(leg.getDetailRef());
                    }
                }
                if (references.isEmpty()) {
                    onStop();
                    return;
                }

                mApiService.getIntermediateStops(references, new Callback<IntermediateResponse>() {
                    @Override
                    public void success(IntermediateResponse intermediateResponse, Response response) {
                        updateStopTimes(intermediateResponse);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "No response for stop times");
                    }
                });
            }
        };

    }

    void updateStopTimes(IntermediateResponse intermediateResponse) {
        boolean updated = false;

        ArrayList<LegViewModel> legViewModels = new ArrayList<>();
        for (int pos = 0; pos < mSubTripAdapter.getCount(); pos++) {
            legViewModels.add(mSubTripAdapter.getItem(pos));
        }

        for (LegViewModel leg : legViewModels) {
            String reference = leg.leg.getDetailRef();
            if (reference != null) {
                List<IntermediateStop> stopTimes = intermediateResponse.getStops(leg.leg.getDetailRef());
                boolean applied = leg.leg.updateTimes(stopTimes);
                if (applied) {
                    updated = true;
                }
            }
        }

        if (updated) {
            mSubTripAdapter.setLegs(legViewModels);
            updateFooterView(legViewModels);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAdProxy != null) {
            mAdProxy.onResume();
        }

        mMonitor.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAdProxy != null) {
            mAdProxy.onPause();
        }

        mMonitor.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenuAbove = menu;
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
                Site s = Site.toSite(mRoute.fromStop()); //Changed to handle tabs
                departuresIntent.putExtra(DeparturesActivity.EXTRA_SITE, s);                    //Oskar Hahr
                startActivity(departuresIntent);
                return true;
            case R.id.actionbar_item_star:
                handleStarAction();
                supportInvalidateOptionsMenu();
                return true;
            /** Blenda: **/
            case R.id.actionbar_item_alarm:
                Intent intent = new Intent(this, AlarmPreferencesActivity.class);
                intent.putExtra("ParceableTest", mRoute);
                startActivity(intent);
                return true;
            /***/
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

    private void tripTimeDestinationUpdater(){
        BidiFormatter bidiFormatter = BidiFormatter.getInstance(Locale.getDefault());

        String timeStr = getString(R.string.time_to,
                bidiFormatter.unicodeWrap(String.valueOf(DateTimeUtil.formatDetailedDuration(getResources(), mRoute.getDuration() * 1000))),
                bidiFormatter.unicodeWrap(String.valueOf(getLocationName(mJourneyQuery.destination.asPlace()))));
        mTimeView.setText(timeStr);
        }

    private String timeDestinationString(){
        BidiFormatter bidiFormatter = BidiFormatter.getInstance(Locale.getDefault());

        String timeStr = getString(R.string.time_to,
                bidiFormatter.unicodeWrap(String.valueOf(DateTimeUtil.formatDetailedDuration(getResources(), mRoute.getDuration() * 1000))),
                bidiFormatter.unicodeWrap(String.valueOf(getLocationName(mJourneyQuery.destination.asPlace()))));
        return timeStr;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mRoute = savedInstanceState.getParcelable(EXTRA_ROUTE);
        ArrayList<LegViewModel> legs = savedInstanceState.getParcelableArrayList(STATE_LEGS);
        showLegs(legs);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(EXTRA_ROUTE, mRoute);
        ArrayList<LegViewModel> legViewModels = new ArrayList<>();
        for (int pos = 0; pos < mSubTripAdapter.getCount(); pos++) {
            legViewModels.add(mSubTripAdapter.getItem(pos));
        }
        outState.putParcelableArrayList(STATE_LEGS, legViewModels);

        super.onSaveInstanceState(outState);
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
        List<LegViewModel> legs = new ArrayList<>();
        for (Leg leg : route.getLegs()) {
            legs.add(new LegViewModel(leg));
        }
        showLegs(legs);
        mRoute = route;
    }

    public void showLegs(List<LegViewModel> legs) {
        mSubTripAdapter.setLegs(legs);
        mFooterView = createFooterView(legs);
        //changing things
        getListView().addFooterView(mFooterView);
        // Add attributions if dealing with a Google result.
        if (mJourneyQuery.destination.getSource() == Site.SOURCE_GOOGLE_PLACES) {
            View attributionView = getLayoutInflater().inflate(
                    R.layout.trip_row_attribution, null, false);
            getListView().addFooterView(attributionView);
        }
        setListAdapter(mSubTripAdapter);

    }

    void updateFooterView(final List<LegViewModel> legs) {
        int numSubTrips = legs.size();
        final LegViewModel legViewModel = legs.get(numSubTrips - 1);

        TextView departureTimeView = (TextView) mFooterView.findViewById(R.id.trip_departure_time);
        TextView expectedDepartureTimeView = (TextView) mFooterView.findViewById(R.id.trip_expected_departure_time);
        departureTimeView.setText(DateFormat.getTimeFormat(this).format(legViewModel.leg.getEndTime()));
        if (legViewModel.leg.getEndTimeRt() != null && legViewModel.leg.hasDepartureDelay()) {
            departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            expectedDepartureTimeView.setVisibility(View.VISIBLE);
            expectedDepartureTimeView.setText(DateFormat.getTimeFormat(this).format(legViewModel.leg.getEndTimeRt()));
            ViewHelper.setTextColorForTimeView(expectedDepartureTimeView, legViewModel.leg, false);
        } else {
            departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            ViewHelper.setTextColorForTimeView(departureTimeView, legViewModel.leg, false);
            expectedDepartureTimeView.setVisibility(View.GONE);
        }
    }

    void updateFooterView(final LegViewModel legViewModel) {
        if(mFooterView != null){
            TextView departureTimeView = (TextView) mFooterView.findViewById(R.id.trip_departure_time);
            TextView expectedDepartureTimeView = (TextView) mFooterView.findViewById(R.id.trip_expected_departure_time);
            departureTimeView.setText(DateFormat.getTimeFormat(this).format(legViewModel.leg.getEndTime()));
            if (legViewModel.leg.getEndTimeRt() != null && legViewModel.leg.hasDepartureDelay()) {
                departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                expectedDepartureTimeView.setVisibility(View.VISIBLE);
                expectedDepartureTimeView.setText(DateFormat.getTimeFormat(this).format(legViewModel.leg.getEndTimeRt()));
                ViewHelper.setTextColorForTimeView(expectedDepartureTimeView, legViewModel.leg, false);
            } else {
                departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                ViewHelper.setTextColorForTimeView(departureTimeView, legViewModel.leg, false);
                expectedDepartureTimeView.setVisibility(View.GONE);
            }
        }
    }


    private View createFooterView(final List<LegViewModel> legs) {
        int numSubTrips = legs.size();
        final LegViewModel legViewModel = legs.get(numSubTrips - 1);

        ViewGroup convertView = (ViewGroup) getLayoutInflater().inflate(R.layout.trip_row_stop_layout, null);
        mNameView = (Button) convertView.findViewById(R.id.trip_stop_title);
        if (TravelMode.FOOT.equals(legViewModel.leg.getTravelMode())) {
            mNameView.setText(getLocationName(mJourneyQuery.destination.asPlace()));
        } else {
            mNameView.setText(legViewModel.leg.getTo().getName());
        }
        mNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*startActivity(ViewOnMapActivity.createIntent(RouteDetailActivity.this,
                        mRoute, mJourneyQuery, Site.toSite(legViewModel.leg.getTo())));
                changed this function to be able to handle tabs
                Oskar Hahr
                */
                startActivity(ViewOnMapActivity.createIntent(RouteDetailActivity.this,
                        mRoute, mJourneyQuery, mJourneyQuery.destination));
            }
        });

        View endSegment = convertView.findViewById(R.id.trip_line_segment_end);
        endSegment.setVisibility(View.VISIBLE);

        convertView.findViewById(R.id.trip_layout_intermediate_stop).setVisibility(View.GONE);

        TextView departureTimeView = (TextView) convertView.findViewById(R.id.trip_departure_time);
        TextView expectedDepartureTimeView = (TextView) convertView.findViewById(R.id.trip_expected_departure_time);
        departureTimeView.setText(DateFormat.getTimeFormat(this).format(legViewModel.leg.getEndTime()));
        if (legViewModel.leg.getEndTimeRt() != null && legViewModel.leg.hasDepartureDelay()) {
            departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            expectedDepartureTimeView.setVisibility(View.VISIBLE);
            expectedDepartureTimeView.setText(DateFormat.getTimeFormat(this).format(legViewModel.leg.getEndTimeRt()));
            ViewHelper.setTextColorForTimeView(expectedDepartureTimeView, legViewModel.leg, false);
        } else {
            departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            ViewHelper.setTextColorForTimeView(departureTimeView, legViewModel.leg, false);
            expectedDepartureTimeView.setVisibility(View.GONE);
        }
        convertView.findViewById(R.id.trip_intermediate_stops_layout).setVisibility(View.GONE);

        return convertView;
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
    private class SubTripAdapter extends ArrayAdapter<LegViewModel> {
        private LayoutInflater mInflater;
        private boolean mIsFetchingSubTrips;

        public SubTripAdapter(Context context) {
            super(context, R.layout.route_details_row);
            mInflater = LayoutInflater.from(context);
        }

        public void setLegs(List<LegViewModel> legs) {
            clear();
            for (LegViewModel leg : legs) {
                add(leg);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final LegViewModel legViewModel = getItem(position);

            convertView = mInflater.inflate(R.layout.trip_row_stop_layout, parent, false);
            boolean isFirst = false;

            if (position > 0) {
                final LegViewModel previousLeg = getItem(position - 1);
                convertView.findViewById(R.id.trip_intermediate_departure_time).setVisibility(View.GONE);
                TextView descView = (TextView) convertView.findViewById(R.id.trip_intermediate_stop_title);
                descView.setTextSize(12);
                descView.setText(getLocationName(previousLeg.leg.getTo()));
                TextView arrivalView = (TextView) convertView.findViewById(R.id.trip_intermediate_arrival_time);
                arrivalView.setText(DateFormat.getTimeFormat(getContext()).format(previousLeg.leg.getEndTime()));
                TextView expectedArrivalView = (TextView) convertView.findViewById(R.id.trip_intermediate_expected_arrival_time);

                if (previousLeg.leg.getEndTimeRt() != null && previousLeg.leg.hasArrivalDelay()) {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    expectedArrivalView.setVisibility(View.VISIBLE);
                    expectedArrivalView.setText(DateFormat.getTimeFormat(getContext()).format(previousLeg.leg.getEndTimeRt()));
                    ViewHelper.setTextColorForTimeView(expectedArrivalView, previousLeg.leg, false);
                } else {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                    ViewHelper.setTextColorForTimeView(arrivalView, previousLeg.leg, false);
                    expectedArrivalView.setVisibility(View.GONE);
                }
            } else {
                convertView.findViewById(R.id.trip_layout_intermediate_stop).setVisibility(View.GONE);
                convertView.findViewById(R.id.trip_line_segment_start).setVisibility(View.VISIBLE);
                isFirst = true;
            }

            Button nameView = (Button) convertView.findViewById(R.id.trip_stop_title);
            boolean shouldUseOriginName = isFirst && TravelMode.FOOT.equals(legViewModel.leg.getTravelMode());
            nameView.setText(shouldUseOriginName ?
                    getLocationName(mJourneyQuery.origin.asPlace()) : getLocationName(legViewModel.leg.getFrom()));
            nameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (legViewModel.leg.getFrom().hasLocation()) {
                        startActivity(ViewOnMapActivity.createIntent(getContext(),
                                mRoute, mJourneyQuery, Site.toSite(legViewModel.leg.getFrom())));
                    } else {
                        Toast.makeText(getContext(), "Missing geo data", Toast.LENGTH_LONG).show();
                    }
                }
            });

            TextView departureTimeView = (TextView) convertView.findViewById(R.id.trip_departure_time);
            TextView expectedDepartureTimeView = (TextView) convertView.findViewById(R.id.trip_expected_departure_time);

            departureTimeView.setText(DateFormat.getTimeFormat(getContext()).format(legViewModel.leg.getStartTime()));
            if (legViewModel.leg.getStartTimeRt() != null && legViewModel.leg.hasDepartureDelay()) {
                departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                expectedDepartureTimeView.setVisibility(View.VISIBLE);
                expectedDepartureTimeView.setText(DateFormat.getTimeFormat(getContext()).format(legViewModel.leg.getStartTimeRt()));
                ViewHelper.setTextColorForTimeView(expectedDepartureTimeView, legViewModel.leg, true);
            } else {
                departureTimeView.setPaintFlags(departureTimeView.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                ViewHelper.setTextColorForTimeView(departureTimeView, legViewModel.leg, true);
                expectedDepartureTimeView.setVisibility(View.GONE);
            }

            // Add description data
            ViewStub descriptionStub = (ViewStub) convertView.findViewById(R.id.trip_description_stub);
            View descriptionLayout = descriptionStub.inflate();
            TextView descriptionView = (TextView) descriptionLayout.findViewById(R.id.trip_description);
            descriptionView.setText(createDescription(legViewModel));
            ImageView descriptionIcon = (ImageView) descriptionLayout.findViewById(R.id.trip_description_icon);
            descriptionIcon.setImageDrawable(LegUtil.getTransportDrawable(getContext(), legViewModel.leg));
            if (RtlUtils.isRtl(Locale.getDefault())) {
                ViewCompat.setScaleX(descriptionIcon, -1f);
            }
            //descriptionView.setCompoundDrawablesWithIntrinsicBounds(subTrip.transport.getImageResource(), 0, 0, 0);

            inflateMessages(legViewModel, convertView);
            inflateIntermediate(legViewModel, position, convertView);

            return convertView;
        }

        private void inflateIntermediate(final LegViewModel legViewModel, final int position, final View convertView) {
            final ToggleButton btnIntermediateStops = (ToggleButton) convertView.findViewById(R.id.trip_btn_intermediate_stops);
            if (TravelMode.FOOT.equals(legViewModel.leg.getTravelMode())) {
                btnIntermediateStops.setVisibility(View.GONE);
                return;
            }

            CharSequence durationText = DateTimeUtil.formatDetailedDuration(getResources(), legViewModel.leg.getDuration() * 1000);
            btnIntermediateStops.setText(durationText);
            btnIntermediateStops.setTextOn(durationText);
            btnIntermediateStops.setTextOff(durationText);

            if (legViewModel.leg.getDetailRef() == null && legViewModel.leg.getIntermediateStops().isEmpty()) {
                btnIntermediateStops.setCompoundDrawables(null, null, null, null);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    btnIntermediateStops.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.expander_intermediate_stops, 0, 0, 0);
                } else {
                    btnIntermediateStops.setCompoundDrawablesWithIntrinsicBounds(R.drawable.expander_intermediate_stops, 0, 0, 0);
                }
            }

            final LinearLayout stopsLayout = (LinearLayout) convertView.findViewById(R.id.trip_intermediate_stops);

            btnIntermediateStops.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    legViewModel.isExpanded = isChecked;
                    if (legViewModel.leg.getDetailRef() == null && legViewModel.leg.getIntermediateStops().isEmpty()) {
                        return;
                    }

                    if (isChecked) {
                        List<IntermediateStop> intermediateStops = legViewModel.leg.getIntermediateStops();
                        if (stopsLayout.getChildCount() == 0
                                && intermediateStops.isEmpty() && !mIsFetchingSubTrips) {
                            mIsFetchingSubTrips = true;

                            List<String> references = new ArrayList<>();
                            references.add(legViewModel.leg.getDetailRef());
                            mApiService.getIntermediateStops(references, new Callback<IntermediateResponse>() {
                                @Override
                                public void success(IntermediateResponse intermediateResponse, Response response) {
                                    List<IntermediateStop> intermediateStops = intermediateResponse.getStops(legViewModel.leg.getDetailRef());
                                    if (intermediateStops.isEmpty()) {
                                        stopsLayout.addView(inflateText(getText(R.string.no_intermediate_stops), stopsLayout));
                                    } else {
                                        legViewModel.leg.setIntermediateStops(intermediateStops);
                                        for (IntermediateStop is : intermediateStops) {
                                            if (legViewModel.leg.hasStopIndex(is.getLocation().getStopIndex())) {
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
                                if (legViewModel.leg.hasStopIndex(is.getLocation().getStopIndex())) {
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
            btnIntermediateStops.setChecked(legViewModel.isExpanded);
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
                Pair<Integer, RealTimeState> delay = stop.delay();
                if (stop.getTimeRt() != null && stop.hasDelay()) {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    expectedArrivalView.setVisibility(View.VISIBLE);
                    expectedArrivalView.setText(DateFormat.getTimeFormat(getContext()).format(
                            stop.getTimeRt()));
                    expectedArrivalView.setTextColor(ContextCompat.getColor(
                            expectedArrivalView.getContext(),
                            ViewHelper.getTextColorByRealtimeState(delay.second)));
                } else {
                    arrivalView.setPaintFlags(arrivalView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    arrivalView.setTextColor(ContextCompat.getColor(expectedArrivalView.getContext(),
                            ViewHelper.getTextColorByRealtimeState(delay.second)));
                    expectedArrivalView.setVisibility(View.GONE);
                }
            }

            return view;
        }

        private void inflateMessages(final LegViewModel legViewModel, final View convertView) {
            LinearLayout messagesLayout = (LinearLayout) convertView.findViewById(R.id.trip_messages);

            if (legViewModel.leg.hasAlerts()) {
                for (Alert alert : legViewModel.leg.getAlerts()) {
                    String message = alert.getDescription() != null ? alert.getDescription() : alert.getHeader();
                    messagesLayout.addView(inflateMessage("alert", message, messagesLayout));
                }
            }
            if (legViewModel.leg.hasNotes()) {
                for (Alert note : legViewModel.leg.getNotes()) {
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

        private CharSequence createDescription(final LegViewModel legViewModel) {
            CharSequence description;
            if (TravelMode.FOOT.equals(legViewModel.leg.getTravelMode())) {
                description = getString(R.string.distance_in_meter, legViewModel.leg.getDistance());
            } else if (TravelMode.BOAT.equals(legViewModel.leg.getTravelMode())) {
                description = getString(R.string.trip_description_normal,
                        legViewModel.leg.getRouteName(),
                        legViewModel.leg.getHeadsing().getName());
                if (!TextUtils.isEmpty(legViewModel.leg.getRouteShortName())) {
                    RoundedBackgroundSpan roundedBackgroundSpan = new RoundedBackgroundSpan(
                            LegUtil.getColor(getContext(), legViewModel.leg),
                            Color.WHITE,
                            ViewHelper.dipsToPix(getContext().getResources(), 4));
                    Pattern pattern = Pattern.compile(legViewModel.leg.getRouteShortName());
                    description = SpanUtils.createSpannable(description, pattern, roundedBackgroundSpan);
                }
            } else if (TravelMode.BIKE.equals(legViewModel.leg.getTravelMode())) {
                description = getString(R.string.distance_in_meter, legViewModel.leg.getDistance());
            } else {
                String routeName = LegUtil.getRouteName(legViewModel.leg, true);
                description = getString(R.string.trip_description_normal,
                        routeName,
                        legViewModel.leg.getHeadsing().getName());
                RoundedBackgroundSpan roundedBackgroundSpan = new RoundedBackgroundSpan(
                        LegUtil.getColor(getContext(), legViewModel.leg),
                        Color.WHITE,
                        ViewHelper.dipsToPix(getContext().getResources(), 4));
                Pattern pattern = Pattern.compile(routeName);
                description = SpanUtils.createSpannable(description, pattern, roundedBackgroundSpan);
            }
            return description;
        }
    }

    private class TabDetails{
        public JourneyQuery journeyQ;
        public SubTripAdapter subTA;
        public String timeString;
        public Route routeDetail;

        public TabDetails(){
            this.journeyQ = null;
            this.routeDetail = null;
            this.timeString = null;
            this.subTA = null;
        }
        public TabDetails(JourneyQuery j, SubTripAdapter STA, String TS, Route r){
            this.journeyQ = j;
            this.subTA = STA;
            this.timeString = TS;
            this. routeDetail = r;
        }

    }
    private void swapTabDetails(){
        //Rotates the tabs and displays the new route as the first tab
        //Oskar Hahr

        if(mTabDetails[1] == null)
            mTabLayout.getTabAt(0).getCustomView().findViewById(R.id.tabClose).setVisibility(View.GONE);
        switchTabs();
        if(mNameView!=null)
            mNameView.setText(mJourneyQuery.destination.toString());
        if(mSubTripAdapter != null && mSubTripAdapter.getCount() > 0)
            updateFooterView(mSubTripAdapter.getItem(mSubTripAdapter.getCount() - 1));
        setListAdapter(mSubTripAdapter);
        tripTimeDestinationUpdater();
        updateStartAndEndPointViews(mJourneyQuery);
        updateTabText();
        updateStar();

    }


    /**Rotates the information the tabs need to function and
     * creates the correct amount of tabs depending on the information in mSubTripAdapterA
     and sets the text of the tabs to the destination and the trip time **/
    private void createTabs(boolean add){
        if (add) {
            for (int i = TAB_CAP - 1; i > 0; i--)
                mTabDetails[i] = mTabDetails[i - 1];
            mTabDetails[0] = new TabDetails(mJourneyQuery, mSubTripAdapter, timeDestinationString(), mRoute);
        }


        for (int i = 0; i < TAB_CAP; i++) {
            if (mTabDetails[i] == null)
                break;
            TabLayout.Tab tab = mTabLayout.newTab();
            View layout = LayoutInflater.from(this).inflate(R.layout.tab_layout, null);
            Button button = (Button) layout.findViewById(R.id.tabClose);
            button.setTag("btnClose"+i);

            if(mTabDetails[1] == null)
                button.setVisibility(View.GONE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for(int i = 0; i < mTabLayout.getTabCount(); i++)
                        if(mTabLayout.getTabAt(i).getCustomView().findViewById(R.id.tabClose) == v) {
                            removeTab(i);
                            break;
                        }
                }
            });
            tab.setCustomView(layout);
            mTabLayout.addTab(tab);
        }
        updateTabText();
    }
    private void removeTab(int index){
        if(mTabLayout.getTabCount() > 1) {
            for (int i = index; i < mTabLayout.getTabCount() - 1; i++) {
                mTabDetails[i] = mTabDetails[i + 1];

            }
            mTabDetails[mTabLayout.getTabCount() - 1] = null;
            mTabLayout.removeTabAt(index);
            swapTabDetails();
        }

    }
    /*{
        /*
        mTabDetails[2] = mTabDetails[1];
        mTabDetails[1] = mTabDetails[0];
        mTabDetails[0] = new TabDetails(mJourneyQuery, mSubTripAdapter, timeDestinationString(), mRoute);

        for(int i = 0; i < 3; i++) {
            if (mTabDetails[i] == null) {
                break;
            }
            mTabLayout.addTab(mTabLayout.newTab());
            mTabLayout.getTabAt(i).setText(mTabDetails[i].timeString);
        }

    }*/
    private void switchTabs(){
        mJourneyQuery = mTabDetails[selectTab()].journeyQ;
        mRoute = mTabDetails[selectTab()].routeDetail;
        mTimeDestination = mTabDetails[selectTab()].timeString;
        mSubTripAdapter = mTabDetails[selectTab()].subTA;
    }
    private int selectTab(){
        return mTabLayout.getSelectedTabPosition();
    }

    private void updateTabText(){

        for(int i = 0; i < mTabLayout.getTabCount(); i ++) {
            String origin = mTabDetails[i].journeyQ.origin.getName().replace("MY_LOCATION", "My location");
            if (origin.length() > 17)
                origin = origin.substring(0,16) + "...";

            String destination = mTabDetails[i].journeyQ.destination.getName().replace("MY_LOCATION", "My location");;
            if(destination.length() > 17)
                destination = destination.substring(0,16) + "...";

            ((TextView) mTabLayout.getTabAt(i).getCustomView().findViewById(R.id.tabText)).setText(Html.fromHtml("<b> " +
                    origin
                    + "</b><br> "
                    + destination));
        }
    }
    private void changeTabColor(TabLayout.Tab tab, boolean sel){
        if (sel)
            ((TextView)tab.getCustomView().findViewById(R.id.tabText)).setTextColor(getResources().getColor(R.color.accent));
        else
        if (tab.getCustomView() != null)
            ((TextView)tab.getCustomView().findViewById(R.id.tabText)).setTextColor(getResources().getColor(R.color.primary_light));
    }

    /** updateStar - Made by Jakob & Didrik
     * Makes sure the graphics of the star/favorite icon is updated to display
     * the right graphics corresponding to the current tab shown
     *
     */
    private void updateStar(){
        if(mMenuAbove != null) {
            MenuItem starItem = mMenuAbove.findItem(R.id.actionbar_item_star);
            if (isStarredJourney(mJourneyQuery)) {
                starItem.setIcon(R.drawable.ic_action_star_on);
                ViewHelper.tintIcon(getResources(), starItem.getIcon());
            } else {
                starItem.setIcon(R.drawable.ic_action_star_off);
                ViewHelper.tintIcon(getResources(), starItem.getIcon());
            }
        }
    }
}
