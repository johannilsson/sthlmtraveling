package com.markupartist.sthlmtraveling.service;

import java.util.ArrayList;

import org.json.JSONException;

import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

public class DataMigrationService extends WakefulIntentService {

    private static final String TAG = "DataMigrationService";

    public DataMigrationService() {
        super("data_migration");
    }

    @Override
    void doWakefulWork(Intent intent) {
        Context context = getApplicationContext();

        maybeConvertFavorites(context);

        Log.d(TAG, "Update complete...");
        Intent updateUi = new Intent("sthlmtraveling.intent.action.UPDATE_UI");
        updateUi.putExtra("sthlmtraveling.intent.extra.FAVORITES_UPDATED", true);
        sendBroadcast(updateUi);
    }

    private void maybeConvertFavorites(Context context) {
        if (!isFavoritesMigrated(context)) {
            try {
                convertFavorites(context);
            } catch (Exception e) {
                Log.w(TAG, "Failed to convert favorites. Mark as converted and move on.");
            }
            SharedPreferences settings = context.getSharedPreferences("sthlmtraveling", MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("converted_favorites", true);
            editor.commit();
        }        
    }
    
    private void convertFavorites(Context context) {
        Log.d(TAG, "About to convert favorites...");

        FavoritesDbAdapter favoritesDbAdapter = new FavoritesDbAdapter(this);
        favoritesDbAdapter.open();

        Cursor cursor = favoritesDbAdapter.fetch();

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
                String json = null;
                try {
                    json = journeyQuery.toJson(false).toString();
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to convert journey to a json document.");
                }

                // Malformed json, skip it.
                if (json != null) {
                    ContentValues values = new ContentValues();
                    values.put(Journeys.JOURNEY_DATA, json);
                    values.put(Journeys.STARRED, "1");
                    values.put(Journeys.CREATED_AT,
                            cursor.getString(FavoritesDbAdapter.INDEX_CREATED));
                    getContentResolver().insert(Journeys.CONTENT_URI, values);
    
                    Log.d(TAG, String.format("Converted favorite journey %s -> %s.",
                            journeyQuery.origin.name, journeyQuery.destination.name));
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        favoritesDbAdapter.close();
    }

    private static boolean isFavoritesMigrated(Context context) {
        SharedPreferences settings = context.getSharedPreferences("sthlmtraveling", MODE_PRIVATE);
        boolean migrated =
            settings.getBoolean("converted_favorites", false);
        if (migrated) {
            Log.d(TAG, "Favorites converted.");
            return true;
        }

        FavoritesDbAdapter favoritesDbAdapter = null;
        try {
            favoritesDbAdapter = new FavoritesDbAdapter(context);
            favoritesDbAdapter.open();
            Cursor favoritesCursor = favoritesDbAdapter.fetch();
            if (favoritesCursor.getCount() == 0) {
                // Also mark it as migrated to avoid sending an intent.
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("converted_favorites", true);
                editor.commit();
                migrated = true;
            }
        } finally {
            if (favoritesDbAdapter != null) {
                favoritesDbAdapter.close();
            }
        }
        if (migrated) {
            Log.d(TAG, "No previous favorites, treat as converted.");
            return true;
        }

        String[] projection = new String[] {
                Journeys._ID, // 0
            };
        ContentResolver resolver = context.getContentResolver();
        Cursor journeyCursor = resolver.query(Journeys.CONTENT_URI, projection, null, null, null);
        if (journeyCursor.getCount() > 0) {
            // Also mark it as migrated to avoid sending an intent.
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("converted_favorites", true);
            editor.commit();
            
            Log.d(TAG, "Existing journeys, treat as converted.");
            return true;
        }

        return false;
    }

    public static boolean hasMigrations(Context context) {
        boolean hasMigrations = false;
        try {
            hasMigrations = !isFavoritesMigrated(context);
        } catch (Exception e) {
            Log.w(TAG, "Failed to determine if we had any migrations. Skip and move on.");
        }
        return hasMigrations;
    }

    public static void startService(Context context) {
        if (hasMigrations(context)) {
            WakefulIntentService.acquireStaticLock(context);
            context.startService(new Intent(context, DataMigrationService.class));
        }
    }
}
