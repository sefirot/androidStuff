/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.applang.provider;

import com.applang.provider.NotePad.Notes;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.applang.Util.*;
import static com.applang.Util2.*;

/**
 *	Provides access to a database of notes. Each note has a title, the note
 *	itself, a creation and a modification date.
 */
public class NotePadProvider extends ContentProvider
{
    private static final String TAG = "NotePadProvider";

    public static final String DATABASE_NAME = "note_pad.db";
    public static final int DATABASE_VERSION = 4;
    
    public static final String[] NAMES = {"notes", "bausteine", "words"};
    
    public static String tableName(int index) {
    	if (index > -1 && index < NAMES.length)
    		return NAMES[index];
    	else
    		return "";
    }
    
    public static int tableIndex(String name) {
    	return Arrays.asList(NAMES).indexOf(name);
    }

	public static void saveTableIndex(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt("table", index);
        prefsEditor.commit();
	}

	public static int savedTableIndex(Context context, int defaultIndex) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int index = prefs.getInt("table", defaultIndex);
		return index;
	}
   
    public static Uri contentUri(int index) {
    	return contentUri(tableName(index));
    }
    
    public static Uri contentUri(String name) {
    	return Uri.parse("content://" + NotePad.AUTHORITY + "/" + name);
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
    
    public static long id(long defaultValue, Uri uri) {
    	if (uri == null)
    		return defaultValue;
    	int segments = uri.getPathSegments().size();
    	if (segments < 2)
    		return defaultValue;
    	else
    		return toLong(defaultValue, uri.getPathSegments().get(1));
    }

    public static ContentValues contentValues(Object... args) {
		ContentValues values = new ContentValues();
		if (args.length > 0) values.put(Notes._ID, (Long)args[0]);
		if (args.length > 1) values.put(Notes.TITLE, (String)args[1]);
		if (args.length > 2) values.put(Notes.NOTE, (String)args[2]);
		if (args.length > 3) values.put(Notes.CREATED_DATE, (Long)args[3]);
		if (args.length > 4) values.put(Notes.MODIFIED_DATE, (Long)args[4]);
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

	public static final int NOTES = 1;
	public static final int NOTE_ID = 2;
	public static final int NOTES_WORDS = 3;
	public static final int NOTES_BAUSTEINE = 4;
    
    private static final UriMatcher sUriMatcher;

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
		DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	for (int i = 0; i < NAMES.length; i++) {
				String sql = "CREATE TABLE " + NAMES[i] + " ("
                    + Notes._ID + " INTEGER PRIMARY KEY,"
                    + Notes.TITLE + " TEXT,"
                    + Notes.NOTE + " TEXT,"
                    + Notes.CREATED_DATE + " INTEGER,"
                    + Notes.MODIFIED_DATE + " INTEGER";
				switch (i) {
				case 2:
					sql += ", UNIQUE ("
							+ Notes.CREATED_DATE + ", "
							+ Notes.MODIFIED_DATE + ", "
							+ Notes.TITLE + ")";
					break;
				default:
					sql += ", UNIQUE ("
							+ Notes.CREATED_DATE + ", "
							+ Notes.TITLE + ")";
					break;
				}
				switch (i) {
				case 2:
					sql += ", foreign key(" + Notes.CREATED_DATE +
							") references " +
							NAMES[0] + "(" + Notes._ID + ")";
					sql += ", foreign key(" + Notes.MODIFIED_DATE +
							") references " +
							NAMES[1] + "(" + Notes._ID + ")";
					break;
				}
				sql += ");";
				db.execSQL(sql);
			}
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
        	for (int i = 0; i < NAMES.length; i++)
        		db.execSQL("DROP TABLE IF EXISTS " + NAMES[i]);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case NOTES_WORDS:
        case NOTES:
            return Notes.CONTENT_TYPE;

        case NOTES_BAUSTEINE:
        case NOTE_ID:
      		return Notes.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String tableName = uri.getPathSegments().get(0);
        int type = sUriMatcher.match(uri);
        qb.setProjectionMap(projectionMap(type));
        
		switch (type) {
        case NOTE_ID:
        	qb.appendWhere(Notes._ID + "=" + uri.getPathSegments().get(1));
        	
        case NOTES:
            qb.setTables(tableName);
            break;

        case NOTES_WORDS:
            qb.setTables(NAMES[0]
            		.concat(" JOIN ").concat(NAMES[2])
            		.concat(" ON (").concat(projectionMap(type).get(Notes._ID))
            		.concat(" = ").concat(projectionMap(type).get(Notes.REF_ID)).concat(")"));
            break;

        case NOTES_BAUSTEINE:
            qb.setTables(NAMES[1]
            		.concat(" JOIN ").concat(NAMES[2])
            		.concat(" ON (").concat(projectionMap(type).get(Notes._ID))
            		.concat(" = ").concat(projectionMap(type).get(Notes.REF_ID2)).concat(")"));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Notes.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        String groupBy = null, having = null;
        int index = selection.indexOf("group by");
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
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
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
        if (values.containsKey(Notes.CREATED_DATE) == false) {
            values.put(Notes.CREATED_DATE, now);
        }
        if (values.containsKey(Notes.MODIFIED_DATE) == false) {
            values.put(Notes.MODIFIED_DATE, now);
        }
        if (values.containsKey(Notes.TITLE) == false) {
            values.put(Notes.TITLE, "");
        }
        if (values.containsKey(Notes.NOTE) == false) {
            values.put(Notes.NOTE, "");
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        String tableName = uri.getPathSegments().get(0);
        long rowId = db.insert(tableName, Notes.NOTE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(contentUri(tableName), rowId);
            getContext().getContentResolver().notifyChange(contentUri(tableName), null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String tableName = uri.getPathSegments().get(0);
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.delete(tableName, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(tableName, 
            		Notes._ID + "=" + noteId + 
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), 
                    whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(contentUri(tableName), null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String tableName = uri.getPathSegments().get(0);
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.update(tableName, values, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(tableName, 
            		values, 
            		Notes._ID + "=" + noteId + 
            		(!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), 
            		whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(contentUri(tableName), null);
        return count;
    }

	static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[0], NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[0] + "/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[1], NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[1] + "/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[2], NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[2] + "/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[0] + "/" + NAMES[2], NOTES_WORDS);
        sUriMatcher.addURI(NotePad.AUTHORITY, NAMES[0] + "/" + NAMES[1], NOTES_BAUSTEINE);

        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(Notes._ID, Notes._ID);
        sNotesProjectionMap.put(Notes.TITLE, Notes.TITLE);
        sNotesProjectionMap.put(Notes.NOTE, Notes.NOTE);
        sNotesProjectionMap.put(Notes.CREATED_DATE, Notes.CREATED_DATE);
        sNotesProjectionMap.put(Notes.MODIFIED_DATE, Notes.MODIFIED_DATE);
        sNotesProjectionMap.put("count", "count(*)");

        sNotesProjectionMap2 = new HashMap<String, String>();
        sNotesProjectionMap2.put(Notes._ID, NAMES[0].concat(".").concat(Notes._ID));
        sNotesProjectionMap2.put(Notes.TITLE, NAMES[2].concat(".").concat(Notes.TITLE));
        sNotesProjectionMap2.put(Notes.NOTE, NAMES[0].concat(".").concat(Notes.NOTE));
        sNotesProjectionMap2.put(Notes.REF_ID, NAMES[2].concat(".").concat(Notes.CREATED_DATE));
        sNotesProjectionMap2.put(Notes.REF_ID2, NAMES[2].concat(".").concat(Notes.MODIFIED_DATE));
        sNotesProjectionMap2.put("date", NAMES[0].concat(".").concat(Notes.CREATED_DATE));

        sNotesProjectionMap3 = new HashMap<String, String>();
        sNotesProjectionMap3.put(Notes._ID, NAMES[1].concat(".").concat(Notes._ID));
        sNotesProjectionMap3.put(Notes.NOTE, NAMES[1].concat(".").concat(Notes.TITLE));
        sNotesProjectionMap3.put(Notes.REF_ID, NAMES[2].concat(".").concat(Notes.CREATED_DATE));
        sNotesProjectionMap3.put(Notes.REF_ID2, NAMES[2].concat(".").concat(Notes.MODIFIED_DATE));
    }

    public static Integer[] countNotes(ContentResolver contentResolver, int tableIndex, String selection, String[] selectionArgs) {
		Cursor cursor = contentResolver.query(contentUri(tableIndex), 
				new String[]{"count"}, 
				selection, selectionArgs, 
				Notes.DEFAULT_SORT_ORDER);
		ArrayList<Integer> counts = new ArrayList<Integer>();
		if (cursor.moveToFirst())
			do {
				counts.add(cursor.getInt(0));
			} while (cursor.moveToNext());
		cursor.close();
		return counts.toArray(new Integer[0]);
	}

    public static String[] getTitles(ContentResolver contentResolver, int tableIndex, String selection, String[] selectionArgs) {
		Cursor cursor = contentResolver.query(contentUri(tableIndex), 
				new String[]{Notes.TITLE}, 
				selection, selectionArgs, 
				Notes.DEFAULT_SORT_ORDER);
		ArrayList<String> titles = new ArrayList<String>();
		if (cursor.moveToFirst())
			do {
				titles.add(cursor.getString(0));
			} while (cursor.moveToNext());
		cursor.close();
		return titles.toArray(new String[0]);
	}

	public static long getIdOfNote(ContentResolver contentResolver, int tableIndex, String selection, String[] selectionArgs) {
		Cursor cursor = contentResolver.query(contentUri(tableIndex), 
				new String[]{Notes._ID}, 
				selection, selectionArgs, 
				null);
		long id = -1;
		if (cursor.moveToFirst())
			id = cursor.getLong(0);
		cursor.close();
		return id;
	}

	public static boolean fetchNoteById(long id, ContentResolver contentResolver, int tableIndex, Job<Cursor> job, Object... params) {
		Cursor cursor = contentResolver.query(
				ContentUris.withAppendedId(contentUri(tableIndex), id), 
				Notes.FULL_PROJECTION, 
				"", null, 
	    		null);
		
		boolean retval = cursor.moveToFirst();
		if (retval)
			try {
				job.perform(cursor, params);
			} catch (Exception e) {
				Log.e(TAG, "fetching note", e);
			}
		
		cursor.close();
		return retval;
	}

	public static Set<String> wordSet(ContentResolver contentResolver, int tableIndex, String selection, String... selectionArgs) {
		Cursor cursor = contentResolver.query(
				contentUri(tableIndex), 
				new String[] {Notes.TITLE}, 
				selection, selectionArgs, 
	    		null);
		
	    ValMap map = getResultMap(cursor, 
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
	    
	    return new TreeSet<String>(map.keySet());
	}

	public static ValMap bausteinMap(ContentResolver contentResolver, String selection, String... selectionArgs) {
		Cursor cursor = contentResolver.query(
	    		contentUri(1), 
	    		new String[] { Notes.TITLE, Notes.NOTE }, 
	    		selection, selectionArgs,
	    		null);
		
	    ValMap map = getResultMap(cursor, 
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
