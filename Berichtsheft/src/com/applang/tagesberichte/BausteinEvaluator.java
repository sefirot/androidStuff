package com.applang.tagesberichte;

import com.applang.UserContext;
import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util1.*;

public class BausteinEvaluator extends Activity
{
	private static final String TAG = BausteinEvaluator.class.getSimpleName();
	
	public static final String EVALUATE_ACTION = "com.applang.tagesberichte.action.EVALUATE";
	
    private TextView mTv;
    private ProgressBar mProgress;
    
    private Handler mHandler = new Handler();
    
    Job<Void> progressJob = new Job<Void>() {
    	public void perform(Void t, Object[] params) {
    		int percent = ((Integer)params[0]).intValue();
    		mProgress.setProgress(percent);
    	}
    };

    private Uri mUri;

    int tableIndex;
    String note = "";
    
    ValMap bausteine = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
		setContentView(R.layout.evaluator_view);
    	
    	mTv = (TextView)findViewById(R.id.textView1);
		mTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mProgress = (ProgressBar)findViewById(R.id.progressBar1);
		
        Intent intent = getIntent();
		mUri = intent.getData();
        tableIndex = NotePadProvider.tableIndex(0, mUri);
        
		String selection = "";
		String[] selectionArgs = null;
		if (parseId(-1L, mUri) < 0 && NoteColumns.TITLE.equals(mUri.getPathSegments().get(1))) {
			selection = NoteColumns.TITLE + "=?";
			selectionArgs = new String[] {mUri.getPathSegments().get(2)};
			mUri = NotePadProvider.contentUri(1);
		}
		Cursor cursor = managedQuery(mUri, 
				NotePadProvider.FULL_PROJECTION, 
				selection, selectionArgs, 
				null);
		if (cursor.moveToFirst()) {
			setTitle(NotesList.description(tableIndex, 
					cursor.getLong(3), 
					cursor.getString(1)));
			
			note = cursor.getString(2);
		}
		cursor.close();
		
		bausteine = NotePadProvider.bausteinMap(getContentResolver(), "");
		
		new UserContext.EvaluationTask(this, 
			bausteine,
			mHandler, 
			progressJob, 
			new Job<Object>() {
				public void perform(Object text, Object[] params) {
					mProgress.setVisibility(View.GONE);
			    	mTv.setText(text.toString());
				}
			}).execute(note, this.getString(R.string.title_evaluator));
    }
		
//		test(mHandler);
//		showDialog(0);
/*
    void test(final Handler progressHandler) {
        final int TIMER_RUNTIME = 10000;

        final Thread timerThread = new Thread() {
            @Override
            public void run() {
                try {
                    int waited = 0;
                    while(waited < TIMER_RUNTIME) {
                        sleep(100);
                            waited += 200;
                                // Ignore rounding error here
                                int progress = mProgress.getMax() * waited / TIMER_RUNTIME;
								final Object[] params = new Object[]{progress};
								progressHandler.post(new Runnable() {
			                        public void run() {
			                        	try {
											progressJob.perform(null, params);
										} catch (Exception e) {
										}
			                        }
			                    });
                    }
  	          } catch(InterruptedException e) {
  	              // do nothing
  	          } finally {
  	          }
          }
        };
        timerThread.start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
    	return waitWhileWorking(this, "Evaluating ...",
    		new Job<Activity>() {
	    		public void perform(final Activity activity, Object[] params) throws Exception {
	    			com.applang.UserContext.setupVelocity(activity, true);
	    			
					MapContext noteContext = new MapContext(bausteine);
	    			final String text = evaluation(noteContext, note, "notes");
	    			
	    			runOnUiThread(new Runnable() {
	    				public void run() {
							mTv.setText(text);
	    			    }
	    			});
	    		}
	    	} 
	    );
    }
*/
}
