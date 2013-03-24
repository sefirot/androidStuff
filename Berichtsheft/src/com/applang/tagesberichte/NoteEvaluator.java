package com.applang.tagesberichte;

import com.applang.Util.Function;
import com.applang.Util.ValMap;
import com.applang.VelocityUtil.MapContext;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.Notes;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

public class NoteEvaluator extends Activity implements View.OnClickListener
{
    private static final String TAG = NoteEvaluator.class.getSimpleName();
    
    private Uri mUri;
    private Cursor mCursor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        mUri = getIntent().getData();
        
        MapContext noteContext = new MapContext(bausteinMap(""));
        setupVelocity4Android("com.applang.berichtsheft", getResources());
        
        String text = mUri.toString();
    	mCursor = managedQuery(mUri, 
    			Notes.FULL_PROJECTION, 
    			NotePadProvider.selection(0, ""), null, 
    			null);
    	if (mCursor.moveToFirst()) {
    		setTitle(String.format("%s '%s'", 
    				NotesList.formatDate(mCursor.getLong(3)), 
    				mCursor.getString(1)));
    		
			String note = mCursor.getString(2);
			text = evaluation(noteContext, note, "notes");
    	}
        
        TextView tv = new TextView(this);
		tv.setText(text);
		tv.setTextSize(20);
		tv.setOnClickListener(this);
        
		setContentView(tv);
	}

	@Override
	public void onClick(View v) {
		finish();
	}

    public ValMap bausteinMap(String selection, String... selectionArgs) {
		Cursor cursor = managedQuery(
        		Notes.CONTENT_URI, 
        		new String[] { Notes.TITLE, Notes.NOTE }, 
        		NotePadProvider.selection(1, selection), selectionArgs,
        		Notes.DEFAULT_SORT_ORDER);
		
        ValMap map = getResultMap(cursor, 
        	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(0);
				}
	        }, 
        	new Function<Object>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(1);
				}
	        }
	    );
        
        return map;
    }

}
