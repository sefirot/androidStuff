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

import com.applang.Util;
import com.applang.berichtsheft.R;
import com.applang.provider.NotePad;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.AutoCompleteTextView;

/**
 * An activity that will edit the title of a note. Displays a floating window
 */
public class TitleEditor extends Activity implements View.OnClickListener {

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
    
    /**
     * Cursor which will provide access to the note whose title we are editing.
     */
    private Cursor mCursor;

    private AutoCompleteTextView mTitle;
    private DatePicker mDate;

    /**
     * The content URI to the note that's being edited.
     */
    private Uri mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.title_editor);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("table"))
        	table = extras.getInt("table", 0);
        else
        	table = NotePadProvider.restoreTableIndex(this, 0);

        mUri = getIntent().getData();
        mCursor = managedQuery(mUri, PROJECTION, 
        		NotePadProvider.selection(table, ""), null, 
        		null);

        mTitle = (AutoCompleteTextView) this.findViewById(R.id.title);
        mTitle.setThreshold(1);
        if (table == 0) {
			String[] categories = getResources().getStringArray(
					R.array.category_array);
			mTitle.setAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_dropdown_item_1line, categories));
		}
		mDate = (DatePicker) this.findViewById(R.id.datePicker);
		mDate.setEnabled(table == 0);
        
		for (int id : new int[] {R.id.ok,R.id.cancel}) {
			Button b = (Button) findViewById(id);
			b.setOnClickListener(this);
		}
    }

    int table = 0;

    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor != null) {
            mCursor.moveToFirst();
            mTitle.setText(mCursor.getString(COLUMN_INDEX_TITLE));
			if (!mCursor.isNull(COLUMN_INDEX_CREATED)) {
				calendar.setTimeInMillis(mCursor.getLong(COLUMN_INDEX_CREATED));
				mDate.init(calendar.get(Calendar.YEAR),
						calendar.get(Calendar.MONTH),
						calendar.get(Calendar.DAY_OF_MONTH), null);
			}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null && resultCode != RESULT_CANCELED) {
            ContentValues values = new ContentValues();
            values.put(Notes.TITLE, mTitle.getText().toString());
			calendar.set(mDate.getYear(), mDate.getMonth(), mDate.getDayOfMonth());
			values.put(Notes.CREATED_DATE, calendar.getTimeInMillis());
			
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
}
