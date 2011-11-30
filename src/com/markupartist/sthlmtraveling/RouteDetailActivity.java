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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.google.ads.AdView;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.actionbar.R;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Route;
import com.markupartist.sthlmtraveling.provider.planner.Planner.IntermediateStop;
import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;
import com.markupartist.sthlmtraveling.utils.AdRequestFactory;

public class RouteDetailActivity extends BaseListActivity {
    public static final String TAG = "RouteDetailActivity";
    
    public static final String EXTRA_JOURNEY_TRIP =
        "sthlmtraveling.intent.action.JOURNEY_TRIP";

    public static final String EXTRA_JOURNEY_QUERY =
        "sthlmtraveling.intent.action.JOURNEY_QUERY";

    private static final int DIALOG_BUY_SMS_TICKET = 1;

    private Trip2 mTrip;
    private JourneyQuery mJourneyQuery;
    private SubTripAdapter mSubTripAdapter;

    private ImageButton mFavoriteButton;

    private ActionBar mActionBar;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        registerEvent("Route details");

        Bundle extras = getIntent().getExtras();

        mTrip = extras.getParcelable(EXTRA_JOURNEY_TRIP);
        mJourneyQuery = extras.getParcelable(EXTRA_JOURNEY_QUERY);

        mActionBar = initActionBar(R.menu.actionbar_route_detail);

        mAdView = (AdView) findViewById(R.id.ad_view);
        if (AppConfig.ADS_ENABLED) {
            mAdView.loadAd(AdRequestFactory.createRequest());
        }

        View headerView = getLayoutInflater().inflate(R.layout.route_header, null);
        TextView startPointView = (TextView) headerView.findViewById(R.id.route_from);
        TextView endPointView = (TextView) headerView.findViewById(R.id.route_to);
        //getListView().addHeaderView(headerView, null, false);

        startPointView.setText(getLocationName(mJourneyQuery.origin));
        endPointView.setText(getLocationName(mJourneyQuery.destination));

        if (mJourneyQuery.hasVia()) {
            headerView.findViewById(R.id.via_row).setVisibility(View.VISIBLE);
            TextView viaTextView = (TextView) headerView.findViewById(R.id.route_via);
            viaTextView.setText(mJourneyQuery.via.name);
        }

        String durationInMinutes = mTrip.duration;
        try {
            DateFormat df = new SimpleDateFormat("H:mm");
            Date tripDate = df.parse(mTrip.duration);
            if (tripDate.getHours() == 0) {
                int start = mTrip.duration.indexOf(":") + 1;
                if (mTrip.duration.substring(start).startsWith("0")) {
                    start++;
                }
                durationInMinutes = mTrip.duration.substring(start) + " min";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing duration, " + e.getMessage());
        }

        /*View headerDetailView = getLayoutInflater().inflate(
                R.layout.route_header_details, null);*/

        LinearLayout headerDetailView = (LinearLayout) headerView.findViewById(R.id.header_details);
        headerDetailView.setVisibility(View.VISIBLE);
        
        StringBuilder timeBuilder = new StringBuilder();
        timeBuilder.append(mTrip.departureTime);
        timeBuilder.append(" - ");
        timeBuilder.append(mTrip.arrivalTime);
        timeBuilder.append(" (");
        timeBuilder.append(durationInMinutes);
        timeBuilder.append(")");
        
        //TextView timeView = (TextView) findViewById(R.id.route_date_time);
        TextView timeView = (TextView) headerView.findViewById(R.id.route_date_time);
        timeView.setText(timeBuilder.toString());
        
        // TODO: We should parse the date when getting the results and store a
        // Time object instead.
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");
        Date date = null;
        try {
            date = format.parse(mTrip.departureDate);
        } catch (ParseException e) {
            ;
        }
        SimpleDateFormat otherFormat = new SimpleDateFormat("yyyy-MM-dd");
        //TextView dateView = (TextView) findViewById(R.id.route_date_of_trip);
        TextView dateView = (TextView) headerView.findViewById(R.id.route_date_of_trip);
        if (date != null) {
            dateView.setText(getString(R.string.date_of_trip, otherFormat.format(date)));
        } else {
            dateView.setVisibility(View.GONE);
        }

        //getListView().addHeaderView(headerDetailView, null, false);
        getListView().addHeaderView(headerView, null, false);

        mFavoriteButton = (ImageButton) findViewById(R.id.route_favorite);
        if (isStarredJourney(mJourneyQuery)) {
            mFavoriteButton.setImageResource(android.R.drawable.star_big_on);
        }
        mFavoriteButton.setOnClickListener(new OnStarredJourneyButtonClickListener());

        //initRouteDetails(mRoute);
        onRouteDetailsResult(mTrip);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        onRotationChange(newConfig);

        super.onConfigurationChanged(newConfig);
    }

