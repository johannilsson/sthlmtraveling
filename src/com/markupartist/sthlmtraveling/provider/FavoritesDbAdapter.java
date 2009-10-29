package com.markupartist.sthlmtraveling.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FavoritesDbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_START_POINT = "start_point";
    public static final String KEY_END_POINT = "end_point";
    public static final String KEY_CREATED = "created";

    private static final String DATABASE_NAME = "favorite";
    private static final String DATABASE_TABLE = "favorites";
    private static final int DATABASE_VERSION = 3;

    private static final String TAG = "FavoritesDbAdapter";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mContext;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "CREATE TABLE favorites (" 
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", start_point TEXT NOT NULL" 
                + ", end_point TEXT NOT NULL"
                + ", created date"
                + ");";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param context the Context within which to work
     */
    public FavoritesDbAdapter(Context context) {
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
    public FavoritesDbAdapter open() throws SQLException {
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
     * Creates a new entry.
     * @param startPoint the start point
     * @param endPoint the endPoint
     * @return the row id associated with the created entry or -1 of an error
     * occurred
     */
    public long create(String startPoint, String endPoint) {
        // Create a sql date time format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_START_POINT, startPoint);
        initialValues.put(KEY_END_POINT, endPoint);
        initialValues.put(KEY_CREATED, dateFormat.format(date));

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete an entry with the given id.
     * @param id the id which entry should be deleted
     * @return true if deleted, false otherwise
     */
    public boolean delete(long id) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + id, null) > 0;
    }

    /**
     * Fetch a entry by name.
     * @param type the type
     * @param name the name
     * @return a Cursor object positioned at the first entry
     */
    public Cursor fetch(String startPoint, String endPoint) {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_START_POINT, KEY_END_POINT}, 
                        KEY_START_POINT+ "=\"" + startPoint + "\"" 
                        + " AND " + KEY_END_POINT + "=\"" + endPoint + "\"", 
                    null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Fetch all entries, ordered by created.
     * @return a Cursor object
     */
    public Cursor fetch() {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_START_POINT, KEY_END_POINT}, null, null,
                    null, null, KEY_CREATED + " DESC", null);
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
            db.execSQL("DROP TABLE IF EXISTS favorites");
            onCreate(db);
        }
    }
}
