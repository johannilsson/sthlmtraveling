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

package com.markupartist.sthlmtraveling.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;

public class JourneysProvider extends ContentProvider {
    private static final String TAG = "JourneysProvider";

    private static final String DATABASE_NAME = "journeys.db";
    private static final int DATABASE_VERSION = 2;
    private static final String JOURNEYS_TABLE_NAME = "journeys";

    public static final String AUTHORITY =
        "com.markupartist.sthlmtraveling.journeysprovider";

    /**
     * A UriMatcher instance
     */
    private static final UriMatcher sUriMatcher;

    /**
     * The incoming URI matches the Journeys URI pattern.
     */
    private static final int JOURNEYS = 1;

    /**
     * The incoming URI matches the Journey ID URI pattern
     */
    private static final int JOURNEY_ID = 2;

    /**
     * A projection map used to select columns from the database.
     */
    private static HashMap<String, String> sJourneysProjectionMap;

    private DatabaseHelper dbHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "journeys", JOURNEYS);
        sUriMatcher.addURI(AUTHORITY, "journeys/#", JOURNEY_ID);

        sJourneysProjectionMap = new HashMap<String, String>();
        sJourneysProjectionMap.put(Journeys.ID, Journeys.ID);
        sJourneysProjectionMap.put(Journeys.NAME, Journeys.NAME);
        sJourneysProjectionMap.put(Journeys.POSITION, Journeys.POSITION);
        sJourneysProjectionMap.put(Journeys.STARRED, Journeys.STARRED);
        sJourneysProjectionMap.put(Journeys.CREATED_AT, Journeys.CREATED_AT);
        sJourneysProjectionMap.put(Journeys.UPDATED_AT, Journeys.UPDATED_AT);
        sJourneysProjectionMap.put(Journeys.JOURNEY_DATA, Journeys.JOURNEY_DATA);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String finalSelection;

        int count;

        switch (sUriMatcher.match(uri)) {
            case JOURNEYS:
                count = db.delete(JOURNEYS_TABLE_NAME,
                        selection, selectionArgs);
                break;
            case JOURNEY_ID:
                // Starts a final WHERE clause by restricting it to the
                // desired journey ID.
                finalSelection = Journeys._ID + " = " +
                    uri.getPathSegments().get(Journeys.JOURNEY_ID_PATH_POSITION);

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (selection != null) {
                    finalSelection = finalSelection + " AND " + selection;
                }

                count = db.delete(
                    JOURNEYS_TABLE_NAME,
                    finalSelection,
                    selectionArgs
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case JOURNEYS:
            return Journeys.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != JOURNEYS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        if (!values.containsKey(Journeys.CREATED_AT)) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();

            values.put(Journeys.CREATED_AT,
                    dateFormat.format(date));
            values.put(Journeys.UPDATED_AT,
                    dateFormat.format(date));
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(JOURNEYS_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri journeyUri = ContentUris.withAppendedId(Journeys.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(journeyUri, null);
            return journeyUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(JOURNEYS_TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case JOURNEYS:
                qb.setProjectionMap(sJourneysProjectionMap);
                break;
            case JOURNEY_ID:
                qb.setProjectionMap(sJourneysProjectionMap);
                qb.appendWhere(Journeys._ID + "=" +
                    uri.getPathSegments().get(Journeys.JOURNEY_ID_PATH_POSITION));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String finalSelection;

        if (!values.containsKey(Journeys.UPDATED_AT)
                && !values.containsKey(Journeys.STARRED)) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            values.put(Journeys.UPDATED_AT,
                    dateFormat.format(date));
        }

        int count;

        switch (sUriMatcher.match(uri)) {
            case JOURNEYS:
                count = db.update(JOURNEYS_TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            case JOURNEY_ID:
                // Get the journey ID from the incoming URI.
                //String noteId =
                //    uri.getPathSegments().get(Journeys.JOURNEY_ID_PATH_POSITION);

                // Starts creating the final WHERE clause by restricting it to
                // the incoming ID.
                finalSelection =
                        Journeys._ID +
                        " = " +
                        uri.getPathSegments().
                            get(Journeys.JOURNEY_ID_PATH_POSITION)
                ;

                // If there were additional selection criteria, append them to
                // the final WHERE clause.
                if (selection !=null) {
                    finalSelection = finalSelection + " AND " + selection;
                }

                count = db.update(
                    JOURNEYS_TABLE_NAME,
                    values,
                    finalSelection,
                    selectionArgs
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
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
            Log.w(TAG, "Creating new database.");
            db.execSQL("CREATE TABLE " + JOURNEYS_TABLE_NAME + " (" 
                    + Journeys.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Journeys.NAME + " TEXT NULL, "
                    + Journeys.POSITION + " INTEGER, "
                    + Journeys.STARRED + " INTEGER, "
                    + Journeys.CREATED_AT + " DATE, "
                    + Journeys.UPDATED_AT + " DATE, "
                    + Journeys.JOURNEY_DATA + " TEXT NOT NULL"
                    + ");"
                );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + JOURNEYS_TABLE_NAME);
            onCreate(db);
        }

    }

    public static class Journey {

        public Journey() {
        }

        public static final class Journeys implements BaseColumns {
            public static final Uri CONTENT_URI =
                Uri.parse("content://" + JourneysProvider.AUTHORITY + "/journeys");
            public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.sthlmtraveling.journeys";
            
            public static final int JOURNEY_ID_PATH_POSITION = 1;

            /**
             * The default number of journeys to be saved in history.
             * </p>
             * Total number of journeys is history size + starred journeys.
             */
            public static final int DEFAULT_HISTORY_SIZE = 5;

            public static final String ID = "_id";

            /**
             * The name, example "Take me home" or "To work".
             */
            public static final String NAME = "name";

            /**
             * The position in a list.
             */
            public static final String POSITION = "position";

            /**
             * If the journey is starred.
             */
            public static final String STARRED = "starred";

            /**
             * When the entry was created.
             */
            public static final String CREATED_AT = "created_at";

            /**
             * When the entry was updated.
             */
            public static final String UPDATED_AT = "updated_at";

            /**
             * The journey data as json.
             */
            public static final String JOURNEY_DATA = "journey_data";

            /**
             * The default sort order.
             */
            public static final String DEFAULT_SORT_ORDER =
                "position DESC, updated_at DESC, created_at DESC";

            /**
             * The history sort order, this also includes a limit.
             */
            public static final String HISTORY_SORT_ORDER =
                "updated_at DESC, created_at DESC LIMIT " + DEFAULT_HISTORY_SIZE;


            private Journeys() {
            }
        }

    }
}
