package app.learn.test;

import java.util.*;

import android.test.*;
import android.database.Cursor;

import app.learn.*;

public class CashPointTest extends ActivityInstrumentationTestCase2<CashPoint> {
	public CashPointTest() {
		super("app.learn", CashPoint.class);
	}
	
    private CashPoint mActivity;
    private DbAdapter dbAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();
    	dbAdapter = new DbAdapter(mActivity);
    }

    @Override
    protected void tearDown() throws Exception {
		dbAdapter.close();
        super.tearDown();
	}
    
    public void testEntry() {
    	int entryId = dbAdapter.addEntry("Bob", 100.5f, null, "gas");
    	assertEquals(dbAdapter.getNewEntryId() - 1, entryId);
    	
    	Cursor outputCursor = dbAdapter.fetchEntry(entryId);    	
		assertEquals("Bob", outputCursor.getString(1));
		assertEquals(100.5f, outputCursor.getFloat(2));
		assertEquals(null, outputCursor.getString(3));
		String now = dbAdapter.getTimeStamp(new Date());
		assertEquals(now.substring(0, 16), 
				outputCursor.getString(4).substring(0, 16));
		assertEquals("gas", outputCursor.getString(5));
		outputCursor.close();
    }
    
    public void testDistribution() {
    	float amount = 100.2f;
    	int entryId = dbAdapter.addEntry("Bob", amount, null, "gas");
    	HashMap<String, Number> shares = new HashMap<String, Number>();
    	shares.put("Sue", amount / 3);
    	shares.put("Tom", amount / 3);
    	assertTrue(dbAdapter.doDistribution(entryId, shares));
    	
    	Cursor cursor = dbAdapter.fetchEntry(entryId);
		assertEquals(4, cursor.getCount());
		float sum = 0f;
		do {
			sum += cursor.getFloat(cursor.getColumnIndex("amount"));
		} while (cursor.moveToNext());
		assertFalse("distribution leaks", Math.abs(sum) > 0.001);
		cursor.close();
    }
    
}
