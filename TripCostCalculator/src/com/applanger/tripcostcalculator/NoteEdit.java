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


import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.applanger.tripcostcalculator.R;

public class NoteEdit extends Activity {

    /** declaration of required member fields **/
	private EditText mDateText;
    private EditText mSubmitterText;
    private EditText mAmountText;
    private EditText mCurrencyText;
    private EditText mReceiverText;
    private EditText mPurposeText;
    private EditText mShareText;
    private Long mRowId;
    private NotesDbAdapter mDbHelper;
 
    public ArrayList<String> receivers;
	public ArrayList<String> shares;
	

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
        
        mDateText = (EditText) findViewById(R.id.date);
        mSubmitterText = (EditText) findViewById(R.id.submitter);
        mAmountText = (EditText) findViewById(R.id.amount);
        mCurrencyText = (EditText) findViewById(R.id.currency);
        mReceiverText = (EditText) findViewById(R.id.receiver);
        mPurposeText = (EditText) findViewById(R.id.purpose);
        mShareText = (EditText) findViewById(R.id.share);
        Button BtnConfirm = (Button) findViewById(R.id.BtnConfirm);
        Button BtnPlus = (Button) findViewById(R.id.BtnPlus);
        
        
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

        BtnPlus.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		 receivers.add(mReceiverText.getText().toString());
        		 shares.add(mShareText.getText().toString());    	
    	 
        		 TableLayout table = (TableLayout) findViewById(R.id.AddReceiver);
        		 TableRow row = new TableRow(NoteEdit.this);
        		 TextView listedRec = new TextView(NoteEdit.this);
        		 TextView listedSha = new TextView(NoteEdit.this);
        		 listedRec.setText(mReceiverText.getText().toString(), TextView.BufferType.EDITABLE);
        		 listedRec.setText(mShareText.getText().toString(), TextView.BufferType.EDITABLE);
                     
        		 row.addView(listedRec);
        		 row.addView(listedSha);
        		 table.addView(row,new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        		 
        		 mReceiverText.setText(null,TextView.BufferType.EDITABLE);
        		 mShareText.setText(null,TextView.BufferType.EDITABLE);
        	}
        });    
        
        /** methodinfo: by click 'confirm',...**/ 
        BtnConfirm.setOnClickListener(new View.OnClickListener() {
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
            Cursor note2 = mDbHelper.fetchReceivers(mRowId);
            startManagingCursor(note);
            startManagingCursor(note2);
            mDateText.setText(note.getString(
                        note.getColumnIndexOrThrow(NotesDbAdapter.KEY_DATE)));
            mSubmitterText.setText(note.getString(
                    note.getColumnIndexOrThrow(NotesDbAdapter.KEY_NAME)));
            mAmountText.setText(note.getString(
                    note.getColumnIndexOrThrow(NotesDbAdapter.KEY_AMOUNT)));
            mCurrencyText.setText(note.getString(
                    note.getColumnIndexOrThrow(NotesDbAdapter.KEY_CURRENCY)));
            mPurposeText.setText(note.getString(
                    note.getColumnIndexOrThrow(NotesDbAdapter.KEY_PURPOSE)));
            mReceiverText.setText(note2.getString(
                    note2.getColumnIndexOrThrow(NotesDbAdapter.KEY_NAME)));
            mShareText.setText(note2.getString(
                    note2.getColumnIndexOrThrow(NotesDbAdapter.KEY_AMOUNT)));
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
        
    /** life-cycle part 6: save updated, created & interimely saved editable strings to database respectively the mRowId*/
    private void saveState() {
    String date = mDateText.getText().toString();
    String submitter = mSubmitterText.getText().toString();
    String amount = mAmountText.getText().toString();
    String currency = mCurrencyText.getText().toString();
    String purpose = mPurposeText.getText().toString();

	    if (mRowId == null) {
	        long row_id = mDbHelper.createNote(date, submitter, amount, purpose, currency);
	        mDbHelper.createNote2(receivers, shares, row_id);
	        if (row_id > 0) {
	            mRowId = row_id;
	        }
	    } else {
	        mDbHelper.updateNote(mRowId, date, submitter, amount, purpose);
	        mDbHelper.updateNote2(receivers, shares, mRowId);
	    }
    }
}
