package com.applang.tagesberichte;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.UserContext.*;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePadProvider;

import android.app.Activity;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class Tagesberichte extends TabActivity
{
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tagesberichte);

		showDialog(0);
//		populateView(this);
	}
    
    @Override
    protected Dialog onCreateDialog(int id) {
        return waitWhileWorking(this, "Loading ...", 
        	new Job<Activity>() {
				public void perform(final Activity activity, Object[] params) throws Exception {
					setupVelocity(activity, true);
					
	    			runOnUiThread(new Runnable() {
	    			    public void run() {
	    			 		populateView(activity);
	    			    }
	    			});
				}
	        });
    }

	private void populateView(final Context context) {
		Resources resources = getResources(); 
		TabHost tabHost = getTabHost(); 
		tabHost.clearAllTabs();
		
		Intent intent = new Intent()
			.setClass(context, NotesList.class)
			.setData(NotePadProvider.contentUri(0));
		TabSpec tabSpecNotes = tabHost
			.newTabSpec(NotePadProvider.tableName(0))
			.setIndicator("", resources.getDrawable(R.drawable.note))
			.setContent(intent);

		intent = new Intent()
			.setClass(context, NotesList.class)
			.setData(NotePadProvider.contentUri(1));
		TabSpec tabSpecBausteine = tabHost
			.newTabSpec(NotePadProvider.tableName(1))
			.setIndicator("", resources.getDrawable(R.drawable.bausteine))
			.setContent(intent);
		
		intent = new Intent()
			.setClass(context, Glossary.class)
			.setData(NotePadProvider.contentUri(2));
		TabSpec tabSpecGloss = tabHost
			.newTabSpec(NotePadProvider.tableName(2))
			.setIndicator("", resources.getDrawable(R.drawable.tree))
			.setContent(intent);
			
		tabHost.addTab(tabSpecNotes);
		tabHost.addTab(tabSpecBausteine);
		tabHost.addTab(tabSpecGloss);
		tabHost.setCurrentTab(0);
	}
}