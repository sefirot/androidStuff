package com.applang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import com.applang.Util.ValList;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import static com.applang.Util.*;

public class Util2
{
    private static final String TAG = Util2.class.getSimpleName();
    
    public static String resourcePackageName(Activity activity) {
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
	    	set_State(RUNNING);   
	    	
		    try {
		    	mJob.perform(mActivity, mParams);
			} catch (Exception e) {
	            Log.e(TAG, "doing job", e);
			}
	        
	        while (mHandler != null && get_State() == RUNNING) {
	            delay(40);
	            
	            Message msg = mHandler.obtainMessage();
	            Bundle b = new Bundle();
	            b.putInt("countDown", 0);
	            msg.setData(b);
	            mHandler.sendMessage(msg);
	        }
	    }
	    
	    int mState;
	    
	    public int get_State() {
			return mState;
		}

		// Set current state of thread (use state=ProgressThread.DONE to stop thread)
	    public void set_State(int state) {
	        mState = state;
	    }
	}

	private static WorkerThread workerThread = null;
	
	public static Dialog waitWhileWorking(final Activity activity, String text, Job<Activity> job, Object... params) {
        final ProgressDialog progDialog = new ProgressDialog(activity);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setMessage(text);
		workerThread = new WorkerThread(activity, job, 
			new Handler() {
				public void handleMessage(Message msg) {
					int countDown = msg.getData().getInt("countDown");
					progDialog.setProgress(countDown);
					if (countDown <= 0) {
						activity.dismissDialog(0);
						if (workerThread != null)
							workerThread.set_State(WorkerThread.DONE);
					}
				}
			}, params);
		workerThread.start();
		return progDialog;
	}
	
	public static boolean isExternalStorageAvailable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	private static int apiLevel() {
		return Integer.parseInt(android.os.Build.VERSION.SDK);
	}

	public static class ImpexTask extends AsyncTask<String, Void, Boolean>
	{
	    private static final String TAG = ImpexTask.class.getSimpleName();
	    
	    public interface AsyncCallback{
	        void onTaskCompleted();
	    }
		/**
		 * 
		 * @param export	if true then export else import
		 */
		public ImpexTask(Context context, String[] fileNames, boolean export, AsyncCallback callback) {
			this.context = context;
			this.fileNames = fileNames;
			this.export = export;
			this.callback = callback;
			
			this.progress = new ProgressDialog(this.context);
		}
		
		Context context;
		String[] fileNames;
		boolean export;
		AsyncCallback callback;
		
		public static String actionString(boolean flag) {
			return flag ? "Export" : "Import";
		}
	    
		public static void doImport(Context context, String[] fileNames, AsyncCallback callback) {
	        decision(false, context, fileNames, callback);
		}
	    
		public static void doExport(Context context, String[] fileNames, AsyncCallback callback) {
	        decision(true, context, fileNames, callback);
		}
	
		private static void decision(final boolean export, final Context context, final String[] fileNames, final AsyncCallback callback) {
			String action = actionString(export);
	        if (isExternalStorageAvailable()) {
        		areUsure(context, action + " : " + Arrays.toString(fileNames), new Job<Void>() {
    				public void perform(Void t, Object[] params) throws Exception {
						if (apiLevel() < 3) {
							boolean success = doImpex(context, fileNames, export);
							message(success, context, export);
						}
						else
							new ImpexTask(context, fileNames, export, callback).execute();
    				}
        		});
	        }
	        else
				Toast.makeText(context, 
	        			String.format("External storage is not available, unable to %s data.", action.toLowerCase()), 
	        			Toast.LENGTH_SHORT).show();
		}
		
		public static File directory(Context context, boolean export) {
			String dir = "data/" + context.getPackageName() + "/databases";
	    	return export ? 
	    			new File(Environment.getExternalStorageDirectory(), dir) : 
	    			new File(Environment.getDataDirectory(), dir);
	    }
	    
		public static boolean doImpex(Context context, String[] fileNames, boolean export) {
			try {
				for (String fileName : fileNames) 
					doCopy(context, fileName, export);
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
				return false;
			}
			
			return true;
	    }
	    
		public static boolean doCopy(Context context, String fileName, boolean export) throws IOException {
			File importDir = directory(context, false);
			File exportDir = directory(context, true);
			
			File source, destination;
			if (export) {
				source = new File(importDir, fileName);
				destination = new File(exportDir, fileName);
			}
			else {
				source = new File(exportDir, fileName);
				destination = new File(importDir, fileName);
			}
			if (!source.exists())
				return false;
			
			if (export && !destination.exists()) {
				destination.getParentFile().mkdirs();
				destination.createNewFile();
			}
			
			copyFile(source, destination);
			
			Log.i(TAG, String.format(actionString(export) + "ed : '%s' to '%s'", source, destination));
			return true;
		}
		
		public static void versionFile(File file) throws IOException {
			if (file.exists()) {
				String fileName = file.getName();
				int sep = fileName.lastIndexOf('.');
				if (sep < 0)
					sep = fileName.length();
				fileName = fileName.substring(0, sep) + '.' + now()
						+ fileName.substring(sep);
				file.renameTo(new File(file.getParentFile(), fileName));
			}
		}
		
		@SuppressWarnings("resource")
		public static void copyFile(File src, File dst) throws IOException {
			FileChannel inChannel = new FileInputStream(src).getChannel();
			FileChannel outChannel = new FileOutputStream(dst).getChannel();
			try {
				inChannel.transferTo(0, inChannel.size(), outChannel);
			} finally {
				if (inChannel != null)
					inChannel.close();
				if (outChannel != null)
				 	outChannel.close();
			}
		}
		
		private ProgressDialog progress = null;
	
		// can use UI thread here
		protected void onPreExecute() {
			this.progress.setMessage(String.format(actionString(this.export) + "ing '%s' ...", Arrays.toString(this.fileNames)));
			this.progress.show();
		}
		
		// automatically done on worker thread (separate from UI thread)
		protected Boolean doInBackground(final String... args) {
			return doImpex(this.context, this.fileNames, this.export);
		}
		
		// can use UI thread here
		protected void onPostExecute(final Boolean success) {
			if (this.progress.isShowing()) 
				this.progress.dismiss();
			
			message(success, this.context, this.export);
			
			if (this.callback != null)
				this.callback.onTaskCompleted();
		}
	
		private static void message(boolean success, Context context, boolean flag) {
			String s = actionString(flag);
			if (success) 
				Toast.makeText(context, s + " successful !", Toast.LENGTH_SHORT).show();
			else 
				Toast.makeText(context, s + " failed !", Toast.LENGTH_SHORT).show();
		}
	}

	public static void areUsure(Context context, String message, final Job<Void> job, final Object... params) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder
			.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,	int id) {
					dialog.dismiss();
					try {
						job.perform(null, params);
					} catch (Exception e) {
						Log.e(TAG, "areUsure", e);
					}
				}
			})
			.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	public static ValList listOfStrings(Cursor cursor) {
		ValList list = new ValList();
		for (int i = 0; i < cursor.getColumnCount(); i++)
			list.add(cursor.getString(i));
		return list;
	}
	
}
