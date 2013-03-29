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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *	Provides access to a database of notes. Each note has a title, the note
 *	itself, a creation and a modification date.
 */
public class NotePadProvider extends ContentProvider {

    private static final String TAG = "NotePadProvider";

    public static final String DATABASE_NAME = "note_pad.db";
    public static final int DATABASE_VERSION = 3;
    
    public static final String[] NOTES_TABLE_NAMES = {"notes", "bausteine", "glossary"};
    
    public static String tableName(int index) {
    	if (index > -1 && index < NOTES_TABLE_NAMES.length)
    		return NOTES_TABLE_NAMES[index];
    	else
    		return "";
    }

	public static boolean isTableDirty(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return prefs.getString("dirty", "").indexOf("" + index) > -1;
    }

	public static void setTableState(Context context, int index, boolean dirty) {
		boolean isDirty = isTableDirty(context, index);
		if (dirty == isDirty)
			return;
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String dirt = prefs.getString("dirt", "");
        if (dirty)
        	dirt += index;
        else {
        	int i = dirt.indexOf("" + index);
        	dirt = dirt.substring(0, i) + dirt.substring(i + 1);
        }
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("dirt", dirt);
        prefsEditor.commit();
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
    
	public static int tableIndex(String selection) {
		if (selection != null) {
			String expr = "(?i)%s\\s+is\\s*null";
			if (selection.matches(String.format(expr, Notes.CREATED_DATE)))
				return 1;
	    	else if (selection.matches(String.format(expr, Notes.MODIFIED_DATE)))
	    		return 2;
		}
		return 0;
    }
    
    public static int tableIndex(ContentValues values) {
    	if (values != null) {
    		if (values.containsKey(Notes.CREATED_DATE) && values.get(Notes.CREATED_DATE) == null)
    			return 1;
    		else if (values.containsKey(Notes.MODIFIED_DATE) && values.get(Notes.MODIFIED_DATE) == null)
    			return 2;
    	}
		return 0;
    }
    
    public static String selection(int tableIndex, String selection) {
    	switch (tableIndex) {
		case 2:
			if (selection.length() > 0)
				selection = String.format("(%s) and ", selection);
			selection += Notes.MODIFIED_DATE + " is null";
			break;
		case 1:
			if (selection.length() > 0)
				selection = String.format("(%s) and ", selection);
			selection += Notes.CREATED_DATE + " is null";
			break;
		}
    	return selection;
    }
    
    public static ContentValues selection(int tableIndex, ContentValues values) {
    	switch (tableIndex) {
		case 2:
			values.put(Notes.MODIFIED_DATE, (Long)null);
			break;
		case 1:
			values.put(Notes.CREATED_DATE, (Long)null);
			break;
		}
    	return values;
    }

    public static ContentValues contentValues(int i, Object... args) {
		ContentValues values = new ContentValues();
		if (args.length > i) values.put(Notes.TITLE, args[i].toString());
		if (args.length > i+1) values.put(Notes.NOTE, args[i+1].toString());
		if (args.length > i+2) values.put(Notes.CREATED_DATE, (Long)args[i+2]);
		if (args.length > i+3) values.put(Notes.MODIFIED_DATE, (Long)args[i+3]);
		return values;
    }

	private static HashMap<String, String> sNotesProjectionMap, sNotesProjectionMap2;

    public static Map<String, String> projectionMap(int type) {
    	switch (type) {
		case NOTES_WORDS:
			return sNotesProjectionMap2;
		default:
			return sNotesProjectionMap;
		}
	};

	public static final int NOTES = 1;
	public static final int NOTE_ID = 2;
	public static final int NOTES_WORDS = 3;

    private static final UriMatcher sUriMatcher;


    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	for (int i = 0; i < NOTES_TABLE_NAMES.length; i++) {
				String sql = "CREATE TABLE " + NOTES_TABLE_NAMES[i] + " ("
                    + Notes._ID + " INTEGER PRIMARY KEY,"
                    + Notes.TITLE + " TEXT,"
                    + Notes.NOTE + " TEXT,"
                    + Notes.CREATED_DATE + " INTEGER,"
                    + Notes.MODIFIED_DATE + " INTEGER"
                    + ", UNIQUE ("
                    + Notes.CREATED_DATE + ", "
                    + Notes.TITLE + ")";
				switch (i) {
				case 2:
					sql += ", foreign key(" + Notes.CREATED_DATE +
							") references " +
							NOTES_TABLE_NAMES[0] + "(" + Notes._ID + ")";
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
        	for (int i = 0; i < NOTES_TABLE_NAMES.length; i++)
        		db.execSQL("DROP TABLE IF EXISTS " + NOTES_TABLE_NAMES[i]);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }
    
    public void close() {
    	mOpenHelper.close();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String tableName = tableName(tableIndex(selection));
        int type = sUriMatcher.match(uri);
		switch (type) {
        case NOTES:
            qb.setTables(tableName);
            qb.setProjectionMap(projectionMap(type));
            break;

        case NOTE_ID:
            qb.setTables(tableName);
            qb.setProjectionMap(projectionMap(type));
            qb.appendWhere(Notes._ID + "=" + uri.getPathSegments().get(1));
            break;

        case NOTES_WORDS:
        	qb.setProjectionMap(projectionMap(type));
            qb.setTables(NOTES_TABLE_NAMES[0]
            		.concat(" JOIN ").concat(NOTES_TABLE_NAMES[2])
            		.concat(" ON (").concat(projectionMap(type).get(Notes._ID))
            		.concat(" = ").concat(projectionMap(type).get(Notes.CREATED_DATE)).concat(")"));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Notes.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case NOTES_WORDS:
        case NOTES:
            return Notes.CONTENT_TYPE;

        case NOTE_ID:
        	return Notes.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
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
            values.put(Notes.TITLE, ""/*Resources.getSystem().getString(android.R.string.untitled)*/);
        }
        if (values.containsKey(Notes.NOTE) == false) {
            values.put(Notes.NOTE, "");
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        String tableName = tableName(tableIndex(initialValues));
        long rowId = db.insert(tableName, Notes.NOTE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String tableName = tableName(tableIndex(where));
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

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String tableName = tableName(tableIndex(values));
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

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/words", NOTES_WORDS);

        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(Notes._ID, Notes._ID);
        sNotesProjectionMap.put(Notes.TITLE, Notes.TITLE);
        sNotesProjectionMap.put(Notes.NOTE, Notes.NOTE);
        sNotesProjectionMap.put(Notes.CREATED_DATE, Notes.CREATED_DATE);
        sNotesProjectionMap.put(Notes.MODIFIED_DATE, Notes.MODIFIED_DATE);

        sNotesProjectionMap2 = new HashMap<String, String>();
        sNotesProjectionMap2.put(Notes._ID, NOTES_TABLE_NAMES[0].concat(".").concat(Notes._ID));
        sNotesProjectionMap2.put(Notes.TITLE, NOTES_TABLE_NAMES[2].concat(".").concat(Notes.TITLE));
        sNotesProjectionMap2.put(Notes.NOTE, NOTES_TABLE_NAMES[0].concat(".").concat(Notes.NOTE));
        sNotesProjectionMap2.put(Notes.CREATED_DATE, NOTES_TABLE_NAMES[2].concat(".").concat(Notes.CREATED_DATE));
        sNotesProjectionMap2.put("date", NOTES_TABLE_NAMES[0].concat(".").concat(Notes.CREATED_DATE));
    }
}
