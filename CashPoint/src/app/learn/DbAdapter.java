package app.learn;

import java.util.*;
import java.text.SimpleDateFormat;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
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
			" entry integer not null," +	//	unique for the entries, reference for the portions
			" name text not null," +		//	the person involved
			" amount real not null," +		//	the amount of money, negative for portions
			" currency text," +				//	if null it's the default currency (Euro or Dollar or ...)
			" timestamp text," +			//	if null it's a portion
			" comment text," +				//	optional, for recognition
			" expense integer not null" +	//	boolean, if true then the amount has been expended and possibly shared among others (apportionment)
			" );"
		);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
		
		recreate(db);
	}
    
	private void recreate(SQLiteDatabase db) {
    	db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
    	onCreate(db);
	}
	
    private SQLiteDatabase mDb;
    
    public void clear() {
    	recreate(mDb);
    }
    
    public void drop(String tableName) {
		mDb.execSQL("DROP TABLE IF EXISTS " + tableName);
	}
    
	protected void rename(String oldTableName, String newTableName) {
		drop(newTableName);
		mDb.execSQL("ALTER TABLE " + oldTableName + " RENAME TO " + newTableName);
	}

    protected String[] getFieldNames() {
    	return new String[] {"entry", "name", "amount", "currency", "timestamp", "comment", "expense", "ROWID"};
    }
    
    protected ContentValues putValues(int entry, String name, float amount, String currency, String timestamp, String comment, boolean expense) {
        ContentValues values = new ContentValues();
        
    	String[] fields = getFieldNames();
        values.put(fields[0], entry);
        values.put(fields[1], name);
        values.put(fields[2], amount);
        values.put(fields[3], currency);
        values.put(fields[4], timestamp);
        values.put(fields[5], comment);
        values.put(fields[6], expense ? 1 : 0);
        
        return values;
    }
	
	protected long addRecord(int entryId, String name, float amount, String currency, String timestamp, String comment) {
		return mDb.insert(DATABASE_TABLE, null, 
        		putValues(entryId, name, amount, currency, timestamp, comment, false));
	}
	
	protected Cursor doQuery(String sql, String[] selectionArgs) {
        Cursor cursor = null;
		try {
			cursor = mDb.rawQuery(sql, selectionArgs);
		} catch (SQLiteException e) {}
        if (cursor != null) 
        	cursorSize = cursor.getCount();
        else
        	cursorSize = -1;
        return cursor;
	}
	
	protected int cursorSize = -1;

	protected int updateExpenseFlag(long rowId, boolean expense) {
        ContentValues values = new ContentValues();
        values.put(getFieldNames()[6], expense ? 1 : 0);
		return mDb.update(DATABASE_TABLE, values, "ROWID=" + rowId, null);
	}

	public boolean isExpense(long rowId) {
		boolean expensive = false;
		Cursor cursor = doQuery("select expense from " + DATABASE_TABLE + " where ROWID=" + rowId, null);
		if (cursorSize > 0) {
        	cursor.moveToFirst();
			expensive = cursor.getInt(0) > 0;
		}
		return expensive;
	}
	
	public int getNewEntryId() {
        int entryId = 0;
        
        Cursor cursor = doQuery("select max(entry) from " + DATABASE_TABLE, null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
        	entryId = cursor.getInt(0);
        }
        if (cursor != null)
        	cursor.close();
        
        return ++entryId;
    }
    
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public String getTimestamp(Date date) {
		return mDateFormat.format(date);
	}
    
	public int addEntry(String name, float amount, String currency, String comment) {
    	int entryId = getNewEntryId();
        
        long rowId = addRecord(entryId, name, amount, 
                	currency, getTimestamp(new Date()), comment);
        if (rowId < 0)
        	return -1;
        else 
        	return entryId;
    }
    
    public int removeEntry(int entryId) {
    	return mDb.delete(DATABASE_TABLE, "entry=" + entryId, null);
    }
    
    public Cursor fetchEntry(int entryId) {
        Cursor cursor = mDb.query(true, 
        		DATABASE_TABLE, 
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
        	int columIndex = cursor.getColumnIndex("timestamp");
			do {
				timeStamp = cursor.getString(columIndex);
				if (timeStamp != null)
					break;
			} while (cursor.moveToNext());
	    	
	    	cursor.close();
    	}
    	
    	return timeStamp;
    }
    
    public boolean apportionment(int entryId, Map<String, Number> shares) {
    	Cursor cursor = fetchEntry(entryId);
    	if (cursor == null || cursor.getCount() != 1)
    		return false;
    	
    	long rowId = cursor.getLong(cursor.getColumnIndex("ROWID"));
    	String entrant = cursor.getString(cursor.getColumnIndex("name"));
    	float amount = cursor.getFloat(cursor.getColumnIndex("amount"));
    	String currency = cursor.getString(cursor.getColumnIndex("currency"));
    	String comment = cursor.getString(cursor.getColumnIndex("comment"));
		
    	cursor.close();
    	
    	updateExpenseFlag(rowId, true);
    	
    	for (String name : shares.keySet()) 
    		if (!entrant.equals(name)) {
    			float share = shares.get(name).floatValue();
    			
    			if ((rowId = addRecord(entryId, name, -share, currency, null, comment)) < 0)
    	        	return false;
    	    	
    	    	updateExpenseFlag(rowId, true);
    	        
    			amount -= share;
    		}

    	if ((rowId = addRecord(entryId, entrant, -amount, currency, null, comment)) < 0)
        	return false;
    	
    	updateExpenseFlag(rowId, true);
   	
    	return true;
    }
    
    public Set<String> getNames() {
    	TreeSet<String> names = new TreeSet<String>();
    	
        Cursor cursor = doQuery("select distinct name from " + DATABASE_TABLE, null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
    		do {
    			names.add(cursor.getString(0));
    		} while (cursor.moveToNext());
        }
        if (cursor != null)
        	cursor.close();
        
    	return names;
    }

    public Set<Integer> getEntryIds(String clause) {
    	TreeSet<Integer> ids = new TreeSet<Integer>();
    	
        Cursor cursor = doQuery("select entry from " + DATABASE_TABLE + 
        		(clause != null && clause.length() > 0 ? " where " + clause : ""), null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
    		do {
    			ids.add(cursor.getInt(0));
    		} while (cursor.moveToNext());
        }
        if (cursor != null)
        	cursor.close();
        
        return ids;
    }

    public float getSum(String clause) {
    	float sum = 0f;
    	
        Cursor cursor = doQuery("select sum(amount) from " + DATABASE_TABLE + 
        		(clause != null && clause.length() > 0 ? " where " + clause : ""), null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
        	sum = cursor.getFloat(0);
        }
        if (cursor != null)
        	cursor.close();
        
        return sum;
    }

    public int getCount(String clause) {
    	int count = 0;
    	
        Cursor cursor = doQuery("select count(*) from " + DATABASE_TABLE + 
        		(clause != null && clause.length() > 0 ? " where " + clause : ""), null);
        if (cursorSize > 0) {
        	cursor.moveToFirst();
        	count = cursor.getInt(0);
        }
        if (cursor != null)
        	cursor.close();
        
        return count;
    }
}