package android.app;

import java.io.File;
import java.lang.reflect.Method;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import static com.applang.Util.*;

public class Activity extends Context
{
	protected static final String TAG = Activity.class.getSimpleName();
	
	public static ValMap activities = new ValMap();
	static {
		activities.put("com.applang.action.PROMPT", "com.applang.Dialogs");
		activities.put("com.applang.action.CONSTRUCT", "com.applang.ConstructDialogs");
	}
	
	public void startActivity(Intent intent) {
		String action = intent.getAction();
		if (activities.containsKey(action)) {
			try {
				Class<?> c = Class.forName(activities.get(action).toString());
				Object inst = c.newInstance();
				Method method = c.getMethod("setIntent", Intent.class);
				method.invoke(inst, intent);
				method = c.getMethod("create");
				method.invoke(inst);
			} catch (Exception e) {
				Log.e(TAG, "startActivity", e);
			}
		}
	}
	
	public void startActivityForResult (Intent intent, int requestCode) {
		mRequestCode = requestCode;
		startActivity(intent);
	}
	
	public Integer mRequestCode = null;
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
	
	private Intent intent = null;
    
    public Intent getIntent() {
		return intent;
    }
    
    public void setIntent(Intent intent) {
    	this.intent = intent;
    }

	protected void onCreate(Bundle savedInstanceState) {
	}
	
	protected void onDestroy()  {
	}
    
    @SuppressWarnings("deprecation")
	public final void showDialog(int id) {
    	dlg = onCreateDialog(id);
    	if (dlg != null)
    		dlg.show();
    }
    
    private Dialog dlg = null;
	
	protected Dialog onCreateDialog(int id) {
		return null;
	}

	protected void onPrepareDialog(int id, android.app.Dialog dialog) {
	}

    /** Standard activity result: operation canceled. */
    public static final int RESULT_CANCELED    = 0;
    /** Standard activity result: operation succeeded. */
    public static final int RESULT_OK           = -1;
    /** Start of user-defined activity results. */
    public static final int RESULT_FIRST_USER   = 1;

    public final void setResult(int resultCode) {
		this.mResultCode = resultCode;
		this.mResultData = null;
    }

    public final void setResult(int resultCode, Intent data) {
		this.mResultCode = resultCode;
		this.mResultData = data;
    }
    
    public int mResultCode = RESULT_CANCELED;
    public Intent mResultData = null;
    
    public void finish() {
		try {
			onDestroy();
			finalize();
		} catch (Throwable e) {
			Log.e(TAG, "finish", e);
		}
    }

	public Resources getResources() {
		// TODO Auto-generated method stub
		return null;
	}

	public ContentResolver getContentResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	public Cursor managedQuery(Uri uri, String[] values, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] fileList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getDir(String name, int mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getDatabasePath(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] databaseList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteDatabase(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
}
