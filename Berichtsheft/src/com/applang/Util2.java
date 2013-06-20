package com.applang;

import static com.applang.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
import android.content.pm.ProviderInfo;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class Util2
{
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
    
    public static ValList contentAuthorities(Context context, String startsWith) {
    	ValList list = new ValList();
    	List<ProviderInfo> providers = context.getPackageManager().queryContentProviders(null, 0, 0);
        for (ProviderInfo provider : providers) {
            String authority = provider.authority;
            if (authority.startsWith(startsWith))
            	list.add(authority);
        }
        return list;
    }
    
    public static String[] databases(Context context) {
    	ArrayList<String> list = new ArrayList<String>();
    	for (Object authority : contentAuthorities(context, "com.applang")) 
    		try {
    			Class<?> c = Class.forName(authority.toString() + "Provider");
    			Object name = c.getDeclaredField("DATABASE_NAME").get(null);
    			list.add(name.toString());
    		} catch (Exception e) {};
    	return list.toArray(new String[0]);
    }
	
	public static String readAsset(Activity activity, String fileName) {
        StringBuffer sb = new StringBuffer();
        try {
        	AssetManager am = activity.getResources().getAssets();
			InputStream is = am.open( fileName );
			while( true ) {
			    int c = is.read();
	            if( c < 0 )
	                break;
			    sb.append( (char)c );
			}
		} catch (Exception e) {
			Log.e(TAG, "readAsset", e);
		}
        return sb.toString();
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

	public static ValList getRecord(Cursor cursor) {
		ValList list = new ValList();
		for (int i = 0; i < cursor.getColumnCount(); i++)
			list.add(cursor.getString(i));
		return list;
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

	public static LinearLayout linearLayout(Context context, int orientation, int width, int height) {
        LinearLayout linear = new LinearLayout(context);
        linear.setOrientation(orientation);
		linear.setLayoutParams(new LayoutParams(width, height));
		return linear;
	}

	public static LinearLayout.LayoutParams linearLayoutParams(int width, int height, Integer... ltrb) {
    	LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
		layoutParams.setMargins(param(0, 0, ltrb), param(0, 1, ltrb), param(0, 2, ltrb), param(0, 3, ltrb));
		return layoutParams;
	}

	public static RelativeLayout.LayoutParams relativeLayoutParams(int width, int height, Integer... ltrb) {
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
		layoutParams.setMargins(param(0, 0, ltrb), param(0, 1, ltrb), param(0, 2, ltrb), param(0, 3, ltrb));
		return layoutParams;
	}

	public static RelativeLayout relativeLayout(Context context, int width, int height) {
		RelativeLayout relative = new RelativeLayout(context);
        relative.setLayoutParams(new LayoutParams(width, height));
		return relative;
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
			Integer millis = paramInteger(null, 0, params);
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
    
    public static View getContentView(Activity activity) {
    	View rootView = activity.findViewById(android.R.id.content);
		ViewGroup viewGroup = (ViewGroup)rootView;
		return viewGroup.getChildAt(0);
    }
	
	public static Object[] iterateViews(ViewGroup container, Function<Object[]> func, int indent, Object... params) {
		if (container != null) {
			for (int i = 0; i < container.getChildCount(); i++) {
				View v = container.getChildAt(i);
				params = func.apply(v, indent, params);
				if (v instanceof ViewGroup) {
					iterateViews((ViewGroup) v, func, indent + 1, params);
				}
			}
		}
		return params;
	}
	
	public static String viewHierarchy(Activity activity) {
		Object[] params = iterateViews((ViewGroup)activity.findViewById(android.R.id.content), 
				new Function<Object[]>() {
					public Object[] apply(Object... params) {
						View v = param(null, 0, params);
						int indent = paramInteger(null, 1, params);
						Object[] parms = param(null, 2, params);
						String s = (String) parms[0];
						String line = /*v.getId() + " : " + */v.getClass().getSimpleName();
						s += indentedLine(line, TAB, indent);
						parms[0] = s;
						return parms;
					}
				}, 
				0, 
				new Object[] {""});
		return paramString("", 0, params);
	}
	
}