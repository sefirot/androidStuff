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



/** niklas documented this file according to notepad tutorial exercise 3 */ 



package com.android.tripcostcalculator;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
/** classinfo: "Bundle"  maps contents from string value to parcelable types*/
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;



/** 'ListActivity' is subclass of superclass 'Activity'    
 *  info: type 'ctrl+shift+0' to automatically import subclasses ListActivity + its interfaces 'Menu','MenuItem' & 'Bundle'*/ 
public class Notepadv3 extends ListActivity {
    
	private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    /** 'Menu.FIRST' as constant menu positions (> short & long touch on screen)**/
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    /** 'mDbHelper' is a field of superclass NotesDbAdapter **/
    private NotesDbAdapter mDbHelper;
    /** locally created field 'notesCursor' (instead of 'mNotesCursor' = member field as android standard)*/
    private Cursor notesCursor;

    
    
    /* Called when the activity is first created.*/
    @Override
    /** stubbinfo: opens the database (note access) & populate the list to view**/
    /**  methodinfo: 'onCreate' = initial part of activities' lifecyle to set up resources and state of activity*/   
    public void onCreate(Bundle savedInstanceState) {
        /** methodinfo: 'super' overrides same method in superclass**/
    	super.onCreate(savedInstanceState);
        /** methodinfo: calls the layout resource 'note_list'**/
    	setContentView(R.layout.notes_list);
        /** fieldinfo: opening of database via member field 'mDbHelper' = declared constructor in NotesDbAdapter class 
         *  parameterinfo: the context field "this" communicates with 'android os'**/
    	mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

    
    
    private void fillData() {
        // Get all of the rows from the database and create the item list
        notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{NotesDbAdapter.KEY_TITLE, NotesDbAdapter.KEY_BODY};
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1, R.id.text2};
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
            new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        
        setListAdapter(notes);
       
    }

    
    
    @Override
    /**stubbinfo: create add-button from click on menu & enable it to return boolean value 'INSERT_ID' **/
    public boolean onCreateOptionsMenu(Menu menu) {
        /**method info: overrides method & creates 'options menu' with item 'menu' signalling data input */
    	super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        return true;
    }

    @Override
    /** stubbinfo: if boolean 'INSERT_ID' = true then implement method 'createNote' */
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case INSERT_ID:
                createNote();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    
    
    @Override
    /**  stubbinfo: overrides method & create a 'context menu' to create menu-button "delete" upon view 'v' => long click*/
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }
  
    @Override
    /** stubbinfo: if boolean 'DELETE_ID' returns true then implement method 'deleteNote' in line with item id*/
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteNote(info.id);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    
    
    
    /**methodinfo: combined methods 'startActivityForResult' & 'onResultActivity'(see stubbs below) are asynchronous remote procedure call (RPG) 
     * > the recommended way of programming activity sequences in android*/
    
    /**stubbinfo: pass Intent invocation 'i' to start creating activity in 'NoteEdit' **/
    private void createNote() {
        Intent i = new Intent(this, NoteEdit.class);
        /** methodinfo: passed extra information to Intent 'i' is null 
         *  & passed 'requestCode' to 'onActivityResult' is 'ACTIVITY_CREATE' **/ 
        startActivityForResult(i, ACTIVITY_CREATE);
    }
  
    /** stubbinfo: fire Intent invocation 'i' on item click to start editing activity 'NoteEdit' **/
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, NoteEdit.class);
        i.putExtra(NotesDbAdapter.KEY_ROWID, id);
        /** methodinfo: passed extra information to Intent 'i' is "KEY_ROWID 
         *  & passed 'requestCode' to 'onActivityResult' is 'ACTIVITY_EDIT' **/ 
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    /** stubbinfo: before date is filled back to data base the life cycle in 'NoteEdit' is proceeded **/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
}
