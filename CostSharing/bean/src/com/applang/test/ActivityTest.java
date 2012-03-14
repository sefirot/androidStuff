package com.applang.test;

import java.util.HashMap;

import com.applang.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import junit.framework.TestCase;

/**
 * @author lotharla
 *
 */
public class ActivityTest extends TestCase 
{
	private static final String TAG = ActivityTest.class.getSimpleName();

    static {
		System.setProperty("sqlite4java.library.path", "/home/lotharla/work/sqlite/sqlite4java-213");
    }

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	protected Context getActivity() {
		return null;
	}
    
    public void testConnect() throws SQLiteException {
    	DbAdapter dbAdapter = new Transactor(getActivity());
    	
    	SQLiteDatabase db = dbAdapter.getDb();
    	assertTrue(db.isOpen());
    	Log.i(TAG, "database is open");
    	
        Cursor cursor = null;
		try {
			cursor = db.rawQuery("select * from sqlite_master;", new String[] {});
		    assertTrue(cursor.getCount() > 0);
			cursor.close();
			
		    cursor = db.rawQuery("PRAGMA table_info(" + dbAdapter.table1 + ")", null);
		    assertEquals(7, cursor.getCount());
		    String[] columnDefs = DbAdapter.tableDefs.get(dbAdapter.table1).split(",");
		    assertTrue(cursor.moveToFirst());
		    int i = 0;
            do {
                String columnName = cursor.getString(1);
            	assertTrue(columnDefs[i].startsWith(columnName));
            	i++;
        	} while (cursor.moveToNext());
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
			
		dbAdapter.close();
	}
}
