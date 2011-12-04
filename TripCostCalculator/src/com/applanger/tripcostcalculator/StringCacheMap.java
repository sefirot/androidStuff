package com.applanger.tripcostcalculator;

import android.app.IntentService;
import android.content.ContentQueryMap;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

public class StringCacheMap extends IntentService {
	private NotesDbAdapter mDbHelper;
	private ContentQueryMap namesCache;
	private ContentQueryMap purposesCache;
	
	 /** 
	   * A constructor is required, and must call the super IntentService(String)
	   * constructor with a name for the worker thread.
	   */
	public StringCacheMap() {
	    super("StringCacheMap");
	}
	    
	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	*/
	@Override
	protected void onHandleIntent(Intent intent) {  
		mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
       
        namesCache = new ContentQueryMap(mDbHelper.fetchAllNames(),NotesDbAdapter.KEY_NAMEID,true,null); 
        purposesCache = new ContentQueryMap(mDbHelper.fetchAllPurposes(),NotesDbAdapter.KEY_PURPOSEID,true,null); 
        
        
	}
}
	
