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
package com.markupartist.sthlmtraveling.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.markupartist.sthlmtraveling.provider.site.Site
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoritesDbAdapter
/**
 * Constructor - takes the context to allow the database to be
 * opened/created
 *
 * @param mContext the Context within which to work
 */(private val mContext: Context?) {
    private var mDbHelper: DatabaseHelper? = null
    private var mDb: SQLiteDatabase? = null

    /**
     * Open the history database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     * initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Throws(SQLException::class)
    fun open(): FavoritesDbAdapter {
        mDbHelper = DatabaseHelper(mContext)
        mDb = mDbHelper!!.getWritableDatabase()
        return this
    }

    /**
     * Close any open database connection.
     */
    fun close() {
        mDbHelper!!.close()
    }

    /**
     * Creates a new entry.
     * @param startPoint the start point
     * @param endPoint the endPoint
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    fun create(startPoint: Site, endPoint: Site): Long {
        val initialValues = ContentValues()

        initialValues.put(KEY_START_POINT, startPoint.name)
        if (startPoint.hasLocation() && !startPoint.isMyLocation) {
            initialValues.put(KEY_START_POINT_LATITUDE, startPoint.location!!.getLatitude() * 1E6)
            initialValues.put(KEY_START_POINT_LONGITUDE, startPoint.location!!.getLongitude() * 1E6)
        }

        initialValues.put(KEY_END_POINT, endPoint.name)
        if (endPoint.hasLocation() && !endPoint.isMyLocation) {
            initialValues.put(KEY_END_POINT_LATITUDE, endPoint.location!!.getLatitude() * 1E6)
            initialValues.put(KEY_END_POINT_LONGITUDE, endPoint.location!!.getLatitude() * 1E6)
        }

        // Create a sql date time format
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = Date()
        initialValues.put(KEY_CREATED, dateFormat.format(date))

        return mDb!!.insert(DATABASE_TABLE, null, initialValues)
    }

    /**
     * Delete an entry with the given id.
     * @param id the id which entry should be deleted
     * @return true if deleted, false otherwise
     */
    fun delete(id: Long): Boolean {
        return mDb!!.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0
    }

    /**
     * Deletes all entries.
     * @return true if deleted, false otherwise
     */
    fun deleteAll(): Boolean {
        return mDb!!.delete(DATABASE_TABLE, null, null) > 0
    }

    /**
     * Fetch a entry by name.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    //    public Cursor fetch(Planner.Location startPoint, Planner.Location endPoint) {
    //        ArrayList<String> selectionArgs = new ArrayList<String>();
    //        selectionArgs.add(startPoint.name);
    //        selectionArgs.add(endPoint.name);
    //        StringBuilder selectionBuilder = new StringBuilder();
    //        selectionBuilder.append("start_point=? AND end_point=?");
    //
    //        if (startPoint.hasLocation() && !startPoint.isMyLocation()) {
    //            //int startLatitude = (int) (startPoint.getLocation().getLatitude() * 1E6);
    //            //int startLongitude = (int) (startPoint.getLocation().getLongitude() * 1E6);
    //            selectionArgs.add(String.valueOf(startPoint.latitude));
    //            selectionArgs.add(String.valueOf(startPoint.longitude));
    //
    //            selectionBuilder.append(" AND start_point_latitude=? AND start_point_longitude=?");
    //        }
    //        if (endPoint.hasLocation() && !endPoint.isMyLocation()) {
    //            //int endLatitude = (int) (endPoint.getLocation().getLatitude() * 1E6);
    //            //int endLongitude = (int) (endPoint.getLocation().getLongitude() * 1E6);
    //            selectionArgs.add(String.valueOf(endPoint.latitude));
    //            selectionArgs.add(String.valueOf(endPoint.longitude));
    //
    //            selectionBuilder.append(" AND end_point_latitude=? AND end_point_longitude=?");
    //        }
    //
    //        String[] args = new String[]{};
    //        args = selectionArgs.toArray(args);
    //
    //        //Log.d(TAG, Arrays.toString(args));
    //        Cursor mCursor =
    //            mDb.query(true, DATABASE_TABLE, new String[] {
    //                    KEY_ROWID, KEY_START_POINT, KEY_END_POINT,
    //                    KEY_START_POINT_LATITUDE, KEY_START_POINT_LONGITUDE,
    //                    KEY_END_POINT_LATITUDE, KEY_END_POINT_LONGITUDE},
    //                    selectionBuilder.toString(), args, null, null, null, null);
    //        if (mCursor != null) {
    //            mCursor.moveToFirst();
    //        }
    //        return mCursor;
    //    }
    /**
     * Fetch all entries, ordered by created.
     * @return a Cursor object
     */
    fun fetch(): Cursor {
        val cursor =
            mDb!!.query(
                true, DATABASE_TABLE, arrayOf(
                    KEY_ROWID,
                    KEY_START_POINT, KEY_START_POINT_LATITUDE,
                    KEY_START_POINT_LONGITUDE, KEY_END_POINT,
                    KEY_END_POINT_LATITUDE, KEY_END_POINT_LONGITUDE, KEY_CREATED
                ),
                null, null, null, null, KEY_CREATED + " DESC", null
            )
        return cursor
    }

    /**
     * Internal helper for the database.
     */
    private class DatabaseHelper(context: Context?) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE)
        }

        /**
         * Upgrade the favorites table to the latest version.
         *
         * Based on the example here,
         * http://www.unwesen.de/articles/android-development-database-upgrades
         */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(
                TAG, ("Upgrading database from version " + oldVersion + " to "
                        + newVersion + "")
            )

            db.beginTransaction()
            var success = true

            for (i in oldVersion..<newVersion) {
                val nextVersion = i + 1
                when (nextVersion) {
                    1, 2, 3 -> success = upgradeToVersion3(db)
                    4 -> success = upgradeToVersion4(db)
                }
            }

            if (success) {
                db.setTransactionSuccessful()
            }
            db.endTransaction()
        }

        fun upgradeToVersion3(db: SQLiteDatabase): Boolean {
            db.execSQL("DROP TABLE IF EXISTS favorites")
            onCreate(db)
            return true
        }

        fun upgradeToVersion4(db: SQLiteDatabase): Boolean {
            db.execSQL("ALTER TABLE favorites ADD COLUMN start_point_latitude INTEGER NULL;")
            db.execSQL("ALTER TABLE favorites ADD COLUMN start_point_longitude INTEGER NULL;")
            db.execSQL("ALTER TABLE favorites ADD COLUMN end_point_latitude INTEGER NULL;")
            db.execSQL("ALTER TABLE favorites ADD COLUMN end_point_longitude INTEGER NULL;")
            return true
        }
    }

    companion object {
        const val KEY_ROWID: String = "_id"
        const val KEY_START_POINT: String = "start_point"
        const val KEY_START_POINT_LATITUDE: String = "start_point_latitude"
        const val KEY_START_POINT_LONGITUDE: String = "start_point_longitude"
        const val KEY_END_POINT: String = "end_point"
        const val KEY_END_POINT_LATITUDE: String = "end_point_latitude"
        const val KEY_END_POINT_LONGITUDE: String = "end_point_longitude"
        const val KEY_CREATED: String = "created"

        const val INDEX_ROWID: Int = 0
        const val INDEX_START_POINT: Int = 1
        const val INDEX_START_POINT_LATITUDE: Int = 2
        const val INDEX_START_POINT_LONGITUDE: Int = 3
        const val INDEX_END_POINT: Int = 4
        const val INDEX_END_POINT_LATITUDE: Int = 5
        const val INDEX_END_POINT_LONGITUDE: Int = 6
        const val INDEX_CREATED: Int = 7

        private const val DATABASE_NAME = "favorite"
        private const val DATABASE_TABLE = "favorites"
        private const val DATABASE_VERSION = 4

        private const val TAG = "FavoritesDbAdapter"

        /**
         * Database creation sql statement
         */
        private val DATABASE_CREATE = ("CREATE TABLE favorites ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", start_point TEXT NOT NULL"
                + ", end_point TEXT NOT NULL"
                + ", start_point_latitude INTEGER NULL"
                + ", start_point_longitude INTEGER NULL"
                + ", end_point_latitude INTEGER NULL"
                + ", end_point_longitude INTEGER NULL"
                + ", created date"
                + ");")
    }
}
