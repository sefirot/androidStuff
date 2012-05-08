package com.applang;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 *	facilitates database access for a cost sharing system
 *
 * @author lotharla
 */
public class DbAdapter
{
	private static final String TAG = DbAdapter.class.getSimpleName();

	public static final int VERSION = 1;
	
	public DbAdapter(Context context) {
		if (context != null) {
			SQLiteOpenHelper opener = new SQLiteOpenHelper(context,
					Util.databaseName(), null, VERSION) {
				@Override
				public void onCreate(SQLiteDatabase db) {
					createTables(db);
				}

				@Override
				public void onUpgrade(SQLiteDatabase db, int oldVersion,
						int newVersion) {
					Log.w(TAG, "Upgrading database from version " + oldVersion
							+ " to " + newVersion
							+ ", which will destroy all old data");

					recreateTables(db);
				}
			};
			
			mDb = opener.getWritableDatabase();
		}
		else {
			File dir = Util.databasesDir();
			if (!dir.isDirectory())
				dir.mkdirs();
			
			mDb = SQLiteDatabase.openDatabase(
				Util.databaseFile().getPath(), 
           		null, 
           		SQLiteDatabase.CREATE_IF_NECESSARY);
			
			createTables(mDb, "if not exists");
		}
		
		if (tableList.size() > 0)
			table1 = tableList.get(0);
	}
	
    private SQLiteDatabase mDb = null;
    
    /** @hide */ public SQLiteDatabase getDb() {
		return mDb;
	}

    public static LinkedHashMap<String, String> tableDefs = new LinkedHashMap<String, String>();
    List<String> tableList = new ArrayList<String>(tableDefs.keySet());
    public String table1 = "";

	private void createTables(SQLiteDatabase db, Object... params) {
		for (int i = 0; i < tableList.size(); i++) {
			String table = tableList.get(i);
			String sql = String.format("create table %s %s (%s);", 
					Util.param("", 0, params), 
					table, 
					tableDefs.get(table));
			db.execSQL(sql);
		}
	}
    
	private void recreateTables(SQLiteDatabase db) {
		for (int i = tableList.size() - 1; i > -1; i--) 
			db.execSQL("DROP TABLE IF EXISTS " + tableList.get(i));
		
    	createTables(db);
	}
	
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
    
    public void close() {
        if (mDb != null) 
        	mDb.close();
    }
    
    public void clear() {
    	recreateTables(mDb);
    }
    
    public void drop(String tableName) {
		mDb.execSQL("DROP TABLE IF EXISTS " + tableName);
	}
    
	protected void rename(String oldTableName, String newTableName) {
		mDb.execSQL("ALTER TABLE " + oldTableName + " RENAME TO " + newTableName);
	}

    protected long insert(ContentValues values) {
		long rowId = mDb.insert(table1, null, values);
		return rowId;
	}

	protected int update(long rowId, ContentValues values) {
		return mDb.update(table1, values, "ROWID=" + rowId, null);
	}

	protected int delete(String clause) {
		return mDb.delete(table1, clause, null);
	}

    protected Cursor rawQuery(String sql, String[] selectionArgs) throws SQLiteException {
		return mDb.rawQuery(sql, selectionArgs);
	}

	protected Cursor query(String[] columns, String clause) throws SQLiteException {
		return mDb.query(true, 
				table1, 
				columns, clause, 
				null, null, null, null, null);
	}
}