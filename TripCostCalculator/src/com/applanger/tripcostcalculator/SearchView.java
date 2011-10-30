package com.applanger.tripcostcalculator;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class SearchView extends ListActivity
 {

    // This is the Adapter being used to display the list's data.
    SimpleCursorAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    private NotesDbAdapter mDbHelper;
    private Cursor notesCursor;
    public ArrayList<String> queryStrings;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notes_list);
        /** fieldinfo: opening of database via member field 'mDbHelper' = declared constructor in NotesDbAdapter class 
         *  parameterinfo: the context field "this" communicates with 'android os'**/
    	mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
        
        // following EDitText  will be substituted by Textview for Contents and Buttons for QueryID 
        EditText mQueryId = (EditText) findViewById(R.id.QueryID);
        EditText mContentSText = (EditText) findViewById(R.id.contentS);
        EditText mContentPText = (EditText) findViewById(R.id.contentP);
        EditText mContentRText = (EditText) findViewById(R.id.contentR);
        EditText mContentDminText = (EditText) findViewById(R.id.contentDmin);
        EditText mContentDmaxText = (EditText) findViewById(R.id.contentDmax); 
        
        String queryIdString = mQueryId.getText().toString(); 
        Bundle queryBundle;
        int CountQuerySelect = queryIdString.length() + 1;
        int i;
        char S,P,R,D;
        
        
        for (i= 0; i < CountQuerySelect;i++)
        	
        	switch(queryIdString.charAt(i)){
        	case S : 	
        		queryBundle.putString(NotesDbAdapter.KEY_SUBMITTER,mContentSText.getText().toString()); 
        	break;
        	case P : 	
        		queryBundle.putString(NotesDbAdapter.KEY_PURPOSE,mContentPText.getText().toString()); 
        	break;
        	case R : 	
        		queryBundle.putString(NotesDbAdapter.KEY_RECEIVER,mContentRText.getText().toString()); 
        	break;
        	case D : 	
        		queryBundle.putString(NotesDbAdapter.KEY_DATE,mContentDminText.getText().toString()); 
        		queryBundle.putString(NotesDbAdapter.KEY_DATEMAX,mContentDmaxText.getText().toString()); 
        	break; 
        	
        }
      
        
        
      if(queryBundle.size() != CountQuerySelect) {
    	onCreate(savedInstanceState);
      }else {
    	fillQuery(queryBundle, queryIdString);  
      };
      
        
    }

    private void fillQuery(Bundle queryBundle, String queryIdString) {
        // Get all of the rows from the database and create the item list
        notesCursor = mDbHelper.fetchQueryResultSets(queryBundle, queryIdString);
        startManagingCursor(notesCursor);
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{select1, NotesDbAdapter.KEY_SUBMITTER, NotesDbAdapter.KEY_AMOUNT, NotesDbAdapter.KEY_RECEIVERS, NotesDbAdapter.KEY_PURPOSE};
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.date, R.id.submitter,R.id.amount,R.id.receivers, R.id.purpose};
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
            new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        
        setListAdapter(notes);
       
    }
    
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Place an action bar item for searching.
        MenuItem item = menu.add("Search");
        item.setIcon(android.R.drawable.ic_menu_search);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        SearchView sv = new SearchView(getActivity());
        sv.setOnQueryTextListener(this);
        item.setActionView(sv);
    }

    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        // Don't care about this.
        return true;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        // Insert desired behavior here.
        Log.i("FragmentComplexList", "Item clicked: " + id);
    }

    // These are the Contacts rows that we will retrieve.
    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME,
        Contacts.CONTACT_STATUS,
        Contacts.CONTACT_PRESENCE,
        Contacts.PHOTO_ID,
        Contacts.LOOKUP_KEY,
    };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri;
        if (mCurFilter != null) {
            baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                    Uri.encode(mCurFilter));
        } else {
            baseUri = Contacts.CONTENT_URI;
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                + Contacts.HAS_PHONE_NUMBER + "=1) AND ("
                + Contacts.DISPLAY_NAME + " != '' ))";
        return new CursorLoader(getActivity(), baseUri,
                CONTACTS_SUMMARY_PROJECTION, select, null,
                Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

}
