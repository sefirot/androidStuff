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
import java.util.TreeSet;

import static com.applang.Util.*;
import static com.applang.Util2.*;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * An activity that will edit the title of a note. Displays a floating window
 */
public class TitleEditor extends Activity implements View.OnClickListener
{
    /**
     * This is a special intent action that means "edit the title of a note".
     */
    public static final String EDIT_TITLE_ACTION = "com.applang.tagesberichte.action.EDIT_TITLE";

    /**
     * An array of the columns we are interested in.
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.TITLE, // 1
            Notes.CREATED_DATE, 
    };
    
    /** Index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_CREATED = 2;

    private static Calendar calendar = Calendar.getInstance();

    private AutoCompleteTextView mTitle;
    private DatePicker mDate;
    private Spinner mSpinner;
    
    /**
     * Cursor which will provide access to the note whose title we are editing.
     */
    private Cursor mCursor;

    /**
     * The content URI to the note that's being edited.
     */
    private Uri mUri;
    private long mId;

    int table = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("table"))
        	table = extras.getInt("table", 0);
        else
        	table = Math.abs(NotePadProvider.savedTableIndex(this, 0));
        
        setContentView(new int[] {
        		R.layout.title_editor, 
        		R.layout.word_editor1, 
        		R.layout.word_editor2 }[table]);
        
        String[] strings = getResources().getStringArray(R.array.title_edit_array);
        setTitle(strings[table]);

        mUri = getIntent().getData();
        mId = toLong(-1L, mUri.getPathSegments().get(1));
        
        String selection = "";
        if (table == 2) {
            selection = Notes.CREATED_DATE + "=" + mId;
			mUri = Notes.CONTENT_URI;
		}
        mCursor = managedQuery(mUri, 
        		PROJECTION, 
        		NotePadProvider.selection(table, selection), null, 
        		null);

        mTitle = (AutoCompleteTextView) this.findViewById(R.id.title);
        mTitle.setThreshold(1);
        
        switch (table) {
		case 0:
			strings = getResources().getStringArray(R.array.category_array); 
			break;
		case 1:
		case 2:
			strings = wordSet(this, table, "").toArray(new String[0]);
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
		
		mDate = (DatePicker) this.findViewById(R.id.datePicker);
		mSpinner = (Spinner) this.findViewById(R.id.spinner1);
		
		for (int id : new int[] {R.id.ok,R.id.cancel}) {
			Button b = (Button) findViewById(id);
			b.setOnClickListener(this);
		}
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor != null && mCursor.moveToFirst()) {
        	switch (table) {
			case 2:
			default:
				mTitle.setText(mCursor.getString(COLUMN_INDEX_TITLE));
				break;
			}
			if (mDate != null) {
				calendar.setTimeInMillis(mCursor.getLong(COLUMN_INDEX_CREATED));
				mDate.init(calendar.get(Calendar.YEAR),
						calendar.get(Calendar.MONTH),
						calendar.get(Calendar.DAY_OF_MONTH), null);
			}
			if (mSpinner != null)
				mSpinner.setAdapter(new ArrayAdapter<String>(this, 
						android.R.layout.simple_spinner_item, 
						wordSet(this, 2, Notes.CREATED_DATE + "=" + mId).toArray(new String[0])));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null && resultCode != RESULT_CANCELED) {
        	String title = mTitle.getText().toString();
        	
            ContentValues values = new ContentValues();
			values.put(Notes.TITLE, title);
			
            switch (table) {
			case 0:
				if (mDate != null) {
					calendar.set(mDate.getYear(), mDate.getMonth(),	mDate.getDayOfMonth());
					values.put(Notes.CREATED_DATE, calendar.getTimeInMillis());
				}
				break;

			case 1:
		        Cursor cursor = managedQuery(Notes.CONTENT_URI, 
		        		PROJECTION, 
		        		NotePadProvider.selection(table, Notes.TITLE + "=?"), new String[] {title}, 
		        		null);
		        if (cursor.getCount() > 0 && resultCode == RESULT_OK) {
		        	String exists = getResources().getString(R.string.baustein_exists);
		        	Toast.makeText(this, String.format(exists, title), Toast.LENGTH_LONG).show();
		        	setResult(RESULT_CANCELED);
		        	return;
		        }
		        else
		        	break;

			case 2:
				if (title.length() > 0 && resultCode == RESULT_OK) {
					values.put(Notes.CREATED_DATE, mId);
		            try {
						mUri = getContentResolver().insert(mUri, 
								NotePadProvider.selection(table, values));
					} catch (Exception e) {}
		            NotePadProvider.saveTableIndex(this, -table);
				}
				return;
			}
			
			getContentResolver().update(mUri, 
            		NotePadProvider.selection(table, values), 
            		null, null);
        }
    }
    
    int resultCode = RESULT_FIRST_USER;

    public void onClick(View view) {
    	resultCode = view.getId() == R.id.ok ? RESULT_OK : RESULT_CANCELED;
		setResult(resultCode);
        finish();
    }

    public static Set<String> wordSet(Activity activity, int tableIndex, String selection, String... selectionArgs) {
		Cursor cursor = activity.managedQuery(
				Notes.CONTENT_URI, 
				new String[] {Notes.TITLE}, 
				NotePadProvider.selection(tableIndex, selection), selectionArgs, 
        		null);
		
        ValMap map = getResultMap(cursor, 
        	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(0);
				}
	        }, 
        	new Function<Object>() {
				public Object apply(Object... params) {
					return null;
				}
	        }
	    );
        
        return new TreeSet<String>(map.keySet());
    }
}
