package com.applang.provider;

import com.applang.Util.Job;
import com.applang.provider.PlantInfo.Pictures;
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

import java.util.ArrayList;
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
    public static final int DATABASE_VERSION = 2;
    public static final String PLANTS_TABLE_NAME = "plants";
    public static final String PICTURES_TABLE_NAME = "pictures";
    public static final String PLANT_PICS_TABLE_NAME = "plant_pics";
    public static final String[] DATABASE_TABLES = {PLANTS_TABLE_NAME, PICTURES_TABLE_NAME, PLANT_PICS_TABLE_NAME};
    
    public static String tableName(int index) {
    	if (index > -1 && index < DATABASE_TABLES.length)
    		return DATABASE_TABLES[index];
    	else
    		return "";
    }
    
    public static int tableIndex(String name) {
    	return asList(DATABASE_TABLES).indexOf(name);
    }
   
    public static Uri contentUri(int index) {
    	return contentUri(tableName(index));
    }
    
    public static Uri contentUri(String name) {
    	return com.applang.Util1.contentUri(PlantInfo.AUTHORITY, name);
    }
    
    public static int tableIndex(int defaultValue, Uri uri) {
    	if (uri == null)
    		return defaultValue;
    	int segments = uri.getPathSegments().size();
    	if (segments < 1)
    		return defaultValue;
    	else {
    		int index = tableIndex(uri.getPathSegments().get(0));
    		return index < 0 ? defaultValue : index;
    	}
    }

    private static final int PLANTS = 1;
    private static final int PLANT_ID = 2;
    private static final int RAW = 3;
    private static final int PICTURES = 4;
    private static final int PICTURE_ID = 5;

    private static final UriMatcher sUriMatcher;

    public static ContentValues contentValues(int tableIndex, Object... args) {
		ContentValues values = new ContentValues();
		switch (tableIndex) {
		case 0:
			if (args.length > 0) values.put(Plants._ID, (Long)args[0]);
			if (args.length > 1) values.put(Plants.NAME, (String)args[1]);
			if (args.length > 2) values.put(Plants.FAMILY, (String)args[2]);
			if (args.length > 3) values.put(Plants.BOTNAME, (String)args[3]);
			if (args.length > 4) values.put(Plants.BOTFAMILY, (String)args[4]);
			if (args.length > 5) values.put(Plants.GROUP, (String)args[5]);
			break;
		case 1:
			if (args.length > 0) values.put(Pictures._ID, (Long)args[0]);
			if (args.length > 1) values.put(Pictures.NAME, (String)args[1]);
			if (args.length > 2) values.put(Pictures.TYPE, (String)args[2]);
			if (args.length > 5) values.put(Pictures.BLOB, (String)args[3]);
			break;
		}
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
	public static class DatabaseHelper extends SQLiteOpenHelper
	{
		public DatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	turnForeignKeys(db, true);
        	for (int i = 0; i < DATABASE_TABLES.length; i++) {
				createTable(i, db);
			}
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will try to loose as few as possible of the old data");
        	turnForeignKeys(db, false);
        	for (int i = 0; i < DATABASE_TABLES.length; i++)
            	table_upgrade(db, DATABASE_TABLES[i], new Job<Void>() {
					public void perform(Void t, Object[] parms) throws Exception {
						createTable(param_Integer(null, 0, parms), db);
					}
            	}, i);
        	turnForeignKeys(db, true);
        }
    }

	public static void createTable(int index, SQLiteDatabase db) {
		String sql = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLES[index] + " (";
		switch (index) {
		case 0:
			sql += 
                Plants._ID + " INTEGER PRIMARY KEY,"
                + Plants.NAME + " TEXT,"
                + Plants.FAMILY + " TEXT,"
                + Plants.BOTNAME + " TEXT,"
                + Plants.BOTFAMILY + " TEXT,"
                + Plants.GROUP + " TEXT";
			break;
		case 1:
			sql += 
                Pictures._ID + " INTEGER PRIMARY KEY,"
                + Pictures.NAME + " TEXT,"
                + Pictures.TYPE + " INTEGER,"
                + Pictures.BLOB + " BLOB";
			break;
		case 2:
			sql += 
                "Plant_ID INTEGER NOT NULL,"
                + "Picture_ID INTEGER NOT NULL,"
                + "unique(Plant_ID, Picture_ID), "
                + "foreign key(Plant_ID) references " + DATABASE_TABLES[0] + "(" + Plants._ID + "), "
                + "foreign key(Picture_ID) references " + DATABASE_TABLES[1] + "(" + Pictures._ID + ")";
			break;
		}
		sql += ");";
		db.execSQL(sql);
		switch (index) {
		case 2:
            db.execSQL("CREATE TRIGGER on_delete_plant" +
					" BEFORE DELETE ON " + DATABASE_TABLES[0] +
					" FOR EACH ROW BEGIN" +
					" DELETE FROM " + DATABASE_TABLES[2] + 
					" WHERE " + DATABASE_TABLES[2] + ".Plant_ID = old." + Plants._ID + ";" + 
					" END;");
            db.execSQL("CREATE TRIGGER on_delete_picture" +
					" BEFORE DELETE ON " + DATABASE_TABLES[1] +
					" FOR EACH ROW BEGIN" +
					" DELETE FROM " + DATABASE_TABLES[2] + 
					" WHERE " + DATABASE_TABLES[2] + ".Picture_ID = old." + Pictures._ID + ";" + 
					" END;");
			break;
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
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
        case RAW:
        	return mOpenHelper.getReadableDatabase().rawQuery(selection, selectionArgs);
        case PLANTS:
            qb.setTables(PLANTS_TABLE_NAME);
            qb.setProjectionMap(projectionMaps.get(0));
            break;
        case PLANT_ID:
            qb.setTables(PLANTS_TABLE_NAME);
            qb.setProjectionMap(projectionMaps.get(0));
            qb.appendWhere(Plants._ID + "=" + uri.getPathSegments().get(1));
            break;
        case PICTURES:
            qb.setTables(PICTURES_TABLE_NAME);
            qb.setProjectionMap(projectionMaps.get(1));
            break;
        case PICTURE_ID:
            qb.setTables(PICTURES_TABLE_NAME);
            qb.setProjectionMap(projectionMaps.get(1));
            qb.appendWhere(Pictures._ID + "=" + uri.getPathSegments().get(1));
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        String orderBy;
        if (notNullOrEmpty(sortOrder)) {
        	orderBy = sortOrder;
        } else {
        	orderBy = Plants.DEFAULT_SORT_ORDER;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
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
        case PICTURES:
            return Pictures.CONTENT_TYPE;
        case PICTURE_ID:
            return Pictures.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        int match = sUriMatcher.match(uri);
		if (match != PLANTS && match != PICTURES && match != RAW) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        String tableName = dbTableName(uri);
        String name = null;
		switch (match) {
        case PLANTS:
        	name = Plants.NAME;
        	break;
        case PICTURES:
        	name = Pictures.NAME;
        	break;
        case RAW:
        	tableName = PLANT_PICS_TABLE_NAME;
        	if (!values.containsKey("PLANT_ID") && !values.containsKey("PICTURE_ID")) {
                throw new IllegalArgumentException("Unclear plant/picture relation : " + uri);
        	}
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if (!values.containsKey(name)) {
        	values.put(name, "");
        }
		long rowId = db.insert(tableName, name, values);
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
        case PICTURES:
            count = db.delete(PICTURES_TABLE_NAME, where, whereArgs);
            break;
        case PICTURE_ID:
            String pictureId = uri.getPathSegments().get(1);
            count = db.delete(PICTURES_TABLE_NAME, Pictures._ID + "=" + pictureId
                    + (notNullOrEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case RAW:
        	if (!where.contains("PLANT_ID") && !where.contains("PICTURE_ID")) {
                throw new IllegalArgumentException("Unclear plant/picture relation : " + uri);
        	}
            count = db.delete(PLANT_PICS_TABLE_NAME, where, whereArgs);
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
        case PICTURES:
            count = db.update(PICTURES_TABLE_NAME, values, where, whereArgs);
            break;
        case PICTURE_ID:
            String pictureId = uri.getPathSegments().get(1);
            count = db.update(PICTURES_TABLE_NAME, values, Pictures._ID + "=" + pictureId
                    + (notNullOrEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        notifyChange(uri);
        return count;
    }

    private static ArrayList<HashMap<String, String>> projectionMaps = alist();

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, null, RAW);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, "plants", PLANTS);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, "plants/#", PLANT_ID);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, "pictures", PICTURES);
        sUriMatcher.addURI(PlantInfo.AUTHORITY, "pictures/#", PICTURE_ID);

        projectionMaps.add(new HashMap<String, String>());
        projectionMaps.get(0).put(Plants._ID, Plants._ID);
        projectionMaps.get(0).put(Plants.NAME, Plants.NAME);
        projectionMaps.get(0).put(Plants.FAMILY, Plants.FAMILY);
        projectionMaps.get(0).put(Plants.BOTNAME, Plants.BOTNAME);
        projectionMaps.get(0).put(Plants.BOTFAMILY, Plants.BOTFAMILY);
        projectionMaps.get(0).put(Plants.GROUP, Plants.GROUP);
        
        projectionMaps.add(new HashMap<String, String>());
        projectionMaps.get(1).put(Pictures._ID, Pictures._ID);
        projectionMaps.get(1).put(Pictures.NAME, Pictures.NAME);
        projectionMaps.get(1).put(Pictures.TYPE, Pictures.TYPE);
        projectionMaps.get(1).put(Pictures.BLOB, Pictures.BLOB);
    }
}
