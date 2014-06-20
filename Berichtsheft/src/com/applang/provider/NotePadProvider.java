package com.applang.provider;

import com.applang.provider.NotePad.NoteColumns;

import android.content.ContentProvider;
import android.content.ContentResolver;
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
import java.util.Map;
import java.util.Set;

import static com.applang.Util.*;
import static com.applang.Util1.*;

/**
 *	Provides access to a database of notes. Each note has a title, the note
 *	itself, a creation and a modification date.
 */
public class NotePadProvider extends ContentProvider
{
    private static final String TAG = "NotePadProvider";

    public static final String DATABASE_NAME = "note_pad.db";
    public static final int DATABASE_VERSION = 4;
    public static final String[] DATABASE_TABLES = {"notes", "bausteine", "words"};
    
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
    	return com.applang.Util1.contentUri(NotePad.AUTHORITY, name);
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

    public static ContentValues contentValues(int tableIndex, Object... args) {
		ContentValues values = new ContentValues();
		if (args.length > 0) values.put(NoteColumns._ID, (Long)args[0]);
		if (args.length > 1) values.put(NoteColumns.TITLE, (String)args[1]);
		if (args.length > 2) values.put(NoteColumns.NOTE, (String)args[2]);
		if (args.length > 3) values.put(NoteColumns.CREATED_DATE, (Long)args[3]);
		if (args.length > 4) values.put(NoteColumns.MODIFIED_DATE, (Long)args[4]);
		return values;
    }

	private static HashMap<String, String> sNotesProjectionMap, sNotesProjectionMap2, sNotesProjectionMap3;

    public static Map<String, String> projectionMap(int type) {
    	switch (type) {
		case NOTES_BAUSTEINE:
			return sNotesProjectionMap3;
		case NOTES_WORDS:
			return sNotesProjectionMap2;
		default:
			return sNotesProjectionMap;
		}
	};

	public static final int NOTES = 0;
	public static final int NOTES_BAUSTEINE = 1;
	public static final int NOTES_WORDS = 2;
	public static final int RAW = 3;
	public static final int NOTE_ID = 4;
	public static final int NOTES_CREATE = 5;
	public static final int NOTES_DROP = 6;
	public static final int NOTES_MEMORY = 7;
    
    private static final UriMatcher sUriMatcher;

	public static String[] FULL_PROJECTION = strings(
		NoteColumns._ID, // 0
		NoteColumns.TITLE, // 1
		NoteColumns.NOTE, // 2
		NoteColumns.CREATED_DATE, // 3
		NoteColumns.MODIFIED_DATE // 4
	);

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
    
