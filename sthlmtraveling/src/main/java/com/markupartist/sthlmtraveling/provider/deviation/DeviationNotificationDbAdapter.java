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

package com.markupartist.sthlmtraveling.provider.deviation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DeviationNotificationDbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_REFERENCE = "reference";
    public static final String KEY_VERSION = "version";
    public static final String KEY_NOTIFIED_AT = "notified_at";

    private static final String DATABASE_NAME = "deviation_notifications";
    private static final String DATABASE_TABLE = "deviation_notifications";
    private static final int DATABASE_VERSION = 3;

    private static final String TAG = "DeviationNotificationDb";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mContext;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "CREATE TABLE deviation_notifications (" 
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", reference TEXT NOT NULL" 
                + ", version INTEGER NOT NULL"
                + ", notified_at DATE" 
                + ");";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param context the Context within which to work
     */
    public DeviationNotificationDbAdapter(Context context) {
        this.mContext = context;
    }

    /**
     * Open the deviation notification database. If it cannot be opened it
     * tries to create a new instance of the database. If it cannot be 
     * created, throw an exception to signal the failure.
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DeviationNotificationDbAdapter open() throws SQLException {
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
    public long create(long reference, int version) {
        // Create a sql date time format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date date = new Date();

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_REFERENCE, String.valueOf(reference));
        initialValues.put(KEY_VERSION, version);
        initialValues.put(KEY_NOTIFIED_AT, dateFormat.format(date));

        return mDb.replace(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Fetch a entry by it's reference.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    public boolean containsReference(long reference, int version) {
        Cursor cursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_REFERENCE},
                    KEY_REFERENCE + "=\"" + reference + "\" "
                    + "AND " + KEY_VERSION + "=" + version, 
                    null, null, null, null, null);

        boolean exists = false;

        if (cursor != null) {
            exists = cursor.moveToFirst();
            cursor.close();
        }

        return exists;
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
            db.execSQL("DROP TABLE IF EXISTS deviation_notifications");
            onCreate(db);
        }
    }
}
