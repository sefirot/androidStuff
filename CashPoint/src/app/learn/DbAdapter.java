package app.learn;

import java.util.Date;
import java.text.SimpleDateFormat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter extends SQLiteOpenHelper {

	private static final String TAG = "DbAdapter";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "cashpoint";
    private static final int DATABASE_VERSION = 2;
	
	public DbAdapter(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mDb = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table " + DATABASE_TABLE + " (" +
			" entry integer not null," +	//	unique for the inputs, reference for the distributions
			" name text not null," +		//	the person involved
			" amount real not null," +		//	the amount of money, negative for distributions
			" currency text," +				//	if null it's the default currency (Euro or Dollar or ...)
			" date numeric," +				//	if null it's a distribution
			" comment text" +				//	optional, for recognition
			" );"
		);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS cashpoint");
        onCreate(db);
		
	}
	
    private SQLiteDatabase mDb;
    
    public int getEntryId() {
        Cursor cursor = mDb.rawQuery("select max(entry) from " + DATABASE_TABLE, null);
        int retval = 0;
        if (cursor.getCount() > 0) {
        	cursor.moveToFirst();
        	retval = cursor.getInt(0);
        }
        cursor.close();
        return ++retval;
    }
    
    public void refreshCashpoint() {
    	mDb.execSQL("DROP TABLE IF EXISTS cashpoint");
    	onCreate(mDb);
    }
    
    public int createEntry(String name, double amount, String currency, String comment) {
    	int entryId = getEntryId();
    	
        ContentValues values = new ContentValues();
        values.put("entry", "" + entryId);
        values.put("name", name);
        values.put("amount", amount);
        values.put("currency", currency);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        Date date = new Date();
        values.put("date", dateFormat.format(date));
        values.put("comment", comment);
        long rowId = mDb.insert(DATABASE_TABLE, null, values);
        if (rowId < 0)
        	return -1;
        else {
        	return entryId;
        }
    }
}