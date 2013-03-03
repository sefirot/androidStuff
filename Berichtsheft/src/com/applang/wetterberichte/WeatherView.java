package com.applang.wetterberichte;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.json.JSONObject;

import static com.applang.Util.*;

import com.applang.berichtsheft.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class WeatherView extends Activity
{
    private static final String TAG = "WeatherView";

    private void setupVelocity() {
  		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "com.applang.VelocityLogger");
  		Velocity.setProperty("resource.loader", "android");
  		Velocity.setProperty("android.resource.loader.class", "com.applang.VelocityResourceLoader");
  		Velocity.setProperty("android.content.res.Resources", getResources());
  		Velocity.setProperty("packageName", "com.applang.berichtsheft");
  		Velocity.init();
    }

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview);
		
		setupVelocity();
		
		webView = (WebView) findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		
	    try {
	    	String url = "http://api.openweathermap.org/data/2.1/weather/city/2931361?type=json";
			String jsonText = readFromUrl(url, "UTF-8");
			JSONObject json = new JSONObject(jsonText);
			
			Object openweather = walkJSON("", json, new Function<Object>() {
				public Object apply(Object...params) {
					String name = paramString(null, 0, params);
					Object value = param(null, 1, params);
					if (name.endsWith("dt"))
						value = formatDate(toLong(0L,value.toString()) * 1000);
					else if (name.contains("temp"))
						value = round(toDouble(0D, value.toString()) + absoluteZero, 2);
					return value;
				}
			});
			
			VelocityContext vc = new VelocityContext();
			vc.put("openweather", openweather);
			
			mergeAndLoadTemplate(vc, "weather");
		} catch (Exception e) {
            Log.e(TAG, "URL read", e);
		}
	}
	
	void mergeAndLoadTemplate(Context vc, String templateName) {
		StringWriter sw = new StringWriter();
		Template template = Velocity.getTemplate(templateName);
		template.merge(vc, sw);
		webView.loadData(sw.toString(), "text/html", "UTF-8");
	}
	
	void evaluateAndLoadTemplate(Context vc, String template, String logTag) {
		StringWriter sw = new StringWriter();
		Velocity.evaluate( vc, sw, logTag, template );
		webView.loadData(sw.toString(), "text/html", "UTF-8");
	}
}
