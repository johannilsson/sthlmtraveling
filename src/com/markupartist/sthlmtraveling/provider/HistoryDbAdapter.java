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
import java.util.Date;

import com.markupartist.sthlmtraveling.provider.planner.Stop;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class HistoryDbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_CREATED = "created";

    public static final int TYPE_START_POINT = 0;
    public static final int TYPE_END_POINT = 1;
    public static final int TYPE_DEPARTURE_SITE = 2;

    private static final String DATABASE_NAME = "history";
    private static final String DATABASE_TABLE = "history";
    private static final int DATABASE_VERSION = 5;

    private static final String TAG = "HistoryDbAdapter";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mContext;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "CREATE TABLE history (" 
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", type INTEGER NOT NULL" 
                + ", name TEXT NOT NULL"
                + ", created date"
                //+ ", UNIQUE (name)" 
                + ");";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param context the Context within which to work
     */
    public HistoryDbAdapter(Context context) {
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
    public HistoryDbAdapter open() throws SQLException {
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
     * Creates a new entry. If the entry already exists it will be updated
     * with a new time stamp.
     * @param type the type
     * @param name the name
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    public long create(int type, String name) {
        if (name.equals(Stop.TYPE_MY_LOCATION)) {
            return -1;
        }

        // Create a sql date time format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TYPE, type);
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_CREATED, dateFormat.format(date));

        Cursor rowCursor = fetchByName(type, name);
        if (rowCursor.getCount() >= 1) {
            initialValues.put(KEY_ROWID, 
                    rowCursor.getInt(rowCursor.getColumnIndex(KEY_ROWID)));
        }
        rowCursor.close();

        return mDb.replace(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Fetch a entry by name.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    public Cursor fetchByName(int type, String name) {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_TYPE, KEY_NAME}, KEY_NAME + "=\"" + name + "\"" 
                        + " AND " + KEY_TYPE + "=" + type, 
                    null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Fetch all entries for <code>TYPE_START_POINT</code>.
     * @return a Cursor object
     */
    public Cursor fetchAllStartPoints() {
        return fetchByType(TYPE_START_POINT);        
    }

    /**
     * Fetch all entries for <code>TYPE_END_POINT</code>.
     * @return a Cursor object
     */
    public Cursor fetchAllEndPoints() {
        return fetchByType(TYPE_END_POINT);        
    }

    /**
     * Fetch all entries for a specific type.
     * @param type the type
     * @return a Cursor object
     */
    public Cursor fetchByType(int type) {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_TYPE, KEY_NAME}, KEY_TYPE + "=" + type, null,
                    null, null, KEY_CREATED + " DESC", "10");
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

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS history");
            onCreate(db);
        }
    }
}
