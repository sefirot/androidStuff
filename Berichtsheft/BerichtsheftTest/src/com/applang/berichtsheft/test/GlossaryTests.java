package com.applang.berichtsheft.test;

import java.util.Arrays;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.test.TouchUtils;
import android.view.KeyEvent;
import android.widget.ExpandableListView;

import static com.applang.Util.*;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.provider.NotePad.NoteColumns;
import com.applang.provider.NotePadProvider;
import com.applang.tagesberichte.Glossary;
import com.applang.tagesberichte.Glossary.GlossaryListAdapter;
import com.applang.tagesberichte.Glossary.GlossaryLeaf;

public class GlossaryTests extends InfraTests<Glossary>
{
    private static final String TAG = GlossaryTests.class.getSimpleName();
    
	public GlossaryTests() {
		super("com.applang.berichtsheft", Glossary.class);
	}
	
/*	public GlossaryTests(String method) {
		this();
	}
*/
	@Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(mActivity instanceof Glossary);
        
        ProviderTests.generateTestData(mActivity);
        
        mListView = mActivity.listView;
		mAdapter = mActivity.adapter;
//		mActivity.contentResolver = getInstrumentation().getTargetContext().getContentResolver();
		mContentResolver = mActivity.getContentResolver();
		
		mActivity.populate(true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}

    ExpandableListView mListView;
    GlossaryListAdapter mAdapter;
    ContentResolver mContentResolver;
    
    public void testPopulation() {
		assertNotNull(mAdapter);
		assertNotNull(mContentResolver);
		int cnt = NotePadProvider.countNotes(mContentResolver, 2, "", null)[0];
		String groupSelection = "group by title";
		Integer[] cnts = NotePadProvider.countNotes(mContentResolver, 2, groupSelection, null);
		List<String> titles = Arrays.asList(NotePadProvider.getTitles(mContentResolver, 2, groupSelection, null));
		
		assertTrue(cnts.length > 0);
		assertEquals(cnts.length, titles.size());
		
		int groupCount = mAdapter.getGroupCount();
		assertEquals(cnts.length, groupCount);
		for (int i = 0; i < groupCount; i++) {
			Object group = mAdapter.getGroup(i);
			int index = titles.indexOf(group);
			assertTrue(index > -1);
			assertEquals(cnts[index].intValue(), mAdapter.getChildrenCount(i));
			
			final Cursor cursor = mContentResolver.query(NotePadProvider.contentUri(2), 
					NotePadProvider.FULL_PROJECTION, 
					NoteColumns.TITLE + "= ?", new String[] { "" + group }, 
					null);
			assertTrue(cursor.moveToFirst());
			for (int j = 0; j < mAdapter.getChildrenCount(i); j++) {
				final GlossaryLeaf v = (GlossaryLeaf) mAdapter.getChild(i, j); 
				long refId = cursor.getLong(3);
				assertTrue(NotePadProvider.fetchNoteById(refId, mContentResolver, 0, new Job<Cursor>() {
					public void perform(Cursor c, Object[] params) throws Exception {
						assertEquals(c.getString(1), v.getTitle());
					}
				}));
				cursor.moveToNext();
				cnt--;
			}
			cursor.close();
		}
		assertEquals(0, cnt);
	}
    
    public void testSchlagwort() {
//		Button view = (Button) mActivity.findViewById(R.id.button1);
//		TouchUtils.clickView(this, view);
//		this.sendKeys(KeyEvent.KEYCODE_BACK);
//		TouchUtils.clickView(this, view);
    	
//    	Toast.makeText(mActivity, "on hold ...", Toast.LENGTH_LONG).show();
//    	mActivity.startActivity(
//    			new Intent(TitleEditor.EDIT_TITLE_ACTION, Notes.CONTENT_URI)
//    				.putExtra("table", 0));
    	
        mActivity.runOnUiThread(
            new Runnable() {
                public void run() {
                	mListView.requestFocus();
                	mListView.setSelection(0);
                }
            }
        );

        this.sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        this.sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);

		TouchUtils.longClickView(this, mListView);
        this.sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        this.sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
    }
}
