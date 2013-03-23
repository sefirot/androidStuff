package com.applang.tagesberichte;

import com.applang.berichtsheft.R;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;

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

		Resources ressources = getResources(); 
		TabHost tabHost = getTabHost(); 
		
		Intent intent = new Intent()
			.setClass(this, NotesList.class)
			.setData(Notes.CONTENT_URI)
			.putExtra("table", 0);
		TabSpec tabSpecNotes = tabHost
			.newTabSpec(NotePadProvider.tableName(0))
			.setIndicator("", ressources.getDrawable(R.drawable.note))
			.setContent(intent);

		intent = new Intent()
			.setClass(this, NotesList.class)
			.setData(Notes.CONTENT_URI)
			.putExtra("table", 1);
		TabSpec tabSpecBausteine = tabHost
			.newTabSpec(NotePadProvider.tableName(1))
			.setIndicator("", ressources.getDrawable(R.drawable.bausteine))
			.setContent(intent);
		
		intent = new Intent()
			.setClass(this, Glossary.class)
			.setData(Notes.CONTENT_URI)
			.putExtra("table", 2);
		TabSpec tabSpecGloss = tabHost
			.newTabSpec(NotePadProvider.tableName(2))
			.setIndicator("", ressources.getDrawable(R.drawable.glossary))
			.setContent(intent);
	
		tabHost.addTab(tabSpecNotes);
		tabHost.addTab(tabSpecBausteine);
		tabHost.addTab(tabSpecGloss);
		tabHost.setCurrentTab(0);
	}
}