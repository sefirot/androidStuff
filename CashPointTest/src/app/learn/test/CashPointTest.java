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
    	assertEquals(message, 0, actual.doubleValue(), Util.delta);
    }
    
    String formatAmount(double value) {
    	return String.format("%.2f", value);
    }
    
    void assertAmountEquals(String message, Number expected, Number actual) {
    	String exp = formatAmount(expected.doubleValue());
    	String act = formatAmount(actual.doubleValue());
    	assertEquals(message, exp, act);
    }
   
    <T> void assertArrayEquals(String message, T[] expected, T[] actual) {
    	assertEquals(message, expected.length, actual.length);
    	for (int i = 0; i < actual.length; i++) 
        	assertEquals(message, expected[i], actual[i]);
    }

    void assertEntrySize(final int expected, int entry) {
    	assertTrue("invalid entry", entry > -1);
    	
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
    	int entryId = dbAdapter.addEntry("Bob", 100.5, null, "gas", true);
    	assertEquals(dbAdapter.getNewEntryId() - 1, entryId);
    	
    	dbAdapter.fetchEntry(entryId, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
					assertEquals("Bob", cursor.getString(1));
					assertEquals(100.5, cursor.getDouble(2));
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
    	ShareMap map = new ShareMap(participants, new Double[] {200.,null,300.});
    	assertEquals(2, map.size());
    	assertEquals(200., map.get(participants[0]));
    	assertEquals(300., map.get(participants[2]));
    	
    	map = new ShareMap(participants, 600.);
    	assertEquals(3, map.size());
    	for (int i = 0; i < 3; i++) 
    		assertEquals(-200., map.get(participants[i]));
    	
    	map = new ShareMap(participants, 600., null, 300.);
    	assertEquals(3, map.size());
    	assertEquals(-100., map.get(participants[0]));
    	assertEquals(-300., map.get(participants[1]));
    	assertEquals(-200., map.get(participants[2]));
    	
    	assertEquals(-600., map.sum());
    	assertEquals(600., map.negated().sum());
    	
    	map = new ShareMap(new String[] {participants[0], "", participants[2]}, 600.);
    	assertEquals("", map.firstKey());
    	
    	map = new ShareMap(participants, 120., new int[] {1,1,1});
    	assertEquals(3, map.size());
    	for (int i = 0; i < 3; i++) 
    		assertEquals(-40., map.get(participants[i]));
    	
    	map = new ShareMap(participants, 120., new int[] {0,1,2});
    	assertEquals(2, map.size());
    	assertEquals(-40., map.get(participants[1]));
    	assertEquals(-80., map.get(participants[2]));
    }
    
    void zeroSumTest(int entry) {
		double sum = dbAdapter.getSum("entry=" + entry + " and expense > 0");
		assertAmountZero("allocation leaking.", sum);
    }
    
    int allocationTest(final boolean expense) {
    	final int sign = expense ? 1 : -1;
    	final double allocation = 120 * sign;
    	
    	int entryId = dbAdapter.addEntry(expense ? "Bob" : "", allocation, null, "test", expense);
    	
    	ShareMap portions = new ShareMap(
    			new String[] {"Bob","Sue","Tom"}, 
    			allocation, 
    			null, allocation / 4, allocation / 3);
    	
    	assertTrue(dbAdapter.allocate(expense, entryId, portions));
    	
    	assertEntrySize(4, entryId);
    	zeroSumTest(entryId);
		
		assertNotNull("timestamp missing", dbAdapter.fetchField(entryId, "timestamp"));
		
    	dbAdapter.fetchEntry(entryId, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	do {
			        	assertEquals(expense, dbAdapter.isExpense(cursor.getLong(cursor.getColumnIndex("ROWID"))));
			        	double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
			        	if (cursor.getString(cursor.getColumnIndex("timestamp")) != null)
			        		assertAmountEquals("allocated amount wrong", allocation, amount);
			        	else {
				        	String name = cursor.getString(cursor.getColumnIndex("name"));
							if (name.equals("Sue")) assertAmountEquals("Sue 's portion wrong", -30 * sign, amount);
							if (name.equals("Tom")) assertAmountEquals("Tom 's portion wrong", -40 * sign, amount);
							if (name.equals("Bob")) assertAmountEquals("Bob 's portion wrong", -50 * sign, amount);
						}
					} while (cursor.moveToNext());
					return defaultResult;
				}
			}, null);
		
    	return entryId;
    }
    
    public void testAllocation() {
    	Set<Integer> ids = new HashSet<Integer>();
    	
    	ids.add(allocationTest(true));
    	ids.add(allocationTest(false));
    	
    	for (Integer id : ids) 
    		if (id > -1) {
				dbAdapter.removeEntry(id);
				assertEntrySize(0, id);
	    	}
    	
    	assertTrue(-1 < transactor.performSubmission(participants[0], 99., "test1"));
    	assertEquals(-2, transactor.performExpense("", "test1", new ShareMap(participants, -100.)));
    	assertTrue(-1 < transactor.performExpense("", "test1", new ShareMap(participants, -99.)));
    	totalTest(99.);
    	
    	int id = transactor.performExpense(participants[2], "test2", new ShareMap(participants, 99., new int[] {1,0,2}));
    	assertEntrySize(3, id);
    	zeroSumTest(id);
    }
    
    public void testSaveLoad() {
    	File dbDir = new File(Environment.getDataDirectory(), "data/app.learn/databases");
    	assertTrue(new File(dbDir, DbAdapter.DATABASE_NAME).exists());
    	
    	int count = transactor.getCount(null);
    	double sum = transactor.getSum(null);
    	
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
    	double someAmount = 312.54;
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
    
    void transferTest(String submitter, double amount, String purpose, String recipient) {
    	int entryId = transactor.performTransfer(submitter, amount, purpose, recipient);
    	assertEntrySize(2, entryId);
    	assertAmountZero("transfer leaking", transactor.getSum("entry=" + entryId));
 	}
    
    void discardTest(int entryId) {
    	int discarded = transactor.performDiscard(entryId);
    	assertTrue("nothing deleted.", discarded > 0);
    	assertEntrySize(0, entryId);
 	}
    
    int entryTest(int entry, final int n, final ShareMap shares) {
    	assertTrue("invalid entry", entry > -1);
    	
    	transactor.fetchEntry(entry, 
    		new QueryEvaluator<Void>() {
				public Void evaluate(Cursor cursor, Void defaultResult, Object... params) {
			    	assertNotNull(cursor);
			    	if (shares == null) 
						assertEquals(n, cursor.getCount());
			    	else {
						assertEquals(n + shares.size(), cursor.getCount());
						for (String name : shares.keySet()) {
							double share = shares.get(name).doubleValue();

							do {
								if (cursor.getString(cursor.getColumnIndex("timestamp")) == null
										&& name.equals(cursor.getString(cursor.getColumnIndex("name")))) {
									double amount = cursor.getDouble(cursor
											.getColumnIndex("amount"));
									assertAmountEquals("allocation violated.",
											share, amount);
								}
							} while (cursor.moveToNext());

							cursor.moveToFirst();
						}
					}
					return defaultResult;
				}
			}, null);
    	
    	if (shares != null) 
    		zeroSumTest(entry);
    	
		return entry;
    }
    
    int submissionTest(String submitter, double amount, String purpose) {
    	double total = transactor.total();
    	int entry = transactor.performSubmission(submitter, amount, purpose);
    	assertFalse("transaction blocked because of negative total", entry == -2);
    	
    	entryTest(entry, 1, null);
    	
    	assertAmountEquals("wrong result after input.", total + amount, transactor.total());
    	
    	return entry;
    }
    
    int payoutTest(String[] recipients, double amount, String purpose, Double... portions) {
    	double total = transactor.total();
		ShareMap shares = new ShareMap(recipients, amount, portions);
    	int entry = transactor.performMultiple(shares.negated(), purpose);
    	assertFalse("transaction blocked because of negative total", entry == -2);
    	
    	entryTest(entry, 0, shares);
    	
    	assertAmountEquals("wrong result after input.", total + shares.sum(), transactor.total());
    	
    	return entry;
    }
    
    int simpleExpenseTest(final String submitter, double amount, String purpose, Double... portions) {
		ShareMap shares = new ShareMap(participants, amount, portions);
    	int entry = transactor.performExpense(submitter, purpose, shares);
    	return entryTest(entry, 1, shares);
    }
    
    int complexExpenseTest(String[] names, Double[] amounts, String purpose, Double... portions) {
		ShareMap deals = new ShareMap(names, amounts);
		ShareMap shares = new ShareMap(participants, deals.sum(), portions);
    	int entry = transactor.performComplexExpense(deals, purpose, shares);
    	return entryTest(entry, deals.size() + (deals.containsKey("") ? 1 : 0), shares);
    }
    
    void compensationTest(Double... values) {
    	ShareMap compensations = balanceTest(values).negated();
    	int entry = transactor.performMultiple(compensations, "compensation");
    	if (values.length < 1)
    		return;
    	
    	assertFalse("transaction blocked because of negative total", entry == -2);
    	
    	entryTest(entry, 0, compensations);
   	
     	for (Map.Entry<String, Number> ey : transactor.balances().entrySet()) {
    		double balance = ey.getValue().doubleValue();
			assertAmountZero(String.format("%s 's balance is wrong : %f", ey.getKey(), balance), balance);
    	}
    }
    
    void sharingTest(double expenses) {
    	double costs = transactor.expenses();
		assertAmountEquals("costs are wrong.", expenses, costs);
		ShareMap shares = new ShareMap(participants, costs);
		assertAmountEquals("sharing sucks.", costs, shares.negated().sum());
    }
    
    ShareMap balanceTest(Double... values) {
    	ShareMap balances = transactor.balances();
    	
    	int i = 0;
    	for (Map.Entry<String, Number> ey : balances.entrySet()) {
    		if (Util.isAvailable(i, values))
    			assertAmountEquals(ey.getKey() + "'s balance wrong.", values[i], ey.getValue());
    		i++;
    	}
    	
    	return balances;
    }
    
    void totalTest(double... expected) {
    	double total = transactor.total();
    	if (expected.length > 0)
    		assertAmountEquals("unexpected total : " + formatAmount(total), expected[0], total);
    	else
    		assertAmountZero("total is wrong.", total);
    }
    
    public void test_Scenario1() {
    	//	cost sharing on a trip
    	submissionTest("Tom", 50, "stake");
    	simpleExpenseTest("Bob", 100, "gas");
    	simpleExpenseTest("Sue", 70, "groceries");
    	
    	//	Kassensturz !
    	totalTest(50);
		//	total of expenses
		sharingTest(170);
		
    	balanceTest(43.33, 13.33, -6.67);
    	//	Tom transfers some money to Sue to improve his balance
    	transferTest("Tom", 10, "better balance", "Sue");
   	
    	compensationTest(43.33, 3.33, 3.33);
    	
    	totalTest();
    	
    	saveTest("Scenario1");
 	}
    
    public void test_Scenario2() {
    	//	cost sharing on a trip
    	submissionTest("Tom", 60, "stake");
    	//	Bob pays 100 for gas and uses 50 from the kitty
    	complexExpenseTest(new String[] {"","Bob"}, new Double[] {50.,50.}, "gas");
    	simpleExpenseTest("Sue", 70, "groceries");
    	
    	//	Kassensturz !
    	totalTest(10);
		//	total of expenses
		sharingTest(170);
   	
    	compensationTest(-6.67, 13.33, 3.33);
    	
    	totalTest();
    	
    	saveTest("Scenario2");
 	}
    
}
