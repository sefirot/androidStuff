package app.learn.test;

import java.io.*;
import java.util.*;

import junit.framework.Test;
import junit.framework.TestSuite;

import android.os.Environment;
import android.test.*;
import android.test.suitebuilder.TestSuiteBuilder;

import android.database.Cursor;
import android.util.Log;

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
    
    float epsilon = 0.001f;
    
    public void assertAmountZero(String message, Number number) {
    	assertFalse(message, Math.abs(number.floatValue()) > epsilon);
    }
    
    float minAmount = 0.01f;
    
    public void assertAmountEquals(String message, Number expected, Number actual) {
    	String sexp = String.format("%.2f", expected.floatValue());
    	String sact = String.format("%.2f", actual.floatValue());
    	assertEquals(message, sexp, sact);
    }
    
    String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
    public void testEntry() {
    	int entryId = dbAdapter.addEntry("Bob", 100.5f, null, "gas");
    	assertEquals(dbAdapter.getNewEntryId() - 1, entryId);
    	
    	Cursor cursor = dbAdapter.fetchEntry(entryId);    	
    	assertNotNull(cursor);
		assertEquals("Bob", cursor.getString(1));
		assertEquals(100.5f, cursor.getFloat(2));
		assertEquals(null, cursor.getString(3));
		String now = dbAdapter.getTimestamp(new Date());
		assertEquals(now.substring(0, 16), 
				cursor.getString(4).substring(0, 16));
		assertEquals("gas", cursor.getString(5));
    	assertFalse(cursor.getInt(6) > 0);
		cursor.close();
		
		assertEquals(1, dbAdapter.removeEntry(entryId));
		assertEquals(0, dbAdapter.fetchEntry(entryId).getCount());
	}

    void assertEntrySize(int expected, int entry) {
    	Cursor cursor = dbAdapter.fetchEntry(entry);
    	assertNotNull(cursor);
    	assertEquals(expected, cursor.getCount());
    	cursor.close();
    }
    
    public void testApportionment() {
    	float amount = 100.2f;
    	int entryId = dbAdapter.addEntry("Bob", amount, null, "gas");
    	
    	HashMap<String, Number> shares = new HashMap<String, Number>();
    	shares.put("Sue", amount / 4);
    	shares.put("Tom", amount / 3);
    	assertTrue(dbAdapter.apportionment(entryId, shares));
    	
    	assertEntrySize(4, entryId);
		assertAmountZero("apportionment leaking.", dbAdapter.getSum("entry=" + entryId));
		
		assertNotNull("timestamp missing", dbAdapter.fetchTimestamp(entryId));
		
    	Cursor cursor = dbAdapter.fetchEntry(entryId);
    	assertNotNull(cursor);
    	do {
        	assertTrue(dbAdapter.isExpense(cursor.getLong(cursor.getColumnIndex("ROWID"))));
		} while (cursor.moveToNext());
    	cursor.close();
    	
		Set<String> names = dbAdapter.getNames();
		assertTrue("some name missing.", names.containsAll(Arrays.asList(participants)));
		
		assertEquals(4, dbAdapter.removeEntry(entryId));
		assertEntrySize(0, entryId);
    }
    
    public void test_Save_Load() {
    	File dbDir = new File(Environment.getDataDirectory(), "data/app.learn/databases");
    	assertTrue(new File(dbDir, DbAdapter.DATABASE_NAME).exists());
    	
    	int count = transactor.getCount(null);
    	float sum = transactor.getSum(null);
    	
    	String newTableName = DbAdapter.DATABASE_TABLE + "_test";
    	transactor.drop(newTableName);
    	
    	assertTrue(transactor.saveAs("test"));
    	
    	assertTrue(transactor.savedTables().contains(newTableName));
    	transactor.clear();
		assertEquals(0, transactor.getCount(null));
		
		assertTrue(transactor.loadFrom("test"));
		
		assertEquals(count, transactor.getCount(null));
		assertEquals(sum, transactor.getSum(null));
    }
    
    void transferTest(String submitter, float amount, String purpose, String recipient) {
    	int entryId = transactor.performTransfer(submitter, amount, purpose, recipient);
    	assertEntrySize(2, entryId);
    	assertAmountZero("transfer leaking", transactor.getSum("entry=" + entryId));
 	}
    
    public void testRingTransfer() {
    	float someAmount = 312.54f;
    	String purpose = "ring transfer";
    	
    	Set<String> names = transactor.getNames();
    	Iterator<String> it = names.iterator();
    	String submitter = it.next();
    	while (it.hasNext()) {
	    	String recipient = it.next();
	    	transferTest(submitter, someAmount, purpose, recipient);
	    	submitter = recipient;
    	}
    	transferTest(submitter, someAmount, purpose, names.iterator().next());
    	
    	Set<Integer> ids = transactor.getEntryIds(String.format("comment='%s'", purpose));
    	for (Integer id : ids) 
    		discardTest(id);
    }
    
    void discardTest(int entryId) {
    	int discarded = transactor.performDiscard(entryId);
    	assertTrue("nothing deleted.", discarded > 0);
    	assertEntrySize(0, entryId);
 	}
    
    void inputTest(String submitter, float amount, String purpose) {
    	float total = transactor.total();
    	int entry = transactor.performInput(submitter, amount, purpose);
    	Cursor cursor = transactor.fetchEntry(entry);
    	assertNotNull(cursor);
    	assertEquals(1, cursor.getCount());
    	cursor.close();
    	assertAmountEquals("wrong result after input.", total + amount, transactor.total());
    }
    
    void expenseTest(String submitter, float amount, String purpose) {
    	Map<String, Number> shares = transactor.sharesFor(participants, amount);
    	int entry = transactor.performExpense(submitter, amount, purpose, shares);
    	Cursor cursor = transactor.fetchEntry(entry);
    	assertNotNull(cursor);
    	assertEquals(1 + shares.size(), cursor.getCount());
    	for (String name : shares.keySet())
    		if (!name.equals(submitter)) {
				float share = shares.get(name).floatValue();
				
    			do {
    				if (name.equals(cursor.getString(cursor.getColumnIndex("name")))) {
    					assertAmountZero("apportionment violated.", share + cursor.getFloat(cursor.getColumnIndex("amount")));
    				}
    			} while (cursor.moveToNext());
    			
    			cursor.moveToFirst();
    		}
    	cursor.close();
		assertAmountZero("apportionment leaking.", transactor.getSum("entry=" + entry));
    }
    
    void payoutTest(String recipient, float amount, String purpose) {
    	float total = transactor.total();
    	int entry = transactor.performPayout(recipient, amount, purpose);
    	Cursor cursor = transactor.fetchEntry(entry);
    	assertNotNull(cursor);
    	assertEquals(1, cursor.getCount());
    	cursor.close();
    	assertAmountEquals("wrong result after input.", total - amount, transactor.total());
    }
    
    void sharingTest(float expenses) {
    	float costs = transactor.expenses();
		assertAmountEquals("costs are wrong.", expenses, costs);
    	Map<String, Number> shares = transactor.sharesFor(participants, costs);
    	for (Number share : shares.values()) 
			costs -= share.floatValue();
		assertAmountZero("sharing sucks.", costs);
    }
    
    void balanceTest(Float... values) {
    	Map<String, Number> balances = transactor.balances();
    	
    	int i = 0;
    	for (Map.Entry<String, Number> ey : balances.entrySet()) {
    		if (values.length > i && values[i] != null)
    			assertAmountEquals(ey.getKey() + "'s balance wrong.", values[i], ey.getValue());
    			
    		i++;
		}
    }
    
    void totalTest(float... total) {
    	if (total.length > 0)
    		assertAmountEquals("total is unexpected.", total[0], transactor.total());
    	else
    		assertAmountZero("total is wrong.", transactor.total());
    }
    
    void compensationTest() {
    	for (Map.Entry<String, Number> ey : transactor.balances().entrySet()) {
    		String participant = ey.getKey();
    		float amount = ey.getValue().floatValue();
    		
    		if (amount > minAmount)
		    	payoutTest(participant, amount, "reimbursement");
    		else if (amount < -minAmount)
		    	inputTest(participant, -amount, "pay down");
    		
    		float balance = transactor.getSum(String.format("name='%s'", participant));
			assertAmountZero("balance is wrong.", balance);
    	}
    }
    
    public void testScenario() {
    	transactor.clear();
    	
    	//	cost sharing on a trip
    	inputTest("Tom", 50f, "stake");
    	expenseTest("Bob", 100f, "gas");
    	expenseTest("Sue", 70f, "groceries");
    	
    	//	Kassensturz !
    	totalTest(50f);
		//	total of expenses
		sharingTest(170f);
		
    	balanceTest(43.33f, 13.33f, -6.67f);
    	//	Tom transfers some money to Sue to improve his balance
    	transferTest("Tom", 10f, "better balance", "Sue");
    	balanceTest(43.33f, 3.33f, 3.33f);
   	
    	compensationTest();
    	
    	totalTest();
 	}
    
}
