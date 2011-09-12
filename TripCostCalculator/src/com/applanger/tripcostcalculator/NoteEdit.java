/*
 * Copyright (C) 2008 Google Inc.
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

/* niklas documented this file according to notepad tutorial exercise 3 */ 

package com.applanger.tripcostcalculator;


import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.applanger.tripcostcalculator.R;

public class NoteEdit extends Activity {

    /** declaration of required member fields **/
	private EditText mTitleText;
    private EditText mBodyText;
    private Long mRowId;
    private NotesDbAdapter mDbHelper;

    /**life-cycle part 1: 'onCreate'*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        
        
        /** methodinfo: set view to  'note_edit' layout **/ 
        setContentView(R.layout.note_edit);
        /** methodinfo: change activity title to 'edit_note' **/
        setTitle(R.string.edit_note);

        
        /** field&methodinfo: cast confirm-button & both 'EditText' components to the right view 
         *  sorted by the id's which become automatically generated in 'R.class' */
        mTitleText = (EditText) findViewById(R.id.title);
        mBodyText = (EditText) findViewById(R.id.body);
        Button confirmButton = (Button) findViewById(R.id.confirm);
        
        
        /** check if 'savedInstanceState' is 'null' otherwise (':') getSerializable the long 'KEY_ROWID'*/
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(NotesDbAdapter.KEY_ROWID);
        /** check if mRowId is still 'null' then pass Intent 'extra' information (see PayList.class) and put it into 'bundle'*/
        if (mRowId == null) {
            Bundle extras = getIntent().getExtras();
            /** declare 'mRowID by checking 'extra information' is unequal 'null' then get the long 'KEY_ROWID' otherwise id remains 'null' */
            mRowId = extras != null ? extras.getLong(NotesDbAdapter.KEY_ROWID)
                                    : null;
        }
        
        /** methodinfo: call to view (see below) **/
        populateFields();

        
        /** methodinfo: by click 'confirm',...**/ 
        confirmButton.setOnClickListener(new View.OnClickListener() {
        	/** internalstubbinfo: ... 'RESULT_OK' is returned to 'onActivityResult' in PayList.class **/
        	public void onClick(View view) {
        	    setResult(RESULT_OK);
        	    /** methodinfo: ... then finish activity & return to notepadv3.class **/
        	    finish();
        	}

        });    
    }
    
    /** life-cycle part 2:  bring editable string to view from Cursor 'note' = declared list in NotesDbAdapter.class */
    private void populateFields() {
        if (mRowId != null) {
            Cursor note = mDbHelper.fetchNote(mRowId);
            startManagingCursor(note);
            mTitleText.setText(note.getString(
                        note.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE)));
            mBodyText.setText(note.getString(
                    note.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY)));
        }
    }
    
    /** life-cycle part 3: 'onSaveInstanceState' is called when activity is stopped and/or killed, 
     *  then 'outState' bundle is sent as an instant state back to 'onCreate'*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(NotesDbAdapter.KEY_ROWID, mRowId);
    }
    
    /** life-cycle part 4: 'onPause' is always called at the end of an activity when data is ready to be saved*/
        @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    /** life-cycle part 5: 'onResume' calls 'populateField' to grap the displayed strings (text&body) interimely saved by 'onSavedInstanceState'*/
        @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }
        
    /** life-cycle part 6: save updated, created & interimley saved edtitable strings to database respectively the mRowId*/
    private void saveState() {
    String title = mTitleText.getText().toString();
    String body = mBodyText.getText().toString();

	    if (mRowId == null) {
	        long id = mDbHelper.createNote(title, body);
	        if (id > 0) {
	            mRowId = id;
	        }
	    } else {
	        mDbHelper.updateNote(mRowId, title, body);
	    }
    }
}
