package com.applang.wetterberichte;

import java.io.StringWriter;
import java.util.Arrays;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.json.JSONObject;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;
import com.applang.berichtsheft.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebView;

public class WeatherView extends Activity
{
    private static final String TAG = "WeatherView";

	private WebView webView;

	@SuppressLint("SetJavaScriptEnabled") 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview);
		
		webView = (WebView) findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		
		setupVelocity4Android("com.applang.berichtsheft", getResources());
		
		showDialog(0);
	}
	
	void mergeTemplateAndLoad(Context vc, String templateName) {
		StringWriter sw = new StringWriter();
		Template template = Velocity.getTemplate(templateName);
		template.merge(vc, sw);
		webView.loadData(sw.toString(), "text/html", "UTF-8");
	}
	
	void evaluateTemplateAndLoad(Context vc, String template, String logTag) {
		String s = evaluation( vc, template, logTag );
		webView.loadData(s, "text/html", "UTF-8");
	}
	
	String url = "http://api.openweathermap.org/data/2.1/weather/city/2931361?type=json";
	ProgressThread progThread;
    ProgressDialog progDialog;
    
    @Override
    protected Dialog onCreateDialog(int id) {
        progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setMessage("Loading...");
        progThread = new ProgressThread(handler);
        progThread.start();
        return progDialog;
    }

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int countDown = msg.getData().getInt("countDown");
            progDialog.setProgress(countDown);
            if (countDown <= 0) {
                dismissDialog(0);
                progThread.setState(ProgressThread.DONE);
            }
        }
    };

    private class ProgressThread extends Thread
    {	
    	// Class constants defining state of the thread
    	final static int DONE = 0;
        final static int RUNNING = 1;
        
        Handler mHandler;
       
        ProgressThread(Handler h) {
            mHandler = h;
        }
        
        @Override
        public void run() {
        	mState = RUNNING;   
        	
    	    try {
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
    			
    			VelocityContext vc = new VelocityContext();
    			vc.put("weather", openweather);
    			
    			mergeTemplateAndLoad(vc, "weather");
    		} catch (Exception e) {
                Log.e(TAG, "URL read", e);
    		}
            
            while (mState == RUNNING) {
                delay(40);
                
	            Message msg = mHandler.obtainMessage();
	            Bundle b = new Bundle();
	            b.putInt("countDown", 0);
	            msg.setData(b);
	            mHandler.sendMessage(msg);
            }
        }
        
        int mState;
        
        // Set current state of thread (use state=ProgressThread.DONE to stop thread)
        public void setState(int state) {
            mState = state;
        }
    }
}
