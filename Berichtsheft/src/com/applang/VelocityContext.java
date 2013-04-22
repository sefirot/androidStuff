package com.applang;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.tools.generic.MathTool;

import com.applang.Util.Job;
import com.applang.Util.ValMap;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class VelocityContext extends MapContext implements Serializable
{
	public VelocityContext() {
		super(new ValMap());
	}

	public VelocityContext(ValMap map) {
		super(map);
	}

	@Override
	public String toString() {
		return map.toString();
	}

	private static final long serialVersionUID = 1704932233683596747L;
	private static final String TAG = VelocityContext.class.getSimpleName();
	
	public static final String[] USER_DIRECTIVES = {
		"com.applang.PromptDirective", 
  		"com.applang.SpinnerDirective",
  		"com.applang.CursorDirective",
  	};
    
    public static void setupVelocity(Activity activity, boolean force, Object... params) {
    	String logClassname = "com.applang.VelocityLogger";
		if (!force && logClassname.equals(Velocity.getProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS)))
    		return;
    	
    	String packageName = param(resourcePackageName(activity), 0, params);
  		Velocity.setProperty("packageName", packageName);
  		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, logClassname);
  		Velocity.setProperty(Velocity.RESOURCE_LOADER, "android");
  		Velocity.setProperty("android.resource.loader.class", "com.applang.VelocityResourceLoader");
  		Velocity.setProperty("android.content.res.Resources", activity.getResources());
  		
  		Velocity.setProperty("userdirective", join(",", USER_DIRECTIVES));
  		
  		Velocity.init();
    }
    
    public static Map<String,String> directives() {
    	Map<String,String> map = VelocityUtil.directives();
    	for (String directive : USER_DIRECTIVES) 
			try {
				Class<?> c = Class.forName(directive);
				Object inst = c.newInstance();
				String name = (String) c.getMethod("getName").invoke(inst);
				Method m = c.getMethod("signature", Boolean.TYPE);
				String value = (String) m.invoke(inst, true);
				map.put(name, value);
			} catch (Exception e) {
				Log.e(TAG, "directives", e);
			}
    	return map;
    }
    
    public static class EvaluationTask extends AsyncTask<String, Intent, String>
    {
		public EvaluationTask(Activity activity, String title, ValMap refMap, Job<String> followUp) {
    		this.activity = activity;
    		this.title = title;
			setupVelocity(activity, true);
    		this.velocityContext = new VelocityContext(refMap, this);
    		this.followUp = followUp;
    	}

		private Activity activity;
		private String title;
    	
    	public void doInForeground(Intent intent) {
    		publishProgress(intent);
		}
    	
    	@Override
		protected void onPreExecute() {
    		super.onPreExecute();
    		if (title.length() > 0) {
    		    EventCartridge ec = new EventCartridge();
    		    ec.addEventHandler(new ReferenceInsertionEventHandler() {
					@Override
					public Object referenceInsert(String reference, Object data) {
						if (refs != null) {
							int pos = refs.indexOf(reference);
							if (pos > -1)
								refs.remove(pos);
							if (size > 0) {
								float percent = Dialogs.MAX_PROGRESS;
								percent *= Float.valueOf(size - refs.size()) / size;
								publishProgress(new Intent()
										.putExtra("percent", percent)
										.putExtra("title", String.format("%s : %s", title, reference)));
							}
						}
						return data;
					}
    		    });
    		    ec.attachToContext(velocityContext);
    		    
    		    activity.startActivity(new Intent(Dialogs.PROMPT_ACTION)
    		    		.putExtra("prompt", title)
						.putExtra("type", Dialogs.DIALOG_PROGRESS));
    		}
		}
    	
    	public VelocityContext velocityContext = null;
    	private List<String> refs = null;
    	private int size = 0;

		@Override
		protected String doInBackground(String... params) {
    		if (params.length > 0) {
				String template = params[0];
				refs = referencesIn(template);
				size = refs.size();
				return evaluation(velocityContext, template, TAG);
			}
    		else
    			return null;
		}

		@Override
		protected void onProgressUpdate(Intent... intents) {
			super.onProgressUpdate(intents);
			if (intents.length > 0 && activity != null) {
				Intent intent = intents[0];
				if (notNullOrEmpty(intent.getAction()))
					activity.startActivity(intent);
				else {
					int percent = (int)intent.getFloatExtra("percent", 0f);
					Dialogs.sProgressDialog.setProgress(percent);
					Dialogs.sProgressDialog.setTitle(intent.getStringExtra("title"));
				}
			}
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
    		if (title.length() > 0) 
    			Dialogs.sProgressDialog.dismiss();
			if (followUp != null) 
				try {
					followUp.perform(result, null);
				} catch (Exception e) {
					Log.e(TAG, "follow-up", e);
				}
		}
		
		Job<String> followUp;
    }
	
	public VelocityContext(ValMap map, EvaluationTask evaluationTask) {
		super(map != null ? map : new ValMap());
		this.evaluationTask = evaluationTask;
	}
	
	transient private EvaluationTask evaluationTask;

	public EvaluationTask getEvaluationTask() {
		return evaluationTask;
	}

	public void setEvaluationTask(EvaluationTask evaluationTask) {
		this.evaluationTask = evaluationTask;
	}
	
//	protected void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//	}
//
//	protected void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//	}
}
