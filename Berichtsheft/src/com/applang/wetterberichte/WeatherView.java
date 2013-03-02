package com.applang.wetterberichte;

import org.apache.velocity.app.Velocity;

import com.applang.berichtsheft.R;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class WeatherView extends Activity {

	public WeatherView() {
		setupVelocity();
	}

    private void setupVelocity() {
  		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "com.applang.VelocityLogger");
  		Velocity.setProperty("resource.loader", "android");
  		Velocity.setProperty("android.resource.loader.class", "com.applang.AndroidResourceLoader");
  		Velocity.setProperty("android.content.res.Resources",getResources());
  		Velocity.setProperty("packageName", "com.applang.wetterberichte");
  		Velocity.init();
    }

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview);
		
		webView = (WebView) findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		
		String url = "http://api.openweathermap.org/data/2.1/weather/city/2931361?type=html";
		webView.loadUrl(url);
	}
}
