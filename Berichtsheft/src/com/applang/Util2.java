package com.applang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

import com.applang.Util.ValList;

import dalvik.system.DexFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import static com.applang.Util.*;

public class Util2
{
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
	            Util.delay(40);
	            
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

	public static WorkerThread workerThread = null;
	
	public static Dialog waitWhileWorking(final Activity activity, String text, Job<Activity> job, Object... params) {
        final ProgressDialog progDialog = new ProgressDialog(activity);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setMessage(text);
        final Runnable followUp = param(null, 0, params);
		workerThread = new WorkerThread(activity, job, 
			new Handler() {
				public void handleMessage(Message msg) {
					int countDown = msg.getData().getInt("countDown");
					progDialog.setProgress(countDown);
					if (countDown <= 0) {
						activity.removeDialog(0);
						if (workerThread != null) {
							workerThread.set_State(WorkerThread.DONE);
							if (followUp != null)
								followUp.run();
						}
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

    @SuppressLint("NewApi")
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
	        			String.format("External storage is not available, unable to %s data.", 
	        					action.toLowerCase(Locale.US)), 
	        			Toast.LENGTH_SHORT).show();
		}
		
		public static File directory(String path, boolean export) {
	    	return export ? 
	    			new File(Environment.getExternalStorageDirectory(), path) : 
	    			new File(Environment.getDataDirectory(), path);
	    }
	    
		public static boolean doImpex(Context context, String[] fileNames, boolean export) {
			boolean retval = true;
			try {
				String path = getDatabasesPath(context);
				for (String fileName : fileNames) {
					File importDir = directory(path, false);
					File exportDir = directory(path, true);
					
					retval &= doCopy(fileName, export, importDir, exportDir);
				}
			} catch (IOException e) {
				Log.e(TAG, "doImpex", e);
				retval = false;
			}
			return retval;
	    }

		public static String getDatabasesPath(Context context) {
			return "data/" + context.getPackageName() + "/databases";
		}
	    
		public static boolean doAppex(String[] fileNames) {
			try {
				String path = "app";
				for (String fileName : fileNames) {
					File importDir = directory(path, false);
					File exportDir = directory(path, true);
					
					doCopy(fileName, true, importDir, exportDir);
				}
			} catch (IOException e) {
				Log.e(TAG, "doAppex", e);
				return false;
			}
			
			return true;
	    }
	    
		public static boolean doCopy(String fileName, boolean export, File importDir, File exportDir) throws IOException {
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

	public static void alert(Context context, String message) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder
			.setMessage(message)
			.setCancelable(false)
			.setNeutralButton("Close",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,	int id) {
					dialog.dismiss();
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
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

	public static void popupContextMenu(Activity activity, View view) {
		activity.registerForContextMenu(view);
		activity.openContextMenu(view);
		activity.unregisterForContextMenu(view);
	}
    
    @SuppressLint("NewApi")
	public static abstract class Task<Result> extends AsyncTask<Object, Intent, Result>
    {
		public Task(Activity activity, Job<Result> followUp, Object... params) {
			this.activity = activity;
			this.followUp = followUp;
			this.params = params;
		}

		protected Activity activity;
		
		protected Result doInBackground(Object...params) {
			Integer millis = param_Integer(null, 0, params);
			if (millis != null)
				delay(millis);
			return null;
		}

		@Override
		protected void onProgressUpdate(Intent... intents) {
			super.onProgressUpdate(intents);
			if (intents.length > 0 && activity != null) {
				Intent intent = intents[0];
				if (notNullOrEmpty(intent.getAction()))
					activity.startActivity(intent);
			}
		}

		@Override
		protected void onPostExecute(Result result) {
			super.onPostExecute(result);
			if (followUp != null) 
				try {
					followUp.perform(result, params);
				} catch (Exception e) {
					Log.e(TAG, "follow-up", e);
				}
		}
		
		private Job<Result> followUp;
		private Object[] params;
    }
	
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
	
//	NOTE	there is a different method with the same signature in Util2 for Java
	@SuppressWarnings("rawtypes")
	public static Class[] getLocalClasses(String packageName, Object...params) throws Exception {
		Activity activity = param(null, 0, params);
	   	ValList list = vlist();
        DexFile df = new DexFile(activity.getPackageCodePath());
        for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
        	String entry = iter.nextElement();
			if (entry.startsWith(packageName))
				list.add(Class.forName(entry));
        }
        return arraycast(list.toArray(), new Class[0]);
	}
}
