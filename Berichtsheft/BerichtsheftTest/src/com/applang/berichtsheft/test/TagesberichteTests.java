package com.applang.berichtsheft.test;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import static com.applang.Util.*;

import com.applang.tagesberichte.Glossary;
import com.applang.tagesberichte.Tagesberichte;

import com.jayway.android.robotium.solo.Condition;

public class TagesberichteTests extends InfraTests<Tagesberichte>
{
    private static final String TAG = GlossaryTests.class.getSimpleName();
    
	public TagesberichteTests() {
		super("com.applang.berichtsheft", Tagesberichte.class);
	}
	
/*	public TagesberichteTests(String method) {
		this();
	}
*/
	@Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(mActivity instanceof Tagesberichte);
        
        ProviderTests.generateTestData(mActivity);
        
//		mActivity.contentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mContentResolver = mActivity.getContentResolver();
		
		tabs = (ViewGroup) solo.getView(android.R.id.tabs);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}

    ContentResolver mContentResolver;
    ViewGroup tabs;
    
    public void testNeuerEintrag() {
    	solo.waitForActivity("NotesList");
    	assertTrue(0 < tabs.getChildCount());
    	solo.clickOnView(tabs.getChildAt(0));
    	
		solo.clickOnMenuItem("Neuer Eintrag");
		assertTrue(solo.waitForActivity("TitleEditor"));
		String title = "neuer";
		solo.enterText(0, title);
		solo.assertCurrentActivity("Expected TitleEditor activity", "TitleEditor"); 
		solo.clickOnButton("OK"); 
		solo.assertCurrentActivity("Expected NoteEditor activity", "NoteEditor"); 
		solo.enterText(0, "eintrag");
		solo.goBack(); 
		assertTrue(solo.searchText(title));
		assertFalse(solo.searchText("eintrag"));
		
		solo.clickOnText(title);
		solo.assertCurrentActivity("Expected NoteEditor activity", "NoteEditor"); 
		assertFalse(solo.searchText(title));
		assertTrue(solo.searchText("eintrag"));
		solo.goBack(); 
		
		solo.clickLongOnText(title);
		solo.clickOnText("Löschen");
		solo.clickOnButton(0); 
		solo.assertCurrentActivity("Expected Tagesberichte activity", "Tagesberichte"); 
		assertFalse(solo.waitForText(title, 1, 100));
    }
    
    public void testSchlagwort() {
    	solo.waitForActivity("NotesList");
		solo.waitForCondition(new Condition() {
			public boolean isSatisfied() {
				return 2 < tabs.getChildCount() && tabs.getChildAt(2) != null;
			}
    	}, 1000);
    	solo.clickOnView(tabs.getChildAt(2));
		String schlagwort = "neues";
		assertFalse(solo.searchText(schlagwort));
		
    	solo.clickOnView(tabs.getChildAt(0));
    	for (int i = 1; i <= 3; i++) {
    		solo.clickLongOnText("Velocity" + i);
    		solo.clickOnText("Schlagwort");
    		solo.waitForText("Schlagwort");
    		AutoCompleteTextView actv = (AutoCompleteTextView) solo.getView(com.applang.berichtsheft.R.id.title);
    		assertNotNull(actv);
    		actv.setThreshold(1 + schlagwort.length());
    		solo.typeText(0, schlagwort);
    		solo.clickOnButton("OK"); 
		}
    	
    	solo.clickOnView(tabs.getChildAt(2));
		solo.assertCurrentActivity("Expected Glossary activity", "Glossary"); 
		assertTrue(solo.searchText(schlagwort));
		solo.clickOnText(schlagwort);
		
    	for (int i = 3; i <= 1; i--) 
    		listItemContains(3+i, "Velocity" + i);
		Log.i(TAG, "Schlagwort checked");
    }

	public void listItemContains(int i, String text) {
		String s = "";
		ArrayList<TextView> tvs = solo.clickInList(i);
		for (TextView tv : tvs) {
			s += tv.getText();
		}
		assertTrue(s, s.contains(text));
    }
    
    public void testSchlagwort2() throws Throwable {
    	Glossary.setThreaded(getActivity(), false);
    	
    	solo.waitForActivity("NotesList");
		solo.waitForCondition(new Condition() {
			public boolean isSatisfied() {
				return 2 < tabs.getChildCount() && tabs.getChildAt(2) != null;
			}
    	}, 1000);
    	solo.clickOnView(tabs.getChildAt(2));
    	solo.waitForText("Fehler");
    	solo.assertCurrentActivity("Expected Glossary activity", "Glossary"); 
    	
//		final ExpandableListView lv = solo.getCurrentViews(ExpandableListView.class).get(0);
//		assertNotNull(lv);
//		runTestOnUiThread(new Runnable() {
//			public void run() {
//				lv.setSelection(2);
//				Log.i(TAG, "selectedId " + lv.getSelectedId());
//			}
//		});
		
		String schlagwort = "Kein";
		solo.clickOnText(schlagwort);
    	solo.waitForText("Velocity");
    	for (int i = 3; i > 0; i--) 
    		listItemContains(2+i, "Velocity" + i);
		Log.i(TAG, "Schlagwort checked");
    	
    	solo.clickLongInList(3);
    	solo.waitForText("Entfernen");
		solo.clickOnText("Entfernen");
		solo.clickOnButton(0); 
//		assertFalse(solo.waitForText("Velocity1",1,1000));
		listItemContains(3, "Velocity2");
		Log.i(TAG, "Schlagwort Velocity1 deleted");
		solo.clickOnText(schlagwort);
    	
    	solo.clickOnView(tabs.getChildAt(0));
    	solo.waitForText("Velocity");
    	solo.clickLongOnText("Velocity2");
		solo.clickOnText("Löschen");
		solo.clickOnButton(0); 
//		assertFalse(solo.waitForText("Velocity2",1,1000));
		Log.i(TAG, "Velocity2 deleted");
		
    	solo.clickOnView(tabs.getChildAt(2));
//    	solo.waitForLogMessage("Glossary populated");
    	solo.assertCurrentActivity("Expected Glossary activity", "Glossary"); 
		solo.clickOnText(schlagwort);
		listItemContains(3, "Velocity3");
	}
}
