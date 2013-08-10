package com.applang.wetterberichte;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import java.io.StringWriter;
import java.util.Arrays;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.webkit.WebView;

public class WeatherView extends Activity
{
	private WebView webView;

//	@SuppressLint("SetJavaScriptEnabled") 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		webView = new WebView(this);
//		webView.getSettings().setJavaScriptEnabled(true);
		setContentView(webView);
		
		showDialog(0);
	}
	
	void mergeTemplateAndLoad(Context vc, String templateName) {
		StringWriter sw = new StringWriter();
		Template template = Velocity.getTemplate(templateName);
		template.merge(vc, sw);
		webView.loadData(sw.toString(), "text/html", "UTF-8");
	}
	
	void evaluateTemplateAndLoad(Context vc, String template, String logTag) {
		String s = evaluate( vc, template, logTag );
		webView.loadData(s, "text/html", "UTF-8");
	}
	
	String url = "http://api.openweathermap.org/data/2.1/weather/city/2931361?type=json";
    
    @Override
    protected Dialog onCreateDialog(int id) {
        return waitWhileWorking(this, "Loading ...", 
        	new Job<Activity>() {
				public void perform(Activity activity, Object[] params) throws Exception {
					com.applang.UserContext.setupVelocity(activity, true);
					
	    			String jsonText = readFromUrl(url, "UTF-8");
	    			JSONObject json = new JSONObject(jsonText);
	    			
	    			Object openweather = walkJSON(null, json, new Function<Object>() {
	    				public Object apply(Object...params) {
	    					Object[] path = param(null, 0, params);
	    					Object value = param(null, 1, params);
	    					String name = Arrays.toString(path);
	    					if ("dt".equals(path[path.length - 1]))
	    						value = formatDate(toLong(0L,value.toString()) * 1000);
	    					else if (name.contains("temp"))
	    						value = kelvin2celsius(value);
	    					return value;
	    				}
	    			});
	    			
	    			final VelocityContext vc = new VelocityContext();
	    			vc.put("weather", openweather);
	    			
	    			runOnUiThread(new Runnable() {
	    			     public void run() {
	    			    	 mergeTemplateAndLoad(vc, "weather");
	    			    }
	    			});
				}
	        });
    }
}
