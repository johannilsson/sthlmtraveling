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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.IntermediateStop;
import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.IntentUtil;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class RouteDetailActivity extends BaseListActivity {
    public static final String TAG = "RouteDetailActivity";
    
    public static final String EXTRA_JOURNEY_TRIP = "sthlmtraveling.intent.action.JOURNEY_TRIP";
    public static final String EXTRA_JOURNEY_QUERY = "sthlmtraveling.intent.action.JOURNEY_QUERY";

    private static final int DIALOG_BUY_SMS_TICKET = 1;

    private Trip2 mTrip;
    private JourneyQuery mJourneyQuery;
    private SubTripAdapter mSubTripAdapter;

    private ImageButton mFavoriteButton;

    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        registerEvent("Route details");

        Bundle extras = getIntent().getExtras();

        mTrip = (Trip2) getLastNonConfigurationInstance();
        if (mTrip == null) {
            mTrip = extras.getParcelable(EXTRA_JOURNEY_TRIP);
        }

        mJourneyQuery = extras.getParcelable(EXTRA_JOURNEY_QUERY);

        mActionBar = initActionBar();

        updateStartAndEndPointViews(mJourneyQuery);

        View headerView = getLayoutInflater().inflate(R.layout.route_header_details, null);
        TextView timeView = (TextView) headerView.findViewById(R.id.route_date_time);
        timeView.setText(getString(R.string.time_to, mTrip.getDurationText(), mTrip.destination.getCleanName()));
        if (mTrip.canBuySmsTicket()) {
            TextView zoneView = (TextView) headerView.findViewById(R.id.route_zones);
            zoneView.setText(mTrip.tariffZones);
            zoneView.setVisibility(View.VISIBLE);
        }

        getListView().addHeaderView(headerView, null, false);

        onRouteDetailsResult(mTrip);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
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
        if (!mTrip.canBuySmsTicket()) {
            menu.removeItem(R.id.actionbar_item_sms);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_item_time:
            Intent departuresIntent = new Intent(this, DeparturesActivity.class);
            departuresIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME,
                    mTrip.origin.name);
            startActivity(departuresIntent);
            return true;
        case R.id.actionbar_item_sms:
            if (mTrip.canBuySmsTicket()) {
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
    private CharSequence getLocationName(Planner.Location location) {
        if (location == null) {
            return "Unknown";
        }
        if (location.isMyLocation()) {
            return getText(R.string.my_location);
        }
        return location.name;

        /*if (location.getLocation() != null) {
            string = String.format("%s (%sm)", string, location.getLocation().getAccuracy());
        }*/
    }

    /**
     * Called before this activity is destroyed, returns the previous details.
     * This data is used if the screen is rotated. Then we don't need to ask for
     * the data again.
     * 
     * @return route details
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mTrip;
    }

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
        super.onDestroy();
    }

    /**
     * Called when there is results to display.
     * @param trip the route details
     */
    public void onRouteDetailsResult(Trip2 trip) {

        getListView().addFooterView(createFooterView(trip));

        setListAdapter(mSubTripAdapter);

        mTrip = trip;
    }

    private View createFooterView(final Trip2 trip) {
        mSubTripAdapter = new SubTripAdapter(this, trip.subTrips);

        int numSubTrips = trip.subTrips.size();
        final SubTrip lastSubTrip = trip.subTrips.get(numSubTrips - 1);
        // todo: make sure this is safe.

        View convertView = getLayoutInflater().inflate(R.layout.trip_row_stop_layout, null);
        Button nameView = (Button) convertView.findViewById(R.id.trip_stop_title);
        nameView.setText(getLocationName(lastSubTrip.destination));
        nameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Planner.Location location = lastSubTrip.destination;
                if (location.hasLocation()) {
                    startActivity(createViewOnMapIntent(mTrip, mJourneyQuery, location));
                } else {
                    Toast.makeText(RouteDetailActivity.this, "Missing geo data", Toast.LENGTH_LONG).show();
                }
            }
        });

        View endSegment = convertView.findViewById(R.id.trip_line_segment_end);
        endSegment.setVisibility(View.VISIBLE);

        TextView arrivalTimeView = (TextView) convertView.findViewById(R.id.trip_arrival_time);
        arrivalTimeView.setVisibility(View.GONE);

        TextView departureTimeView = (TextView) convertView.findViewById(R.id.trip_departure_time);
        departureTimeView.setText(lastSubTrip.arrivalTime);

        convertView.findViewById(R.id.trip_intermediate_stops_layout).setVisibility(View.GONE);

        return convertView;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_BUY_SMS_TICKET:
            CharSequence[] smsOptions = {
                    getText(R.string.sms_ticket_price_full) + " " + getFullPrice(), 
                    getText(R.string.sms_ticket_price_reduced) + " " + getReducedPrice()
                };
            return new AlertDialog.Builder(this)
            .setTitle(String.format("%s (%s)", getText(R.string.sms_ticket_label), mTrip.tariffZones))
                .setItems(smsOptions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch(item) {
                        case 0:
                            sendSms(false);
                            break;
                        case 1:
                            sendSms(true);
                            break;
                        }
                    }
                }).create();
        }
        return null;
    }

    private CharSequence getFullPrice() {
        final int[] PRICE = new int[] { 36, 54, 72 };
        return PRICE[mTrip.tariffZones.length() - 1] + " kr";
    }

    private CharSequence getReducedPrice() {
        final int[] PRICE = new int[] { 20, 30, 40 };
        return PRICE[mTrip.tariffZones.length() - 1] + " kr";
    }

    /**
     * Invokes the Messaging application.
     * @param reducedPrice True if the price is reduced, false otherwise. 
     */
    public void sendSms(boolean reducedPrice) {
        registerEvent("Buy SMS Ticket");
        Toast.makeText(this, R.string.sms_ticket_notice_message, Toast.LENGTH_LONG).show();
        String price = reducedPrice ? "R" : "H";
        String number = "0767201010";
        IntentUtil.smsIntent(this, number, price + mTrip.tariffZones);
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    private Intent createViewOnMapIntent(Planner.Trip2 trip,
            JourneyQuery query, Planner.Location location) {
        Intent intent = new Intent(this, ViewOnMapActivity.class);
        intent.putExtra(ViewOnMapActivity.EXTRA_TRIP, trip);
        intent.putExtra(ViewOnMapActivity.EXTRA_JOURNEY_QUERY, query);
        intent.putExtra(ViewOnMapActivity.EXTRA_LOCATION, location);

        return intent;
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
            Planner.Location l = (Planner.Location) v.getTag();
            if (l.hasLocation()) {
                startActivity(createViewOnMapIntent(mTrip, mJourneyQuery, l));
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
    private class SubTripAdapter extends ArrayAdapter<SubTrip> {
        private LayoutInflater mInflater;

        public SubTripAdapter(Context context, List<SubTrip> objects) {
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
            final SubTrip subTrip = getItem(position);
            convertView = mInflater.inflate(R.layout.trip_row_stop_layout, null);

            Button nameView = (Button) convertView.findViewById(R.id.trip_stop_title);
            nameView.setText(getLocationName(subTrip.origin));
            nameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Planner.Location location = subTrip.origin;
                    if (location.hasLocation()) {
                        startActivity(createViewOnMapIntent(mTrip, mJourneyQuery, location));
                    } else {
                        Toast.makeText(getContext(), "Missing geo data", Toast.LENGTH_LONG).show();
                    }
                }
            });

            View startSegment = convertView.findViewById(R.id.trip_line_segment_start);
            TextView arrivalTimeView = (TextView) convertView.findViewById(R.id.trip_arrival_time);
            if (position > 0) {
                final SubTrip prevSubTrip = getItem(position - 1);
                arrivalTimeView.setText(prevSubTrip.arrivalTime);
                startSegment.setVisibility(View.GONE);
            } else {
                startSegment.setVisibility(View.VISIBLE);
                arrivalTimeView.setVisibility(View.GONE);
            }
            TextView departureTimeView = (TextView) convertView.findViewById(R.id.trip_departure_time);
            departureTimeView.setText(subTrip.departureTime);

            // Add description data
            ViewStub descriptionStub = (ViewStub) convertView.findViewById(R.id.trip_description_stub);
            View descriptionLayout = descriptionStub.inflate();
            TextView descriptionView = (TextView) descriptionLayout.findViewById(R.id.trip_description);
            descriptionView.setText(createDescription(subTrip));
            ImageView descriptionIcon = (ImageView) descriptionLayout.findViewById(R.id.trip_description_icon);
            descriptionIcon.setImageResource(subTrip.transport.getImageResource());
            //descriptionView.setCompoundDrawablesWithIntrinsicBounds(subTrip.transport.getImageResource(), 0, 0, 0);

            inflateMessages(subTrip, convertView);
            inflateIntermediate(subTrip, position, convertView);

            return convertView;
        }

        private void inflateIntermediate(final SubTrip subTrip, final int position, final View convertView) {
            final ToggleButton btnIntermediateStops = (ToggleButton) convertView.findViewById(R.id.trip_btn_intermediate_stops);

            CharSequence durationText = DateTimeUtil.formatDuration(getResources(), subTrip.getDurationMillis());
            btnIntermediateStops.setText(durationText);
            btnIntermediateStops.setTextOn(durationText);
            btnIntermediateStops.setTextOff(durationText);
            if ("Walk".equals(subTrip.transport.type)) {
                btnIntermediateStops.setVisibility(View.GONE);
            }

            final LinearLayout stopsLayout = (LinearLayout) convertView.findViewById(R.id.trip_intermediate_stops);

            btnIntermediateStops.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (stopsLayout.getChildCount() == 0
                                && subTrip.intermediateStop.size() == 0) {
                            new GetIntermediateStopTask(new GetIntermediateStopTask.OnResult() {
                                @Override
                                public void onResult(SubTrip st) {
                                    if (st.intermediateStop.isEmpty()) {
                                        //stopsLayout.addView(inflateText(getText(R.string.no_intermediate_stops), stopsLayout));
                                    }
                                    for (IntermediateStop is : st.intermediateStop) {
                                        stopsLayout.addView(inflateIntermediateStop(is, stopsLayout));
                                    }
                                    mTrip.subTrips.set(position, st);
                                }
                            }).execute(subTrip, mJourneyQuery);
                        } else if (stopsLayout.getChildCount() == 0
                                && subTrip.intermediateStop.size() > 0) {
                            for (IntermediateStop is : subTrip.intermediateStop) {
                                stopsLayout.addView(inflateIntermediateStop(is, stopsLayout));
                            }
                        }
                        stopsLayout.setVisibility(View.VISIBLE);
                    } else {
                        stopsLayout.setVisibility(View.GONE);
                    }
                }
            });
        }

        private View inflateIntermediateStop(IntermediateStop stop, LinearLayout stopsLayout) {
            View view = mInflater.inflate(R.layout.trip_row_intermediate_stop, stopsLayout, false);
            TextView descView = (TextView) view.findViewById(R.id.trip_stop_title);
            descView.setTextSize(12);
            descView.setText(stop.location.name);
            TextView arrivalView = (TextView) view.findViewById(R.id.trip_arrival_time);
            view.findViewById(R.id.trip_departure_time).setVisibility(View.GONE);
            arrivalView.setText(stop.arrivalTime);
            return view;
        }

        private void inflateMessages(final SubTrip subTrip, final View convertView) {
            LinearLayout messagesLayout = (LinearLayout) convertView.findViewById(R.id.trip_messages);

            if (!subTrip.remarks.isEmpty()) {
                for (String message : subTrip.remarks) {
                    messagesLayout.addView(inflateMessage("remark", message, messagesLayout));
                }
            }
            if (!subTrip.rtuMessages.isEmpty()) {
                for (String message : subTrip.rtuMessages) {
                    messagesLayout.addView(inflateMessage("rtu", message, messagesLayout));
                }
            }
            if (!subTrip.mt6Messages.isEmpty()) {
                for (String message : subTrip.mt6Messages) {
                    messagesLayout.addView(inflateMessage("mt6", message, messagesLayout));
                }
            }
        }

        private View inflateMessage(String type, String message, LinearLayout messagesLayout) {
            View view = mInflater.inflate(R.layout.trip_row_message, messagesLayout, false);
            TextView messageView = (TextView) view.findViewById(R.id.trip_message);
            messageView.setText(message);
            return view;
        }

        private CharSequence createDescription(final SubTrip subTrip) {
            CharSequence description;
            if ("Walk".equals(subTrip.transport.type)) {
                description = getString(R.string.trip_description_walk,
                        getLocationName(subTrip.origin));
            } else {
                description = getString(R.string.trip_description_normal,
                        subTrip.transport.name,
                        subTrip.transport.towards);
            }
            return description;
        }
    }


    private static class GetIntermediateStopTask extends AsyncTask<Object, Void, SubTrip> {
        GetIntermediateStopTask.OnResult mCallback;
        public GetIntermediateStopTask(GetIntermediateStopTask.OnResult onResult) {
            mCallback = onResult;
        }

        @Override
        protected SubTrip doInBackground(Object... params) {
            SubTrip subTrip = (SubTrip)params[0];
            try {
                Planner.getInstance().addIntermediateStops(
                        subTrip, (JourneyQuery)params[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return subTrip;
        }

        @Override
        protected void onPostExecute(SubTrip result) {
            mCallback.onResult(result);
        }

        private static interface OnResult {
            public void onResult(SubTrip subTrip);
        }
    }
}
