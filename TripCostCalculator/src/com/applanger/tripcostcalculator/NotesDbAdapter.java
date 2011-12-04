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

import java.util.ArrayList;
import java.util.Iterator;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
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
    public static final String KEY_DATEMAX = "dateMax";
    public static final String KEY_NAME = "name";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_CURRENCY = "currency";
    public static final String KEY_PURPOSE = "purpose";
    public static final String KEY_ROWID = "row_id";
    public static final String KEY_NAMEID = "name_id";
    public static final String KEY_RECEIVERSID = "receivers_id";
    public static final String KEY_PURPOSEID = "purposes_id";
    public static final String KEY_CURRNCYID = "currency_id";
    public static final String KEY_ENTRYID = "entry_id";
    public static final String KEY_RECCOUNT = "recCount";

    private static final String TAG = "NotesDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String CREATE_TABLE =
        "create table parent (row_id integer primary key autoincrement, "
    	+ "entry_id integer not null," 
        + "date numeric not null," 
        + "submitter text not null," 
        + "amount real not null,"
        + "currency text not null,"
        + "purpose text not null);";
    
    private static final String CREATE_TABLE2 =
        "create table receivers (receivers_id integer primary key autoincrement, "
    	+ "entry_id integer not null," 
        + "receiver text not null,";
    
    private static final String CREATE_TABLE3 =
        "create table names (name_id integer primary key autoincrement, " 
        + "name text not null,";
    
    private static final String CREATE_TABLE4 =
        "create table purposes (purpose_id integer primary key autoincrement, " 
        + "purpose text not null,";
    
    private static final String CREATE_TABLE5 =
        "create table currencies (currency_id integer primary key autoincrement, "
    	+ "currency text not null," 
        + "toEuro real not null,";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "parent";
    private static final String DATABASE_TABLE2 = "receivers";
    private static final String DATABASE_TABLE3 = "names";
    private static final String DATABASE_TABLE4 = "puposes";
    private static final String DATABASE_TABLE5 = "currencies";
    private static final int DATABASE_VERSION = 6;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(CREATE_TABLE);
            db.execSQL(CREATE_TABLE2);
            db.execSQL(CREATE_TABLE3);
            db.execSQL(CREATE_TABLE4);
            db.execSQL(CREATE_TABLE5);
            
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
    public long createNote(String date, String submitter,String amount,String purpose, String currency) {
        ContentValues initialValues = new ContentValues();
        int entry_id = 0;
        entry_id++;
        initialValues.put(KEY_ENTRYID, entry_id);
        initialValues.put(KEY_DATE, date);
        initialValues.put(KEY_NAME, submitter);
        initialValues.put(KEY_AMOUNT, amount);
        initialValues.put(KEY_CURRENCY, currency);
        initialValues.put(KEY_PURPOSE, purpose);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    public long createNote2(ArrayList<String> receivers, ArrayList<String> shares, long row_id) {
        ContentValues initialValues = new ContentValues();
        
        int recCount = 0;
        String receiver;
		String share;
		
		Iterator<String> a = receivers.iterator();
		Iterator<String> b = shares.iterator();
	    
		while (a.hasNext() && b.hasNext()) {
			recCount++;
			receiver = a.next();
			share = b.next();		
			
			initialValues.put(KEY_NAME, receiver);
			initialValues.put(KEY_AMOUNT, share);
			initialValues.put(KEY_ENTRYID, row_id);
			initialValues.put(KEY_RECCOUNT, recCount);
		}
		return mDb.insert(DATABASE_TABLE, null, initialValues);
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
    public Cursor fetchAllNames() {

        return mDb.query(DATABASE_TABLE3, new String[] {KEY_NAMEID, KEY_NAME}, null, null, null, null, null);
    }
    
    public Cursor fetchAllPurposes() {

        return mDb.query(DATABASE_TABLE4, new String[] {KEY_PURPOSEID, KEY_PURPOSE}, null, null, null, null, null);
    }
    

    
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

            mDb.query(true, DATABASE_TABLE2, new String[] {KEY_RECEIVER}, KEY_ENTRYID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    public Cursor fetchQueryResultSets(Bundle queryBundle, String queryIdStrings)throws SQLException {
    	
    	
    	
    	
    		SQLiteQueryBuilder QueryBuilder = new SQLiteQueryBuilder();
    	
   
    		String[] resultColumns = new String[] {select1,select2};
    		String selection = null;
    		String[] selectionArgs = null;
    		String groupBy = new String (select2);
    		
	        String oneInputSelection = new String (where1 + "= ?");
	        String twoInput = new String (oneInputSelection + "AND" + where2 + "= ?");
	        String threeInput = new String (twoInput + "AND" + where3 + "= ?");
	        String dateSelection = new String (KEY_DATE + "BETWEEN ?s AND ?s");
	        String oneInputDate = new String (oneInputSelection + "AND" + dateSelection);
	        String twoInputDate = new String (twoInput + "AND" + dateSelection);
	        String threeInputDate = new String (threeInput + "AND" + dateSelection);
	        
	       
	        
	        if(QueryID.contains("D")==true) {
	    	   if(QueryID.length()==1) {
	          		selectionArgs = new String [] {dateMin,dateMax};
	          		selection = dateSelection;
	           }else if(QueryID.length()==2) {
	          		selectionArgs = new String [] {input1,dateMin,dateMax};
	          		selection = oneInputDate;
	           }else if(QueryID.length()==3) {
	          		selectionArgs = new String [] {input1,input2,dateMin,dateMax};
	          		selection = twoInputDate;
	           }else if(QueryID.length()==4) {
	          		selectionArgs = new String [] {input1,input2,dateMin,dateMax};
	          		selection = threeInputDate;
	           }
	       }else {    
	    	   if(QueryID.length()==1) {
	       			selectionArgs = new String [] {input1};
	       			selection = oneInputSelection;
	    	   }else if(QueryID.length()==2) {
	       			selectionArgs = new String [] {where1,where2};
	       			selection = twoInput;
	    	   }else if(QueryID.length()==3) {
	       			selectionArgs = new String [] {where1,where2,where3};
	       			selection = threeInput;
	    	   }
	 
	       }
	         
	       if(QueryID.contains("R")==true) {
	        	QueryBuilder.setTables("notes INNER JOIN notes2 ON (notes.row_id = notes2.event_id)");	
	       }else {
	        	QueryBuilder.setTables("notes");
	       } 
       
	       Cursor mCursor =
	    	   QueryBuilder.query(mDb,resultColumns,selection,selectionArgs,groupBy,null,null);
        
	       if (mCursor != null) {
	    	   mCursor.moveToFirst();
	       }
	       return mCursor;
    }

    public boolean updateNote(long rowId, String date, String submitter,String amount, String purpose) {
        ContentValues args = new ContentValues();
        args.put(KEY_DATE, date);
        args.put(KEY_SUBMITTER, submitter);
        args.put(KEY_AMOUNT, amount);
        args.put(KEY_PURPOSE, purpose);
        
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean updateNote2(ArrayList<String> receivers, ArrayList<String> shares, long rowId) {
    	ContentValues args = new ContentValues();
    	int recCount = 0;
        String receiver;
		String share;
		
		Iterator<String> a = receivers.iterator();
		Iterator<String> b = shares.iterator();
	    
		while (a.hasNext()) {
			recCount++;
			receiver = a.next();
			share = b.next();		
			
			args.put(KEY_RECEIVER, receiver);
			args.put(KEY_SHARE, share);
			args.put(KEY_ENTRYID, rowId);
			args.put(KEY_RECCOUNT, recCount);
		}
		
        
        return mDb.update(DATABASE_TABLE2, args, KEY_ENTRYID + "=" + rowId, null) > 0;
    }
}
