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
package com.markupartist.sthlmtraveling.provider.deviation

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviationNotificationDbAdapter
/**
 * Constructor - takes the context to allow the database to be
 * opened/created
 *
 * @param mContext the Context within which to work
 */(private val mContext: Context?) {
    private var mDbHelper: DatabaseHelper? = null
    private var mDb: SQLiteDatabase? = null

    /**
     * Open the deviation notification database. If it cannot be opened it
     * tries to create a new instance of the database. If it cannot be
     * created, throw an exception to signal the failure.
     *
     * @return this (self reference, allowing this to be chained in an
     * initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Throws(SQLException::class)
    fun open(): DeviationNotificationDbAdapter {
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
     * @param name the name
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    fun create(reference: Long, version: Int): Long {
        // Create a sql date time format
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = Date()

        val initialValues = ContentValues()
        initialValues.put(KEY_REFERENCE, reference.toString())
        initialValues.put(KEY_VERSION, version)
        initialValues.put(KEY_NOTIFIED_AT, dateFormat.format(date))

        return mDb!!.replace(DATABASE_TABLE, null, initialValues)
    }

    /**
     * Fetch a entry by it's reference.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    fun containsReference(reference: Long, version: Int): Boolean {
        val cursor =
            mDb!!.query(
                true, DATABASE_TABLE, arrayOf<String>(KEY_ROWID, KEY_REFERENCE),
                (KEY_REFERENCE + "=\"" + reference + "\" "
                        + "AND " + KEY_VERSION + "=" + version),
                null, null, null, null, null
            )

        var exists = false

        if (cursor != null) {
            exists = cursor.moveToFirst()
            cursor.close()
        }

        return exists
    }

    /**
     * Internal helper for the database.
     */
    private class DatabaseHelper(context: Context?) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(
                TAG, ("Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data")
            )
            db.execSQL("DROP TABLE IF EXISTS deviation_notifications")
            onCreate(db)
        }
    }

    companion object {
        const val KEY_ROWID: String = "_id"
        const val KEY_REFERENCE: String = "reference"
        const val KEY_VERSION: String = "version"
        const val KEY_NOTIFIED_AT: String = "notified_at"

        private const val DATABASE_NAME = "deviation_notifications"
        private const val DATABASE_TABLE = "deviation_notifications"
        private const val DATABASE_VERSION = 3

        private const val TAG = "DeviationNotificationDb"

        /**
         * Database creation sql statement
         */
        private val DATABASE_CREATE = ("CREATE TABLE deviation_notifications ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", reference TEXT NOT NULL"
                + ", version INTEGER NOT NULL"
                + ", notified_at DATE"
                + ");")
    }
}
