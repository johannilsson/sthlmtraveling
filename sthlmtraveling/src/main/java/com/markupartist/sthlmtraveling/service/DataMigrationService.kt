package com.markupartist.sthlmtraveling.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.BaseColumns
import android.util.Log
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys
import com.markupartist.sthlmtraveling.provider.TransportMode
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery
import org.json.JSONException

class DataMigrationService : WakefulIntentService("data_migration") {
    override fun doWakefulWork(intent: Intent?) {
        val context = getApplicationContext()

        maybeConvertFavorites(context)

        Log.d(TAG, "Update complete...")
        val updateUi = Intent("sthlmtraveling.intent.action.UPDATE_UI")
        updateUi.putExtra("sthlmtraveling.intent.extra.FAVORITES_UPDATED", true)
        sendBroadcast(updateUi)
    }

    private fun maybeConvertFavorites(context: Context) {
        if (!isFavoritesMigrated(context)) {
            try {
                convertFavorites(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to convert favorites. Mark as converted and move on.")
            }
            val settings = context.getSharedPreferences("sthlmtraveling", MODE_PRIVATE)
            val editor = settings.edit()
            editor.putBoolean("converted_favorites", true)
            editor.apply()
        }
    }

    private fun convertFavorites(context: Context?) {
        Log.d(TAG, "About to convert favorites...")

        val favoritesDbAdapter = FavoritesDbAdapter(this)
        favoritesDbAdapter.open()

        val cursor = favoritesDbAdapter.fetch()

        if (cursor.moveToFirst()) {
            val transportModes = arrayListOf(
                TransportMode.BUS,
                TransportMode.METRO,
                TransportMode.TRAIN,
                TransportMode.TRAM,
                TransportMode.WAX
            )
            do {
                val journeyQuery = JourneyQuery.Builder()
                    .transportModes(transportModes)
                    .origin(
                        cursor.getString(FavoritesDbAdapter.INDEX_START_POINT),
                        cursor.getInt(FavoritesDbAdapter.INDEX_START_POINT_LATITUDE),
                        cursor.getInt(FavoritesDbAdapter.INDEX_START_POINT_LONGITUDE)
                    )
                    .destination(
                        cursor.getString(FavoritesDbAdapter.INDEX_END_POINT),
                        cursor.getInt(FavoritesDbAdapter.INDEX_END_POINT_LATITUDE),
                        cursor.getInt(FavoritesDbAdapter.INDEX_END_POINT_LONGITUDE)
                    )
                    .create()

                // Store new journey
                val json = try {
                    journeyQuery.toJson(false).toString()
                } catch (e: JSONException) {
                    Log.e(TAG, "Failed to convert journey to a json document.")
                    null
                }

                // Malformed json, skip it.
                if (json != null) {
                    val values = ContentValues()
                    values.put(Journeys.JOURNEY_DATA, json)
                    values.put(Journeys.STARRED, "1")
                    values.put(
                        Journeys.CREATED_AT,
                        cursor.getString(FavoritesDbAdapter.INDEX_CREATED)
                    )
                    getContentResolver().insert(Journeys.CONTENT_URI, values)

                    Log.d(
                        TAG, String.format(
                            "Converted favorite journey %s -> %s.",
                            journeyQuery.origin!!.name, journeyQuery.destination!!.name
                        )
                    )
                }
            } while (cursor.moveToNext())
        }

        cursor.close()
        favoritesDbAdapter.close()
    }

    companion object {
        private const val TAG = "DataMigrationService"

        private fun isFavoritesMigrated(context: Context): Boolean {
            val settings = context.getSharedPreferences("sthlmtraveling", MODE_PRIVATE)
            if (settings.getBoolean("converted_favorites", false)) {
                Log.d(TAG, "Favorites converted.")
                return true
            }

            val favoritesDbAdapter = FavoritesDbAdapter(context)
            favoritesDbAdapter.open()
            val favoritesCursor = favoritesDbAdapter.fetch()
            val hasNoFavorites = favoritesCursor.count == 0
            favoritesDbAdapter.close()

            if (hasNoFavorites) {
                // Also mark it as migrated to avoid sending an intent.
                settings.edit()
                    .putBoolean("converted_favorites", true)
                    .apply()
                Log.d(TAG, "No previous favorites, treat as converted.")
                return true
            }

            val projection = arrayOf(BaseColumns._ID)
            val resolver = context.contentResolver
            val journeyCursor = resolver.query(Journeys.CONTENT_URI, projection, null, null, null)
            val hasJourneys = journeyCursor?.count ?: 0 > 0
            journeyCursor?.close()

            if (hasJourneys) {
                // Also mark it as migrated to avoid sending an intent.
                settings.edit()
                    .putBoolean("converted_favorites", true)
                    .apply()
                Log.d(TAG, "Existing journeys, treat as converted.")
                return true
            }

            return false
        }

        fun hasMigrations(context: Context): Boolean {
            var hasMigrations = false
            try {
                hasMigrations = !isFavoritesMigrated(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to determine if we had any migrations. Skip and move on.")
            }
            return hasMigrations
        }

        @JvmStatic
        fun startService(context: Context) {
            if (hasMigrations(context)) {
                WakefulIntentService.acquireStaticLock(context)
                context.startService(Intent(context, DataMigrationService::class.java))
            }
        }
    }
}
