package app.learn.test;

import java.util.Date;

import app.learn.*;

import android.test.*;
import android.widget.TextView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class CashPointTest extends ActivityInstrumentationTestCase2<CashPoint> {		//	AndroidTestCase
	public CashPointTest() {
		super("app.learn", CashPoint.class);
	}
	
    private CashPoint mActivity;
    private TextView mTextView;
    private String resourceString;
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();
    }
    
    public void testPreconditions() {
        mTextView = (TextView) mActivity.findViewById(app.learn.R.id.nameLabel);
    	assertNotNull(mTextView);
    }
    
    public void testText() {
        resourceString = mActivity.getString(app.learn.R.string.nameLabelText);
    	assertEquals(resourceString,(String)mTextView.getText());
    }
    
    public void testDbInOut() {
    	DbAdapter dbAdapter = new DbAdapter(mActivity);
    	
    	int entry = dbAdapter.createEntry("Bob", 100.5, null, "gas");
    	assertEquals(dbAdapter.getNewEntryId() - 1, entry);
    	
    	Cursor outputCursor = dbAdapter.fetchEntry(entry);    	
		assertEquals("Bob", outputCursor.getString(1));
		assertEquals("" + 100.5, outputCursor.getString(2));
		assertEquals(null, outputCursor.getString(3));
		String now = dbAdapter.getDateFormat(new Date());
		assertEquals(now.substring(0, 16), 
				outputCursor.getString(4).substring(0, 16));
		assertEquals("gas", outputCursor.getString(5));
		outputCursor.close();
    	
		dbAdapter.close();
    	
    }
    
}
