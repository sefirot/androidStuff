package com.applang;

import com.applang.Util.Function;
import com.applang.Util.Job;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.applang.Util.*;

public class Util2
{
    private static final String TAG = Util2.class.getSimpleName();
    
    public static String packageName(Activity activity) {
    	//	needs : android.permission.GET_TASKS
    	String permission = "android.permission.GET_TASKS";
        int res = activity.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {
	        ActivityManager actMngr = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
	        return actMngr.getRunningTasks(1).get(0).topActivity.getPackageName();
        }
        else 
        	return "";
    }
	
	public static ValMap getResultMap(Cursor cursor, Function<String> key, Function<Object> value) {
		ValMap map = new ValMap();
		try {
	    	if (cursor.moveToFirst()) 
	    		do {
					String k = key.apply(cursor);
					Object v = value.apply(cursor);
					map.put(k, v);
	    		} while (cursor.moveToNext());
		} catch (Exception e) {
            Log.e(TAG, "traversing cursor", e);
			return null;
		}
		finally {
			cursor.close();
		}
		return map;
	}

	public static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread was Interrupted");
        }
	}

	public static class WorkerThread extends Thread
	{	
		// Class constants defining state of the thread
		public final static int DONE = 0, RUNNING = 1;
	    
	    Job<Activity> mJob;
	    Handler mHandler;
	    Object[] mParams;
	    
	    Activity mActivity;
		
	    public WorkerThread(Activity activity, Job<Activity> job, Handler handler, Object... params) {
	        mJob = job;
	        mHandler = handler;
	        mParams = params;
	    	mActivity = activity;
	    }
	    
	    @Override
	    public void run() {
	    	mState = RUNNING;   
	    	
		    try {
		    	mJob.dispatch(mActivity, mParams);
			} catch (Exception e) {
	            Log.e(TAG, "doing job", e);
			}
	        
	        while (mHandler != null && mState == RUNNING) {
	            delay(40);
	            
	            Message msg = mHandler.obtainMessage();
	            Bundle b = new Bundle();
	            b.putInt("countDown", 0);
	            msg.setData(b);
	            mHandler.sendMessage(msg);
	        }
	    }
	    
	    int mState;
	    
	    // Set current state of thread (use state=ProgressThread.DONE to stop thread)
	    public void setState(int state) {
	        mState = state;
	    }
	}

	private static WorkerThread workerThread = null;
	
	public static Dialog waitWhileWorking(final Activity activity, String text, Job<Activity> job, Object... params) {
        final ProgressDialog progDialog = new ProgressDialog(activity);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setMessage(text);
        workerThread = new WorkerThread(
        	activity, 
        	job, 
	        new Handler() {
	            public void handleMessage(Message msg) {
	                int countDown = msg.getData().getInt("countDown");
	                progDialog.setProgress(countDown);
	                if (countDown <= 0) {
	                	activity.dismissDialog(0);
	                    workerThread.setState(WorkerThread.DONE);
	                }
	            }
	        }, 
	        params
	    );
        workerThread.start();
        return progDialog;
	}
}
