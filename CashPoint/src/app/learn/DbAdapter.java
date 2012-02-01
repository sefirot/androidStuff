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
    public static final String DATABASE_TABLE = "kitty";
	
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
			" expense integer not null" +	//	boolean, if true then the amount has been expended and likely shared among others
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
		mDb.execSQL("ALTER TABLE " + oldTableName + " RENAME TO " + newTableName);
	}

    protected String[] getFieldNames() {
    	return new String[] {"entry", "name", "amount", "currency", "timestamp", "comment", "expense", "ROWID"};
    }
    
    protected ContentValues putValues(int entry, String name, double amount, String currency, String timestamp, String comment, boolean expense) {
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
	
    protected long addRecord(int entryId, String name, double amount, String currency, String timestamp, String comment) {
    	ContentValues values = putValues(entryId, name, amount, currency, timestamp, comment, false);
    	long rowId = mDb.insert(DATABASE_TABLE, null, values);
    	if (rowId < 0)
    		Log.w(TAG, String.format("addRecord failed with : %s", values.toString()));
    	return rowId;
	}
	
    public interface QueryEvaluator<T> 
	{
		public T evaluate(Cursor cursor, T defaultResult, Object... params);
	}
	
	protected <T> T rawQuery(String sql, String[] selectionArgs, QueryEvaluator<T> qe, T defaultResult, Object... params) {
        Cursor cursor = null;
        
		try {
			cursor = mDb.rawQuery(sql, selectionArgs);
	        if (cursor != null) {
	        	if (cursor.getCount() > 0)
	        		cursor.moveToFirst();
	        	
	        	return qe.evaluate(cursor, defaultResult, params);
	        }
		} 
		catch (SQLiteException ex) {
		}
		finally {
			if (cursor != null)
				cursor.close();
		}

		return defaultResult;
	}

	protected int updateExpenseFlag(long rowId, boolean expense) {
        ContentValues values = new ContentValues();
        values.put(getFieldNames()[6], expense ? 1 : 0);
		return mDb.update(DATABASE_TABLE, values, "ROWID=" + rowId, null);
	}

	public boolean isExpense(long rowId) {
		return rawQuery("select expense from " + DATABASE_TABLE + " where ROWID=" + rowId, null, 
			new QueryEvaluator<Boolean>() {
				public Boolean evaluate(Cursor cursor, Boolean defaultResult, Object... params) {
					return cursor.getCount() > 0 && cursor.getInt(0) > 0;
				}
			}, false);
	}
	
	public int getNewEntryId() {
		return 1 + rawQuery("select max(entry) from " + DATABASE_TABLE, null, 
			new QueryEvaluator<Integer>() {
				public Integer evaluate(Cursor cursor, Integer defaultResult, Object... params) {
					if (cursor.getCount() > 0)
						return cursor.getInt(0);
					else
						return defaultResult;
				}
			}, -1);
    }
    
    public String timestampNow() {
		return timestamp(new Date());
	}
    
	public int addEntry(String name, double amount, String currency, String comment, boolean expense) {
    	int entryId = getNewEntryId();
        
        long rowId = addRecord(entryId, name, amount, currency, timestampNow(), comment);
        if (rowId < 0) {
    		removeEntry(entryId);
        	return -1;
        }
        else {
			if (updateExpenseFlag(rowId, expense) != 1)
				return -1;
			
        	return entryId;
        }
    }
    
    public int removeEntry(int entryId) {
    	return mDb.delete(DATABASE_TABLE, "entry=" + entryId, null);
    }
    
    public <T> T fetchEntry(int entryId, QueryEvaluator<T> qe, T defaultResult, Object... params) {
        Cursor cursor = null;
        
		try {
	        cursor = mDb.query(true, 
	        		DATABASE_TABLE, 
	        		getFieldNames(), 
	        		"entry=" + entryId, 
	        		null, null, null, null, null);
	        
	        if (cursor != null) {
	        	if (cursor.getCount() > 0)
	        		cursor.moveToFirst();
	        }
	        	
        	return qe.evaluate(cursor, defaultResult, params);
		} 
		catch (SQLiteException ex) {
		}
		finally {
			if (cursor != null)
				cursor.close();
		}

		return defaultResult;
    }
    
    public String fetchField(int entryId, final String name) {
    	return fetchEntry(entryId, 
			new QueryEvaluator<String>() {
				public String evaluate(Cursor cursor, String defaultResult, Object... params) {
					int timestampIndex = cursor.getColumnIndex("timestamp");
					do {
						if (cursor.getString(timestampIndex) != null)
							return cursor.getString(cursor.getColumnIndex(name));
					} while (cursor.moveToNext());
					return defaultResult;
				}
			}, null);
    }
    
    public boolean allocate(boolean expense, int entryId, Map<String, Number> portions) {
    	long rowId;
    	
		for (String name : portions.keySet()) {
			double portion = portions.get(name).doubleValue();

			if ((rowId = addRecord(entryId, name, portion, null, null, null)) < 0)
				return false;

			if (updateExpenseFlag(rowId, expense) != 1)
				return false;
		}
		
		return portions.size() > 0;
    }
    
    public Set<String> getNames() {
		return rawQuery("select distinct name from " + DATABASE_TABLE + " where length(name) > 0", null, 
			new QueryEvaluator<Set<String>>() {
				public Set<String> evaluate(Cursor cursor, Set<String> defaultResult, Object... params) {
			    	TreeSet<String> names = new TreeSet<String>();
		    		do {
		    			names.add(cursor.getString(0));
		    		} while (cursor.moveToNext());
					return names;
				}
			}, null);
    }
    
    public Set<Integer> getEntryIds(String clause) {
		return rawQuery("select entry from " + DATABASE_TABLE + 
				(Util.notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Set<Integer>>() {
				public Set<Integer> evaluate(Cursor cursor, Set<Integer> defaultResult, Object... params) {
			    	TreeSet<Integer> ids = new TreeSet<Integer>();
		    		do {
		       			ids.add(cursor.getInt(0));
		       		} while (cursor.moveToNext());
					return ids;
				}
			}, null);
    }
    
    public Set<Long> getRowIds(String clause) {
		return rawQuery("select rowid from " + DATABASE_TABLE + 
				(Util.notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Set<Long>>() {
				public Set<Long> evaluate(Cursor cursor, Set<Long> defaultResult, Object... params) {
			    	TreeSet<Long> ids = new TreeSet<Long>();
		    		do {
		       			ids.add(cursor.getLong(0));
		       		} while (cursor.moveToNext());
					return ids;
				}
			}, null);
    }

    public double getSum(String clause) {
		return rawQuery("select sum(amount) from " + DATABASE_TABLE + 
				(Util.notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Double>() {
				public Double evaluate(Cursor cursor, Double defaultResult, Object... params) {
					return cursor.getDouble(0);
				}
			}, 0.);
   }

    public int getCount(String clause) {
		return rawQuery("select count(*) from " + DATABASE_TABLE + 
				(Util.notNullOrEmpty(clause) ? " where " + clause : ""), null, 
			new QueryEvaluator<Integer>() {
				public Integer evaluate(Cursor cursor, Integer defaultResult, Object... params) {
					return cursor.getInt(0);
				}
			}, 0);
    }
    
	public static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    public static String timestamp(Date date) {
		return timestampFormat.format(date);
	}
}