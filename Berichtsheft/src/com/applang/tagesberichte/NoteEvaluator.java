package com.applang.tagesberichte;

import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.Notes;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

public class NoteEvaluator extends Activity
{
    private TextView tv;
    
    private Uri mUri;

    int table = 0;
    String note = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
    	tv = new TextView(this);
		tv.setTextSize(20);
		tv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		setContentView(tv);
    	
        Intent intent = getIntent();
		mUri = intent.getData();

        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("table"))
        	table = extras.getInt("table", 0);
    	
		Cursor cursor = managedQuery(mUri, 
				Notes.FULL_PROJECTION, 
				NotePadProvider.selection(table, ""), null, 
				null);
		if (cursor.moveToFirst()) {
			setTitle(NotesList.description(table, 
					cursor.getLong(3), 
					cursor.getString(1)));
			
			note = cursor.getString(2);
		}
		cursor.close();
		
		showDialog(0);
	}
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	return waitWhileWorking(this, "Evaluating ...",
    		new Job<Activity>() {
	    		public void dispatch(final Activity activity, Object[] params) throws Exception {
	    			setupVelocity4Android(packageName(activity), getResources());
	    			
	    			MapContext noteContext = new MapContext(bausteinMap(NoteEvaluator.this, ""));
	    			final String text = evaluation(noteContext, note, "notes");
	    			
	    			runOnUiThread(new Runnable() {
	    			     public void run() {
	    			    	 tv.setText(text);
	    			    }
	    			});
	    		}
	    	} 
	    );
    }

    public static ValMap bausteinMap(Activity activity, String selection, String... selectionArgs) {
		Cursor cursor = activity.managedQuery(
        		Notes.CONTENT_URI, 
        		new String[] { Notes.TITLE, Notes.NOTE }, 
        		NotePadProvider.selection(1, selection), selectionArgs,
        		null);
		
        ValMap map = getResultMap(cursor, 
        	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(0);
				}
	        }, 
        	new Function<Object>() {
				public Object apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					return cursor.getString(1);
				}
	        }
	    );
        
        return map;
    }

}
