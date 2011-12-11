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
	protected void onHandleIntent(Intent onStartStringCache) {  
		mDbHelper = new NotesDbAdapter(this);
        mDbHelper.open();
        Bundle extras = onStartStringCache.getExtras();
        
        
        if (extras != null) {
			
        	
        	
        	String state = extras.getString(TelephonyManager.EXTRA_STATE);
			Log.w("DEBUG", state);
			if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
				String phoneNumber = extras
						.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
				Log.w("DEBUG", phoneNumber);
			} else {
				namesCache = new ContentQueryMap(mDbHelper.fetchAllNames(),NotesDbAdapter.KEY_NAMEID,true,null); 
				purposesCache = new ContentQueryMap(mDbHelper.fetchAllPurposes(),NotesDbAdapter.KEY_PURPOSEID,true,null); 
				Bundle extra = extra.putString(key, value)
				T[] hi;
				
						namesCache.getRows().values().toArray( Array); 
				Bundle extra = test;
				onStartStringCache.putExtras(extras);
        
	}
}
	
