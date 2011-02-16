package com.markupartist.sthlmtraveling.provider;

import java.util.HashMap;

import com.markupartist.sthlmtraveling.provider.SitesContentProvider.Site.Sites;

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

public class SitesContentProvider extends ContentProvider {

    private static final String TAG = "SitesContentProvider";
    private static final String DATABASE_NAME = "sites.db";
    private static final int DATABASE_VERSION = 1;
    private static final String SITES_TABLE_NAME = "sites";
    public static final String AUTHORITY = "com.markupartist.sthlmtraveling.sitesprovider";
    private static final UriMatcher sUriMatcher;
    private static final int SITES = 1;
    private static HashMap<String, String> sitesProjectionMap;

    private DatabaseHelper dbHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, SITES_TABLE_NAME, SITES);

        sitesProjectionMap = new HashMap<String, String>();
        sitesProjectionMap.put(Sites.SITE_ID, Sites.SITE_ID);
        sitesProjectionMap.put(Sites.TRAFFIC_SITE_ID, Sites.TRAFFIC_SITE_ID);
        sitesProjectionMap.put(Sites.NAME, Sites.NAME);
        sitesProjectionMap.put(Sites.PREFERRED_TRANSPORT_MODE, Sites.PREFERRED_TRANSPORT_MODE);
        sitesProjectionMap.put(Sites.IS_FAVORITE, Sites.IS_FAVORITE);
        sitesProjectionMap.put(Sites.POSITION, Sites.POSITION);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case SITES:
            return Sites.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != SITES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(SITES_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Sites.CONTENT_URI, rowId);
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
            case SITES:
                qb.setTables(SITES_TABLE_NAME);
                qb.setProjectionMap(sitesProjectionMap);
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
            case SITES:
                count = db.update(SITES_TABLE_NAME, values, selection, selectionArgs);
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
            db.execSQL("CREATE TABLE " + SITES_TABLE_NAME + " (" 
                    + Sites.SITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Sites.TRAFFIC_SITE_ID + " INTEGER, "
                    + Sites.NAME + " TEXT NOT NULL, "
                    + Sites.PREFERRED_TRANSPORT_MODE + " INTEGER, "
                    + Sites.IS_FAVORITE + " INTEGER, "
                    + Sites.POSITION + " INTEGER"
                    + ");"
                );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SITES_TABLE_NAME);
            onCreate(db);
        }
    }

    public static class Site {

        public Site() {
        }

        public static final class Sites implements BaseColumns {
            private Sites() {
            }

            public static final Uri CONTENT_URI = Uri.parse("content://"
                    + SitesContentProvider.AUTHORITY + "/sites");

            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sthlmtraveling.sites";

            public static final String SITE_ID = "_id";

            public static final String TRAFFIC_SITE_ID = "site_id";

            public static final String NAME = "name";

            public static final String PREFERRED_TRANSPORT_MODE = "preferred_transport_mode";

            public static final String IS_FAVORITE = "is_favorite";

            public static final String POSITION = "position";
        }

    }
}
