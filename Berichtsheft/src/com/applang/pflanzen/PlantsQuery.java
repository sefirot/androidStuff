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

package com.applang.pflanzen;

import java.util.Random;

import com.applang.berichtsheft.R;
import com.applang.berichtsheft.R.layout;
import com.applang.provider.NotePad.NoteColumns;
import com.applang.provider.PlantInfo.Plants;

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
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A generic activity for editing a note in a database.  This can be used
 * either to simply view a note {@link Intent#ACTION_VIEW}, view and edit a note
 * {@link Intent#ACTION_EDIT}, or create a new note {@link Intent#ACTION_INSERT}.  
 */
public class PlantsQuery extends Activity {
    private static final String TAG = "PlantsQuery";

//    /**
//     * Standard projection for the interesting columns of a normal note.
//     */
    private static final String[] PROJECTION = new String[] {
    	Plants._ID, // 0
        Plants.NAME, // 1
        Plants.FAMILY, // 2
        Plants.BOTNAME, // 3
        Plants.BOTFAMILY, // 4
        Plants.GROUP, // 5
    };
   
    private static final String[] PROJECTION_ID = new String[] {
    	Plants._ID, // 0 
    };
    
//    /** The index of the note column */
    private static final int COLUMN_INDEX_NOTE = 1;
//    private static final int COLUMN_INDEX_FAMILY = 2;
//    private static final int COLUMN_INDEX_BOTNAME = 3;
//    private static final int COLUMN_INDEX_BOTFAMILY = 4;
//    private static final int COLUMN_INDEX_PLANTGROUP = 5;
//    
//    // This is our state data that is stored when freezing.
//    private static final String ORIGINAL_CONTENT = "origContent";
//
//    // Identifiers for our menu items.
//    private static final int REVERT_ID = Menu.FIRST;
//    private static final int DISCARD_ID = Menu.FIRST + 1;
//    private static final int DELETE_ID = Menu.FIRST + 2;
//
//    // The different distinct states the activity can be run in.
//    private static final int STATE_EDIT = 0;
//    private static final int STATE_INSERT = 1;
//
//    private int mState;
//    private boolean mNoteOnly = false;
    private Uri mUri;
    private Cursor mCursor;
//    private EditText mText;
//    private String mOriginalContent;
//
	private TextView mLargeText;
//
//	private EditText mPlantFamilyText;
//
//	private EditText mBotNameText;
//
//	private EditText mBotFamilyText;
//
//	private EditText mPlantGroupText;

 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
//
//        // Do some setup based on the action being performed.
//
//        final String action = intent.getAction();
//        if (Intent.ACTION_EDIT.equals(action)) {
//            // Requested to edit: set that state, and the data being edited.
//            mState = STATE_EDIT;
            mUri = intent.getData();
//        } else if (Intent.ACTION_INSERT.equals(action)) {
//            // Requested to insert: set that state, and create a new entry
//            // in the container.
//            mState = STATE_INSERT;
//            mUri = getContentResolver().insert(intent.getData(), null);
//
//            // If we were unable to create a new note, then just finish
//            // this activity.  A RESULT_CANCELED will be sent back to the
//            // original activity if they requested a result.
//            if (mUri == null) {
//                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
//                finish();
//                return;
//            }
//
//         //   startActivity(new Intent(TitleEditor.EDIT_TITLE_ACTION, mUri));
//            Toast.makeText(this, "Here we are!!", Toast.LENGTH_LONG).show();
//            
//            // The new entry was created, so assume all will end well and
//            // set the result to be returned.
//            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
//
//        } else {
//            // Whoops, unknown action!  Bail.
//            Log.e(TAG, "Unknown action, exiting");
//            finish();
//            return;
//        }

     // Set the layout for this activity.  You can find it in res/layout-port/plant_editor.xml
        setContentView(R.layout.plants_query_view);
        
//     // The text view for our note, identified by its ID in the XML file.
        mLargeText = (TextView) findViewById(R.id.largeText);
//        mPlantFamilyText = (EditText) findViewById(R.id.PlantFamily);
//        mBotNameText = (EditText) findViewById(R.id.BotName);
//        mBotFamilyText = (EditText) findViewById(R.id.BotFamily);
//        mPlantGroupText = (EditText) findViewById(R.id.PlantGroup);
//        
//        // Get the note!
        mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        mCursor.moveToFirst();
		// mLargeText.setText(mCursor.getString(1));
        
        String randomString = "";
        randomString = randomString + getRandomId(); 
        mLargeText.setText(randomString);
//
//        // If an instance of this activity had previously stopped, we can
//        // get the original text it started with.
//        if (savedInstanceState != null) {
//            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
//        }
    }

    protected Random getRandomId() {
    	mCursor = managedQuery(mUri, PROJECTION_ID, null, null, null);
    	int allEntries = mCursor.getCount();
    	Random randomId = new Random();
    	randomId.nextInt(allEntries);
    	return randomId;
    }
    
    
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // If we didn't have any trouble retrieving the data, it is now
//        // time to get at the stuff.
//        if (mCursor != null) {
//            // Make sure we are at the one and only row in the cursor.
//            mCursor.moveToFirst();
//
//            // Modify our overall title depending on the mode we are running in.
//            if (mState == STATE_EDIT) {
//                setTitle(getText(R.string.plant_edit));
//            } else if (mState == STATE_INSERT) {
//                setTitle(getText(R.string.plant_note));
//            }
//
//            // This is a little tricky: we may be resumed after previously being
//            // paused/stopped.  We want to put the new text in the text view,
//            // but leave the user where they were (retain the cursor position
//            // etc).  This version of setText does that for us.
//            String plantName = mCursor.getString(COLUMN_INDEX_NOTE);
//            String plantFamily = mCursor.getString(COLUMN_INDEX_FAMILY);
//            String botName = mCursor.getString(COLUMN_INDEX_BOTNAME);
//            String botFamily = mCursor.getString(COLUMN_INDEX_BOTFAMILY);
//            String plantGroup = mCursor.getString(COLUMN_INDEX_PLANTGROUP);
//            mPlantNameText.setTextKeepState(plantName);
//            mPlantFamilyText.setText(plantFamily);
//            mBotNameText.setText(botName);
//            mBotFamilyText.setText(botFamily);
//            mPlantGroupText.setText(plantGroup);
//            
//            // If we hadn't previously retrieved the original text, do so
//            // now.  This allows the user to revert their changes.
//            if (mOriginalContent == null) {
//                mOriginalContent = plantName;
//            }
//
//        } else {
//            setTitle(getText(R.string.error_title));
//            mText.setText(getText(R.string.error_message));
//        }
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        // Save away the original text, so we still have it if the activity
//        // needs to be killed while paused.
//        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//
//        // The user is going somewhere else, so make sure their current
//        // changes are safely saved away in the provider.  We don't need
//        // to do this if only editing.
//        if (mCursor != null) {
//            String plantName = mPlantNameText.getText().toString();
//            String plantFamily = mPlantFamilyText.getText().toString();
//            String botName = mBotNameText.getText().toString();
//            String botFamily = mBotFamilyText.getText().toString();
//            String plantGroup = mPlantGroupText.getText().toString();
//            int nameLength = plantName.length();
//            int familyLength = plantFamily.length();
//            
//            // If this activity is finished, and there is no text, then we
//            // do something a little special: simply delete the note entry.
//            // Note that we do this both for editing and inserting...  it
//            // would be reasonable to only do it when inserting.
//            if (isFinishing() && (nameLength == 0) && (familyLength == 0) && !mNoteOnly) {
//                setResult(RESULT_CANCELED);
//                deleteNote();
//
//            // Get out updates into the provider.
//            } else {
//                ContentValues values = new ContentValues();
//
//                // This stuff is only done when working with a full-fledged note.
//                if (!mNoteOnly) {
//                    // Bump the modification time to now.
//                    //values.put(Plants.MODIFIED_DATE, System.currentTimeMillis());
//
//                    // If we are creating a new note, then we want to also create
//                    // an initial title for it.
///*					if (mState == STATE_INSERT) {
//                        String title = text.substring(0, Math.min(30, length));
//                        if (length > 30) {
//                            int lastSpace = title.lastIndexOf(' ');
//                            if (lastSpace > 0) {
//                                title = title.substring(0, lastSpace);
//                            }
//                        }
//                        values.put(Notes.TITLE, title);
//                    }
//*/				}
//
//                // Write our text back into the provider.
//                values.put(Plants.NAME, plantName);
//                values.put(Plants.FAMILY, plantFamily);
//                values.put(Plants.BOTNAME, botName);
//                values.put(Plants.BOTFAMILY, botFamily);
//                values.put(Plants.GROUP, plantGroup);
//                
//                // Commit all of our changes to persistent storage. When the update completes
//                // the content provider will notify the cursor of the change, which will
//                // cause the UI to be updated.
//                getContentResolver().update(mUri, values, null, null);
//            }
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//
//        // Build the menus that are shown when editing.
//        if (mState == STATE_EDIT) {
//            menu.add(0, REVERT_ID, 0, R.string.menu_revert)
//                    .setShortcut('0', 'r')
//                    .setIcon(android.R.drawable.ic_menu_revert);
//            if (!mNoteOnly) {
//                menu.add(0, DELETE_ID, 0, R.string.menu_delete)
//                        .setShortcut('1', 'd')
//                        .setIcon(android.R.drawable.ic_menu_delete);
//            }
//
//        // Build the menus that are shown when inserting.
//        } else {
//            menu.add(0, DISCARD_ID, 0, R.string.menu_discard)
//                    .setShortcut('0', 'd')
//                    .setIcon(android.R.drawable.ic_menu_delete);
//        }
//
//        // If we are working on a full note, then append to the
//        // menu items for any other activities that can do stuff with it
//        // as well.  This does a query on the system for any activities that
//        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
//        // for each one that is found.
//        if (!mNoteOnly) {
//            Intent intent = new Intent(null, getIntent().getData());
//            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
//            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
//                    new ComponentName(this, PlantsQuery.class), null, intent, 0, null);
//        }
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle all of the possible menu actions.
//        switch (item.getItemId()) {
//        case DELETE_ID:
//            deleteNote();
//            finish();
//            break;
//        case DISCARD_ID:
//            cancelNote();
//            break;
//        case REVERT_ID:
//            cancelNote();
//            break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    /**
//     * Take care of canceling work on a note.  Deletes the note if we
//     * had created it, otherwise reverts to the original text.
//     */
//    private final void cancelNote() {
//        if (mCursor != null) {
//            if (mState == STATE_EDIT) {
//                // Put the original note text back into the database
//                mCursor.close();
//                mCursor = null;
//                ContentValues values = new ContentValues();
//                values.put(Notes.NOTE, mOriginalContent);
//                getContentResolver().update(mUri, values, null, null);
//            } else if (mState == STATE_INSERT) {
//                // We inserted an empty note, make sure to delete it
//                deleteNote();
//            }
//        }
//        setResult(RESULT_CANCELED);
//        finish();
//    }
//
//    /**
//     * Take care of deleting a note.  Simply deletes the entry.
//     */
//    private final void deleteNote() {
//        if (mCursor != null) {
//            mCursor.close();
//            mCursor = null;
//            getContentResolver().delete(mUri, null, null);
//            mPlantNameText.setText("");
//        }
//    }
}
