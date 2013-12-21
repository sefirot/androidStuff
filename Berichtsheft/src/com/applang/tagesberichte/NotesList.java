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

import java.util.Locale;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link NotePadProvider}
 */
public class NotesList extends ListActivity
{
	private static final String TAG = NotesList.class.getSimpleName();
    
    private static String dateFormat = "EEEEE, dd.MM.yyyy";
    
    public static String formatDate(long time) {
    	return com.applang.Util.formatDate(time, dateFormat, Locale.getDefault());
    }

    private static final int MENU_ITEM_INSERT = Menu.FIRST;
    private static final int MENU_ITEM_DUPLICATE = Menu.FIRST + 1;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            NoteColumns._ID, // 0
            NoteColumns.TITLE, // 1
            NoteColumns.CREATED_DATE, 
    };

    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_CREATED = 2;
    
    public static String description(int tableIndex, long time, String title) {
		return tableIndex == 0 ? 
				String.format("%s '%s'", formatDate(time), title) : 
				title;
    }

    int tableIndex;
    String selection = "";
    String[] selectionArgs = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		setContentView(R.layout.noteslist);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NoteColumns.CONTENT_URI);
        }
        tableIndex = NotePadProvider.tableIndex(0, intent.getData());

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
        
        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Cursor cursor = managedQuery(intent.getData(), 
        		PROJECTION, 
        		selection, selectionArgs,
                tableIndex > 0 ? NoteColumns.TITLE_SORT_ORDER : NoteColumns.DEFAULT_SORT_ORDER);

        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
        		new int[] {R.layout.noteslist_item,R.layout.noteslist_item1,R.layout.noteslist_item2}[tableIndex], 
        		cursor,
                new String[] { NoteColumns.TITLE, NoteColumns.CREATED_DATE }, 
                new int[] { R.id.title, R.id.date })
        {
        	@Override
        	public void setViewText(TextView v, String text) {
        		switch (v.getId()) {
				case R.id.date:
					Long time = toLong(null, text);
					text = time == null ? "" : formatDate(time);

				default:
	        		super.setViewText(v, text);
				}
        	}
        };
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } 
        else {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new note into the list.
        menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_insert)
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final boolean haveItems = getListAdapter().getCount() > 0;
        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Build menu...  always starts with the EDIT action...
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];

            // ... is followed by whatever other actions are available...
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null, specifics, intent, 0,
                    items);

            // Give a shortcut to the edit action.
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // Launch activity to insert a new item
            Uri data = getIntent().getData();
			startActivity(new Intent(Intent.ACTION_INSERT, data));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
        
        description = "";

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        description = description(tableIndex, 
        		cursor.getLong(COLUMN_INDEX_CREATED), 
        		cursor.getString(COLUMN_INDEX_TITLE));
		menu.setHeaderTitle(description);
        
		getMenuInflater().inflate(R.menu.contextmenu_noteslist, menu);
		if (tableIndex > 0)
			menu.removeItem(R.id.menu_item_schlagwort);
    }
    
    String description;
        
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        final Uri uri = getIntent().getData();
		final Uri noteUri = ContentUris.withAppendedId(uri, info.id);
        
        switch (item.getItemId()) {
        case R.id.menu_item_schlagwort:
            startActivityForResult(
            	new Intent(TitleEditor.EDIT_TITLE_ACTION, noteUri)
					.putExtra("header", description)
					.putExtra("tableIndex", 2)
					.putExtra("state", NoteEditor.STATE_INSERT), Menu.NONE);
            return true;
            
/*		case R.id.menu_item_evaluate: 
			startActivity(new Intent()
					.setClass(this, NoteEvaluator.class)
					.setData(noteUri));
            return true;
*/            
        case R.id.menu_item_edit: 
			startActivity(new Intent()
					.setClass(this, TitleEditor.class)
					.setData(noteUri)
					.putExtra("state", NoteEditor.STATE_EDIT));
            return true;
            
        case R.id.menu_item_duplicate:
        	NotePadProvider.fetchNoteById(info.id, getContentResolver(), tableIndex, new Job<Cursor>() {
				@Override
				public void perform(Cursor c, Object[] params) throws Exception {
		            ContentValues values = new ContentValues();
		            values.put(NoteColumns.NOTE, c.getString(2));
		            
					Uri newNoteUri = getContentResolver().insert(uri, values);
					
					startActivityForResult(
						new Intent(TitleEditor.EDIT_TITLE_ACTION, newNoteUri)
							.putExtra("state", NoteEditor.STATE_INSERT)
							.putExtra("followUp", Intent.ACTION_EDIT), MENU_ITEM_DUPLICATE);
				}
        	});
            return true;
            
        case R.id.menu_item_delete: 
    		Long id = parseId(-1L, noteUri);
    		String description = NoteEditor.getNoteDescription(getContentResolver(), tableIndex, id);
    		description = getResources().getString(R.string.areUsure, description);
    		areUsure(this, description, new Job<Void>() {
				@Override
				public void perform(Void t, Object[] params) throws Exception {
		            getContentResolver().delete(noteUri, "", null);
		            if (tableIndex == 0)
		                getContentResolver().notifyChange(NotePadProvider.contentUri(2), null);
				}
    		});
            return true;
        }
        return false;
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent followUp) {
		switch (requestCode) {
		case MENU_ITEM_DUPLICATE:
			if (resultCode == RESULT_OK)
				startActivity(followUp);
			else
				getContentResolver().delete(followUp.getData(), "", null);
			break;

		}
	}
}
