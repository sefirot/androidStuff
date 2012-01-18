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

public class DbAdapter extends SQLiteOpenHelper 
{
	private static final String TAG = "DbAdapter";

	public static final String DATABASE_NAME = "data";
	public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_TABLE = "cashpoint";
	
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
			" timestamp text," +			//	if null it's a distribution
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
    	return new String[] {"entry", "name", "amount", "currency", "timestamp", "comment"};
    }
    
    protected ContentValues putValues(int entry, String name, float amount, String currency, String timeStamp, String comment) {
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
	
	protected long addRecord(int entryId, String name, float amount, String currency, String timeStamp, String comment) {
		return mDb.insert(DATABASE_TABLE, null, 
        		putValues(entryId, name, amount, currency, timeStamp, comment));
	}

	public Cursor doQuery(String sql, String[] selectionArgs) {
        Cursor cursor = mDb.rawQuery(sql, selectionArgs);
        if (cursor != null) 
        	cursorSize = cursor.getCount();
        else
        	cursorSize = -1;
        return cursor;
	}
	
	public int cursorSize = -1;
	
	public int getNewEntryId() {
        int entryId = 0;
        
        Cursor cursor = doQuery("select max(entry) from " + DATABASE_TABLE, null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
        	entryId = cursor.getInt(0);
        }
        cursor.close();
        
        return ++entryId;
    }
    
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public String getTimeStamp(Date date) {
		return mDateFormat.format(date);
	}
    
	public int addEntry(String name, float amount, String currency, String comment) {
    	int entryId = getNewEntryId();
        
        long rowId = addRecord(entryId, name, amount, 
                	currency, getTimeStamp(new Date()), comment);
        if (rowId < 0)
        	return -1;
        else 
        	return entryId;
    }
    
    public int removeEntry(int entryId) {
    	return mDb.delete(DATABASE_TABLE, "entry=" + entryId, null);
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
    
    public String fetchTimestamp(int entryId) {
    	String timeStamp = null;
    	
    	Cursor cursor = fetchEntry(entryId);
    	if (cursor != null) {
			do {
				timeStamp = cursor.getString(cursor.getColumnIndex("timestamp"));
				if (timeStamp != null)
					break;
			} while (cursor.moveToNext());
	    	
	    	cursor.close();
    	}
    	
    	return timeStamp;
    }
    
    public boolean doDistribution(int entryId, Map<String, Number> shares) {
    	Cursor cursor = fetchEntry(entryId);
    	if (cursor == null || cursor.getCount() != 1)
    		return false;
    	
    	String entrant = cursor.getString(cursor.getColumnIndex("name"));
    	float amount = cursor.getFloat(cursor.getColumnIndex("amount"));
    	String currency = cursor.getString(cursor.getColumnIndex("currency"));
    	String comment = cursor.getString(cursor.getColumnIndex("comment"));
		
    	cursor.close();
    	
    	for (String name : shares.keySet()) 
    		if (!entrant.equals(name)) {
    			float share = shares.get(name).floatValue();
    			
    			if (addRecord(entryId, name, -share, currency, null, comment) < 0)
    	        	return false;
    	        
    			amount -= share;
    		}

    	if (addRecord(entryId, entrant, -amount, currency, null, comment) < 0)
        	return false;
   	
    	return true;
    }
    
    public Set<String> getNames() {
    	TreeSet<String> names = new TreeSet<String>();
    	
        Cursor cursor = doQuery("select distinct name from " + DATABASE_TABLE, null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
    		do {
    			names.add(cursor.getString(cursor.getColumnIndex("name")));
    		} while (cursor.moveToNext());
        }
        cursor.close();
        
    	return names;
    }

    public float getSum(String clause) throws Exception {
    	float sum = 0f;
    	
        Cursor cursor = doQuery("select sum(amount) from " + DATABASE_TABLE + " where " + clause, null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
        	sum = cursor.getFloat(0);
        }
        cursor.close();
        
        return sum;
    }
}