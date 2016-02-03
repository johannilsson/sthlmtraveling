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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.site.Site;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDbAdapter {
    private static final String DATABASE_NAME = "history";
    private static final String DATABASE_TABLE = "history";
    private static final int DATABASE_VERSION = 8;

    public static final String KEY_ROWID = "_id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_CREATED = "created";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
//    @Deprecated
//    public static final String KEY_SITE_ID = "site_id";
    public static final String KEY_LOCALITY = "locality";
    public static final String KEY_PLACE_ID = "place_id";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_CATEGORY = "category";

    public static final int INDEX_ROWID = 0;
    public static final int INDEX_TYPE = 1;
    public static final int INDEX_NAME = 2;
    public static final int INDEX_CREATED = 3;
    public static final int INDEX_LATITUDE = 4;
    public static final int INDEX_LONGITUDE = 5;
//    @Deprecated
//    public static final int INDEX_SITE_ID = 6;
    public static final int INDEX_LOCALITY = 6;
    public static final int INDEX_PLACE_ID = 7;
    public static final int INDEX_SOURCE = 8;
    public static final int INDEX_CATEGORY = 9;

    /**
     * @deprecated
     */
    @Deprecated
    public static final int TYPE_START_POINT = 0;
    /**
     * @deprecated
     */
    @Deprecated
    public static final int TYPE_END_POINT = 1;
    /**
     * Value for the departure type.
     */
    public static final int TYPE_DEPARTURE_SITE = 2;
    /**
     * @deprecated
     */
    @Deprecated
    public static final int TYPE_VIA_POINT = 3;
    /**
     * Value for the journey planner site.
     */
    public static final int TYPE_JOURNEY_PLANNER_SITE = 4;


    private static final String TAG = "HistoryDbAdapter";
    private static final String[] ALL = new String[] {
            KEY_ROWID,
            KEY_TYPE,
            KEY_NAME,
            KEY_CREATED,
            KEY_LATITUDE,
            KEY_LONGITUDE,
            KEY_LOCALITY,
            KEY_PLACE_ID,
            KEY_SOURCE,
            KEY_CATEGORY,
    };

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
                + ", latitude INTEGER NULL"
                + ", longitude INTEGER NULL"
                + ", locality TEXT NULL"
                + ", place_id TEXT NULL"
                + ", source INTEGER NULL"
                + ", category INTEGER NULL"
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
     * @param site a site
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    public long create(int type, Site site) {
        if (site.isMyLocation() || !site.looksValid()) {
            return -1;
        }

        Log.d(TAG, "Storing: " + site);

        // Create a sql date time format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date date = new Date();

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TYPE, type);
        initialValues.put(KEY_NAME, site.getName());
        initialValues.put(KEY_LOCALITY, site.getLocality());
        if (site.hasLocation()) {
            initialValues.put(KEY_LATITUDE, (int)(site.getLocation().getLatitude() * 1E6));
            initialValues.put(KEY_LONGITUDE, (int)(site.getLocation().getLongitude() * 1E6));
        }
        initialValues.put(KEY_PLACE_ID, site.getId());
        initialValues.put(KEY_SOURCE, site.getSource());

        initialValues.put(KEY_CREATED, dateFormat.format(date));

        int category = Site.CATEGORY_UNKNOWN;
        if (site.hasType()) {
            String categoryType = site.getType();
            category = Site.TYPE_TRANSIT_STOP.equals(categoryType) ?
                    Site.CATEGORY_TRANSIT_STOP :
                    Site.CATEGORY_ADDRESS;
        }
        initialValues.put(KEY_CATEGORY, category);

        Cursor rowCursor = fetchByName(type, site.getName());
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
        String selection = KEY_NAME + "=? AND " + KEY_TYPE + "=?";
        String[] selectionArgs = new String[] {name, String.valueOf(type)};
        Cursor cursor = mDb.query(DATABASE_TABLE, ALL, selection,
                selectionArgs, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Fetch all transit stop entries
     *
     * @return a Cursor object
     */
    public Cursor fetchStops() {
        String[] params = new String[]{
                String.valueOf(Site.CATEGORY_TRANSIT_STOP),
                String.valueOf(Site.SOURCE_STHLM_TRAVELING)
        };
        // Can we filter out duplicates.
        return mDb.query(true, DATABASE_TABLE, ALL,
                KEY_CATEGORY + "= ? AND " +
                KEY_SOURCE + "= ?", params, null, null,
                KEY_CREATED + " DESC", "15");
    }

    /**
     * Fetch all entries for a specific type.
     * @return a Cursor object
     */
    public Cursor fetchLatest() {
        return mDb.query(true, DATABASE_TABLE, ALL,
                null, null, null, null,
                KEY_CREATED + " DESC", "20");
    }

    
    public void deleteAll() {
        mDb.delete(DATABASE_TABLE, null, null);
    }

    public static Site mapToSite(Cursor cursor) {
        Site site = new Site();
        site.setName(cursor.getString(HistoryDbAdapter.INDEX_NAME));
        site.setLocation(
                cursor.getInt(HistoryDbAdapter.INDEX_LATITUDE),
                cursor.getInt(HistoryDbAdapter.INDEX_LONGITUDE));
        site.setId(cursor.getString(HistoryDbAdapter.INDEX_PLACE_ID));
        site.setSource(cursor.getInt(HistoryDbAdapter.INDEX_SOURCE));
        site.setLocality(cursor.getString(HistoryDbAdapter.INDEX_LOCALITY));

        int category = cursor.getInt(HistoryDbAdapter.INDEX_CATEGORY);
        String categoryType = null;
        if (category == Site.CATEGORY_TRANSIT_STOP) {
            categoryType = Site.TYPE_TRANSIT_STOP;
        } else if (category == Site.CATEGORY_ADDRESS) {
            categoryType = Site.TYPE_ADDRESS;
        }
        site.setType(categoryType);
        return site;
    }

    /**
     * Internal helper for the database.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) throws SQLException {
            db.execSQL(DATABASE_CREATE);
        }

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
                    case 4:
                    case 5:
                        success = upgradeToVersion5(db);
                        break;
                    case 6:
                        success = upgradeToVersion6(db);
                        break;
                    case 7:
                        success = upgradeToVersion7(db);
                        break;
                    case 8:
                        success = upgradeToVersion8(db);
                        break;
                }
            }

            if (success) {
                db.setTransactionSuccessful();
            }
            db.endTransaction();
        }

        private boolean upgradeToVersion8(SQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN category INTEGER NULL;");
            } catch (SQLException e) {
                Log.e(TAG, "Upgrade to version 8 failed, " + e.getMessage());
                return false;
            }

            return true;
        }

        private boolean upgradeToVersion7(SQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN locality TEXT NULL;");
                db.execSQL("ALTER TABLE history ADD COLUMN source INTEGER NULL;");
                db.execSQL("ALTER TABLE history ADD COLUMN place_id TEXT NULL;");
                db.execSQL("UPDATE history SET place_id = site_id;");
            } catch (SQLException e) {
                Log.e(TAG, "Upgrade to version 7 failed, " + e.getMessage());
                return false;
            }

            return true;
        }

        private boolean upgradeToVersion6(SQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN latitude INTEGER NULL;");
                db.execSQL("ALTER TABLE history ADD COLUMN longitude INTEGER NULL;");
                db.execSQL("ALTER TABLE history ADD COLUMN site_id INTEGER NULL;");
            } catch (SQLException e) {
                Log.e(TAG, "Upgrade to version 6 failed, " + e.getMessage());
                return false;
            }

            return true;
        }

        private boolean upgradeToVersion5(SQLiteDatabase db) {
            try {
                db.execSQL("DROP TABLE IF EXISTS history");
                onCreate(db);
            } catch (SQLException e) {
                Log.e(TAG, "Upgrade to version 5 failed, " + e.getMessage());
                return false;
            }

            return true;
        }
    }

}
