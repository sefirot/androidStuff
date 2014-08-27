package android.app;

import java.lang.reflect.Method;

import javax.swing.JFrame;

import com.applang.SwingUtil.Behavior;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import static com.applang.Util.*;

public class Activity extends Context
{
	protected static final String TAG = Activity.class.getSimpleName();
	
	public static ValMap activities = vmap(
		"com.applang.action.PROMPT", "com.applang.Dialogs", 
		"com.applang.action.CONSTRUCT", "com.applang.ConstructDialogs"
	);

	public static Activity getInstance() {
		return new Activity();
	}
	
	private Activity mParent = null;
	
	public Activity getParent() {
		return mParent;
	}

	public void startActivity(Intent intent) {
		String action = intent.getAction();
		if (activities.containsKey(action)) {
			try {
				Class<?> c = Class.forName(activities.get(action).toString());
				Object inst = c.newInstance();
				Method method = c.getMethod("setPackageInfo", String.class, Object[].class);
				method.invoke(inst, null, objects(this.mPackageInfo));
				method = c.getMethod("setIntent", Intent.class);
				method.invoke(inst, intent);
				setPrivateField(Activity.class, inst, "mParent", this);
				setPrivateField(Activity.class, inst, "mRequestCode", mRequestCode);
				method = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
				method.invoke(inst, _null());
			} catch (Exception e) {
				Log.e(TAG, "startActivity", e);
			}
		}
	}
	
	public void startActivityForResult (Intent intent, int requestCode) {
		mRequestCode = requestCode;
		startActivity(intent);
	}
	
	private Integer mRequestCode = null;
	
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
    
    public Dialog dialog = null;
	
	public final void showDialog(int id) {
		AlertDialog.behavior = Behavior.setModal(AlertDialog.behavior, id / 100 < 1);
    	dialog = onCreateDialog(id);
    	if (dialog != null) {
    		dialog.open();
    		if (dialog.isModal()) {
    			setResult(dialog.result != null ? RESULT_OK : RESULT_CANCELED, intent);
    			Activity ancestor = getParent();
    			while (ancestor != null) {
    				try {
						Method method = ancestor.getClass().getDeclaredMethod("onActivityResult", 
								Integer.TYPE, Integer.TYPE, Intent.class);
						if (method != null) {
							ancestor.onActivityResult(mRequestCode, mResultCode, mResultData);
							break;
						}
					} catch (Exception e) {
						Log.e(TAG, "showDialog", e);
					}
    				ancestor = ancestor.getParent();
    			}
    		}
    	}
    }
    
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

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// handles KEYCODE_BACK to stop the activity and go back.
		return true;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// handles KEYCODE_BACK to stop the activity and go back.
		return true;
	}

	public static JFrame frame = null;
	
	private ViewGroup viewGroup = new ViewGroup(this);
	
	public void setContentView(View view) {
		viewGroup.setId(android.R.id.content);
		viewGroup.addView(view, null);
	}

	public View findViewById(int id) {
		return viewGroup.findViewById(id);
    }
}
