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

import com.markupartist.sthlmtraveling.provider.StarredJourneysProvider.StarredJourney.StarredJourneyColumns;

public class StarredJourneysProvider extends ContentProvider {
    private static final String TAG = "StarredJourneysProvider";

    private static final String DATABASE_NAME = "starredjourneys.db";
    private static final int DATABASE_VERSION = 1;
    private static final String STARRED_JOURNEYS_TABLE_NAME = "journeys";

    public static final String AUTHORITY =
        "com.markupartist.sthlmtraveling.starredjourneysprovider";
    private static final UriMatcher sUriMatcher;
    private static final int STARRED_JOURNEYS = 1;
    private static HashMap<String, String> sProjectionMap;

    private DatabaseHelper dbHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, STARRED_JOURNEYS_TABLE_NAME, STARRED_JOURNEYS);

        sProjectionMap = new HashMap<String, String>();
        sProjectionMap.put(StarredJourneyColumns.ID, StarredJourneyColumns.ID);
        sProjectionMap.put(StarredJourneyColumns.NAME, StarredJourneyColumns.NAME);
        sProjectionMap.put(StarredJourneyColumns.POSITION, StarredJourneyColumns.POSITION);
        sProjectionMap.put(StarredJourneyColumns.CREATED_AT, StarredJourneyColumns.CREATED_AT);
        sProjectionMap.put(StarredJourneyColumns.JOURNEY_DATA, StarredJourneyColumns.JOURNEY_DATA);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case STARRED_JOURNEYS:
                count = db.delete(STARRED_JOURNEYS_TABLE_NAME,
                        selection, selectionArgs);
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
        case STARRED_JOURNEYS:
            return StarredJourney.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != STARRED_JOURNEYS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        if (!values.containsKey(StarredJourneyColumns.CREATED_AT)) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            initialValues.put(StarredJourneyColumns.CREATED_AT,
                    dateFormat.format(date));
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(STARRED_JOURNEYS_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(StarredJourney.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
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

        switch (sUriMatcher.match(uri)) {
            case STARRED_JOURNEYS:
                qb.setTables(STARRED_JOURNEYS_TABLE_NAME);
                qb.setProjectionMap(sProjectionMap);
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
        int count;
        switch (sUriMatcher.match(uri)) {
            case STARRED_JOURNEYS:
                count = db.update(STARRED_JOURNEYS_TABLE_NAME,
                        values, selection, selectionArgs);
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
            db.execSQL("CREATE TABLE " + STARRED_JOURNEYS_TABLE_NAME + " (" 
                    + StarredJourneyColumns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + StarredJourneyColumns.NAME + " TEXT NULL, "
                    + StarredJourneyColumns.POSITION + " INTEGER, "
                    + StarredJourneyColumns.CREATED_AT + " DATE, "
                    + StarredJourneyColumns.JOURNEY_DATA + " TEXT NOT NULL"
                    + ");"
                );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + STARRED_JOURNEYS_TABLE_NAME);
            onCreate(db);
        }

    }

    public static class StarredJourney {
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + StarredJourneysProvider.AUTHORITY + "/journeys");
        public static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.sthlmtraveling.starredjourneys";


        public StarredJourney() {
        }

        public static final class StarredJourneyColumns implements BaseColumns {
            private StarredJourneyColumns() {
            }

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
             * When the entry was created.
             */
            public static final String CREATED_AT = "created_at";

            /**
             * The journey data as json.
             */
            public static final String JOURNEY_DATA = "journey_data";

        }

    }
}
