package com.markupartist.sthlmtraveling.provider;

import java.util.HashMap;

import com.markupartist.sthlmtraveling.provider.PlacesProvider.Place.Places;

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

public class PlacesProvider extends ContentProvider {

    private static final String TAG = "PlacesProvider";
    private static final String DATABASE_NAME = "places.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PLACES_TABLE_NAME = "places";
    public static final String AUTHORITY = "com.markupartist.sthlmtraveling.placesprovider";
    private static final UriMatcher sUriMatcher;
    private static final int PLACES = 1;
    private static HashMap<String, String> sPlacesProjectionMap;

    private DatabaseHelper dbHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, PLACES_TABLE_NAME, PLACES);

        sPlacesProjectionMap = new HashMap<String, String>();
        sPlacesProjectionMap.put(Places.PLACE_ID, Places.PLACE_ID);
        sPlacesProjectionMap.put(Places.SITE_ID, Places.SITE_ID);
        sPlacesProjectionMap.put(Places.NAME, Places.NAME);
        sPlacesProjectionMap.put(Places.PREFERRED_TRANSPORT_MODE,
                Places.PREFERRED_TRANSPORT_MODE);
        sPlacesProjectionMap.put(Places.STARRED, Places.STARRED);
        sPlacesProjectionMap.put(Places.POSITION, Places.POSITION);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case PLACES:
                count = db.delete(PLACES_TABLE_NAME, selection, selectionArgs);
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
        case PLACES:
            return Places.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != PLACES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(PLACES_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Places.CONTENT_URI, rowId);
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
            case PLACES:
                qb.setTables(PLACES_TABLE_NAME);
                qb.setProjectionMap(sPlacesProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case PLACES:
                count = db.update(PLACES_TABLE_NAME, values, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PLACES_TABLE_NAME + " (" 
                    + Places.PLACE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Places.SITE_ID + " INTEGER, "
                    + Places.NAME + " TEXT NOT NULL, "
                    + Places.PREFERRED_TRANSPORT_MODE + " INTEGER, "
                    + Places.STARRED + " INTEGER, "
                    + Places.POSITION + " INTEGER"
                    + ");"
                );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + PLACES_TABLE_NAME);
            onCreate(db);
        }
    }

    public static class Place {

        public Place() {
        }

        public static final class Places implements BaseColumns {
            private Places() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + PlacesProvider.AUTHORITY + "/places");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sthlmtraveling.places";

            public static final String PLACE_ID = "_id";

            public static final String SITE_ID = "site_id";

            public static final String NAME = "name";

            public static final String PREFERRED_TRANSPORT_MODE = "preferred_transport_mode";

            public static final String STARRED = "starred";

            public static final String POSITION = "position";
        }

    }
}
