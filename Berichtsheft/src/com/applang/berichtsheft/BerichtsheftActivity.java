package com.applang.berichtsheft;

import com.applang.ImpexTask;
import com.applang.pflanzen.PlantsList;
import com.applang.provider.*;
import com.applang.tagesberichte.*;
import com.applang.wetterberichte.WeatherList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
    	final String[] fileNames = new String[]{
    			"databases/" + WeatherInfoProvider.DATABASE_NAME, 
    			"databases/" + PlantInfoProvider.DATABASE_NAME, 
    			"databases/" + NotePadProvider.DATABASE_NAME};
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("Application data");
		alertDialogBuilder
				.setMessage("Export copies data to SD card\nImport copies data from SD card")
				.setCancelable(false)
				.setPositiveButton("Export",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int id) {
								ImpexTask.doExport(BerichtsheftActivity.this, fileNames, null);
							}
						})
				.setNegativeButton("Import",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,	int id) {
								ImpexTask.doImport(BerichtsheftActivity.this, fileNames, null);
							}
						})
				.setNeutralButton("Cancel", null);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
    }

}
