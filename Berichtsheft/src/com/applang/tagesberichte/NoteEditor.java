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

import java.util.Map;
import java.util.TreeSet;

import com.applang.UserContext;
import com.applang.berichtsheft.R;
import com.applang.provider.NotePad.NoteColumns;
import com.applang.provider.NotePadProvider;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

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
            NoteColumns._ID, // 0
            NoteColumns.NOTE, // 1
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
    private static final int SCHLAGWORT_ID = Menu.FIRST + 5;
    private static final int ANWEISUNG_ID = Menu.FIRST + 6;
    private static final int STRUKTUR_ID = Menu.FIRST + 7;

    // The different distinct states the activity can be run in.
    public static final int STATE_EDIT = 0;
    public static final int STATE_INSERT = 1;
    
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

        public String getFirstWord() {
        	Editable text = getText();
			int start = 0;
			int end = -1;
        	for (int i = 0; i < text.length(); i++) {
				if (Character.isWhitespace(text.charAt(i))) {
					if (end < start)
						start = i + 1;
					else
						break;
				}
				else
					end = i + 1;
			}
    		return end < start ? "" : text.subSequence(start, end).toString();
        }

		public boolean hasWord() {
			return getFirstWord().length() > 0;
		}

        public String getSelectedText() {
        	if (hasSelection()) {
				int start = getSelectionStart();
				int end = getSelectionEnd();
        		return getText().subSequence(Math.min(start, end), Math.max(start, end)).toString();
        	}
        	else
        		return "";
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

    int tableIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        tableIndex = NotePadProvider.tableIndex(0, intent.getData());

        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } 
        else if (Intent.ACTION_INSERT.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), 
            		new ContentValues());

            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }

            startActivityForResult(new Intent(TitleEditor.EDIT_TITLE_ACTION, mUri)
					.putExtra("state", NoteEditor.STATE_INSERT), 0);
        } 
        else {
            // Whoops, unknown action!  Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }
    	
    	setContentView(R.layout.note_editor);
    	
    	mText = (LinedEditText) findViewById(R.id.note);
		mText.addTextChangedListener(new TextWatcher() {
			boolean first = true;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
//				if (tableIndex == 1)
//					analyze(s);
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (first)
					mText.setSelection(Math.max(0, s.length() - 1));
				else if (tableIndex == 1) {
					if (count - before == 1 && start + count > 0) {
						char ch = s.charAt(start + count - 1);
						if (VRI.equals(ch)) {
							requestBaustein(0);
						} else if (VDI.equals(ch)) {
							requestDirective(0);
						}
					}
				}
				first = false;
			}
		});
		
		if (savedInstanceState != null) {
    		mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
    	}
    }

	int[] requestCode = new int[] {0,0,0};

	private void requestBaustein(int code) {
		requestCode[0] = code;
		popupContextMenu(this, mText);
	}

    private void requestSchlagwort(int code) {
		requestCode[1] = code;
		popupContextMenu(this, mText);
	}

	private void requestDirective(int code) {
		requestCode[2] = code;
		popupContextMenu(this, mText);
	}
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        
        menu.clear();
        
        int q, id = 0;
        if (requestCode[0] > 0) {
        	id = R.string.menu_baustein;
			
			bausteine = NotePadProvider.bausteinMap(getContentResolver(), "");
			for (String key : new TreeSet<String>(bausteine.keySet())) 
				menu.add(key);
			
			q = 0;
		}
        else if (requestCode[1] > 0) {
        	id = R.string.menu_schlagwort;
			getMenuInflater().inflate(R.menu.contextmenu_noteeditor, menu);
			
			MenuItem mi = menu.findItem(R.id.menu_item_selected);
			if (mi != null)
				mi.setEnabled(mText.hasSelection());
			mi = menu.findItem(R.id.menu_item_first);
			if (mi != null)
				mi.setEnabled(mText.hasWord());
			
			q = 1;
		}
        else if (requestCode[2] > 0) {
        	id = R.string.menu_anweisung;
			
			anweisungen = UserContext.directives();
			for (String key : anweisungen.keySet()) 
				menu.add(key);
			
			q = 2;
		}
        else {
        	for (int i = 0; i < requestCode.length; i++) 
        		requestCode[i] = 0;
        	return;
        }
        
		menu.setHeaderTitle(getResources().getString(id));
    	requestCode[q] = -requestCode[q];
    }
    
    private ValMap bausteine = null;
    Map<String,String> anweisungen = null;

	public static final String SPAN = "caret";
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	String text = item.getTitle().toString();
    	int q;
        if (requestCode[0] < 0) {
        	if (requestCode[0] < -1) {
	        	if (bausteine != null) {
	        		text = bausteine.get(text).toString();
	        		new UserContext.EvaluationTask(this, bausteine, null, null, new Job<Object>() {
	        			public void perform(Object text, Object[] params) {
	        	        	mText.insertAtCaretPosition(text.toString());
	        	        	updateNote(mText.getText().toString(), true);
	        			}
	        		}).execute(text, getString(R.string.title_evaluator));
	            	requestCode[0] = 0;
	        		return true;
	        	}
	        	else
	        		text = VRI + enclose("{", text, "}");
        	}
        	else {
        		text = enclose("{", text, "}");
        	}
        	mText.insertAtCaretPosition(text);
        	q = 0;
		}
        else if (requestCode[1] < 0) {
        	String word = "";
        	switch (item.getItemId()) {
			case R.id.menu_item_first:
				word = mText.getFirstWord();
				break;
			case R.id.menu_item_selected:
				word = mText.getSelectedText();
				break;
			}
        	if (word.length() > 0) {
        		Long id = parseId(-1L, mUri);
        		String description = getNoteDescription(getContentResolver(), tableIndex, id);
        		Uri uri = NotePadProvider.contentUri(2);
        		uri = ContentUris.withAppendedId(uri, id);
        		id = NotePadProvider.getIdOfNote(getContentResolver(), 2, 
        				NoteColumns.REF_ID + "=? and " + NoteColumns.TITLE + "=?", 
        				strings("" + id, word));
        		int state = id < 0 ? NoteEditor.STATE_INSERT : NoteEditor.STATE_EDIT;
	        	startActivity(new Intent()
	        			.setClass(this, TitleEditor.class)
	        			.setData(uri)
	        			.putExtra("title", word)
	        			.putExtra("header", description)
	        			.putExtra("state", state));
        	}
        	q = 1;
		}
        else if (requestCode[2] < 0) {
    		String signature = anweisungen.get(text);
    		synthesize(signature);
        	q = 2;
        }
        else {
        	return super.onContextItemSelected(item);
        }
    	requestCode[q] = 0;
    	return true;
    }

	private void synthesize(String anweisung) {
		UserContext.buildDirective(anweisung, this, new ValMap(), new Job<Object>() {
			public void perform(Object t, Object[] params) {
				if (t != null) {
					String text = t.toString();
					if (requestCode[2] < -1)
						text = VDI + text;
					mText.insertAtCaretPosition(text);
					updateNote(mText.getText().toString(), true);
				}
			}
		});
	}
    
    protected void analyze() {
        String[] strings = getResources().getStringArray(R.array.title_edit_array);
		final String text = mText.getText().toString();
		new Baustein.AnalysisTask(this, new Job<Boolean>() {
			public void perform(Boolean problem, Object[] params) {
				if (problem) {
					int offset = getTextOffsets(text, getProblemCoordinates())[0];
					mText.setSelection(offset, offset + 1);
				}
				else {
					Intent intent = new Intent()
							.setClass(NoteEditor.this, Baustein.class)
							.setData(NotePadProvider.contentUri(2));
					startActivityForResult(intent, 2);
				}
			}
		}).execute(text, strings[tableIndex]);
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
		case 1:
	    	mCursor = managedQuery(mUri, PROJECTION, "", null, null);
			break;

		case 2:
			if (resultCode == RESULT_OK) {
				int[] span = data.getIntArrayExtra(SPAN);
				String string = mText.getText().toString();
				int[] offsets = getTextOffsets(string, span);
				int length = string.length();
				mText.setSelection(
						Math.max(0, offsets[0]), 
						Math.min(length, offsets[1]));
			}
			break;

		default:
			setResult(resultCode, (new Intent()).setAction(mUri.toString()));
			if (resultCode == RESULT_CANCELED) {
				deleteNote();
				finish();
			}
			break;
		}
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        mCursor = managedQuery(mUri, 
        		PROJECTION, 
        		"", null, 
        		null);

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
        
        if (mTextUndoRedo == null)
    		mTextUndoRedo = new Helper.TextViewUndoRedo(mText);
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
                updateNote(text, mNoteOnly);
            }
        }
    }

	public void updateNote(String text, boolean noteOnly) {
		ContentValues values = new ContentValues();

		// This stuff is only done when working with a full-fledged note.
		if (!noteOnly) {
		    // Bump the modification time to now.
		    values.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
		}

		// Write our text back into the provider.
		values.put(NoteColumns.NOTE, text);

		// Commit all of our changes to persistent storage. When the update completes
		// the content provider will notify the cursor of the change, which will
		// cause the UI to be updated.
		getContentResolver().update(mUri, 
				values, 
				null, null);
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
        if (tableIndex == 1) {
			menu.add(0, ANWEISUNG_ID, 0, R.string.menu_anweisung)
					.setShortcut('5', 'a');
			menu.add(0, STRUKTUR_ID, 0, R.string.menu_struktur)
					.setShortcut('7', 'k');
			menu.add(0, EVALUATE_ID, 0, R.string.menu_evaluate)
					.setShortcut('3', 'e');
        }
        if (tableIndex == 0)
	        menu.add(0, SCHLAGWORT_ID, 0, R.string.menu_schlagwort)
	    			.setShortcut('4', 's');
        
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
    		Long id = parseId(-1L, mUri);
    		String description = getNoteDescription(getContentResolver(), tableIndex, id);
    		description = getResources().getString(R.string.areUsure, description);
    		areUsure(this, description, new Job<Void>() {
				@Override
				public void perform(Void t, Object[] params) throws Exception {
		            deleteNote();
		            finish();
				}
    		});
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
        case ANWEISUNG_ID:
			requestDirective(2);
            break;
        case STRUKTUR_ID:
			analyze();
            break;
        case SCHLAGWORT_ID:
			requestSchlagwort(2);
            break;
        case EVALUATE_ID:
			startActivityForResult(new Intent()
				.setAction(BausteinEvaluator.EVALUATE_ACTION)
				.setData(mUri), 1);
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
                values.put(NoteColumns.NOTE, mOriginalContent);
                getContentResolver().update(mUri, 
                		values, 
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
            getContentResolver().delete(mUri, "", null);
            mText.setText("");
        }
    }

	public static String getNoteDescription(ContentResolver contentResolver, final int tableIndex, long id) {
		Object[] params = new Object[1];
		NotePadProvider.fetchNoteById(id, contentResolver, 0, new Job<Cursor>() {
			@Override
			public void perform(Cursor c, Object[] params) throws Exception {
				params[0] = NotesList.description(tableIndex, 
						c.getLong(3), 
						c.getString(1));
			}
		}, params);
		return params[0] != null ? params[0].toString() : "";
	}
    
    private Helper.TextViewUndoRedo mTextUndoRedo = null;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
	    	if (mTextUndoRedo != null)
	    		mTextUndoRedo.redo();
	    	return true;
	    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
	    	if (mTextUndoRedo != null)
	    		mTextUndoRedo.undo();
	    	return true;
	    } else {
	    	return super.onKeyDown(keyCode, event);
	    }
	}
}
