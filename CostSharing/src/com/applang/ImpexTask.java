package com.applang;

import java.io.*;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.content.res.Resources;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ImpexTask extends AsyncTask<String, Void, Boolean> {
	/**
	 * 
	 * @param impex	if true then import else export
	 */
	public ImpexTask(Context context, boolean impex) {
		this.context = context;
		this.impex = impex;
	}
	
	Context context;
	boolean impex;
    
	public static void doImpex(Context context, boolean flag) {
		String s = flag ? "import" : "export";
        if (isExternalStorageAvailable()) {
        	new ImpexTask(context, flag).execute();
        } else {
        	Toast.makeText(context, 
        			String.format("External storage is not available, unable to %s data.", s), 
        			Toast.LENGTH_SHORT).show();
        }
	}
	
	public static boolean isExternalStorageAvailable() {
    	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
	
	String dbName = Util.databaseName();
	String dbDir = Util.pathToDatabases();
	
	private final ProgressDialog dialog = new ProgressDialog(this.context);

	// can use UI thread here
	protected void onPreExecute() {
		this.dialog.setMessage(String.format(impex ? "Importing '%s' ..." : "Exporting '%s' ...", this.dbName));
		this.dialog.show();
	}
	
	// automatically done on worker thread (separate from UI thread)
	protected Boolean doInBackground(final String... args) {
		File importDir = new File(Environment.getDataDirectory(), dbDir);
		File exportDir = new File(Environment.getExternalStorageDirectory(), dbDir);
		
		if (!exportDir.exists()) 
			exportDir.mkdirs();
		
		try {
			File source, destination;
			if (impex) {
				source = new File(exportDir, this.dbName);
				destination = new File(importDir, this.dbName);
			}
			else {
				source = new File(importDir, this.dbName);
				destination = new File(exportDir, this.dbName);
			}
			if (!source.exists())
				return false;
			
			if (!impex && !destination.exists()) 
				destination.createNewFile();
			
			this.copyFile(source, destination);
		} catch (IOException e) {
			Resources res = this.context.getResources();
			Log.e(res.getString(R.string.app_name), e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	void copyFile(File src, File dst) throws IOException {
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
	
	// can use UI thread here
	protected void onPostExecute(final Boolean success) {
		if (this.dialog.isShowing()) {
			this.dialog.dismiss();
		}
		
		String s = impex ? "Import" : "Export";
		if (success) {
			Toast.makeText(this.context, s + " successful !", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this.context, s + " failed !", Toast.LENGTH_SHORT).show();
		}
	}
}

