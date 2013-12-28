package com.applang.provider;

import com.applang.provider.WeatherInfo.Weathers;

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
import android.util.Log;

import java.util.HashMap;

import static com.applang.Util.*;
import static com.applang.Util1.*;

/**
 * Provides access to a database of weathers. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class WeatherInfoProvider extends ContentProvider
{
    private static final String TAG = "WeatherInfoProvider";

    public static final String DATABASE_NAME = "weather_info.db";
    public static final int DATABASE_VERSION = 1;
    public static final String WEATHERS_TABLE_NAME = "weathers";

    private static HashMap<String, String> sWeathersProjectionMap;

    private static final int WEATHERS = 1;
    private static final int WEATHER_ID = 2;
    private static final int RAW = 3;

    private static final UriMatcher sUriMatcher;

    public static ContentValues contentValues(int tableIndex, Object... args) {
		ContentValues values = new ContentValues();
		if (args.length > 0) values.put(Weathers._ID, (Long)args[0]);
		if (args.length > 1) values.put(Weathers.LOCATION, (String)args[1]);
		if (args.length > 2) values.put(Weathers.DESCRIPTION, (String)args[2]);
		if (args.length > 3) values.put(Weathers.PRECIPITATION, (Float)args[3]);
		if (args.length > 4) values.put(Weathers.MAXTEMP, (Float)args[4]);
		if (args.length > 5) values.put(Weathers.MINTEMP, (Float)args[5]);
		if (args.length > 6) values.put(Weathers.CREATED_DATE, (Long)args[6]);
		if (args.length > 7) values.put(Weathers.MODIFIED_DATE, (Long)args[7]);
		return values;
    }

    public static String[] FULL_PROJECTION = strings(
    		Weathers._ID, // 0
    		Weathers.DESCRIPTION, // 1
    		Weathers.LOCATION, // 2
    		Weathers.PRECIPITATION, // 3
    		Weathers.MAXTEMP, // 4
    		Weathers.MINTEMP, // 5
    		Weathers.CREATED_DATE, // 6
    		Weathers.MODIFIED_DATE // 7
    );

    /**
     * This class helps open, create, and upgrade the database file.
     */
    public static class DatabaseHelper extends SQLiteOpenHelper
    {
    	public DatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + WEATHERS_TABLE_NAME + " ("
                    + Weathers._ID + " INTEGER PRIMARY KEY,"
                    + Weathers.DESCRIPTION + " TEXT,"
                    + Weathers.LOCATION + " TEXT,"
                    + Weathers.PRECIPITATION + " FLOAT,"
                    + Weathers.MAXTEMP + " FLOAT,"
                    + Weathers.MINTEMP + " FLOAT,"
                    + Weathers.CREATED_DATE + " INTEGER,"
                    + Weathers.MODIFIED_DATE + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will try to loose as few as possible of the old data");
            table_upgrade(db, WEATHERS_TABLE_NAME, new Job<Void>() {
            	public void perform(Void t, Object[] parms) throws Exception {
            		onCreate(db);
            	}
            });
        }
    }

    private DatabaseHelper mOpenHelper;
    
    public SQLiteOpenHelper openHelper() {
    	return mOpenHelper;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext(), DATABASE_NAME);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case RAW:
        	return mOpenHelper.getReadableDatabase().rawQuery(selection, selectionArgs);

        case WEATHERS:
            qb.setTables(WEATHERS_TABLE_NAME);
            qb.setProjectionMap(sWeathersProjectionMap);
            break;

        case WEATHER_ID:
            qb.setTables(WEATHERS_TABLE_NAME);
            qb.setProjectionMap(sWeathersProjectionMap);
            qb.appendWhere(Weathers._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (notNullOrEmpty(sortOrder)) {
        	orderBy = sortOrder;
        } else {
        	orderBy = WeatherInfo.Weathers.DEFAULT_SORT_ORDER;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

	private void notifyChange(Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null);
	}

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case RAW:
        case WEATHERS:
            return Weathers.CONTENT_TYPE;

        case WEATHER_ID:
            return Weathers.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != WEATHERS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(Weathers.CREATED_DATE) == false) {
            values.put(Weathers.CREATED_DATE, now);
        }

        if (values.containsKey(Weathers.MODIFIED_DATE) == false) {
            values.put(Weathers.MODIFIED_DATE, now);
        }

        if (values.containsKey(Weathers.LOCATION) == false) {
            values.put(Weathers.LOCATION, (String) null);
        }

        if (values.containsKey(Weathers.DESCRIPTION) == false) {
            values.put(Weathers.DESCRIPTION, (String) null);
        }
        
        if (values.containsKey(Weathers.PRECIPITATION) == false) {
            values.put(Weathers.PRECIPITATION, (Float) null);
        }

        if (values.containsKey(Weathers.MAXTEMP) == false) {
            values.put(Weathers.MAXTEMP, (Float) null);
        }

        if (values.containsKey(Weathers.MINTEMP) == false) {
            values.put(Weathers.MINTEMP, (Float) null);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(WEATHERS_TABLE_NAME, Weathers.DESCRIPTION, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Weathers.CONTENT_URI, rowId);
            notifyChange(noteUri);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case WEATHERS:
            count = db.delete(WEATHERS_TABLE_NAME, where, whereArgs);
            break;

        case WEATHER_ID:
            String weatherId = uri.getPathSegments().get(1);
            count = db.delete(WEATHERS_TABLE_NAME, Weathers._ID + "=" + weatherId
                    + (notNullOrEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        notifyChange(uri);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case WEATHERS:
            count = db.update(WEATHERS_TABLE_NAME, values, where, whereArgs);
            break;

        case WEATHER_ID:
            String weatherId = uri.getPathSegments().get(1);
            count = db.update(WEATHERS_TABLE_NAME, values, Weathers._ID + "=" + weatherId
                    + (notNullOrEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        notifyChange(uri);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(WeatherInfo.AUTHORITY, null, RAW);
        sUriMatcher.addURI(WeatherInfo.AUTHORITY, "weathers", WEATHERS);
        sUriMatcher.addURI(WeatherInfo.AUTHORITY, "weathers/#", WEATHER_ID);

        sWeathersProjectionMap = new HashMap<String, String>();
        sWeathersProjectionMap.put(Weathers._ID, Weathers._ID);
        sWeathersProjectionMap.put(Weathers.DESCRIPTION, Weathers.DESCRIPTION);
        sWeathersProjectionMap.put(Weathers.LOCATION, Weathers.LOCATION);
        sWeathersProjectionMap.put(Weathers.PRECIPITATION, Weathers.PRECIPITATION);
        sWeathersProjectionMap.put(Weathers.MAXTEMP, Weathers.MAXTEMP);
        sWeathersProjectionMap.put(Weathers.MINTEMP, Weathers.MINTEMP);
        sWeathersProjectionMap.put(Weathers.CREATED_DATE, Weathers.CREATED_DATE);
        sWeathersProjectionMap.put(Weathers.MODIFIED_DATE, Weathers.MODIFIED_DATE);
    }
}
