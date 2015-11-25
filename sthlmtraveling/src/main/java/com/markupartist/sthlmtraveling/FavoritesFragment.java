/*
 * Copyright (C) 2009-2011 Johan Nilsson <http://markupartist.com>
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

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FavoritesFragment extends BaseListFragment {

    /**
     * Tag used for logging.
     */
    public static final String TAG = "FavoritesFragment";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
        Journeys._ID,          // 0
        Journeys.JOURNEY_DATA, // 1
        Journeys.POSITION,     // 2
    };

    /**
     * The index of the journey data column
     */
    private static final int COLUMN_INDEX_JOURNEY_DATA = 1;

    /**
     * The index of the position column
     */
    private static final int COLUMN_INDEX_POSITION = 2;

    /**
     * The item id of the delete action. 
     */
    private static final int CONTEXT_MENU_DELETE = 1;

    /**
     * The item id of the reverse search action. 
     */
    private static final int CONTEXT_MENU_REVERSE_SEARCH = 2;

    /**
     * The item id of the increase priority action. 
     */
    private static final int CONTEXT_MENU_INCREASE_PRIO = 3;

    /**
     * The item id of the decrease priority action. 
     */
    private static final int CONTEXT_MENU_DECREASE_PRIO = 4;

    private BroadcastReceiver mUpdateUIReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("sthlmtraveling.intent.extra.FAVORITES_UPDATED")) {
                initListAdapter();
            }
        }
    };

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().registerReceiver(mUpdateUIReceiver, new IntentFilter("sthlmtraveling.intent.action.UPDATE_UI"));
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.favorites_list_fragment, container, false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	maybeInitListAdapter();
    	super.onActivityCreated(savedInstanceState);
    }

    private void initListAdapter() {
        Cursor cursor = getActivity().managedQuery(
                Journeys.CONTENT_URI,
                PROJECTION,
                Journeys.STARRED + " = ?",  // We only want the
                new String[] { "1" },       // starred journeys
                Journeys.DEFAULT_SORT_ORDER
            );

        setListAdapter(new JourneyAdapter(getActivity(), cursor));
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Actions");
        menu.add(ContextMenu.NONE, CONTEXT_MENU_DELETE,
                ContextMenu.NONE, R.string.delete);
        menu.add(ContextMenu.NONE, CONTEXT_MENU_REVERSE_SEARCH,
                ContextMenu.NONE, R.string.reverse_search);
        // Disabled the possibility to prioritize journeys in the list for now.
        // Not sure if this was the best way to do it. Figure we could test with
        // labels like HIGH, LOW and have a colored marker indicate the priority
        // to the left first.
        /*
        menu.add(ContextMenu.NONE, CONTEXT_MENU_INCREASE_PRIO,
                ContextMenu.NONE, "Increase priority");
        menu.add(ContextMenu.NONE, CONTEXT_MENU_DECREASE_PRIO,
                ContextMenu.NONE, "Decrease priority");
        */
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case CONTEXT_MENU_DELETE:
        	getActivity().getContentResolver().delete(
                    ContentUris.withAppendedId(Journeys.CONTENT_URI, menuInfo.id),
                    null, null);
            return true;
        case CONTEXT_MENU_REVERSE_SEARCH:
            doSearch(menuInfo.id, true);
            return true;
        case CONTEXT_MENU_INCREASE_PRIO:
            updateListPosition(menuInfo.id, true);
            return true;
        case CONTEXT_MENU_DECREASE_PRIO:
            updateListPosition(menuInfo.id, false);
            return true;
        }
        return false;
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        doSearch(id, false);
    }

    private void doSearch(long id, boolean reversed) {
        Uri uri = ContentUris.withAppendedId(Journeys.CONTENT_URI, id);

        Cursor cursor = getActivity().managedQuery(uri, PROJECTION, null, null, null);
        cursor.moveToFirst();
        JourneyQuery journeyQuery = getJourneyQuery(cursor);

        if (reversed) {
            Site tmpStartPoint = new Site(journeyQuery.destination);
            Site tmpEndPoint = new Site(journeyQuery.origin);
            journeyQuery.origin = tmpStartPoint;
            journeyQuery.destination = tmpEndPoint;
        }

        Intent routesIntent = new Intent(getActivity(), RoutesActivity.class);
        routesIntent.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY,
                journeyQuery);
        startActivity(routesIntent);
    }

    private void updateListPosition(long id, boolean increase) {
        Uri uri = ContentUris.withAppendedId(Journeys.CONTENT_URI, id);

        Cursor cursor = getActivity().managedQuery(uri, PROJECTION, null, null, null);
        cursor.moveToFirst();

        int position = cursor.getInt(COLUMN_INDEX_POSITION);
        if (increase) {
            position = position + 1;
        } else {
            position = position - 1;
        }

        ContentValues values = new ContentValues();
        values.put(Journeys.POSITION, position);
        getActivity().getContentResolver().update(uri, values, null, null);
    }
    
    /**
     * Converts old favorites to the new journey table.
     */
    private void maybeInitListAdapter() {
        // This for legacy resons.
        SharedPreferences localSettings =
            getActivity().getPreferences(FragmentActivity.MODE_PRIVATE);
        boolean isFavoritesConvertedLegacy =
            localSettings.getBoolean("converted_favorites", false);
        // This is the new settings.
        SharedPreferences settings =
            getActivity().getSharedPreferences("sthlmtraveling", FragmentActivity.MODE_PRIVATE);
        boolean isFavoritesConverted =
            settings.getBoolean("converted_favorites", false);
        if (isFavoritesConvertedLegacy || isFavoritesConverted) {
            initListAdapter();
            return;
        }
        Toast.makeText(getActivity(), "Converting Favorites...", Toast.LENGTH_SHORT).show();
    }

    @Override
	public void onDestroy() {
        getActivity().unregisterReceiver(mUpdateUIReceiver);
        super.onDestroy();
    }

    private class JourneyAdapter extends CursorAdapter {
        
        public JourneyAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            JourneyQuery journeyQuery = getJourneyQuery(cursor);

            inflate(view, journeyQuery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.row_journey, parent, false);
            JourneyQuery journeyQuery = getJourneyQuery(cursor);
            return inflate(v, journeyQuery);
        }

        private View inflate(View v, JourneyQuery journeyQuery) {
            // Hide the checkbox
            v.findViewById(R.id.journey_star_check).setVisibility(View.GONE);
            String originStr;
            String destinationStr;
            if (journeyQuery.origin.isMyLocation()) {
                originStr = getString(R.string.my_location);
            } else {
                originStr = journeyQuery.origin.getName();
            }
            if (journeyQuery.destination.isMyLocation()) {
                destinationStr = getString(R.string.my_location);
            } else {
                destinationStr = journeyQuery.destination.getName();
            }

            TextView journeyDescriptionText = (TextView) v.findViewById(R.id.favorite_journey_title);
            journeyDescriptionText.setText(originStr);

            TextView journeyToText = (TextView) v.findViewById(R.id.favorite_to);
            journeyToText.setText(destinationStr);

            TextView viaText = (TextView) v.findViewById(R.id.favorite_via_point);
            if (journeyQuery.hasVia()) {
                viaText.setVisibility(View.VISIBLE);
                viaText.setText(journeyQuery.via.getName());
            } else {
                viaText.setVisibility(View.GONE);
            }

            addTransportModeViews(journeyQuery, v);

            return v;
        }

        private void addTransportModeViews(JourneyQuery journeyQuery, View v) {
            if (journeyQuery.transportModes == null) {
                journeyQuery.transportModes = new ArrayList<>();
            }

            ImageView metroView = (ImageView) v.findViewById(R.id.favorite_transport_mode_metro);
            ImageView busView = (ImageView) v.findViewById(R.id.favorite_transport_mode_bus);
            ImageView trainView = (ImageView) v.findViewById(R.id.favorite_transport_mode_train);
            ImageView tramView = (ImageView) v.findViewById(R.id.favorite_transport_mode_tram);
            ImageView waxView = (ImageView) v.findViewById(R.id.favorite_transport_mode_wax);

            int inactiveColor = getResources().getColor(R.color.transport_icon_inactive);
            int activeColor = getResources().getColor(R.color.icon_default);
            ViewHelper.tint(metroView, inactiveColor);
            ViewHelper.tint(busView, inactiveColor);
            ViewHelper.tint(trainView, inactiveColor);
            ViewHelper.tint(tramView, inactiveColor);
            ViewHelper.tint(waxView, inactiveColor);

            if (journeyQuery.transportModes != null) {
                for (String transportMode : journeyQuery.transportModes) {
                    switch (transportMode) {
                        case TransportMode.METRO:
                            ViewHelper.tint(metroView, activeColor);
                            break;
                        case TransportMode.BUS:
                            ViewHelper.tint(busView, activeColor);
                            break;
                        case TransportMode.TRAIN:
                            ViewHelper.tint(trainView, activeColor);
                            break;
                        case TransportMode.TRAM:
                            ViewHelper.tint(tramView, activeColor);
                            break;
                        case TransportMode.WAX:
                            ViewHelper.tint(waxView, activeColor);
                            break;
                    }
                }
            }
        }
    }

    private static JourneyQuery getJourneyQuery(Cursor cursor) {
        // TODO: Investigate if we can add some kind of caching here.
        String jsonJourneyQuery = cursor.getString(COLUMN_INDEX_JOURNEY_DATA);
        JourneyQuery journeyQuery = null;

        try {
            journeyQuery = JourneyQuery.fromJson(
                    new JSONObject(jsonJourneyQuery));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to covert to journey from json.");
        }
        return journeyQuery;
    }
}
