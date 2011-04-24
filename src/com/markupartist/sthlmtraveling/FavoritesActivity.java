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

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Location;

public class FavoritesActivity extends BaseListActivity {

    /**
     * Tag used for logging.
     */
    public static final String TAG = "FavoritesActivity";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
        Journeys._ID,          // 0
        Journeys.JOURNEY_DATA, // 1
    };

    /**
     * The index of the journey data column
     */
    private static final int COLUMN_INDEX_JOURNEY_DATA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorites_list);

        registerEvent("Favorites");

        convertFavorites();

        Cursor cursor = managedQuery(
                Journeys.CONTENT_URI,
                PROJECTION,
                Journeys.STARRED + " = ?",  // We only want the
                new String[] { "1" },       // starred journeys
                Journeys.DEFAULT_SORT_ORDER
            );

        setListAdapter(new JourneyAdapter(this, cursor));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(Journeys.CONTENT_URI, id);

        Cursor cursor = managedQuery(uri, PROJECTION, null, null, null);
        cursor.moveToFirst();
        JourneyQuery journeyQuery = getJourneyQuery(cursor);

        Intent routesIntent = new Intent(this, RoutesActivity.class);
        routesIntent.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY,
                journeyQuery);
        startActivity(routesIntent);
    }

    /**
     * Converts old favorites to the new journey table.
     */
    private void convertFavorites() {

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        boolean isFavoritesConverted =
            settings.getBoolean("converted_favorites", false);
        if (isFavoritesConverted) {
            return;
        }

        FavoritesDbAdapter favoritesDbAdapter = new FavoritesDbAdapter(this);
        favoritesDbAdapter.open();

        Cursor cursor = favoritesDbAdapter.fetch();
        startManagingCursor(cursor);

        if (cursor.moveToFirst()) {
            ArrayList<String> transportModes = new ArrayList<String>();
            transportModes.add(TransportMode.BUS);
            transportModes.add(TransportMode.FLY);
            transportModes.add(TransportMode.METRO);
            transportModes.add(TransportMode.TRAIN);
            transportModes.add(TransportMode.TRAM);
            transportModes.add(TransportMode.WAX);
            do {
                JourneyQuery journeyQuery = new JourneyQuery.Builder()
                    .transportModes(transportModes)
                    .origin(
                            cursor.getString(FavoritesDbAdapter.INDEX_START_POINT),
                            cursor.getInt(FavoritesDbAdapter.INDEX_START_POINT_LATITUDE),
                            cursor.getInt(FavoritesDbAdapter.INDEX_START_POINT_LONGITUDE))
                    .destination(
                            cursor.getString(FavoritesDbAdapter.INDEX_END_POINT),
                            cursor.getInt(FavoritesDbAdapter.INDEX_END_POINT_LATITUDE),
                            cursor.getInt(FavoritesDbAdapter.INDEX_END_POINT_LONGITUDE))
                    .create();

                // Store new journey
                String json;
                try {
                    json = journeyQuery.toJson(false).toString();
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to convert journey to a json document.");
                    return;
                }

                ContentValues values = new ContentValues();
                values.put(Journeys.JOURNEY_DATA, json);
                values.put(Journeys.STARRED, "1");
                values.put(Journeys.CREATED_AT,
                        cursor.getString(FavoritesDbAdapter.INDEX_CREATED));
                getContentResolver().insert(Journeys.CONTENT_URI, values);

                Log.d(TAG, String.format("Converted favorite journey %s -> %s.",
                        journeyQuery.origin.name, journeyQuery.destination.name));
            } while (cursor.moveToNext());
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("converted_favorites", true);
        editor.commit();

        stopManagingCursor(cursor);
        favoritesDbAdapter.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mFavoritesDbAdapter.close();
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    private class JourneyAdapter extends CursorAdapter {
        
        public JourneyAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            JourneyQuery journeyQuery = getJourneyQuery(cursor);

            TextView originText =
                (TextView) view.findViewById(R.id.favorite_start_point);
            if (Location.TYPE_MY_LOCATION.equals(journeyQuery.origin.name)) {
                originText.setText(getString(R.string.my_location));
            } else {
                originText.setText(journeyQuery.origin.name);
            }

            TextView destinationText =
                (TextView) view.findViewById(R.id.favorite_end_point);
            if (Location.TYPE_MY_LOCATION.equals(journeyQuery.destination.name)) {
                destinationText.setText(getString(R.string.my_location));
            } else {
                destinationText.setText(journeyQuery.destination.name);
            }

            addTransportModeViews(journeyQuery, view);;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.favorite_row, parent, false);

            JourneyQuery journeyQuery = getJourneyQuery(cursor);

            TextView originText =
                (TextView) v.findViewById(R.id.favorite_start_point);
            originText.setText(journeyQuery.origin.name);
            TextView destinationText =
                (TextView) v.findViewById(R.id.favorite_end_point);
            destinationText.setText(journeyQuery.destination.name);

            addTransportModeViews(journeyQuery, v);

            return v;
        }

        private void addTransportModeViews(JourneyQuery journeyQuery, View v) {
            for (String transportMode : journeyQuery.transportModes) {
                if (transportMode.equals(TransportMode.METRO)) {
                    ImageView transportView =
                        (ImageView) v.findViewById(R.id.favorite_transport_mode_metro);
                    transportView.setVisibility(View.VISIBLE);
                } else if (transportMode.equals(TransportMode.BUS)) {
                    ImageView transportView =
                        (ImageView) v.findViewById(R.id.favorite_transport_mode_bus);
                    transportView.setVisibility(View.VISIBLE);
                } else if (transportMode.equals(TransportMode.TRAIN)) {
                    ImageView transportView =
                        (ImageView) v.findViewById(R.id.favorite_transport_mode_train);
                    transportView.setVisibility(View.VISIBLE);
                } else if (transportMode.equals(TransportMode.TRAM)) {
                    ImageView transportView =
                        (ImageView) v.findViewById(R.id.favorite_transport_mode_tram);
                    transportView.setVisibility(View.VISIBLE);
                } else if (transportMode.equals(TransportMode.WAX)) {
                    ImageView transportView =
                        (ImageView) v.findViewById(R.id.favorite_transport_mode_wax);
                    transportView.setVisibility(View.VISIBLE);
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
