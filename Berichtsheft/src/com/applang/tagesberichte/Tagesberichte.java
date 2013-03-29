package com.applang.tagesberichte;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;

import android.app.Activity;
import android.app.Dialog;
import android.app.TabActivity;
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
	}
    
    @Override
    protected Dialog onCreateDialog(int id) {
        return waitWhileWorking(this, "Loading ...", 
        	new Job<Activity>() {
				public void dispatch(Activity activity, Object[] params) throws Exception {
					setupVelocity4Android(packageName(activity), getResources());
					
	    			runOnUiThread(new Runnable() {
	    			    public void run() {
	    			 		Resources resources = getResources(); 
	    					TabHost tabHost = getTabHost(); 
	    					tabHost.clearAllTabs();
	    					
	    					Intent intent = new Intent()
	    						.setClass(Tagesberichte.this, NotesList.class)
	    						.setData(Notes.CONTENT_URI)
	    						.putExtra("table", 0);
	    					TabSpec tabSpecNotes = tabHost
	    						.newTabSpec(NotePadProvider.tableName(0))
	    						.setIndicator("", resources.getDrawable(R.drawable.note))
	    						.setContent(intent);

	    					intent = new Intent()
	    						.setClass(Tagesberichte.this, NotesList.class)
	    						.setData(Notes.CONTENT_URI)
	    						.putExtra("table", 1);
	    					TabSpec tabSpecBausteine = tabHost
	    						.newTabSpec(NotePadProvider.tableName(1))
	    						.setIndicator("", resources.getDrawable(R.drawable.bausteine))
	    						.setContent(intent);
	    					
	    					intent = new Intent()
	    						.setClass(Tagesberichte.this, Glossary.class)
	    						.setData(Notes.CONTENT_URI)
	    						.putExtra("table", 2);
	    					TabSpec tabSpecGloss = tabHost
	    						.newTabSpec(NotePadProvider.tableName(2))
	    						.setIndicator("", resources.getDrawable(R.drawable.glossary))
	    						.setContent(intent);
	    				
	    					tabHost.addTab(tabSpecNotes);
	    					tabHost.addTab(tabSpecBausteine);
	    					tabHost.addTab(tabSpecGloss);
	    					tabHost.setCurrentTab(0);
	    			    }
	    			});
				}
	        });
    }
}