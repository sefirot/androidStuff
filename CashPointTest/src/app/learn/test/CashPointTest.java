package app.learn.test;

import java.io.*;
import java.util.*;

import junit.framework.Test;
import junit.framework.TestSuite;

import android.os.Environment;
import android.test.*;
import android.test.suitebuilder.TestSuiteBuilder;

import android.database.Cursor;

import app.learn.*;

public class CashPointTest extends ActivityInstrumentationTestCase2<CashPoint> 
{
	public static void main(String[] args) {
//		TestSuite suite = new TestSuite(CashPointTest.class);
//		InstrumentationTestRunner.run(suite);
	}
	
	public static Test suite() {
	    return new TestSuiteBuilder(CashPointTest.class).
	    		includePackages("app.learn.test.CashPointTest").build();
	}

	public CashPointTest() {
		super("app.learn", CashPoint.class);
	}
	
    private CashPoint mActivity;
    private DbAdapter dbAdapter;
    private Transactor transactor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        mActivity = this.getActivity();
        transactor = new Transactor(mActivity);
    	dbAdapter = (DbAdapter)transactor;
    }

    @Override
    protected void tearDown() throws Exception {
		dbAdapter.close();
        super.tearDown();
	}
    
    public void assertZero(String message, float number) {
    	assertFalse(message, Math.abs(number) > 0.001f);
    }
    
    public void assertAmountEquals(String message, Number expected, Number actual) {
    	String sexp = String.format("%.2f", expected.floatValue());
    	String sact = String.format("%.2f", actual.floatValue());
    	assertEquals(message, sexp, sact);
    }
    
    String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
    public void testClear() {
    	dbAdapter.recreate(dbAdapter.getDb());
    }
    
    public void testEntry() {
    	int entryId = dbAdapter.addEntry("Bob", 100.5f, null, "gas");
    	assertEquals(dbAdapter.getNewEntryId() - 1, entryId);
    	
    	Cursor cursor = dbAdapter.fetchEntry(entryId);    	
    	assertNotNull(cursor);
		assertEquals("Bob", cursor.getString(1));
		assertEquals(100.5f, cursor.getFloat(2));
		assertEquals(null, cursor.getString(3));
		String now = dbAdapter.getTimeStamp(new Date());
		assertEquals(now.substring(0, 16), 
				cursor.getString(4).substring(0, 16));
		assertEquals("gas", cursor.getString(5));
		cursor.close();
		
		assertEquals(1, dbAdapter.removeEntry(entryId));
		assertEquals(0, dbAdapter.fetchEntry(entryId).getCount());
	}

    void assertSize(int expected, int entry) {
    	Cursor cursor = dbAdapter.fetchEntry(entry);
    	assertNotNull(cursor);
    	assertEquals(expected, cursor.getCount());
    	cursor.close();
    }
    
    public void testDistribution() throws Exception {
    	float amount = 100.2f;
    	int entryId = dbAdapter.addEntry("Bob", amount, null, "gas");
    	
    	HashMap<String, Number> shares = new HashMap<String, Number>();
    	shares.put("Sue", amount / 4);
    	shares.put("Tom", amount / 3);
    	assertTrue(dbAdapter.doDistribution(entryId, shares));
    	
    	assertSize(4, entryId);
		assertZero("distribution leaking", dbAdapter.getSum("entry=" + entryId));
		
		assertNotNull("timestamp missing", dbAdapter.fetchTimestamp(entryId));
		
		Set<String> names = dbAdapter.getNames();
		assertTrue("some name missing", names.containsAll(Arrays.asList(participants)));
		
		assertEquals(4, dbAdapter.removeEntry(entryId));
		assertEquals(0, dbAdapter.fetchEntry(entryId).getCount());
    }
    
    public void testNames() {
    	File dbFile = new File(Environment.getDataDirectory(), "data/app.learn/databases/data");
    	assertTrue(dbFile.exists());
    	
    	assertEquals(new TreeSet<String>(Arrays.asList(participants)), dbAdapter.getNames());
    }
    
    void expenseTest(String submitter, float amount, String purpose) {
    	Map<String, Number> shares = transactor.sharesFor(participants, amount);
    	int entry = transactor.performExpense(submitter, amount, null, purpose, shares);
    	Cursor cursor = transactor.fetchEntry(entry);
    	assertNotNull(cursor);
    	assertEquals(cursor.getCount(), 1 + shares.keySet().size());
    	for (String name : shares.keySet())
    		if (!name.equals(submitter)) {
				float share = shares.get(name).floatValue();
    			do {
    				if (name.equals(cursor.getString(cursor.getColumnIndex("name")))) {
    					assertZero("sharing violated", share + cursor.getFloat(cursor.getColumnIndex("amount")));
    				}
    			} while (cursor.moveToNext());
    			cursor.moveToFirst();
    		}
    	cursor.close();
    }
    
    float[] balances = new float[] {43.33f, 13.33f, -56.67f};
    
    public void testExpenses() {
    	testClear();
    	expenseTest("Bob", 100f, "gas");
    	expenseTest("Sue", 70f, "groceries");
    	
    	Cursor cursor = transactor.queryBalances();
    	assertEquals(balances.length, cursor.getCount());
    	cursor.moveToFirst();
    	int i = 0;
		do {
			assertAmountEquals(cursor.getString(0) + "'s balance wrong", balances[i], cursor.getFloat(1));
			i++;
		} while (cursor.moveToNext());
    	cursor.close();
    	
		assertAmountEquals("total of expenses wrong", 170f, transactor.totalExpenses());
 	}
    
    void transferTest(String submitter, String receiver) throws Exception {
    	float amount = 312.50f;
    	int entryId = transactor.performTransfer(submitter, amount, null, "ring transfer", receiver);
    	assertSize(2, entryId);
    	assertZero("transfer leaking", transactor.getSum("entry=" + entryId));
 	}
    
    public void testTransfer() throws Exception {
    	Set<String> names = transactor.getNames();
    	Iterator<String> it = names.iterator();
    	String submitter = it.next();
    	while (it.hasNext()) {
	    	String receiver = it.next();
	    	transferTest(submitter, receiver);
	    	submitter = receiver;
    	}
    	transferTest(submitter, names.iterator().next());
    }
    
}
