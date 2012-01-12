package app.learn.test;

import android.test.*;
import android.widget.*;

import app.learn.CashPoint;

public class ActivityTest extends ActivityInstrumentationTestCase2<CashPoint> {
	public ActivityTest() {
		super("app.learn", CashPoint.class);
	}
	
    private CashPoint mActivity;
    private TextView mTextView;
    private String resourceString;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = this.getActivity();
        mTextView = (TextView) mActivity.findViewById(app.learn.R.id.nameLabel);
    }
    
    public void testPreconditions() {
    	assertNotNull(mTextView);
    }
    
    public void testText() {
        resourceString = mActivity.getString(app.learn.R.string.nameLabelText);
    	assertEquals(resourceString,(String)mTextView.getText());
    }

}
