package com.applang.berichtsheft;

import static com.applang.Util.*;
import static com.applang.Util2.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.applang.pflanzen.PlantsList;
import com.applang.provider.NotePad;
import com.applang.provider.NotePadProvider;
import com.applang.provider.PlantInfo;
import com.applang.provider.PlantInfoProvider;
import com.applang.provider.WeatherInfo;
import com.applang.provider.WeatherInfoProvider;
import com.applang.tagesberichte.Tagesberichte;
import com.applang.wetterberichte.WeatherList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class BerichtsheftActivity extends Activity
{
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        for (final int id : new int[] {R.id.button1, R.id.button2, R.id.button3, R.id.button4}) {
        	Button btn = (Button) findViewById(id);
        	btn.setOnClickListener(new View.OnClickListener() {
        	    @Override
        	    public void onClick(View v) {
        	        switch (id) {
					case R.id.button1:
						showTagesberichte(v);
						break;
					case R.id.button2:
						showPflanze(v);
						break;
					case R.id.button3:
						showMore(v);
						break;
					case R.id.button4:
						showEvenMore(v);
						break;
					}
        	    }
        	});	
        }
        
//		String mButtonMessage = android.os.Build.VERSION.SDK;	//		getString(R.string.button_message_template)
//		Toast.makeText(this, mButtonMessage, Toast.LENGTH_LONG).show();	 
	}
	
	public void showTagesberichte(View clickedButton) {
		Intent activityIntent =
				new Intent(this, Tagesberichte.class);
		startActivity(activityIntent);
		}

	public void showPflanze(View clickedButton) {
		Intent activityIntent =
				new Intent(this, PlantsList.class);
		startActivity(activityIntent);
		}
	
	public void showMore(View clickedButton) {
		Intent activityIntent =
				new Intent(this, WeatherList.class);
		startActivity(activityIntent);
		}
	
	public void showEvenMore(View clickedButton) {
		impex();
		}
	
    public void impex() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Application data");
		alertDialogBuilder
				.setMessage("Export copies data to SD card\nImport copies data from SD card")
				.setCancelable(false)
				.setPositiveButton("Export",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int id) {
								ImpexTask.doExport(BerichtsheftActivity.this, databases(), null);
							}
						})
				.setNegativeButton("Import",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int id) {
								ImpexTask.doImport(BerichtsheftActivity.this, databases(), null);
							}
						})
				.setNeutralButton("Cancel", null);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
    }
    
    public static String[] databases() {
    	ArrayList<String> list = new ArrayList<String>();
    	for (Object provider : providers().values()) 
    		try {
    			Class<?> c = Class.forName(provider.toString());
    			Object name = c.getDeclaredField("DATABASE_NAME").get(null);
    			list.add(name.toString());
    		} catch (Exception e) {};
    	return list.toArray(new String[0]);
    }
    
    public static ValMap providers() {
    	String pkg = "com.applang.provider";
    	ValMap map = new ValMap();
		map.put(NotePad.Notes.CONTENT_URI.toString(), pkg + ".NotePadProvider");
		map.put(PlantInfo.Plants.CONTENT_URI.toString(), pkg + ".PlantInfoProvider");
		map.put(WeatherInfo.Weathers.CONTENT_URI.toString(), pkg + ".WeatherInfoProvider");
    	return map;
    }

}
