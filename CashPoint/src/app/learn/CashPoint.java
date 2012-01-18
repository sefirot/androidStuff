package app.learn;

import java.io.*;
import java.nio.channels.FileChannel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.*;
import android.widget.*;

public class CashPoint extends Activity {
    /** Called when the activity is first created. */
    
	private Button exportButton;
	private Button importButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
  
        setContentView(R.layout.main);

        this.importButton = (Button)this.findViewById(R.id.importButton);
        this.importButton.setOnClickListener(new OnClickListener() {
        	public void onClick(final View v) {
        		doImpex(true);
        	}
        });

        this.exportButton = (Button)this.findViewById(R.id.exportButton);
        this.exportButton.setOnClickListener(new OnClickListener() {
        	public void onClick(final View v) {
        		doImpex(false);
        	}
        });
    }
    
	void doImpex(boolean flag) {
		String s = flag ? "import" : "export";
        if (isExternalStorageAvailable()) {
        	new ImpexTask(flag).execute();
        } else {
        	Toast.makeText(CashPoint.this, 
        			String.format("External storage is not available, unable to %s data.", s), 
        			Toast.LENGTH_SHORT).show();
        }
	}
	
    private boolean isExternalStorageAvailable() {
    	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
    
    private class ImpexTask extends AsyncTask<String, Void, Boolean> {
    	/**
    	 * 
    	 * @param impex	if true then import else export
    	 */
    	public ImpexTask(boolean impex) {
    		this.impex = impex;
    	}
    	
    	boolean impex;
		
		String dbName = DbAdapter.DATABASE_NAME;
    	
    	private final ProgressDialog dialog = new ProgressDialog(CashPoint.this);
  
		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage(String.format(impex ? "Importing '%s' ..." : "Exporting '%s' ...", this.dbName));
			this.dialog.show();
		}
		
		// automatically done on worker thread (separate from UI thread)
		protected Boolean doInBackground(final String... args) {
			String path = "data/app.learn/databases";
			File importDir = new File(Environment.getDataDirectory(), path);
			File exportDir = new File(Environment.getExternalStorageDirectory(), path);
			
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
				Resources res = CashPoint.this.getResources();
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
				Toast.makeText(CashPoint.this, s + " successful !", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(CashPoint.this, s + " failed !", Toast.LENGTH_SHORT).show();
			}
		}
    }

}