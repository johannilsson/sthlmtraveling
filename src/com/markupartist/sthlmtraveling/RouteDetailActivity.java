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

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Route;
import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;

public class RouteDetailActivity extends ListActivity {
    public static final String TAG = "RouteDetailActivity";
    
    public static final String EXTRA_JOURNEY_TRIP =
        "sthlmtraveling.intent.action.JOURNEY_TRIP";

    public static final String EXTRA_JOURNEY_QUERY =
        "sthlmtraveling.intent.action.JOURNEY_QUERY";

    private static final int DIALOG_BUY_SMS_TICKET = 1;

    private FavoritesDbAdapter mFavoritesDbAdapter;
    private Trip2 mTrip;
    private JourneyQuery mJourneyQuery;
    private SubTripAdapter mSubTripAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        Bundle extras = getIntent().getExtras();

        mTrip = extras.getParcelable(EXTRA_JOURNEY_TRIP);
        mJourneyQuery = extras.getParcelable(EXTRA_JOURNEY_QUERY);

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        TextView startPointView = (TextView) findViewById(R.id.route_from);
        startPointView.setText(mJourneyQuery.origin.name);
        TextView endPointView = (TextView) findViewById(R.id.route_to);
        endPointView.setText(mJourneyQuery.destination.name);

        if (mJourneyQuery.origin.isMyLocation()) {
            startPointView.setText(getMyLocationString(mJourneyQuery.origin));
        }
        if (mJourneyQuery.destination.isMyLocation()) {
            endPointView.setText(getMyLocationString(mJourneyQuery.destination));
        }

        TextView dateTimeView = (TextView) findViewById(R.id.route_date_time);
        dateTimeView.setText(mTrip.toText());

        FavoriteButtonHelper favoriteButtonHelper = new FavoriteButtonHelper(
                this, mFavoritesDbAdapter, mJourneyQuery.origin,
                mJourneyQuery.destination);
        favoriteButtonHelper.loadImage();

        //initRouteDetails(mRoute);
        onRouteDetailsResult(mTrip);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        /*
        RouteDetail detail = mTrip.subTrips.get(position);
        if (detail.getSite().getLocation() != null) {
            //String uri = "geo:"+ detail.getSite().getLocation().getLatitude() + "," + detail.getSite().getLocation().getLongitude() + "?q=" + detail.getSite().getName();  
            //startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
            Intent i = new Intent(this, ViewOnMapActivity.class);
            i.putExtra(ViewOnMapActivity.EXTRA_LOCATION, detail.getSite().getLocation());
            i.putExtra(ViewOnMapActivity.EXTRA_MARKER_TEXT, detail.getSite().getName());
            startActivity(i);
        } else {
            Toast.makeText(this, "Missing geo data", Toast.LENGTH_LONG).show();
        }
        */
        Toast.makeText(this, "Missing geo data", Toast.LENGTH_LONG).show();
    }

    /**
     * Helper that returns the my location text representation. If the {@link Location}
     * is set the accuracy will also be appended.
     * @param location the stop
     * @return a text representation of my location
     */
    private CharSequence getMyLocationString(Planner.Location location) {
        CharSequence string = getText(R.string.my_location);
        /*if (location.getLocation() != null) {
            string = String.format("%s (%sm)", string, location.getLocation().getAccuracy());
        }*/
        return string;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_route_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_search :
                Intent i = new Intent(this, StartActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            case R.id.menu_departures_for_start:
                Intent departuresIntent = new Intent(this, DeparturesActivity.class);
                departuresIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME,
                        mTrip.origin.name);
                startActivity(departuresIntent);
                return true;
            case R.id.menu_share:
                share(mTrip);
                return true;
            case R.id.menu_sms_ticket:
                showDialog(DIALOG_BUY_SMS_TICKET);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mFavoritesDbAdapter.close();
    }

    /**
     * Called when there is results to display.
     * @param details the route details
     */
    public void onRouteDetailsResult(Trip2 trip) {
        
        mSubTripAdapter = new SubTripAdapter(this, trip.subTrips);

        setListAdapter(mSubTripAdapter);
        mTrip = trip;

        // Add zones
        /*
        String zones = RouteDetail.getZones(mTrip);
        if (zones.length() > 0) {
            TextView zoneView = (TextView) findViewById(R.id.route_zones);
            zoneView.setText(zones);
            zoneView.setVisibility(View.VISIBLE);
            zoneView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDialog(DIALOG_BUY_SMS_TICKET);
                }
            });
        }
        */
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_BUY_SMS_TICKET:
            CharSequence[] smsOptions = {
                    getText(R.string.sms_ticket_price_full), 
                    getText(R.string.sms_ticket_price_reduced)
                };
            return new AlertDialog.Builder(this)
                .setTitle(getText(R.string.sms_ticket_label))
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
        Toast.makeText(this, "SMS-Tickets temporally disabled.",
                Toast.LENGTH_LONG).show();
        /*
        final Intent intent = new Intent(Intent.ACTION_VIEW);

        String price = reducedPrice ? "R" : "H";
        intent.setType("vnd.android-dir/mms-sms");
        intent.putExtra("address", "72150");
        intent.putExtra("sms_body", price + RouteDetail.getZones(mTrip));

        Toast.makeText(this, R.string.sms_ticket_notice_message,
                Toast.LENGTH_LONG).show();

        startActivity(intent);
        */
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
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
                        "<b>" + subTrip.origin.name + "</b>",
                        "<b>" + subTrip.destination.name + "</b>");
            } else {
                description = getString(R.string.trip_description_normal,
                        subTrip.departureTime, subTrip.arrivalTime,
                        "<b>" + subTrip.transport.name + "</b>",
                        "<b>" + subTrip.origin.name + "</b>",
                        "<b>" + subTrip.transport.towards + "</b>",
                        "<b>" + subTrip.destination.name + "</b>");
            }

            Log.d(TAG, "desc: " + description);
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
            
            return convertView;
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
}
