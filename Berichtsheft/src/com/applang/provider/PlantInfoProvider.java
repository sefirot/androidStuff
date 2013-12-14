package com.applang.provider;

import com.applang.provider.PlantInfo.Plants;

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
 * Provides access to a database of plants.
 */
public class PlantInfoProvider extends ContentProvider
{
    private static final String TAG = "PlantInfoProvider";

    public static final String DATABASE_NAME = "plant_info.db";
    public static final int DATABASE_VERSION = 1;
    public static final String PLANTS_TABLE_NAME = "plants";

    private static HashMap<String, String> sPlantsProjectionMap;

    private static final int PLANTS = 1;
    private static final int PLANT_ID = 2;
    private static final int RAW = 3;

    private static final UriMatcher sUriMatcher;

    public static ContentValues contentValues(Object... args) {
		ContentValues values = new ContentValues();
		if (args.length > 0) values.put(Plants._ID, (Long)args[0]);
		if (args.length > 1) values.put(Plants.NAME, (String)args[1]);
		if (args.length > 2) values.put(Plants.FAMILY, (String)args[2]);
		if (args.length > 3) values.put(Plants.BOTNAME, (String)args[3]);
		if (args.length > 4) values.put(Plants.BOTFAMILY, (String)args[4]);
		if (args.length > 5) values.put(Plants.GROUP, (String)args[5]);
		return values;
    }

	public static String[] FULL_PROJECTION = strings(
			Plants._ID, // 0
	        Plants.NAME, // 1
	        Plants.FAMILY, // 2
	        Plants.BOTNAME, // 3
	        Plants.BOTFAMILY, // 4
	        Plants.GROUP // 5
	);

    /**
     * This class helps open, create, and upgrade the database file.
     */
	public static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PLANTS_TABLE_NAME + " ("
                    + Plants._ID + " INTEGER PRIMARY KEY,"
                    + Plants.NAME + " TEXT,"
                    + Plants.FAMILY + " TEXT,"
                    + Plants.BOTNAME + " TEXT,"
                    + Plants.BOTFAMILY + " TEXT,"
                    + Plants.GROUP + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.beginTransaction();
            table_upgrade(db, PLANTS_TABLE_NAME, new Job<Void>() {
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

        case PLANTS:
            qb.setTables(PLANTS_TABLE_NAME);
            qb.setProjectionMap(sPlantsProjectionMap);
            break;

        case PLANT_ID:
            qb.setTables(PLANTS_TABLE_NAME);
            qb.setProjectionMap(sPlantsProjectionMap);
            qb.appendWhere(Plants._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (notNullOrEmpty(sortOrder)) {
        	orderBy = sortOrder;
        } else {
        	orderBy = Plants.DEFAULT_SORT_ORDER;
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
        case PLANTS:
            return Plants.CONTENT_TYPE;

        case PLANT_ID:
            return Plants.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != PLANTS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        if (values.containsKey(Plants.NAME) == false) {
            values.put(Plants.NAME, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(PLANTS_TABLE_NAME, Plants.NAME, values);
        if (rowId > 0) {
            Uri plantUri = ContentUris.withAppendedId(Plants.CONTENT_URI, rowId);
            notifyChange(plantUri);
            return plantUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case PLANTS:
            count = db.delete(PLANTS_TABLE_NAME, where, whereArgs);
            break;

        case PLANT_ID:
            String plantId = uri.getPathSegments().get(1);
            count = db.delete(PLANTS_TABLE_NAME, Plants._ID + "=" + plantId
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
        case PLANTS:
            count = db.update(PLANTS_TABLE_NAME, values, where, whereArgs);
            break;

        case PLANT_ID:
            String plantId = uri.getPathSegments().get(1);
            count = db.update(PLANTS_TABLE_NAME, values, Plants._ID + "=" + plantId
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
        sUriMatcher.addURI(PlantInfo.AUTHORITY, null, RAW);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, "plants", PLANTS);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, "plants/#", PLANT_ID);

        sPlantsProjectionMap = new HashMap<String, String>();
        sPlantsProjectionMap.put(Plants._ID, Plants._ID);
        sPlantsProjectionMap.put(Plants.NAME, Plants.NAME);
        sPlantsProjectionMap.put(Plants.FAMILY, Plants.FAMILY);
        sPlantsProjectionMap.put(Plants.BOTNAME, Plants.BOTNAME);
        sPlantsProjectionMap.put(Plants.BOTFAMILY, Plants.BOTFAMILY);
        sPlantsProjectionMap.put(Plants.GROUP, Plants.GROUP);
    }
}
