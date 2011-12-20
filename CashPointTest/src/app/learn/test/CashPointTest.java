package app.learn.test;

import app.learn.*;

import android.test.*;
import android.widget.TextView;
import android.database.Cursor;
import android.database.sqlite.*;
import junit.framework.Assert;

public class CashPointTest extends ActivityInstrumentationTestCase2<CashPoint> {		//	AndroidTestCase
	public CashPointTest() {
		super("app.learn", CashPoint.class);
	}
	
    private CashPoint mActivity;
    private TextView mView;
    private String resourceString;
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();
        mView = (TextView) mActivity.findViewById(app.learn.R.id.textview);
        resourceString = mActivity.getString(app.learn.R.string.hello);
    }
    
    public void testPreconditions() {
    	assertNotNull(mView);
    }
    
    public void testText() {
    	assertEquals(resourceString,(String)mView.getText());
    }
    
    public void testDbInOut() {
    	
    	DbAdapter dbAdapter = new DbAdapter(mActivity);
//    	int entry = dbAdapter.createEntry("Bob", 100.0, null, "gas");
//    	assertTrue(0 < entry);
    	
//    	db.execSQL("insert into cashpoint (entry,name,amount,date,comment) values (1,'Bob',100.0,'12/24/11','gas');");
//    	
//    	Cursor outputCursor = db.query("cashpoint", new String[]{"name"}, "where entry = 1", null, null, null, null);    	
//		assertEquals("Bob", outputCursor.getString(0));
		dbAdapter.close();
    	
    }
    
	private NotesDbAdapter mDbHelper;
    
    public void testNotesDbInOut() {
        mDbHelper = new NotesDbAdapter(mActivity);
        mDbHelper.open();
        
        mDbHelper.createNote("Note1", "bla bla");
        Cursor c = mDbHelper.fetchAllNotes();
        assertTrue(c.getCount() > 0);
        
        mDbHelper.close();
    }
}