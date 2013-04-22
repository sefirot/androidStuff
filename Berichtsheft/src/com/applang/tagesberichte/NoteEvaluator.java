package com.applang.tagesberichte;

import com.applang.Util.ValMap;
import com.applang.VelocityContext;
import com.applang.Util.Job;
import com.applang.berichtsheft.R;
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

    int tableIndex;
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
        tableIndex = NotePadProvider.tableIndex(0, mUri);
    	
		Cursor cursor = managedQuery(mUri, 
				Notes.FULL_PROJECTION, 
				"", null, 
				null);
		if (cursor.moveToFirst()) {
			setTitle(NotesList.description(tableIndex, 
					cursor.getLong(3), 
					cursor.getString(1)));
			
			note = cursor.getString(2);
		}
		cursor.close();
		
		bausteine = NotePadProvider.bausteinMap(getContentResolver(), "");
		
//		new VelocityContext.EvaluationTask(this, 
//			getResources().getString(R.string.title_evaluator), 
//			bausteine, 
//			new Job<String>() {
//				public void perform(String text, Object[] params) {
//			    	 tv.setText(text);
//				}
//			}).execute(note);
		showDialog(0);
	}
    
    ValMap bausteine = null;

    @Override
    protected Dialog onCreateDialog(int id) {
    	return waitWhileWorking(this, "Evaluating ...",
    		new Job<Activity>() {
	    		public void perform(final Activity activity, Object[] params) throws Exception {
	    			com.applang.VelocityContext.setupVelocity(activity, true);
	    			
					MapContext noteContext = new MapContext(bausteine);
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

}
