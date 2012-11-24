package com.applang.provider;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ImpexTask extends AsyncTask<String, Void, Boolean> {
	
    private static final String TAG = ImpexTask.class.getSimpleName();
    
    public interface AsyncCallback{
        void onTaskCompleted();
    }
	/**
	 * 
	 * @param impex	if true then import else export
	 */
	public ImpexTask(Context context, String[] fileNames, boolean impex, AsyncCallback callback) {
		this.context = context;
		this.fileNames = fileNames;
		this.impex = impex;
		this.callback = callback;
		
		this.progress = new ProgressDialog(this.context);
	}
	
	Context context;
	String[] fileNames;
	boolean impex;
	AsyncCallback callback;
    
	public static void doImport(Context context, String[] fileNames, AsyncCallback callback) {
        decision(true, context, fileNames, callback);
	}
    
	public static void doExport(Context context, String[] fileNames, AsyncCallback callback) {
        decision(false, context, fileNames, callback);
	}

	private static void decision(final boolean flag, final Context context, final String[] fileNames, final AsyncCallback callback) {
		String action = flag ? "Import" : "Export";
        if (isExternalStorageAvailable()) {
    		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
    		alertDialogBuilder
				.setMessage(action + " : " + Arrays.toString(fileNames))
    			.setCancelable(false)
				.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,	int id) {
							if (Integer.parseInt(android.os.Build.VERSION.SDK) < 3) {
								boolean success = doImpex(context, fileNames, flag);
								message(success, context, flag);
							}
							else
								new ImpexTask(context, fileNames, flag, callback).execute();
							
							dialog.dismiss();
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
        else
			Toast.makeText(context, 
        			String.format("External storage is not available, unable to %s data.", action.toLowerCase()), 
        			Toast.LENGTH_SHORT).show();
	}
	
	public static boolean isExternalStorageAvailable() {
    	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
	
	public static File directory(Context context, boolean flag) {
		String dir = "data/" + context.getPackageName();
    	return flag ? 
    			new File(Environment.getDataDirectory(), dir) : 
    			new File(Environment.getExternalStorageDirectory(), dir);
    }
    
	public static boolean doImpex(Context context, String[] fileNames, boolean flag) {
		try {
			for (String fileName : fileNames) 
				doCopy(context, fileName, flag);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}
		
		return true;
    }
    
	public static boolean doCopy(Context context, String fileName, boolean flag) throws IOException {
		File importDir = directory(context, true);
		File exportDir = directory(context, false);
		
		File source, destination;
		if (flag) {
			source = new File(exportDir, fileName);
			destination = new File(importDir, fileName);
		}
		else {
			source = new File(importDir, fileName);
			destination = new File(exportDir, fileName);
		}
		if (!source.exists())
			return false;
		
		if (!flag && !destination.exists()) {
			destination.getParentFile().mkdirs();
			destination.createNewFile();
		}
		
		copyFile(source, destination);
		
		Log.i(TAG, String.format((flag ? "Import" : "Export") + "ed : '%s' to '%s'", source, destination));
		return true;
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
		this.progress.setMessage(String.format((this.impex ? "Import" : "Export") + "ing '%s' ...", Arrays.toString(this.fileNames)));
		this.progress.show();
	}
	
	// automatically done on worker thread (separate from UI thread)
	protected Boolean doInBackground(final String... args) {
		return doImpex(this.context, this.fileNames, this.impex);
	}
	
	// can use UI thread here
	protected void onPostExecute(final Boolean success) {
		if (this.progress.isShowing()) 
			this.progress.dismiss();
		
		message(success, this.context, this.impex);
		
		if (this.callback != null)
			this.callback.onTaskCompleted();
	}

	private static void message(boolean success, Context context, boolean flag) {
		String s = flag ? "Import" : "Export";
		if (success) 
			Toast.makeText(context, s + " successful !", Toast.LENGTH_SHORT).show();
		else 
			Toast.makeText(context, s + " failed !", Toast.LENGTH_SHORT).show();
	}
}

