package com.applanger.tripcostcalculator;

import com.applanger.tripcostcalculator.NotesDbAdapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ReceiversDB {
	

    public static final String KEY_RECEIVER = "receiver";
    public static final String KEY_SHARE = "share";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_EVENTID = "eventId";

    private static final String TAG = "ReceiversDB";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table notes2 (_id integer, "
        + "receiver text not null, share integer not null, eventId integer not null);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "notes2";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
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
    public ReceiversDB(Context ctx) {
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
    public ReceiversDB open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }



public long createNote(String receiver, String share, String event) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_RECEIVER, receiver);
    initialValues.put(KEY_SHARE, share);
    initialValues.put(KEY_EVENTID, event);

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

    return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_RECEIVER, KEY_SHARE}, null, null, null, null, null);
}

/**
 * Return a Cursor positioned at the note that matches the given rowId
 * 
 * @param rowId id of note to retrieve
 * @return Cursor positioned to matching note, if found
 * @throws SQLException if note could not be found/retrieved
 */
public Cursor fetchNote(long rowId) throws SQLException {

	String[] columns = new String[]{KEY_RECEIVER, KEY_SHARE, KEY_EVENTID};
			
    Cursor mCursor =

        mDb.query(true, DATABASE_TABLE, columns, null, null,KEY_EVENTID + "=" + rowId, null, null, null);
    
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
    args.put(KEY_RECEIVER, date);
    args.put(KEY_SHARE, submitter);
    
    return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
}
}