    private static void dropTable(int index, SQLiteDatabase db) {
    	db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLES[index]);
    }

	public static void createTable(int index, SQLiteDatabase db) {
		String sql = "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLES[index] + " ("
		    + NoteColumns._ID + " INTEGER PRIMARY KEY,"
		    + NoteColumns.TITLE + " TEXT,"
		    + NoteColumns.NOTE + " TEXT,"
		    + NoteColumns.CREATED_DATE + " INTEGER,"
		    + NoteColumns.MODIFIED_DATE + " INTEGER";
		switch (index) {
		default:
			sql += ", UNIQUE ("
					+ NoteColumns.CREATED_DATE + ", "
					+ NoteColumns.TITLE + ")";
			break;
		}
		switch (index) {
		case 2:
			sql += ", foreign key(" + NoteColumns.REF_ID +
					") references " +
					DATABASE_TABLES[0] + "(" + NoteColumns._ID + ")";
			break;
		}
		sql += ");";
		db.execSQL(sql);
		switch (index) {
		case 2:
			sql = "CREATE TRIGGER on_delete_note" +
					" BEFORE DELETE ON " + DATABASE_TABLES[0] +
					" FOR EACH ROW BEGIN" +
					" DELETE FROM " + DATABASE_TABLES[2] + 
					" WHERE " + DATABASE_TABLES[2] + "." + NoteColumns.REF_ID + "=" + "old." + NoteColumns._ID + ";" + 
					" END;";
			db.execSQL(sql);
			break;
		}
	}

    private DatabaseHelper mOpenHelper = null;
    
    public SQLiteOpenHelper openHelper() {
    	return mOpenHelper;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext(), DATABASE_NAME);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case RAW:
        case NOTES_CREATE:
        case NOTES_DROP:
        case NOTES_MEMORY:
        case NOTES_WORDS:
        case NOTES_BAUSTEINE:
        case NOTES:
            return NoteColumns.CONTENT_TYPE;

        case NOTE_ID:
      		return NoteColumns.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String tableName = dbTableName(uri);
        int type = sUriMatcher.match(uri);
        qb.setProjectionMap(projectionMap(type));
        
		switch (type) {
        case RAW:
        	return mOpenHelper.getReadableDatabase().rawQuery(selection, selectionArgs);
        	
        case NOTE_ID:
        	qb.appendWhere(NoteColumns._ID + "=" + uri.getPathSegments().get(1));
        	
        case NOTES:
            qb.setTables(tableName);
            break;

        case NOTES_WORDS:
            qb.setTables(DATABASE_TABLES[0]
            		.concat(" JOIN ").concat(DATABASE_TABLES[2])
            		.concat(" ON (").concat(projectionMap(type).get(NoteColumns._ID))
            		.concat(" = ").concat(projectionMap(type).get(NoteColumns.REF_ID)).concat(")"));
            break;

        case NOTES_BAUSTEINE:
            qb.setTables(DATABASE_TABLES[1]
            		.concat(" JOIN ").concat(DATABASE_TABLES[2])
            		.concat(" ON (").concat(projectionMap(type).get(NoteColumns._ID))
            		.concat(" = ").concat(projectionMap(type).get(NoteColumns.REF_ID2)).concat(")"));
            break;

        case NOTES_MEMORY:
        	if (mOpenHelper != null)
        		mOpenHelper.close();
        	
            mOpenHelper = new DatabaseHelper(getContext(), 
            		uri.getPathSegments().get(1).equals("on") ? null : DATABASE_NAME);
            return null;

        case NOTES_DROP:
        	dropTable(tableIndex(tableName), mOpenHelper.getWritableDatabase());
            return null;

        case NOTES_CREATE:
        	createTable(tableIndex(tableName), mOpenHelper.getWritableDatabase());
            return null;
			
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        String orderBy;
        if (notNullOrEmpty(sortOrder)) {
        	orderBy = sortOrder;
        } else {
        	orderBy = NoteColumns.DEFAULT_SORT_ORDER;
        }

        String groupBy = null, having = null;
        int index = notNullOrEmpty(selection) ? selection.indexOf("group by") : -1;
        if (index > -1) {
        	groupBy = selection.substring(index + 8).trim();
        	selection = selection.substring(0, index).trim();
        	having = "";
        	index = groupBy.indexOf("having");
        	if (index > -1) {
        		having = groupBy.substring(index + 6).trim();
        		groupBy = groupBy.substring(0, index).trim();
        	}
        }
        
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

	private void notifyChange(Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null);
	}

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != NOTES) {
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
        if (values.containsKey(NoteColumns.CREATED_DATE) == false) {
            values.put(NoteColumns.CREATED_DATE, now);
        }
        if (values.containsKey(NoteColumns.MODIFIED_DATE) == false) {
            values.put(NoteColumns.MODIFIED_DATE, now);
        }
        if (values.containsKey(NoteColumns.TITLE) == false) {
            values.put(NoteColumns.TITLE, "");
        }
        if (values.containsKey(NoteColumns.NOTE) == false) {
            values.put(NoteColumns.NOTE, "");
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
		String tableName = dbTableName(uri);
        long rowId = db.insert(tableName, NoteColumns.NOTE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(contentUri(tableName), rowId);
            notifyChange(noteUri);
            return noteUri;
        }

        throw new SQLException(String.format("Failed to insert row %s into %s", values, uri));
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
		String tableName = dbTableName(uri);
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.delete(tableName, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(tableName, 
            		NoteColumns._ID + "=" + noteId + 
                    (notNullOrEmpty(where) ? " AND (" + where + ')' : ""), 
                    whereArgs);
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
		String tableName = dbTableName(uri);
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.update(tableName, values, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(tableName, 
            		values, 
            		NoteColumns._ID + "=" + noteId + 
            		(notNullOrEmpty(where) ? " AND (" + where + ')' : ""), 
            		whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        notifyChange(uri);
        return count;
    }

	static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, null, RAW);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[0], NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[0] + "/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[1], NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[1] + "/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[1] + "/" + NoteColumns.TITLE + "/*", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[2], NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[2] + "/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[0] + "/" + DATABASE_TABLES[2], NOTES_WORDS);
        sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[0] + "/" + DATABASE_TABLES[1], NOTES_BAUSTEINE);
        sUriMatcher.addURI(NotePad.AUTHORITY, "memory/on", NOTES_MEMORY);
        sUriMatcher.addURI(NotePad.AUTHORITY, "memory/off", NOTES_MEMORY);
        for (int j = 0; j < DATABASE_TABLES.length; j++) {
			sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[j] + "/create",	NOTES_CREATE);
			sUriMatcher.addURI(NotePad.AUTHORITY, DATABASE_TABLES[j] + "/drop", NOTES_DROP);
		}
		sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(NoteColumns._ID, NoteColumns._ID);
        sNotesProjectionMap.put(NoteColumns.TITLE, NoteColumns.TITLE);
        sNotesProjectionMap.put(NoteColumns.NOTE, NoteColumns.NOTE);
        sNotesProjectionMap.put(NoteColumns.CREATED_DATE, NoteColumns.CREATED_DATE);
        sNotesProjectionMap.put(NoteColumns.MODIFIED_DATE, NoteColumns.MODIFIED_DATE);
        sNotesProjectionMap.put("count", "count(*)");

        sNotesProjectionMap2 = new HashMap<String, String>();
        sNotesProjectionMap2.put(NoteColumns._ID, DATABASE_TABLES[0].concat(".").concat(NoteColumns._ID));
        sNotesProjectionMap2.put(NoteColumns.TITLE, DATABASE_TABLES[2].concat(".").concat(NoteColumns.TITLE));
        sNotesProjectionMap2.put(NoteColumns.NOTE, DATABASE_TABLES[0].concat(".").concat(NoteColumns.NOTE));
        sNotesProjectionMap2.put(NoteColumns.REF_ID, DATABASE_TABLES[2].concat(".").concat(NoteColumns.CREATED_DATE));
        sNotesProjectionMap2.put(NoteColumns.REF_ID2, DATABASE_TABLES[2].concat(".").concat(NoteColumns.MODIFIED_DATE));
        sNotesProjectionMap2.put("date", DATABASE_TABLES[0].concat(".").concat(NoteColumns.CREATED_DATE));

        sNotesProjectionMap3 = new HashMap<String, String>();
        sNotesProjectionMap3.put(NoteColumns._ID, DATABASE_TABLES[1].concat(".").concat(NoteColumns._ID));
        sNotesProjectionMap3.put(NoteColumns.NOTE, DATABASE_TABLES[1].concat(".").concat(NoteColumns.TITLE));
        sNotesProjectionMap3.put(NoteColumns.REF_ID, DATABASE_TABLES[2].concat(".").concat(NoteColumns.CREATED_DATE));
        sNotesProjectionMap3.put(NoteColumns.REF_ID2, DATABASE_TABLES[2].concat(".").concat(NoteColumns.MODIFIED_DATE));
    }

    public static Integer[] countNotes(ContentResolver contentResolver, int tableIndex, String selection, String[] selectionArgs) {
    	ValList counts = vlist();
		Cursor cursor = contentResolver.query(contentUri(tableIndex), 
				strings("count"), 
				selection, selectionArgs, 
				NoteColumns.DEFAULT_SORT_ORDER);
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor cursor, Object[] parms) throws Exception {
				ValList counts = (ValList) parms[0];
				counts.add(cursor.getInt(0));
			}
		}, counts);
		return counts.toArray(new Integer[0]);
	}

    public static String[] getTitles(ContentResolver contentResolver, int tableIndex, String selection, String[] selectionArgs) {
    	ValList titles = vlist();
		Cursor cursor = contentResolver.query(contentUri(tableIndex), 
				strings(NoteColumns.TITLE), 
				selection, selectionArgs, 
				NoteColumns.DEFAULT_SORT_ORDER);
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor cursor, Object[] parms) throws Exception {
				ValList titles = (ValList) parms[0];
				titles.add(cursor.getString(0));
			}
		}, titles);
		return toStrings(titles);
	}

	public static long getIdOfNote(ContentResolver contentResolver, int tableIndex, String selection, String[] selectionArgs) {
		Cursor cursor = contentResolver.query(contentUri(tableIndex), 
				strings(NoteColumns._ID), 
				selection, selectionArgs, 
				null);
		Object[] parms = {-1L};
		traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor cursor, Object[] parms) throws Exception {
				parms[0] = cursor.getLong(0);
			}
		}, parms);
		return (Long) parms[0];
	}

	public static boolean fetchNoteById(long id, ContentResolver contentResolver, int tableIndex, 
			Job<Cursor> job, Object... params)
	{
		return traverse(
			contentResolver.query(
				ContentUris.withAppendedId(contentUri(tableIndex), id), 
				NotePadProvider.FULL_PROJECTION, 
				"", null, 
	    		null), 
    		job, 
    		params);
	}

	public static Set<String> wordSet(ContentResolver contentResolver, int tableIndex, String selection, String... selectionArgs) {
		Cursor cursor = contentResolver.query(
				contentUri(tableIndex), 
				strings(NoteColumns.TITLE), 
				selection, selectionArgs, 
	    		null);
		
	    ValMap map = getResults(cursor, 
	    	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(0);
				}
	        }, 
	    	new Function<Object>() {
				public Object apply(Object... params) {
					return null;
				}
	        }
	    );
	    
	    return sortedSet(map.keySet());
	}

	public static ValMap bausteinMap(ContentResolver contentResolver, String selection, String... selectionArgs) {
		Cursor cursor = contentResolver.query(
	    		contentUri(1), 
	    		strings(NoteColumns.TITLE, NoteColumns.NOTE), 
	    		selection, selectionArgs,
	    		null);
		
	    ValMap map = getResults(cursor, 
	    	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(0);
				}
	        }, 
	    	new Function<Object>() {
				public Object apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(1);
				}
	        }
	    );
	    
	    return map;
	}
}
