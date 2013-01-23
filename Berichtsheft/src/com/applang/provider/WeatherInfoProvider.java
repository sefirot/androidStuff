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
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * Provides access to a database of weathers. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class WeatherInfoProvider extends ContentProvider {

    private static final String TAG = "WeatherInfoProvider";

    public static final String DATABASE_NAME = "weather_info.db";
    private static final int DATABASE_VERSION = 1;
    private static final String WEATHERS_TABLE_NAME = "weathers";

    private static HashMap<String, String> sWeathersProjectionMap;

    private static final int WEATHERS = 1;
    private static final int WEATHER_ID = 2;

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
            db.execSQL("CREATE TABLE " + WEATHERS_TABLE_NAME + " ("
                    + Weathers._ID + " INTEGER PRIMARY KEY,"
                    + Weathers.DESCRIPTION + " TEXT,"
                    + Weathers.PRECIPITATION + " FLOAT,"
                    + Weathers.MAXTEMP + " FLOAT,"
                    + Weathers.MINTEMP + " FLOAT,"
                    + Weathers.CREATED_DATE + " INTEGER,"
                    + Weathers.MODIFIED_DATE + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
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
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
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
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = WeatherInfo.Weathers.DEFAULT_SORT_ORDER;
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
            getContext().getContentResolver().notifyChange(noteUri, null);
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
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
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
        int count;
        switch (sUriMatcher.match(uri)) {
        case WEATHERS:
            count = db.update(WEATHERS_TABLE_NAME, values, where, whereArgs);
            break;

        case WEATHER_ID:
            String weatherId = uri.getPathSegments().get(1);
            count = db.update(WEATHERS_TABLE_NAME, values, Weathers._ID + "=" + weatherId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", WEATHERS);
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", WEATHER_ID);

        sWeathersProjectionMap = new HashMap<String, String>();
        sWeathersProjectionMap.put(Weathers._ID, Weathers._ID);
        sWeathersProjectionMap.put(Weathers.DESCRIPTION, Weathers.DESCRIPTION);
        sWeathersProjectionMap.put(Weathers.PRECIPITATION, Weathers.PRECIPITATION);
        sWeathersProjectionMap.put(Weathers.MAXTEMP, Weathers.MAXTEMP);
        sWeathersProjectionMap.put(Weathers.MINTEMP, Weathers.MINTEMP);
        sWeathersProjectionMap.put(Weathers.CREATED_DATE, Weathers.CREATED_DATE);
        sWeathersProjectionMap.put(Weathers.MODIFIED_DATE, Weathers.MODIFIED_DATE);
    }
}
