/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.applanger.tripcostcalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;


/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class NotesDbAdapter {

    public static final String KEY_DATE = "date";
    public static final String KEY_SUBMITTER = "submitter";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_PURPOSE = "purpose";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_RECEIVER = "receiver";
    public static final String KEY_RECEIVERS = "receivers";
    public static final String KEY_SHARE = "share";
    public static final String KEY_ENTRYID = "entry_id";

    private static final String TAG = "NotesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String CREATE_TABLE =
        "create table notes (_id integer primary key autoincrement, "
        + "date text not null," 
        + "submitter text not null," 
        + "amount integer not null,"  
        + "purpose text not null);";
    
    private static final String CREATE_TABLE2 =
        "create table notes2 (_id integer primary key autoincrement, "
        + "receiver text not null," 
        + "share integer not null," 
        + "entry_id integer not null);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "notes";
    private static final String DATABASE_TABLE2 = "notes2";
    private static final int DATABASE_VERSION = 5;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(CREATE_TABLE);
            db.execSQL(CREATE_TABLE2);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS notes");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public NotesDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public NotesDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createNote(String date, String submitter,String amount,String purpose) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_SUBMITTER, submitter);
        initialValues.put(KEY_AMOUNT, amount);
        initialValues.put(KEY_PURPOSE, purpose);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public long createNote2(String receivers, String share, long entry_id) {
        ContentValues initialValues = new ContentValues();
		char delimiter = 12;
		TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(delimiter);
		splitter.setString(receivers);
		for (String receiver : splitter) {
			initialValues.put(KEY_RECEIVER, receiver);
			initialValues.put(KEY_SHARE, share);
			initialValues.put(KEY_ENTRYID, entry_id);
		}
        return mDb.insert(DATABASE_TABLE2, null, initialValues);
    }
    
    
    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteNote(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllNotes() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_DATE, KEY_SUBMITTER, KEY_AMOUNT,KEY_PURPOSE}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchNote(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_DATE, KEY_SUBMITTER, KEY_AMOUNT,KEY_PURPOSE}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    public Cursor fetchReceivers(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_DATE, KEY_SUBMITTER, KEY_AMOUNT,KEY_PURPOSE}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateNote(long rowId, String date, String submitter,String amount,String receivers, String purpose) {
        ContentValues args = new ContentValues();
        args.put(KEY_DATE, date);
        args.put(KEY_SUBMITTER, submitter);
        args.put(KEY_AMOUNT, amount);
        args.put(KEY_PURPOSE, purpose);
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
