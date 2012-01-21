package app.learn;

import java.util.*;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

public class Transactor extends DbAdapter
{
	public static final String TAG = "Transactor";
	
	public Transactor(Context context) {
		super(context);
	}
	
	private String currency = "";
	
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
	/**
	 * The transaction registers the amount as an expense and splits it according to a share map (apportionment).
	 * @param submitter	the name of the participant who did the expense
	 * @param amount	the amount of the expense
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @param shares	a <code>Map</code> containing the shares of participants involved
	 * @return	the unique entry id common to the records of the transaction
	 */
	public int performExpense(String submitter, float amount, String comment, Map<String, Number> shares) {
    	int entryId = addEntry(submitter, amount, currency, comment);
    	if (apportionment(entryId, shares)) {
    		Log.i(TAG, String.format("entry %d: %s expended %f %s for '%s' shared by %s", entryId, submitter, amount, currency, comment, shares.toString()));
    		return entryId;
    	}
    	else {
    		removeEntry(entryId);
    		return -1;
    	}
	}
	/**
	 * The transaction registers the amount as a contribution.
	 * @param submitter	the name of the participant who did the contribution
	 * @param amount	the amount of the contribution
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @return	the unique entry id common to the records of the transaction
	 */
	public int performInput(String submitter, float amount, String comment) {
		int entryId = addEntry(submitter, amount, currency, comment);
		Log.i(TAG, String.format("entry %d: %s contributed %f %s as '%s'", entryId, submitter, amount, currency, comment));
		return entryId;
	}
	/**
	 * The transaction registers the amount as a disbursement.
	 * @param recipient	the name of the participant who got the amount
	 * @param amount	the amount of the disbursement
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @return	the unique entry id common to the records of the transaction
	 */
	public int performPayout(String recipient, float amount, String comment) {
		int entryId = addEntry(recipient, -amount, currency, comment);
		Log.i(TAG, String.format("entry %d: %s received %f %s as '%s'", entryId, recipient, amount, currency, comment));
		return entryId;
	}
	/**
	 * The transaction performs a transfer of the amount from a submitter to a recipient.
	 * This is the same as performing a contribution with the submitter and then a payout to the recipient.
	 * @param submitter	the name of the participant who lost the amount
	 * @param amount	the amount of the transfer
	 * @param comment	a <code>String</code> to make the transaction recognizable
	 * @param recipient	the name of the participant who got the amount
	 * @return	the unique entry id common to the records of the transaction
	 */
	public int performTransfer(String submitter, float amount, String comment, String recipient) {
		int entryId = addEntry(submitter, amount, currency, comment);
		if (entryId < 0)
			return -1;
		
		if (addRecord(entryId, recipient, -amount, currency, null, comment) < 0)
			return -2;

		Log.i(TAG, String.format("entry %d: %s transferred %f %s as '%s' to %s", entryId, submitter, amount, currency, comment, recipient));
		return entryId;
	}
	/**
	 * The transaction discards any record with that entry id.
	 * @param entryId	the entry to be discarded
	 * @return	the number of affected records
	 */
    public int performDiscard(int entryId) {
		int affected = removeEntry(entryId);
		Log.i(TAG, String.format("entry %d: discarded, %d records deleted", entryId, affected));
		return affected;
    }
	/**
	 * creates a map of shares for a number of 'sharers' optionally allowing for fixed portions of each or all of the sharers.
	 * If no portion is given for an individual sharer an equally shared portion is assumed (amount divided by the number of sharers)
	 * @param names	the <code>String</code> array of names of the sharers
	 * @param amount	the <code>float</code> value of the amount to share
	 * @param portions	given portions, if any, for individual sharers according to the sorted (!) order of the names
	 * @return	a map containing the names as keys and accordingly the shares as values
	 */
    public Map<String, Number> sharesFor(String[] names, float amount, Float... portions) {
    	TreeMap<String, Number> map = new TreeMap<String, Number>();
    	
    	int n = names.length;
        for (int i = 0; i < n; i++) 
        	map.put(names[i], 
        			i < portions.length && portions[i] != null ? 
        					portions[i] : 
        					amount / n);
        
    	return map;
    }
    /**
     * calculates the balances of all participants identified by their names
	 * @return	a map containing the names as keys and the balances as values
     */
    public Map<String, Number> balances() {
    	TreeMap<String, Number> map = new TreeMap<String, Number>();
    	
    	Cursor cursor = doQuery("select name, sum(amount) as balance from " + DATABASE_TABLE + " group by name order by name", null);
		do {
			map.put(cursor.getString(0), cursor.getFloat(1));
		} while (cursor.moveToNext());
		if (cursor != null)
        	cursor.close();
        
    	Log.i(TAG, String.format("balances : %s", map.toString()));
    	return map;
    }
    /**
     * calculates the amounted 'value' of the table
     * @return	the sum over the amounts of all records in the table
     */
    public float total() {
    	return getSum(null);
    }
    /**
     * calculates the accumulated costs
     * @return	the sum over all entries marked as 'expense'
     */
    public float expenses() {
    	return getSum("expense > 0 and timestamp not null");
    }
    /**
     * retrieves the names of tables that had been 'saved' in the past
     * @return	the <code>Set</code> of saved table names
     */
    public Set<String> savedTables() {
    	TreeSet<String> names = new TreeSet<String>();
    	
        Cursor cursor = doQuery("select name from sqlite_master where type = 'table'", null);
        if (cursorSize > 0) {
        	do {
        		String name = cursor.getString(0);
        		if (name.startsWith(DATABASE_TABLE + "_"))
        			names.add(name);
           	} while (cursor.moveToNext());
        }
        if (cursor != null)
        	cursor.close();
        
    	return names;
    }
    /**
     * changes the name of the table that has been worked on via transactions (current table). 
     * Thus this table is 'saved' in the same database. Note that the current table is non-existing after this operation. 
     * In order to continue the current table has to be restored (loadFrom) or recreated (clear).
     * @param newSuffix	the <code>String</code> to append to the name of the current table in order to form the new table name 
     * @return	success if true
     */
    public boolean saveAs(String newSuffix) {
    	if (newSuffix == null || newSuffix.length() < 1)
    		return false;
    	
    	String newTableName = DATABASE_TABLE + "_" + newSuffix;
    	if (savedTables().contains(newTableName))
    		return false;
    		
    	rename(DATABASE_TABLE, newTableName);
		Log.i(TAG, String.format("table saved as '%s'", newTableName));
    	return true;
    }
    /**
     * restores one of the saved tables as the current table. Note that the table that was current up to this point will be dropped.
     * Also there will be one less 'saved' table after this operation.
     * @param oldSuffix	the <code>String</code> to append to the name of the current table in order to form the old table name 
     * @return	success if true
     */
    public boolean loadFrom(String oldSuffix) {
    	if (oldSuffix == null || oldSuffix.length() < 1)
    		return false;
    	
    	String oldTableName = DATABASE_TABLE + "_" + oldSuffix;
    	if (!savedTables().contains(oldTableName))
    		return false;
    		
    	rename(oldTableName, DATABASE_TABLE);
		Log.i(TAG, String.format("table loaded from '%s'", oldTableName));
    	return true;
    }
    /**
     * deletes all records from the current table and recreates it
     */
    @Override
    public void clear() {
    	int count = getCount(null);
    	super.clear();
		Log.i(TAG, String.format("table cleared, %d records deleted", count));
    }
    /**
     * clears the current table and drops all saved tables
     */
    public void clearAll() {
    	for (String table : savedTables())
    		drop(table);
    	
    	clear();
    }
    
}
