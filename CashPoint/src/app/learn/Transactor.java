package app.learn;

import java.io.*;
import java.util.*;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

public class Transactor extends DbAdapter
{
	private static final String TAG = "Transactor";
	
	public Transactor(Context context) {
		super(context);
	}

	public int performInput(String submitter, float amount, String currency, String comment) {
    	return addEntry(submitter, amount, currency, comment);
	}

	public int performPayout(String receiver, float amount, String currency, String comment) {
    	return addEntry(receiver, -amount, currency, comment);
	}

	public int performTransfer(String submitter, float amount, String currency, String comment, String receiver) {
		int entryId = addEntry(submitter, -amount, currency, comment);
		if (entryId < 0)
			return -1;
		
		if (addRecord(entryId, receiver, amount, currency, null, comment) < 0)
			return -2;

		return entryId;
	}

	public int performExpense(String submitter, float amount, String currency, String comment, Map<String, Number> shares) {
    	int entryId = addEntry(submitter, amount, currency, comment);
    	if (doDistribution(entryId, shares)) 
    		return entryId;
    	else {
    		removeEntry(entryId);
    		return entryId;
    	}
	}
    
    public Map<String, Number> sharesFor(String[] names, float amount, Float... shares) {
    	int n = names.length;
    	TreeMap<String, Number> map = new TreeMap<String, Number>();
        for (int i = 0; i < n; i++) {
        	map.put(names[i], i < shares.length ? shares[i] : amount / n);
        }
    	return map;
    }
    
    public Cursor queryBalances() {
    	return doQuery("select name, sum(amount) as balance from " + DATABASE_TABLE + " group by name order by name", null);
    }
    
    public float totalExpenses() {
    	Cursor cursor = doQuery("select sum(amount) as expenses from " + DATABASE_TABLE + " where timestamp not null", null);
    	cursor.moveToFirst();
    	float total = cursor.getFloat(0);
    	cursor.close();
    	return total;
    }
}
