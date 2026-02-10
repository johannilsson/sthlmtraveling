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
import com.markupartist.sthlmtraveling.provider.PlacesProvider.Place.Places

class PlacesProvider : ContentProvider() {
    private var dbHelper: DatabaseHelper? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper!!.getWritableDatabase()
        val count: Int
        when (sUriMatcher.match(uri)) {
            PLACES -> count = db.delete(PLACES_TABLE_NAME, selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }

        getContext()!!.getContentResolver().notifyChange(uri, null)
        return count
    }

    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            PLACES -> Places.CONTENT_TYPE
            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        require(sUriMatcher.match(uri) == PLACES) { "Unknown URI " + uri }

        val values: ContentValues?
        if (initialValues != null) {
            values = ContentValues(initialValues)
        } else {
            values = ContentValues()
        }

        val db = dbHelper!!.getWritableDatabase()
        val rowId = db.insert(PLACES_TABLE_NAME, null, values)
        if (rowId > 0) {
            val noteUri = ContentUris.withAppendedId(Places.CONTENT_URI, rowId)
            getContext()!!.getContentResolver().notifyChange(noteUri, null)
            return noteUri
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

        when (sUriMatcher.match(uri)) {
            PLACES -> {
                qb.setTables(PLACES_TABLE_NAME)
                qb.setProjectionMap(sPlacesProjectionMap)
            }

            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }

        val db = dbHelper!!.getReadableDatabase()
        val c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder)

        c.setNotificationUri(getContext()!!.getContentResolver(), uri)
        return c
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val db = dbHelper!!.getWritableDatabase()
        val count: Int
        when (sUriMatcher.match(uri)) {
            PLACES -> count = db.update(PLACES_TABLE_NAME, values, selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI " + uri)
        }

        getContext()!!.getContentResolver().notifyChange(uri, null)
        return count
    }

    private class DatabaseHelper(context: Context?) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                ("CREATE TABLE " + PLACES_TABLE_NAME + " ("
                        + Places.PLACE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + Places.SITE_ID + " INTEGER, "
                        + Places.NAME + " TEXT NOT NULL, "
                        + Places.PREFERRED_TRANSPORT_MODE + " INTEGER, "
                        + Places.STARRED + " INTEGER, "
                        + Places.POSITION + " INTEGER"
                        + ");")
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(
                TAG, ("Upgrading database from version " + oldVersion + " to " + newVersion
                        + ", which will destroy all old data")
            )
            db.execSQL("DROP TABLE IF EXISTS " + PLACES_TABLE_NAME)
            onCreate(db)
        }
    }

    class Place {
        object Places : BaseColumns {
            @JvmField
            val CONTENT_URI: Uri = Uri.parse(
                ("content://"
                        + AUTHORITY + "/places")
            )

            const val CONTENT_TYPE: String = "vnd.android.cursor.dir/vnd.sthlmtraveling.places"

            const val PLACE_ID: String = "_id"

            const val SITE_ID: String = "site_id"

            const val NAME: String = "name"

            const val PREFERRED_TRANSPORT_MODE: String = "preferred_transport_mode"

            const val STARRED: String = "starred"

            const val POSITION: String = "position"
        }
    }

    companion object {
        private const val TAG = "PlacesProvider"
        private const val DATABASE_NAME = "places.db"
        private const val DATABASE_VERSION = 1
        private const val PLACES_TABLE_NAME = "places"
        val AUTHORITY: String = BuildConfig.BASE_PROVIDER_AUTHORITY + ".placesprovider"
        private val sUriMatcher: UriMatcher
        private const val PLACES = 1
        private val sPlacesProjectionMap: HashMap<String, String> = hashMapOf(
            Places.PLACE_ID to Places.PLACE_ID,
            Places.SITE_ID to Places.SITE_ID,
            Places.NAME to Places.NAME,
            Places.PREFERRED_TRANSPORT_MODE to Places.PREFERRED_TRANSPORT_MODE,
            Places.STARRED to Places.STARRED,
            Places.POSITION to Places.POSITION
        )

        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher.addURI(AUTHORITY, PLACES_TABLE_NAME, PLACES)
        }
    }
}
