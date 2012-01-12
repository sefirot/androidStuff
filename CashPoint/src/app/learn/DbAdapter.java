package app.learn;

import java.util.*;
import java.text.SimpleDateFormat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter extends SQLiteOpenHelper {

	private static final String TAG = "DbAdapter";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "cashpoint";
    private static final int DATABASE_VERSION = 1;
	
	public DbAdapter(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table " + DATABASE_TABLE + " (" +
			" entry integer not null," +	//	unique for the entries, reference for the distributions
			" name text not null," +		//	the person involved
			" amount real not null," +		//	the amount of money, negative for distributions
			" currency text," +				//	if null it's the default currency (Euro or Dollar or ...)
			" date numeric," +				//	if null it's a distribution
			" comment text" +				//	optional, for recognition
			" );"
		);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
		
		recreate(db);
	}
    
    public void recreate(SQLiteDatabase db) {
    	db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
    	onCreate(db);
    }
	
    private SQLiteDatabase mDb;
    
    public SQLiteDatabase getDb() {
		return mDb;
	}

    String[] getFieldNames() {
    	return new String[] {"entry", "name", "amount", "currency", "date", "comment"};
    }
    
    ContentValues putValues(int entry, String name, float amount, String currency, String timeStamp, String comment) {
        ContentValues values = new ContentValues();
        
    	String[] fields = getFieldNames();
        values.put(fields[0], entry);
        values.put(fields[1], name);
        values.put(fields[2], amount);
        values.put(fields[3], currency);
        values.put(fields[4], timeStamp);
        values.put(fields[5], comment);
        
        return values;
    }

	public int getNewEntryId() {
        Cursor cursor = mDb.rawQuery("select max(entry) from " + DATABASE_TABLE, null);
        int retval = 0;
        if (cursor.getCount() > 0) {
        	cursor.moveToFirst();
        	retval = cursor.getInt(0);
        }
        cursor.close();
        return ++retval;
    }
    
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public String getTimeStamp(Date date) {
		return mDateFormat.format(date);
	}
    
	public int addEntry(String name, float amount, String currency, String comment) {
    	int entryId = getNewEntryId();
        
        long rowId = mDb.insert(DATABASE_TABLE, null, 
        		putValues(entryId, name, amount, 
                	currency, getTimeStamp(new Date()), comment));
        if (rowId < 0)
        	return -1;
        else 
        	return entryId;
    }
    
    public Cursor fetchEntry(int entryId) {
        Cursor cursor = mDb.query(true, DATABASE_TABLE, 
        		getFieldNames(), 
        		"entry=" + entryId, 
        		null, null, null, null, null);
        if (cursor != null) 
            cursor.moveToFirst();
        return cursor;
    }
    
    public boolean doDistribution(int entryId, Map<String, Number> shares) {
    	Cursor cursor = fetchEntry(entryId);
    	if (cursor == null)
    		return false;
    	
    	String entrant = cursor.getString(cursor.getColumnIndex("name"));
    	float amount = cursor.getFloat(cursor.getColumnIndex("amount"));
    	String currency = cursor.getString(cursor.getColumnIndex("currency"));
    	
    	for (String name : shares.keySet()) 
    		if (!entrant.equals(name)) {
    			float share = shares.get(name).floatValue();
    			
    			if (mDb.insert(DATABASE_TABLE, null, 
    					putValues(entryId, name, -share, currency, null, null)) < 0)
    	        	return false;
    	        
    			amount -= share;
    		}

    	if (mDb.insert(DATABASE_TABLE, null, 
    		   putValues(entryId, entrant, -amount, currency, null, null)) < 0)
        	return false;
   	
    	return true;
    }

}