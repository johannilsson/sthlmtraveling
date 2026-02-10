/*
 * Copyright (C) 2011 Johan Nilsson <http://markupartist.com>
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

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log
import com.markupartist.sthlmtraveling.BuildConfig
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JourneysProvider : ContentProvider() {
    private var dbHelper: DatabaseHelper? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper!!.getWritableDatabase()
        var finalSelection: String?

        val count: Int

        when (sUriMatcher.match(uri)) {
            JOURNEYS -> count = db.delete(
                JOURNEYS_TABLE_NAME,
                selection, selectionArgs
            )

            JOURNEY_ID -> {
                // Starts a final WHERE clause by restricting it to the
                // desired journey ID.
                finalSelection = BaseColumns._ID + " = " +
                        uri.getPathSegments()[Journeys.JOURNEY_ID_PATH_POSITION]

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (selection != null) {
                    finalSelection = finalSelection + " AND " + selection
                }

                count = db.delete(
                    JOURNEYS_TABLE_NAME,
                    finalSelection,
                    selectionArgs
                )
            }

            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }

        getContext()!!.getContentResolver().notifyChange(uri, null)

        return count
    }

    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            JOURNEYS -> Journeys.CONTENT_TYPE
            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        require(sUriMatcher.match(uri) == JOURNEYS) { "Unknown URI " + uri }

        val values: ContentValues?
        if (initialValues != null) {
            values = ContentValues(initialValues)
        } else {
            values = ContentValues()
        }

        if (!values.containsKey(Journeys.CREATED_AT)) {
            val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = Date()

            values.put(
                Journeys.CREATED_AT,
                dateFormat.format(date)
            )
            values.put(
                Journeys.UPDATED_AT,
                dateFormat.format(date)
            )
        }

        val db = dbHelper!!.getWritableDatabase()
        val rowId = db.insert(JOURNEYS_TABLE_NAME, null, values)
        if (rowId > 0) {
            val journeyUri = ContentUris.withAppendedId(Journeys.CONTENT_URI, rowId)
            getContext()!!.getContentResolver().notifyChange(journeyUri, null)
            return journeyUri
        }

        throw SQLException("Failed to insert row into " + uri)
    }

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(getContext())
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val qb = SQLiteQueryBuilder()
        qb.setTables(JOURNEYS_TABLE_NAME)

        when (sUriMatcher.match(uri)) {
            JOURNEYS -> qb.setProjectionMap(sJourneysProjectionMap)
            JOURNEY_ID -> {
                qb.setProjectionMap(sJourneysProjectionMap)
                qb.appendWhere(
                    BaseColumns._ID + "=" +
                            uri.getPathSegments()[Journeys.JOURNEY_ID_PATH_POSITION]
                )
            }

            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }

        val db = dbHelper!!.getReadableDatabase()
        val c = qb.query(
            db, projection, selection,
            selectionArgs, null, null, sortOrder
        )

        c.setNotificationUri(getContext()!!.getContentResolver(), uri)
        return c
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val db = dbHelper!!.getWritableDatabase()
        var finalSelection: String?

        if (values != null && !values.containsKey(Journeys.UPDATED_AT)
            && !values.containsKey(Journeys.STARRED)
        ) {
            val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = Date()
            values.put(
                Journeys.UPDATED_AT,
                dateFormat.format(date)
            )
        }

        val count: Int

        when (sUriMatcher.match(uri)) {
            JOURNEYS -> count = db.update(
                JOURNEYS_TABLE_NAME,
                values, selection, selectionArgs
            )

            JOURNEY_ID -> {
                // Get the journey ID from the incoming URI.
                //String noteId =
                //    uri.getPathSegments().get(Journeys.JOURNEY_ID_PATH_POSITION);

                // Starts creating the final WHERE clause by restricting it to
                // the incoming ID.
                finalSelection =
                    BaseColumns._ID +
                            " = " +
                            uri.getPathSegments()[Journeys.JOURNEY_ID_PATH_POSITION]

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (selection != null) {
                    finalSelection = finalSelection + " AND " + selection
                }

                count = db.update(
                    JOURNEYS_TABLE_NAME,
                    values,
                    finalSelection,
                    selectionArgs
                )
            }

            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }

        getContext()!!.getContentResolver().notifyChange(uri, null)
        return count
    }

    /**
     * Internal helper for the database.
     */
    private class DatabaseHelper(context: Context?) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            Log.w(TAG, "Creating new database.")
            db.execSQL(
                ("CREATE TABLE " + JOURNEYS_TABLE_NAME + " ("
                        + Journeys.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + Journeys.NAME + " TEXT NULL, "
                        + Journeys.POSITION + " INTEGER, "
                        + Journeys.STARRED + " INTEGER, "
                        + Journeys.CREATED_AT + " DATE, "
                        + Journeys.UPDATED_AT + " DATE, "
                        + Journeys.JOURNEY_DATA + " TEXT NOT NULL"
                        + ");")
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(
                TAG, ("Upgrading database from version " + oldVersion + " to " + newVersion
                        + ", which will destroy all old data")
            )
            db.execSQL("DROP TABLE IF EXISTS " + JOURNEYS_TABLE_NAME)
            onCreate(db)
        }
    }

    class Journey {
        object Journeys : BaseColumns {
            @JvmField
            val CONTENT_URI: Uri = Uri.parse("content://" + AUTHORITY + "/journeys")
            const val CONTENT_TYPE: String = "vnd.android.cursor.dir/vnd.sthlmtraveling.journeys"

            const val JOURNEY_ID_PATH_POSITION: Int = 1

            /**
             * The default number of journeys to be saved in history.
             *
             * Total number of journeys is history size + starred journeys.
             */
            const val DEFAULT_HISTORY_SIZE: Int = 5

            const val ID: String = "_id"

            /**
             * The name, example "Take me home" or "To work".
             */
            const val NAME: String = "name"

            /**
             * The position in a list.
             */
            const val POSITION: String = "position"

            /**
             * If the journey is starred.
             */
            const val STARRED: String = "starred"

            /**
             * When the entry was created.
             */
            const val CREATED_AT: String = "created_at"

            /**
             * When the entry was updated.
             */
            const val UPDATED_AT: String = "updated_at"

            /**
             * The journey data as json.
             */
            const val JOURNEY_DATA: String = "journey_data"

            /**
             * The default sort order.
             */
            const val DEFAULT_SORT_ORDER: String = "position DESC, updated_at DESC, created_at DESC"

            /**
             * The history sort order, this also includes a limit.
             */
            @JvmField
            val HISTORY_SORT_ORDER: String =
                "updated_at DESC, created_at DESC LIMIT " + DEFAULT_HISTORY_SIZE
        }
    }

    companion object {
        private const val TAG = "JourneysProvider"

        private const val DATABASE_NAME = "journeys.db"
        private const val DATABASE_VERSION = 2
        private const val JOURNEYS_TABLE_NAME = "journeys"

        val AUTHORITY: String = BuildConfig.BASE_PROVIDER_AUTHORITY + ".journeysprovider"

        /**
         * A UriMatcher instance
         */
        private val sUriMatcher: UriMatcher

        /**
         * The incoming URI matches the Journeys URI pattern.
         */
        private const val JOURNEYS = 1

        /**
         * The incoming URI matches the Journey ID URI pattern
         */
        private const val JOURNEY_ID = 2

        /**
         * A projection map used to select columns from the database.
         */
        private val sJourneysProjectionMap: HashMap<String, String> = hashMapOf(
            Journeys.ID to Journeys.ID,
            Journeys.NAME to Journeys.NAME,
            Journeys.POSITION to Journeys.POSITION,
            Journeys.STARRED to Journeys.STARRED,
            Journeys.CREATED_AT to Journeys.CREATED_AT,
            Journeys.UPDATED_AT to Journeys.UPDATED_AT,
            Journeys.JOURNEY_DATA to Journeys.JOURNEY_DATA
        )

        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher.addURI(AUTHORITY, "journeys", JOURNEYS)
            sUriMatcher.addURI(AUTHORITY, "journeys/#", JOURNEY_ID)
        }
    }
}
