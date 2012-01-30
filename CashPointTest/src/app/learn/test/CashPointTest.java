package app.learn.test;

import java.io.*;
import java.util.*;

import junit.framework.*;

import android.os.Environment;
import android.test.*;
import android.test.suitebuilder.TestSuiteBuilder;

import android.database.Cursor;

import app.learn.*;
import app.learn.DbAdapter.QueryEvaluator;

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
        
    	dbAdapter.clear();
    }

    @Override
    protected void tearDown() throws Exception {
		dbAdapter.close();
        super.tearDown();
	}
    
    void assertAmountZero(String message, Number actual) {
    	assertEquals(message, 0f, actual.floatValue(), transactor.delta);
    }
    
    String formatAmount(float value) {
    	return String.format("%.2f", value);
    }
    
    void assertAmountEquals(String message, Number expected, Number actual) {
    	assertEquals(message, 
    			formatAmount(expected.floatValue()), 
    			formatAmount(actual.floatValue()));
    }
   
    <T> void assertArrayEquals(String message, T[] expected, T[] actual) {
    	assertEquals(message, expected.length, actual.length);
    	for (int i = 0; i < actual.length; i++) 
        	assertEquals(message, expected[i], actual[i]);
    }

    void assertEntrySize(final int expected, int entry) {
    	dbAdapter.fetchEntry(entry, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertEquals(expected, cursor.getCount());
					return defaultResult;
				}
			}, null);
    }
    
    public void testEntry() {
    	int entryId = dbAdapter.addEntry("Bob", 100.5f, null, "gas", true);
    	assertEquals(dbAdapter.getNewEntryId() - 1, entryId);
    	
    	dbAdapter.fetchEntry(entryId, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
					assertEquals("Bob", cursor.getString(1));
					assertEquals(100.5f, cursor.getFloat(2));
					assertEquals(null, cursor.getString(3));
					String now = dbAdapter.timestampNow();
					assertEquals(now.substring(0, 16), 
							cursor.getString(4).substring(0, 16));
					assertEquals("gas", cursor.getString(5));
			    	assertTrue(cursor.getInt(6) > 0);
					return defaultResult;
				}
			}, null);    	
		
		assertEquals(1, dbAdapter.removeEntry(entryId));
		assertEquals(0, dbAdapter.getCount("entry=" + entryId));
	}
    
    String[] participants = new String[] {"Sue", "Bob", "Tom"};
    
    public void testShareMap() {
    	Transactor.ShareMap map = new Transactor.ShareMap(participants, new Float[] {200f,null,300f});
    	assertEquals(2, map.size());
    	assertEquals(200f, map.get(participants[0]));
    	assertEquals(300f, map.get(participants[2]));
    	
    	map = new Transactor.ShareMap(participants, 600f);
    	assertEquals(3, map.size());
    	for (int i = 0; i < 3; i++) 
    		assertEquals(-200f, map.get(participants[i]));
    	
    	map = new Transactor.ShareMap(participants, 600f, null, 300f);
    	assertEquals(3, map.size());
    	assertEquals(-100f, map.get(participants[0]));
    	assertEquals(-300f, map.get(participants[1]));
    	assertEquals(-200f, map.get(participants[2]));
    	
    	assertEquals(-600f, map.sum());
    	assertEquals(600f, map.negated().sum());
    	
    	participants[1] = "";
    	map = new Transactor.ShareMap(participants, 600f);
    	assertEquals("", map.firstKey());
    }
    
    int allocateTest(final boolean expense) {
    	final int sign = expense ? 1 : -1;
    	final float allocation = 120f * sign;
    	
    	int entryId = dbAdapter.addEntry(expense ? "Bob" : "", allocation, null, "test", expense);
    	
    	Transactor.ShareMap portions = new Transactor.ShareMap(
    			new String[] {"Bob","Sue","Tom"}, 
    			allocation, 
    			null, allocation / 4, allocation / 3);
    	
    	assertTrue(dbAdapter.allocate(expense, entryId, portions));
    	
    	assertEntrySize(4, entryId);
		assertAmountZero("allocation leaking.", dbAdapter.getSum("entry=" + entryId));
		
		assertNotNull("timestamp missing", dbAdapter.fetchField(entryId, "timestamp"));
		
    	dbAdapter.fetchEntry(entryId, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	do {
			        	assertEquals(expense, dbAdapter.isExpense(cursor.getLong(cursor.getColumnIndex("ROWID"))));
			        	float amount = cursor.getFloat(cursor.getColumnIndex("amount"));
			        	if (cursor.getString(cursor.getColumnIndex("timestamp")) != null)
			        		assertAmountEquals("allocated amount wrong", allocation, amount);
			        	else {
				        	String name = cursor.getString(cursor.getColumnIndex("name"));
							if (name.equals("Sue")) assertAmountEquals("Sue 's portion wrong", -30f * sign, amount);
							if (name.equals("Tom")) assertAmountEquals("Tom 's portion wrong", -40f * sign, amount);
							if (name.equals("Bob")) assertAmountEquals("Bob 's portion wrong", -50f * sign, amount);
						}
					} while (cursor.moveToNext());
					return defaultResult;
				}
			}, null);
		
    	return entryId;
    }
    
    public void testAllocation() {
    	Set<Integer> ids = new HashSet<Integer>();
    	
    	ids.add(allocateTest(true));
    	ids.add(allocateTest(false));
    	
    	ids.add(submissionTest("Sue", 99f, "test1"));
    	assertEquals(-2, expenseTest("", 100f, "test1"));
    	ids.add(expenseTest("", 99f, "test1"));
    	ids.add(expenseTest("Sue", 100f, "test2"));
    	
    	for (Integer id : ids) 
    		if (id > -1) {
				dbAdapter.removeEntry(id);
				assertEntrySize(0, id);
	    	}
    }
    
    public void test_Save_Load() {
    	File dbDir = new File(Environment.getDataDirectory(), "data/app.learn/databases");
    	assertTrue(new File(dbDir, DbAdapter.DATABASE_NAME).exists());
    	
    	int count = transactor.getCount(null);
    	float sum = transactor.getSum(null);
    	
    	saveTest("test");
    	
    	transactor.clear();
		assertEquals(0, transactor.getCount(null));
		
		loadTest("test");
		
		assertEquals(count, transactor.getCount(null));
		assertEquals(sum, transactor.getSum(null));
 	}
    
    void saveTest(String suffix) {
    	String tableName = transactor.tableName(suffix);
    	transactor.drop(tableName);
    	assertTrue(transactor.saveAs(suffix));
    	assertTrue(transactor.savedTables().contains(tableName));
    }
    
    void loadTest(String suffix) {
    	String tableName = transactor.tableName(suffix);
    	assertTrue(transactor.savedTables().contains(tableName));
    	transactor.drop(DbAdapter.DATABASE_TABLE);
		assertTrue(transactor.loadFrom(suffix));
    }
    
    public void testRingTransfer() {
    	float someAmount = 312.54f;
    	String purpose = "ring transfer";
    	
    	List<String> names = Arrays.asList(participants);
    	Iterator<String> it = names.iterator();
    	String submitter = it.next();
    	while (it.hasNext()) {
	    	String recipient = it.next();
	    	transferTest(submitter, someAmount, purpose, recipient);
	    	submitter = recipient;
    	}
    	transferTest(submitter, someAmount, purpose, names.iterator().next());
    	
		Set<String> names2 = transactor.getNames();
		assertTrue("some name missing.", names2.containsAll(Arrays.asList(participants)));
    	
    	Set<Integer> ids = transactor.getEntryIds(String.format("comment='%s'", purpose));
    	for (Integer id : ids) 
    		discardTest(id);
    	
    	assertEquals("discard failed.", 0, transactor.getCount(null));
    }
    
    void transferTest(String submitter, float amount, String purpose, String recipient) {
    	int entryId = transactor.performTransfer(submitter, amount, purpose, recipient);
    	assertEntrySize(2, entryId);
    	assertAmountZero("transfer leaking", transactor.getSum("entry=" + entryId));
 	}
    
    void discardTest(int entryId) {
    	int discarded = transactor.performDiscard(entryId);
    	assertTrue("nothing deleted.", discarded > 0);
    	assertEntrySize(0, entryId);
 	}
    
    int submissionTest(String submitter, float amount, String purpose) {
    	float total = transactor.total();
    	int entry = transactor.performSubmission(submitter, amount, purpose);
    	dbAdapter.fetchEntry(entry, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertEquals(1, cursor.getCount());
					return defaultResult;
				}
			}, null);
    	assertAmountEquals("wrong result after input.", total + amount, transactor.total());
    	
    	return entry;
    }
    
    int cursorTest(int entry, final int n, final Transactor.ShareMap shares) {
    	transactor.fetchEntry(entry, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertEquals(n + shares.size(), cursor.getCount());
			    	for (String name : shares.keySet()) {
						float share = shares.get(name).floatValue();
						
		    			do {
		    				if (cursor.getString(cursor.getColumnIndex("timestamp")) == null && 
		    						name.equals(cursor.getString(cursor.getColumnIndex("name")))) {
		    					float amount = cursor.getFloat(cursor.getColumnIndex("amount"));
		    					assertAmountEquals("allocation violated.", share, amount);
		    				}
		    			} while (cursor.moveToNext());
		    			
		    			cursor.moveToFirst();
		    		}
					return defaultResult;
				}
			}, null);
    	
    	float sum = transactor.getSum("entry=" + entry + " and expense > 0");
    	assertAmountZero("allocation leaking.", sum);
   	
    	return entry;
    }
    
    int expenseTest(final String submitter, float amount, String purpose, Float... portions) {
		Transactor.ShareMap shares = new Transactor.ShareMap(participants, amount, portions);
    	int entry = transactor.performExpense(submitter, purpose, shares);
    	if (entry < 0)
    		return entry;
   	
    	return cursorTest(entry, 1, shares);
    }
    
    int complexExpenseTest(String[] names, Float[] amounts, String purpose, Float... portions) {
		Transactor.ShareMap deals = new Transactor.ShareMap(names, amounts);
		Transactor.ShareMap shares = new Transactor.ShareMap(participants, deals.sum(), portions);
    	int entry = transactor.performComplexExpense(deals, purpose, shares);
    	if (entry < 0)
    		return entry;

    	return cursorTest(entry, deals.size() + (deals.containsKey("") ? 1 : 0), shares);
    }
    
    void payoutTest(String[] recipients, float amount, String purpose, Float... portions) {
    	float total = transactor.total();
		Transactor.ShareMap shares = new Transactor.ShareMap(recipients, amount, portions);
    	int entry = transactor.performMultiple(shares.negated(), purpose);
    	dbAdapter.fetchEntry(entry, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertTrue(cursor.getCount() > 0);
					return defaultResult;
				}
			}, null);
    	assertAmountEquals("wrong result after input.", total + shares.sum(), transactor.total());
    }
    
    void sharingTest(float expenses) {
    	float costs = transactor.expenses();
		assertAmountEquals("costs are wrong.", expenses, costs);
		Transactor.ShareMap shares = new Transactor.ShareMap(participants, costs);
		assertAmountEquals("sharing sucks.", costs, shares.negated().sum());
    }
    
    Transactor.ShareMap balanceTest(Float... values) {
    	Transactor.ShareMap balances = transactor.balances();
    	
    	int i = 0;
    	for (Map.Entry<String, Number> ey : balances.entrySet()) {
    		if (DbAdapter.isAvailable(i, values))
    			assertAmountEquals(ey.getKey() + "'s balance wrong.", values[i], ey.getValue());
    		i++;
    	}
    	
    	return balances;
    }
    
    void compensationTest(Float... values) {
    	Transactor.ShareMap compensations = balanceTest(values).negated();
    	int entry = transactor.performMultiple(compensations, "compensation");
    	assertFalse("transaction blocked because of negative total", entry == -2);
    	dbAdapter.fetchEntry(entry, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	assertEquals(transactor.getNames().size(), cursor.getCount());
					return defaultResult;
				}
			}, null);
     	for (Map.Entry<String, Number> ey : transactor.balances().entrySet()) {
    		float balance = ey.getValue().floatValue();
			assertAmountZero(String.format("%s 's balance is wrong : %f", ey.getKey(), balance), balance);
    	}
    }
    
    void totalTest(float... expected) {
    	float total = transactor.total();
    	if (expected.length > 0)
    		assertAmountEquals("unexpected total : " + formatAmount(total), expected[0], total);
    	else
    		assertAmountZero("total is wrong.", total);
    }
    
    public void testScenario1() {
    	//	cost sharing on a trip
    	submissionTest("Tom", 50f, "stake");
    	expenseTest("Bob", 100f, "gas");
    	expenseTest("Sue", 70f, "groceries");
    	
    	//	Kassensturz !
    	totalTest(50f);
		//	total of expenses
		sharingTest(170f);
		
    	balanceTest(43.33f, 13.33f, -6.67f);
    	//	Tom transfers some money to Sue to improve his balance
    	transferTest("Tom", 10f, "better balance", "Sue");
   	
    	compensationTest(43.33f, 3.33f, 3.33f);
    	
    	totalTest();
    	
    	saveTest("Scenario1");
 	}
    
    public void testScenario2() {
    	//	cost sharing on a trip
    	submissionTest("Tom", 60f, "stake");
    	//	Bob pays 100f for gas and uses 50f from the kitty
    	complexExpenseTest(new String[] {"","Bob"}, new Float[] {50f,50f}, "gas");
    	expenseTest("Sue", 70f, "groceries");
    	
    	//	Kassensturz !
    	totalTest(10f);
		//	total of expenses
		sharingTest(170f);
   	
    	compensationTest(-6.67f, 13.33f, 3.33f);
    	
    	totalTest();
    	
    	saveTest("Scenario2");
 	}
    
}
