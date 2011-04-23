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

package com.markupartist.sthlmtraveling.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.planner.Planner;

public class FavoritesDbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_START_POINT = "start_point";
    public static final String KEY_START_POINT_LATITUDE = "start_point_latitude";
    public static final String KEY_START_POINT_LONGITUDE = "start_point_longitude";
    public static final String KEY_END_POINT = "end_point";
    public static final String KEY_END_POINT_LATITUDE = "end_point_latitude";
    public static final String KEY_END_POINT_LONGITUDE = "end_point_longitude";
    public static final String KEY_CREATED = "created";

    public static final int INDEX_ROWID = 0;
    public static final int INDEX_START_POINT = 1;
    public static final int INDEX_START_POINT_LATITUDE = 2;
    public static final int INDEX_START_POINT_LONGITUDE = 3;
    public static final int INDEX_END_POINT = 4;
    public static final int INDEX_END_POINT_LATITUDE = 5;
    public static final int INDEX_END_POINT_LONGITUDE = 6;
    public static final int INDEX_CREATED = 7;

    private static final String DATABASE_NAME = "favorite";
    private static final String DATABASE_TABLE = "favorites";
    private static final int DATABASE_VERSION = 4;

    private static final String TAG = "FavoritesDbAdapter";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mContext;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "CREATE TABLE favorites (" 
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", start_point TEXT NOT NULL" 
                + ", end_point TEXT NOT NULL"
                + ", start_point_latitude INTEGER NULL"
                + ", start_point_longitude INTEGER NULL"
                + ", end_point_latitude INTEGER NULL"
                + ", end_point_longitude INTEGER NULL"
                + ", created date"
                + ");";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param context the Context within which to work
     */
    public FavoritesDbAdapter(Context context) {
        this.mContext = context;
    }

    /**
     * Open the history database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public FavoritesDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mContext);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    /**
     * Close any open database connection.
     */
    public void close() {
        mDbHelper.close();
    }

    /**
     * Creates a new entry.
     * @param startPoint the start point
     * @param endPoint the endPoint
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    public long create(Planner.Location startPoint, Planner.Location endPoint) {
        ContentValues initialValues = new ContentValues();

        initialValues.put(KEY_START_POINT, startPoint.name);
        if (startPoint.hasLocation() && !startPoint.isMyLocation()) {
            //int startLatitude = (int) (startPoint.getLocation().getLatitude() * 1E6);
            //int startLongitude = (int) (startPoint.getLocation().getLongitude() * 1E6);
            initialValues.put(KEY_START_POINT_LATITUDE, startPoint.latitude);
            initialValues.put(KEY_START_POINT_LONGITUDE, startPoint.longitude);
        }

        initialValues.put(KEY_END_POINT, endPoint.name);
        if (endPoint.hasLocation() && !endPoint.isMyLocation()) {
            //int endLatitude = (int) (endPoint.getLocation().getLatitude() * 1E6);
            //int endLongitude = (int) (endPoint.getLocation().getLongitude() * 1E6);
            initialValues.put(KEY_END_POINT_LATITUDE, endPoint.latitude);
            initialValues.put(KEY_END_POINT_LONGITUDE, endPoint.longitude);
        }

        // Create a sql date time format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        initialValues.put(KEY_CREATED, dateFormat.format(date));

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete an entry with the given id.
     * @param id the id which entry should be deleted
     * @return true if deleted, false otherwise
     */
    public boolean delete(long id) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0;
    }

    /**
     * Deletes all entries.
     * @return true if deleted, false otherwise
     */
    public boolean deleteAll() {
        return mDb.delete(DATABASE_TABLE, null, null) > 0;
    }

    /**
     * Fetch a entry by name.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    public Cursor fetch(Planner.Location startPoint, Planner.Location endPoint) {
        ArrayList<String> selectionArgs = new ArrayList<String>();
        selectionArgs.add(startPoint.name);
        selectionArgs.add(endPoint.name);
        StringBuilder selectionBuilder = new StringBuilder();
        selectionBuilder.append("start_point=? AND end_point=?");

        if (startPoint.hasLocation() && !startPoint.isMyLocation()) {
            //int startLatitude = (int) (startPoint.getLocation().getLatitude() * 1E6);
            //int startLongitude = (int) (startPoint.getLocation().getLongitude() * 1E6);
            selectionArgs.add(String.valueOf(startPoint.latitude));
            selectionArgs.add(String.valueOf(startPoint.longitude));

            selectionBuilder.append(" AND start_point_latitude=? AND start_point_longitude=?");
        }
        if (endPoint.hasLocation() && !endPoint.isMyLocation()) {
            //int endLatitude = (int) (endPoint.getLocation().getLatitude() * 1E6);
            //int endLongitude = (int) (endPoint.getLocation().getLongitude() * 1E6);
            selectionArgs.add(String.valueOf(endPoint.latitude));
            selectionArgs.add(String.valueOf(endPoint.longitude));

            selectionBuilder.append(" AND end_point_latitude=? AND end_point_longitude=?");
        }

        String[] args = new String[]{};
        args = selectionArgs.toArray(args);

        //Log.d(TAG, Arrays.toString(args));
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {
                    KEY_ROWID, KEY_START_POINT, KEY_END_POINT,
                    KEY_START_POINT_LATITUDE, KEY_START_POINT_LONGITUDE,
                    KEY_END_POINT_LATITUDE, KEY_END_POINT_LONGITUDE},
                    selectionBuilder.toString(), args, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Fetch all entries, ordered by created.
     * @return a Cursor object
     */
    public Cursor fetch() {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID,
                    KEY_START_POINT, KEY_START_POINT_LATITUDE,
                    KEY_START_POINT_LONGITUDE, KEY_END_POINT,
                    KEY_END_POINT_LATITUDE, KEY_END_POINT_LONGITUDE, KEY_CREATED },
                    null, null, null, null, KEY_CREATED + " DESC", null);
        return mCursor;        
    }

    /**
     * Internal helper for the database.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        /**
         * Upgrade the favorites table to the latest version.
         * 
         * Based on the example here,
         * http://www.unwesen.de/articles/android-development-database-upgrades
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + "");

            db.beginTransaction();
            boolean success = true;

            for (int i = oldVersion ; i < newVersion ; ++i) {
                int nextVersion = i + 1;
                switch (nextVersion) {
                    case 1:
                    case 2:
                    case 3:
                        success = upgradeToVersion3(db);
                        break;
                    case 4:
                        success = upgradeToVersion4(db);
                        break;
                }
            }

            if (success) {
                db.setTransactionSuccessful();
            }
            db.endTransaction();
        }

        private boolean upgradeToVersion3(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS favorites");
            onCreate(db);
            return true;
        }

        private boolean upgradeToVersion4(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorites ADD COLUMN start_point_latitude INTEGER NULL;");
            db.execSQL("ALTER TABLE favorites ADD COLUMN start_point_longitude INTEGER NULL;");
            db.execSQL("ALTER TABLE favorites ADD COLUMN end_point_latitude INTEGER NULL;");
            db.execSQL("ALTER TABLE favorites ADD COLUMN end_point_longitude INTEGER NULL;");
            return true;
        }
    }
}
