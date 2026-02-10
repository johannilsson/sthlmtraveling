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

class HistoryDbAdapter
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
    fun open(): HistoryDbAdapter {
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
     * Creates a new entry. If the entry already exists it will be updated
     * with a new time stamp.
     * @param type the type
     * @param site a site
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    fun create(type: Int, site: Site): Long {
        if (site.isMyLocation || !site.looksValid()) {
            return -1
        }

        Log.d(TAG, "Storing: " + site)

        // Create a sql date time format
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = Date()

        val initialValues = ContentValues()
        initialValues.put(KEY_TYPE, type)
        initialValues.put(KEY_NAME, site.name)
        initialValues.put(KEY_LOCALITY, site.locality)
        if (site.hasLocation()) {
            initialValues.put(KEY_LATITUDE, (site.location!!.getLatitude() * 1E6).toInt())
            initialValues.put(KEY_LONGITUDE, (site.location!!.getLongitude() * 1E6).toInt())
        }
        initialValues.put(KEY_PLACE_ID, site.id)
        initialValues.put(KEY_SOURCE, site.source)

        initialValues.put(KEY_CREATED, dateFormat.format(date))

        var category = Site.CATEGORY_UNKNOWN
        if (site.hasType()) {
            val categoryType = site.type
            category =
                if (Site.TYPE_TRANSIT_STOP == categoryType) Site.CATEGORY_TRANSIT_STOP else Site.CATEGORY_ADDRESS
        }
        initialValues.put(KEY_CATEGORY, category)

        val rowCursor = fetchByName(type, site.name)
        if (rowCursor.getCount() >= 1) {
            initialValues.put(
                KEY_ROWID,
                rowCursor.getInt(rowCursor.getColumnIndex(KEY_ROWID))
            )
        }
        rowCursor.close()

        return mDb!!.replace(DATABASE_TABLE, null, initialValues)
    }

    /**
     * Fetch a entry by name.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    fun fetchByName(type: Int, name: String?): Cursor {
        val selection: String = KEY_NAME + "=? AND " + KEY_TYPE + "=?"
        val selectionArgs = arrayOf(name, type.toString())
        val cursor = mDb!!.query(
            DATABASE_TABLE, ALL, selection,
            selectionArgs, null, null, null
        )
        if (cursor != null) {
            cursor.moveToFirst()
        }
        return cursor
    }

    /**
     * Fetch all transit stop entries
     *
     * @return a Cursor object
     */
    fun fetchStops(): Cursor {
        val params = arrayOf(
            Site.CATEGORY_TRANSIT_STOP.toString(),
            Site.SOURCE_STHLM_TRAVELING.toString()
        )
        // Can we filter out duplicates.
        return mDb!!.query(
            true, DATABASE_TABLE, ALL,
            KEY_CATEGORY + "= ? AND " +
                    KEY_SOURCE + "= ?", params, null, null,
            KEY_CREATED + " DESC", "15"
        )
    }

    /**
     * Fetch all entries for a specific type.
     * @return a Cursor object
     */
    fun fetchLatest(): Cursor {
        return mDb!!.query(
            true, DATABASE_TABLE, ALL,
            null, null, null, null,
            KEY_CREATED + " DESC", "20"
        )
    }


    fun deleteAll() {
        mDb!!.delete(DATABASE_TABLE, null, null)
    }

    /**
     * Internal helper for the database.
     */
    private class DatabaseHelper(context: Context?) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        @Throws(SQLException::class)
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE)
        }

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
                    1, 2, 3, 4, 5 -> success = upgradeToVersion5(db)
                    6 -> success = upgradeToVersion6(db)
                    7 -> success = upgradeToVersion7(db)
                    8 -> success = upgradeToVersion8(db)
                }
            }

            if (success) {
                db.setTransactionSuccessful()
            }
            db.endTransaction()
        }

        fun upgradeToVersion8(db: SQLiteDatabase): Boolean {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN category INTEGER NULL;")
            } catch (e: SQLException) {
                Log.e(TAG, "Upgrade to version 8 failed, " + e.message)
                return false
            }

            return true
        }

        fun upgradeToVersion7(db: SQLiteDatabase): Boolean {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN locality TEXT NULL;")
                db.execSQL("ALTER TABLE history ADD COLUMN source INTEGER NULL;")
                db.execSQL("ALTER TABLE history ADD COLUMN place_id TEXT NULL;")
                db.execSQL("UPDATE history SET place_id = site_id;")
            } catch (e: SQLException) {
                Log.e(TAG, "Upgrade to version 7 failed, " + e.message)
                return false
            }

            return true
        }

        fun upgradeToVersion6(db: SQLiteDatabase): Boolean {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN latitude INTEGER NULL;")
                db.execSQL("ALTER TABLE history ADD COLUMN longitude INTEGER NULL;")
                db.execSQL("ALTER TABLE history ADD COLUMN site_id INTEGER NULL;")
            } catch (e: SQLException) {
                Log.e(TAG, "Upgrade to version 6 failed, " + e.message)
                return false
            }

            return true
        }

        fun upgradeToVersion5(db: SQLiteDatabase): Boolean {
            try {
                db.execSQL("DROP TABLE IF EXISTS history")
                onCreate(db)
            } catch (e: SQLException) {
                Log.e(TAG, "Upgrade to version 5 failed, " + e.message)
                return false
            }

            return true
        }
    }

    companion object {
        private const val DATABASE_NAME = "history"
        private const val DATABASE_TABLE = "history"
        private const val DATABASE_VERSION = 8

        const val KEY_ROWID: String = "_id"
        const val KEY_TYPE: String = "type"
        const val KEY_NAME: String = "name"
        const val KEY_CREATED: String = "created"
        const val KEY_LATITUDE: String = "latitude"
        const val KEY_LONGITUDE: String = "longitude"

        //    @Deprecated
        //    public static final String KEY_SITE_ID = "site_id";
        const val KEY_LOCALITY: String = "locality"
        const val KEY_PLACE_ID: String = "place_id"
        const val KEY_SOURCE: String = "source"
        const val KEY_CATEGORY: String = "category"

        const val INDEX_ROWID: Int = 0
        const val INDEX_TYPE: Int = 1
        const val INDEX_NAME: Int = 2
        const val INDEX_CREATED: Int = 3
        const val INDEX_LATITUDE: Int = 4
        const val INDEX_LONGITUDE: Int = 5

        //    @Deprecated
        //    public static final int INDEX_SITE_ID = 6;
        const val INDEX_LOCALITY: Int = 6
        const val INDEX_PLACE_ID: Int = 7
        const val INDEX_SOURCE: Int = 8
        const val INDEX_CATEGORY: Int = 9

        @Deprecated("")
        const val TYPE_START_POINT: Int = 0

        @Deprecated("")
        const val TYPE_END_POINT: Int = 1

        /**
         * Value for the departure type.
         */
        const val TYPE_DEPARTURE_SITE: Int = 2

        @Deprecated("")
        const val TYPE_VIA_POINT: Int = 3

        /**
         * Value for the journey planner site.
         */
        const val TYPE_JOURNEY_PLANNER_SITE: Int = 4


        private const val TAG = "HistoryDbAdapter"
        private val ALL: Array<String> = arrayOf(
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
        )

        /**
         * Database creation sql statement
         */
        private val DATABASE_CREATE = ("CREATE TABLE history ("
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
                + ");")

        @JvmStatic
        fun mapToSite(cursor: Cursor): Site {
            val site = Site()
            site.name = cursor.getString(INDEX_NAME)
            site.setLocation(
                cursor.getInt(INDEX_LATITUDE),
                cursor.getInt(INDEX_LONGITUDE)
            )
            site.setId(cursor.getString(INDEX_PLACE_ID))
            site.source = cursor.getInt(INDEX_SOURCE)
            site.locality = cursor.getString(INDEX_LOCALITY)

            val category = cursor.getInt(INDEX_CATEGORY)
            var categoryType: String? = null
            if (category == Site.CATEGORY_TRANSIT_STOP) {
                categoryType = Site.TYPE_TRANSIT_STOP
            } else if (category == Site.CATEGORY_ADDRESS) {
                categoryType = Site.TYPE_ADDRESS
            }
            site.type = categoryType
            return site
        }
    }
}
