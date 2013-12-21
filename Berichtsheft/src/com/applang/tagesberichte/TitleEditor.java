/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.applang.tagesberichte;

import java.util.Calendar;
import java.util.Set;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePad.NoteColumns;
import com.applang.provider.NotePadProvider;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import static com.applang.Util.*;
import static com.applang.Util1.*;

/**
 * An activity that will edit the title of a note. Displays a floating window
 */
public class TitleEditor extends Activity implements View.OnClickListener
{
    private static final String TAG = TitleEditor.class.getSimpleName();
    
    /**
     * This is a special intent action that means "edit the title of a note".
     */
    public static final String EDIT_TITLE_ACTION = "com.applang.tagesberichte.action.EDIT_TITLE";

    /**
     * An array of the columns we are interested in.
     */
    private static final String[] PROJECTION = new String[] {
    	NoteColumns._ID, // 0
    	NoteColumns.TITLE, // 1
    	NoteColumns.CREATED_DATE, 
    	NoteColumns.REF_ID2, 
    };
    
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_CREATED = 2;
    private static final int COLUMN_INDEX_REF_ID2 = 3;

    private static Calendar calendar = Calendar.getInstance();

    private AutoCompleteTextView mTitle;
    private DatePicker mDate;
    private CheckBox mCheck;
    
    private Cursor mCursor;
    private Uri mUri;
    private Long mId = -1L;

    int mState = NoteEditor.STATE_EDIT;
    String header = "";
    int tableIndex = 0;
    String selection = "";
    String[] selectionArgs = null;
    String title = "";
    String followUp = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mUri = getIntent().getData();
        tableIndex = NotePadProvider.tableIndex(tableIndex, mUri);
        mId = parseId(mId, mUri);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	if (extras.containsKey("tableIndex"))
        		tableIndex = extras.getInt("tableIndex", tableIndex);
        	
        	if (extras.containsKey("state"))
        		mState = extras.getInt("state", mState);
        	
        	if (extras.containsKey("header"))
        		header = extras.getString("header");
        	
        	if (extras.containsKey("followUp"))
        		followUp = extras.getString("followUp");
        	
        	if (extras.containsKey("title")) {
        		title = extras.getString("title");
        		selection = NoteColumns.TITLE + "=?";
				selectionArgs = new String[] {title};
        	}
	    }
        
        setContentView(new int[] {
        		R.layout.title_editor, 
        		R.layout.word_editor1, 
        		R.layout.word_editor2 }[tableIndex]);
        
        String[] strings = getResources().getStringArray(R.array.title_edit_array);
        setTitle(strings[tableIndex] + " : ");
        
        if (tableIndex == 2 && mState == NoteEditor.STATE_INSERT) {
            selection = NoteColumns.REF_ID + "=?";
    		selectionArgs = new String[] {"" + mId};
            mUri = NotePadProvider.contentUri(tableIndex);
		}
        
        mCursor = managedQuery(mUri, 
        		PROJECTION, 
        		selection, selectionArgs, 
        		null);

        mTitle = (AutoCompleteTextView) this.findViewById(R.id.title);
        mTitle.setThreshold(1);
        
        switch (tableIndex) {
		case 0:
			strings = getResources().getStringArray(R.array.category_array); 
			break;
		case 1:
		case 2:
			strings = NotePadProvider.wordSet(this.getContentResolver(), tableIndex, "").toArray(new String[0]);
			break;
		}
        
        mTitle.setAdapter(new ArrayAdapter<String>(this,
        		android.R.layout.simple_dropdown_item_1line, strings));
        
		ImageButton btn = (ImageButton) findViewById(R.id.button1);
		if (btn != null)
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mTitle.isPopupShowing())
						mTitle.dismissDropDown();
					else
						mTitle.showDropDown();
				}
			});
        
		final Button bt = (Button) findViewById(R.id.button2);
		if (bt != null) { 
			if (mState == NoteEditor.STATE_INSERT) {
				mTitle.setHint(R.string.new_item);
				bt.setText(R.string.old_items);
				bt.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						openContextMenu(bt);
					}
				});
				registerForContextMenu(bt);
			}
			else
				bt.setVisibility(View.GONE);
		}
		
		for (int id : new int[] {R.id.ok,R.id.cancel}) {
			Button b = (Button) findViewById(id);
			b.setOnClickListener(this);
		}
		
		mDate = (DatePicker) this.findViewById(R.id.datePicker);
