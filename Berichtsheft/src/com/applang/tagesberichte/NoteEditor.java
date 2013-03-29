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

import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.Notes;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;

import static com.applang.Util.*;

/**
 * A generic activity for editing a note in a database.  This can be used
 * either to simply view a note {@link Intent#ACTION_VIEW}, view and edit a note
 * {@link Intent#ACTION_EDIT}, or create a new note {@link Intent#ACTION_INSERT}.  
 */
public class NoteEditor extends Activity
{
    private static final String TAG = NoteEditor.class.getSimpleName();
    
    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.NOTE, // 1
    };
    /** The index of the note column */
    private static final int COLUMN_INDEX_NOTE = 1;
    
    // This is our state data that is stored when freezing.
    private static final String ORIGINAL_CONTENT = "origContent";

    // Identifiers for our menu items.
    private static final int REVERT_ID = Menu.FIRST;
    private static final int DISCARD_ID = Menu.FIRST + 1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private static final int BAUSTEIN_ID = Menu.FIRST + 3;
    private static final int EVALUATE_ID = Menu.FIRST + 4;

    // The different distinct states the activity can be run in.
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;
    
    private static final Character BAUSTEIN_IDENTIFICATOR = '$';

    private int mState;
    private boolean mNoteOnly = false;
    private Uri mUri;
    private Cursor mCursor;
    private LinedEditText mText;
    private String mOriginalContent;

    /**
     * A custom EditText that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // we need this constructor for LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
		}
        
        @Override
        protected void onDraw(Canvas canvas) {
            int count = getLineCount();
            Rect r = mRect;
            Paint paint = mPaint;

            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, r);

                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            super.onDraw(canvas);
        }

        public void insertAtCaretPosition(CharSequence text) {
        	int length = text.length();
    		if (length > 0) {
				int start = getSelectionStart();
				int end = getSelectionEnd();
				setText(getText().replace(Math.min(start, end),
						Math.max(start, end), text, 0, length));
				setSelection(start + length);
			}
    	}
    }

    int table = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("table"))
        	table = extras.getInt("table", 0);

        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } 
        else if (Intent.ACTION_INSERT.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), 
            		NotePadProvider.selection(table, new ContentValues()));

            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }

            startActivityForResult(new Intent(TitleEditor.EDIT_TITLE_ACTION, mUri).putExtra("table", table), 0);
        } 
        else {
            // Whoops, unknown action!  Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }
    	
    	setContentView(R.layout.note_editor);
    	
    	mText = (LinedEditText) findViewById(R.id.note);
    	mText.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            boolean first = true;
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	if (first)
            		mText.setSelection(Math.max(0, s.length() - 1));
            	else if (count - before == 1 && start + count > 0 && 
						BAUSTEIN_IDENTIFICATOR.equals(s.charAt(start + count - 1))) {
/*					Toast.makeText(NoteEditor.this, 
							String.format("change in '%s' start %d before %d count %d", s, start, before, count), 
							Toast.LENGTH_LONG).show();
*/
					requestBaustein(1);
				}
				first = false;
            }

			@Override
			public void afterTextChanged(Editable s) {
			}
        });
    	registerForContextMenu(mText);
    	
    	mCursor = managedQuery(mUri, 
    			PROJECTION, 
    			NotePadProvider.selection(table, ""), null, 
    			null);
    	
    	if (savedInstanceState != null) {
    		mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
    	}
    }
    
    int bausteinRequested = 0;

	private void requestBaustein(int requestCode) {
		bausteinRequested = requestCode;
		NoteEditor.this.openContextMenu(mText);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (bausteinRequested > 0) {
			menu.clear();
			menu.setHeaderTitle(getResources().getString(R.string.baustein));
			for (String title : TitleEditor.wordSet(this, 1, "")) {
				menu.add(title);
			}
		}
        else
        	bausteinRequested = 0;
        
		bausteinRequested = -bausteinRequested;
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (bausteinRequested < 0) {
        	String text = item.getTitle().toString();
        	if (bausteinRequested < -1)
        		text = BAUSTEIN_IDENTIFICATOR + enclose("{", text, "}");
        	
        	mText.insertAtCaretPosition(text);
        	
        	bausteinRequested = 0;
        }
        else
        	return super.onContextItemSelected(item);
        
    	return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	setResult(resultCode, (new Intent()).setAction(mUri.toString()));
    	if (resultCode == RESULT_CANCELED) {
            deleteNote();
    		finish();
    	}
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (mCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            mCursor.moveToFirst();

            // Modify our overall title depending on the mode we are running in.
            if (mState == STATE_EDIT) {
                setTitle(getText(R.string.title_edit));
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            // This is a little tricky: we may be resumed after previously being
            // paused/stopped.  We want to put the new text in the text view,
            // but leave the user where they were (retain the cursor position
            // etc).  This version of setText does that for us.
            String note = mCursor.getString(COLUMN_INDEX_NOTE);
            mText.setTextKeepState(note);
            
            // If we hadn't previously retrieved the original text, do so
            // now.  This allows the user to revert their changes.
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }
        } 
        else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only editing.
        if (mCursor != null) {
            String text = mText.getText().toString();
            int length = text.length();

            // If this activity is finished, and there is no text, then we
            // do something a little special: simply delete the note entry.
            // Note that we do this both for editing and inserting...  it
            // would be reasonable to only do it when inserting.
            if (isFinishing() && (length == 0) && !mNoteOnly) {
                setResult(RESULT_CANCELED);
                deleteNote();

            // Get out updates into the provider.
            } 
            else {
                ContentValues values = new ContentValues();

                // This stuff is only done when working with a full-fledged note.
                if (!mNoteOnly) {
                    // Bump the modification time to now.
                    values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
				}

                // Write our text back into the provider.
                values.put(Notes.NOTE, text);

                // Commit all of our changes to persistent storage. When the update completes
                // the content provider will notify the cursor of the change, which will
                // cause the UI to be updated.
                getContentResolver().update(mUri, 
                		NotePadProvider.selection(table, values), 
                		null, null);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Build the menus that are shown when editing.
        if (mState == STATE_EDIT) {
            menu.add(0, REVERT_ID, 0, R.string.menu_revert)
                    .setShortcut('0', 'r')
                    .setIcon(android.R.drawable.ic_menu_revert);
            if (!mNoteOnly) {
                menu.add(0, DELETE_ID, 0, R.string.menu_delete)
                        .setShortcut('1', 'd')
                        .setIcon(android.R.drawable.ic_menu_delete);
            }

        // Build the menus that are shown when inserting.
        } else {
            menu.add(0, DISCARD_ID, 0, R.string.menu_discard)
                    .setShortcut('0', 'd')
                    .setIcon(android.R.drawable.ic_menu_delete);
        }

        menu.add(0, BAUSTEIN_ID, 0, R.string.menu_baustein)
    		.setShortcut('2', 'b');
        menu.add(0, EVALUATE_ID, 0, R.string.menu_evaluate)
    		.setShortcut('3', 'e');
        
        // If we are working on a full note, then append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        if (!mNoteOnly) {
            Intent intent = new Intent(null, getIntent().getData());
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case DELETE_ID:
            deleteNote();
            finish();
            break;
        case DISCARD_ID:
            cancelNote();
            break;
        case REVERT_ID:
            cancelNote();
            break;
        case BAUSTEIN_ID:
			requestBaustein(2);
            break;
        case EVALUATE_ID:
			startActivity(new Intent()
				.setClass(this, NoteEvaluator.class)
				.setData(mUri)
				.putExtra("table", table));
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Take care of canceling work on a note.  Deletes the note if we
     * had created it, otherwise reverts to the original text.
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(Notes.NOTE, mOriginalContent);
                getContentResolver().update(mUri, 
                		NotePadProvider.selection(table, values), 
                		null, null);
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, 
            		NotePadProvider.selection(table, ""), 
            		null);
            mText.setText("");
        }
    }
}