    private void onRotationChange(Configuration newConfig) {
        if (newConfig.orientation == newConfig.ORIENTATION_LANDSCAPE) {
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE);
            }
        } else {
            if (mAdView != null) {
                mAdView.setVisibility(View.VISIBLE);
            }
        }        
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
            showDialog(DIALOG_BUY_SMS_TICKET);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        int headerViewsCount = getListView().getHeaderViewsCount();
        // Compensate for the added header views. Is this how we do it?
        position -= headerViewsCount;

        Planner.Location location;
        // Detects the footer view.
        if (position + 1 > mSubTripAdapter.getCount()) {
            int numSubTrips = mTrip.subTrips.size();
            SubTrip subTrip = mTrip.subTrips.get(numSubTrips - 1);
            location = subTrip.destination;
        } else {
            SubTrip subTrip = mSubTripAdapter.getItem(position);
            location = subTrip.origin;
        }

        if (location.hasLocation()) {
            startActivity(createViewOnMapIntent(location));
        } else {
            Toast.makeText(this, "Missing geo data", Toast.LENGTH_LONG).show();
        }
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
     * Called before this activity is destroyed, returns the previous details. This data is used 
     * if the screen is rotated. Then we don't need to ask for the data again.
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
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Called when there is results to display.
     * @param details the route details
     */
    public void onRouteDetailsResult(Trip2 trip) {
        mSubTripAdapter = new SubTripAdapter(this, trip.subTrips);

        int numSubTrips = trip.subTrips.size();
        SubTrip lastSubTrip = trip.subTrips.get(numSubTrips - 1); 

        View footerView = getLayoutInflater().inflate(R.layout.route_details_row, null);
        TextView message = (TextView) footerView.findViewById(R.id.routes_row);
        message.setText(lastSubTrip.destination.name);

        ImageView image = (ImageView) footerView.findViewById(R.id.routes_row_transport);
        image.setImageResource(R.drawable.bullet_black);

        getListView().addFooterView(footerView);

        setListAdapter(mSubTripAdapter);

        if (trip.canBuySmsTicket()) {
            mActionBar.addAction(
                mActionBar.newAction(R.id.actionbar_item_sms)
                    .setIcon(R.drawable.ic_actionbar_sms)
            );
        }

        mTrip = trip;
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
            .setTitle(String.format("%s (%s)",
                    getText(R.string.sms_ticket_label), mTrip.tariffZones))
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
     * Share a {@link Route} with others.
     * @param route the route to share
     */
    public void share(Trip2 route) {
        Toast.makeText(this, "Share is temporally disabled.",
                Toast.LENGTH_LONG).show();
        /*
        final Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.route_details_label));
        intent.putExtra(Intent.EXTRA_TEXT, route.toTextRepresentation());

        startActivity(Intent.createChooser(intent, getText(R.string.share_label)));
        */
    }

    /**
     * Invokes the Messaging application.
     * @param reducedPrice True if the price is reduced, false otherwise. 
     */
    public void sendSms(boolean reducedPrice) {
        registerEvent("Buy SMS Ticket");

        final Intent intent = new Intent(Intent.ACTION_VIEW);

        String price = reducedPrice ? "R" : "H";
        intent.setType("vnd.android-dir/mms-sms");
        intent.putExtra("address", "72150");
        intent.putExtra("sms_body", price + mTrip.tariffZones);

        Toast.makeText(this, R.string.sms_ticket_notice_message,
                Toast.LENGTH_LONG).show();

        startActivity(intent);
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    private Intent createViewOnMapIntent(Planner.Location location) {
        Location l = new Location("sthlmtraveling");
        l.setLatitude(location.latitude / 1E6);
        l.setLongitude(location.longitude / 1E6);

        Intent intent = new Intent(this, ViewOnMapActivity.class);
        intent.putExtra(ViewOnMapActivity.EXTRA_LOCATION, l);
        intent.putExtra(ViewOnMapActivity.EXTRA_MARKER_TEXT, getLocationName(location));

        return intent;
    }

    private class SubTripAdapter extends ArrayAdapter<SubTrip> {

        private LayoutInflater mInflater;

        public SubTripAdapter(Context context, List<SubTrip> objects) {
            super(context, R.layout.route_details_row, objects);

            mInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SubTrip subTrip = getItem(position);
            
            convertView = mInflater.inflate(R.layout.route_details_row, null);
            ImageView transportImage = (ImageView) convertView.findViewById(R.id.routes_row_transport);
            TextView descriptionView = (TextView) convertView.findViewById(R.id.routes_row);

            transportImage.setImageResource(subTrip.transport.getImageResource());
            
            CharSequence description;
            if ("Walk".equals(subTrip.transport.type)) {
                description = getString(R.string.trip_description_walk,
                        "<b>" + subTrip.departureTime + "</b>",
                        "<b>" + subTrip.arrivalTime + "</b>",
                        "<b>" + getLocationName(subTrip.origin) + "</b>",
                        "<b>" + getLocationName(subTrip.destination) + "</b>");
            } else {
                description = getString(R.string.trip_description_normal,
                        subTrip.departureTime, subTrip.arrivalTime,
                        "<b>" + subTrip.transport.name + "</b>",
                        "<b>" + getLocationName(subTrip.origin) + "</b>",
                        "<b>" + subTrip.transport.towards + "</b>",
                        "<b>" + getLocationName(subTrip.destination) + "</b>");
            }

            descriptionView.setText(android.text.Html.fromHtml(description.toString()));
            //descriptionView.setText(description);
            
            LinearLayout messagesLayout = (LinearLayout) convertView.findViewById(R.id.routes_messages);
            if (!subTrip.remarks.isEmpty()) {
                for (String message : subTrip.remarks) {
                    messagesLayout.addView(inflateMessage("remark", message,
                                    messagesLayout, position));
                }
            }
            if (!subTrip.rtuMessages.isEmpty()) {
                for (String message : subTrip.rtuMessages) {
                    messagesLayout.addView(inflateMessage("rtu", message,
                                    messagesLayout, position));
                }
            }
            if (!subTrip.mt6Messages.isEmpty()) {
                for (String message : subTrip.mt6Messages) {
                    messagesLayout.addView(inflateMessage("mt6", message,
                                    messagesLayout, position));
                }
            }

            final LinearLayout intermediateStopLayout =
                (LinearLayout) convertView.findViewById(R.id.intermediate_stops);
            ToggleButton showHideIntermediateStops =
                (ToggleButton) convertView.findViewById(
                        R.id.show_hide_intermediate_stops);
            showHideIntermediateStops.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        intermediateStopLayout.setVisibility(View.VISIBLE);
                    } else {
                        intermediateStopLayout.setVisibility(View.GONE);
                    }
                }
            });
            if (!subTrip.intermediateStop.isEmpty()) {
                showHideIntermediateStops.setVisibility(View.VISIBLE);
                for (IntermediateStop is : subTrip.intermediateStop) {
                    intermediateStopLayout.addView(
                            inflateIntermediateStop(is, intermediateStopLayout));
                }
            } else {
                showHideIntermediateStops.setVisibility(View.GONE);
            }

            return convertView;
        }

        private View inflateIntermediateStop(IntermediateStop stop,
                ViewGroup layout) {
            View view = mInflater.inflate(R.layout.intermediate_stop, layout, false);
            TextView descView =
                (TextView) view.findViewById(R.id.intermediate_stop_description);
            descView.setText(String.format("%s %s",
                    stop.arrivalTime, stop.location.name));
            return view;
        }

        private View inflateMessage(String messageType, String message,
                ViewGroup messagesLayout, int position) {
            View view = mInflater.inflate(R.layout.route_details_message_row,
                    messagesLayout, false);

            TextView messageView = (TextView) view.findViewById(R.id.routes_warning_message);
            messageView.setText(message);

            return view;
        }
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

    private class OnStarredJourneyButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
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
            // TODO: Replace button with a checkbox and check with that instead?
            if (isStarredJourney(mJourneyQuery)) {
                values.put(Journeys.STARRED, "0");
                getContentResolver().update(
                        uri, values, where, selectionArgs);
                mFavoriteButton.setImageResource(
                        android.R.drawable.star_big_off);
            } else {
                values.put(Journeys.STARRED, "1");
                int affectedRows = getContentResolver().update(
                        uri, values, where, selectionArgs);
                if (affectedRows <= 0) {
                    values.put(Journeys.STARRED, "1");
                    getContentResolver().insert(
                            Journeys.CONTENT_URI, values);
                }
                mFavoriteButton.setImageResource(
                        android.R.drawable.star_big_on);
            }
        }
    }
}