//		mCheck = (CheckBox) this.findViewById(R.id.checkBox1);
	}

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        
		menu.clear();
		NotePadProvider.fetchNoteById(mId, this.getContentResolver(), 0, new Job<Cursor>() {
			@Override
			public void perform(Cursor c, Object[] params) throws Exception {
				menu.setHeaderTitle(NotesList.description(0, c.getLong(3), c.getString(1)));
//				menu.setHeaderTitle(getResources().getString(R.string.title_list_schlagwort, 
//						NotesList.description(0, c.getLong(3), c.getString(1))));
			}
		});
		Set<String> words = NotePadProvider.wordSet(this.getContentResolver(), 2, NoteColumns.CREATED_DATE + "=" + mId);
		if (words.size() > 0)
			for (String word : words) 
				menu.add(word);
		else
			view.setEnabled(false);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	return super.onContextItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTitle.setText(title);
        if (mCursor != null && mCursor.moveToFirst()) {
        	if (tableIndex != 2 || mState == NoteEditor.STATE_EDIT) 
				mTitle.setText(mCursor.getString(COLUMN_INDEX_TITLE));
        	
			if (mDate != null) {
				calendar.setTimeInMillis(mCursor.getLong(COLUMN_INDEX_CREATED));
				mDate.init(calendar.get(Calendar.YEAR),
						calendar.get(Calendar.MONTH),
						calendar.get(Calendar.DAY_OF_MONTH), null);
			}
			if (mCheck != null) {
				mCheck.setChecked(mCursor.getLong(COLUMN_INDEX_REF_ID2) < 0);
			}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null && resultCode != RESULT_CANCELED) {
        	title = mTitle.getText().toString();
        	
            ContentValues values = new ContentValues();
			values.put(NoteColumns.TITLE, title);
			
            switch (tableIndex) {
			case 0:
				if (mDate != null) {
					calendar.set(mDate.getYear(), mDate.getMonth(),	mDate.getDayOfMonth());
					values.put(NoteColumns.CREATED_DATE, calendar.getTimeInMillis());
				}
				break;

			case 1:
				if (mCheck != null) {
					long refId2 = Math.abs(mCursor.getLong(COLUMN_INDEX_REF_ID2));
					values.put(NoteColumns.REF_ID2, mCheck.isChecked() ? -refId2 : refId2);
				}
		        Cursor cursor = managedQuery(NotePadProvider.contentUri(tableIndex), 
		        		PROJECTION, 
		        		NoteColumns.TITLE + "=?", new String[] {title}, 
		        		null);
		        if (cursor.getCount() > 0 && resultCode == RESULT_OK) {
		        	Toast.makeText(this, 
		        			getResources().getString(R.string.baustein_exists, title), 
		        			Toast.LENGTH_LONG).show();
		        	setResult(RESULT_CANCELED);
		        	return;
		        }
		        else
		        	break;

			case 2:
				if (title.length() > 0 && resultCode == RESULT_OK && mState == NoteEditor.STATE_INSERT) {
			        selection = 
			        		NoteColumns.REF_ID + "=? and " + 
			        				NoteColumns.TITLE + "=?";
			        selectionArgs = new String[] {"" + mId, title};
					mCursor = managedQuery(mUri, 
			        		arrayappend(PROJECTION, NoteColumns.REF_ID2), 
			        		selection, 
			        		selectionArgs, 
			        		null);
			        
		            if (mCursor.getCount() < 1) {
		            	values.put(NoteColumns.REF_ID, mId);
		            	values.put(NoteColumns.REF_ID2, (Long)null);
						mUri = getContentResolver().insert(mUri, values);
						
						Toast.makeText(this, 
								getResources().getString(R.string.new_word, header), 
								Toast.LENGTH_SHORT).show();
					}
		            return;
				}
				else if (title.length() < 1 || resultCode == RESULT_CANCELED)
					return;
				else
					break;
			}
			
			getContentResolver().update(mUri, 
            		values, 
            		selection, 
            		selectionArgs);
        }
    }
    
    int resultCode = RESULT_FIRST_USER;

    public void onClick(View view) {
    	resultCode = view.getId() == R.id.ok ? RESULT_OK : RESULT_CANCELED;
    	if (notNullOrEmpty(followUp))
    		setResult(resultCode, new Intent(followUp, mUri));
    	else
    		setResult(resultCode);
        finish();
    }
}
