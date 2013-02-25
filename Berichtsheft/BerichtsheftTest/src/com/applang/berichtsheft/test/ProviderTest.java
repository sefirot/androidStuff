package com.applang.berichtsheft.test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.*;

import com.applang.berichtsheft.*;
import com.applang.provider.ImpexTask;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.PlantInfo;
import com.applang.provider.PlantInfo.Plants;
import com.applang.provider.PlantInfoProvider;
import com.applang.provider.WeatherInfo.Weathers;

public class ProviderTest extends ActivityInstrumentationTestCase2<BerichtsheftActivity>  {

	public ProviderTest() {
		super("com.applang.berichtsheft", BerichtsheftActivity.class);
	}
	
	public ProviderTest(String method) {
		this();
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}

    private String[] PROJECTION_NOTES = new String[] {
    	Notes._ID, // 0
    	Notes.TITLE, // 1
    	Notes.NOTE, // 2
    	Notes.CREATED_DATE, // 3
    	Notes.MODIFIED_DATE, // 4
    };

    public void testNotepadProvider() {
    	assertTrue(getActivity() instanceof BerichtsheftActivity);
        Cursor cursor = getActivity().managedQuery(
        		Notes.CONTENT_URI, 
        		PROJECTION_NOTES, 
        		null, null,
        		Notes.DEFAULT_SORT_ORDER);
        assertNotNull(cursor);
    }

    private String[] PROJECTION_WEATHERS = new String[] {
            Weathers._ID, // 0
            Weathers.DESCRIPTION, // 1
            Weathers.LOCATION, // 2
            Weathers.PRECIPITATION, // 3
            Weathers.MAXTEMP, // 4
            Weathers.MINTEMP, // 5
            Weathers.CREATED_DATE, // 6
            Weathers.MODIFIED_DATE, // 7
    };

    public void testWeatherInfoProvider() throws IOException {
    	assertTrue(getActivity() instanceof BerichtsheftActivity);
    	
        ContentResolver contentResolver = getActivity().getContentResolver();
        contentResolver.delete(Weathers.CONTENT_URI, Weathers.LOCATION + "=?", new String[]{"here"});
        
        Cursor cursor = contentResolver.query(
        		Weathers.CONTENT_URI, 
        		PROJECTION_WEATHERS, 
        		Weathers.LOCATION + "=?", new String[]{"here"},
        		Weathers.DEFAULT_SORT_ORDER);
		assertEquals(0, cursor.getCount());
        cursor.close();
    	
    	String fileName = "databases/weather_info.db";
        ImpexTask.doImpex(getActivity(), new String[]{fileName}, false);	//	Export
        
        ContentValues values = new ContentValues();
        values.put(Weathers.LOCATION, "here");
        values.put(Weathers.DESCRIPTION, "overcast");
        values.put(Weathers.PRECIPITATION, 11.1);
        values.put(Weathers.MAXTEMP, 1.0);
        values.put(Weathers.MINTEMP, -1.0);
        values.put(Weathers.CREATED_DATE, 0);
		Uri uri = contentResolver.insert(Weathers.CONTENT_URI, values);
        assertEquals(Weathers.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
    	
        values = new ContentValues();
        values.put(Weathers.MODIFIED_DATE, new Date().getTime());
        assertEquals(1, contentResolver.update(Weathers.CONTENT_URI, 
				values, 
				Weathers.LOCATION + "=?", new String[]{"here"}));
    	
        cursor = contentResolver.query(
        		Weathers.CONTENT_URI, 
        		PROJECTION_WEATHERS, 
        		Weathers.LOCATION + "=?", new String[]{"here"},
        		Weathers.DEFAULT_SORT_ORDER);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals("here", cursor.getString(2));
        assertEquals("overcast", cursor.getString(1));
        assertEquals(11.1f, cursor.getFloat(3));
        cursor.close();
    	
        ImpexTask.doImpex(getActivity(), new String[]{fileName}, true);	//	Import
        
        cursor = contentResolver.query(
        		Weathers.CONTENT_URI, 
        		PROJECTION_WEATHERS, 
        		Weathers.LOCATION + "=?", new String[]{"here"},
        		Weathers.DEFAULT_SORT_ORDER);
        assertEquals(0, cursor.getCount());
        cursor.close();
    };

    private String[] PROJECTION_PLANTS = new String[] {
            Plants._ID, // 0
            Plants.NAME, // 1
            Plants.FAMILY, // 2
            Plants.BOTNAME, // 3
            Plants.BOTFAMILY, // 4
            Plants.GROUP, // 5
    };

    public void testPlantInfoProvider() throws IOException {
    	assertTrue(getActivity() instanceof BerichtsheftActivity);
    	
        ContentResolver contentResolver = getActivity().getContentResolver();
        contentResolver.delete(Plants.CONTENT_URI, Plants.NAME + "=?", new String[]{"Paradeiser"});
        
        Cursor cursor = contentResolver.query(
        		Plants.CONTENT_URI, 
        		PROJECTION_PLANTS, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER);
        assertEquals(0, cursor.getCount());
        cursor.close();
    	
    	String fileName = "databases/plant_info.db";
        ImpexTask.doImpex(getActivity(), new String[]{fileName}, false);	//	Export
        
        ContentValues values = new ContentValues();
        values.put(Plants.NAME, "Paradeiser");
        values.put(Plants.FAMILY, "Nachtschattengewächse");
        values.put(Plants.BOTNAME, "Solanum lycopersicum");
        values.put(Plants.BOTFAMILY, "Solanaceae");
		Uri uri = contentResolver.insert(Plants.CONTENT_URI, values);
        assertEquals(Plants.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
    	
        values = new ContentValues();
        values.put(Plants.GROUP, "xitomatl");
        assertEquals(1, contentResolver.update(Plants.CONTENT_URI, 
				values, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"}));
    	
        cursor = contentResolver.query(
        		Plants.CONTENT_URI, 
        		PROJECTION_PLANTS, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals("Paradeiser", cursor.getString(1));
        assertEquals("Solanum lycopersicum", cursor.getString(3));
        assertEquals("xitomatl", cursor.getString(5));
        cursor.close();
    	
        ImpexTask.doImpex(getActivity(), new String[]{fileName}, true);	//	Import
        
        cursor = contentResolver.query(
        		Plants.CONTENT_URI, 
        		PROJECTION_PLANTS, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER);
        assertEquals(0, cursor.getCount());
        cursor.close();
    };

    private String[] PROJECTION_ID = new String[] {
            Plants._ID, // 0
            
    };

    public void testgetAllEntries() throws IOException {
      	assertTrue(getActivity() instanceof BerichtsheftActivity);
      	
      	ContentResolver contentResolver = getActivity().getContentResolver();
		String fileName = "databases/plant_info.db";
		ImpexTask.doImpex(getActivity(), new String[]{fileName}, false);	//	Export 
		contentResolver.delete(Plants.CONTENT_URI, null, null);
		
		assertEquals(0, contentResolver.query(
			Plants.CONTENT_URI, 
			PROJECTION_PLANTS, 
			null, null, null).getCount());
		
		  
		for (int i = 1; i < 21; i++) {
			String insertName = "testName" + i;
			ContentValues values = new ContentValues();
			values.put(Plants.NAME, insertName);
			contentResolver.insert(Plants.CONTENT_URI, values);
			
			if (i%2 == 0) {
				String deleteName = "testName" + (i - 1);
				contentResolver.delete(Plants.CONTENT_URI, Plants.NAME + "=?", new String[]{deleteName});
			}	
		}
		
		Cursor cursor = contentResolver.query(
			Plants.CONTENT_URI, 
			PROJECTION_ID, 
			null, null,
			Plants.ROWID_SORT_ORDER);
		assertEquals(10, cursor.getCount());
		assertTrue(cursor.moveToFirst());
		for ( int i= 1; i < 21;i++) {
			if (i%2 == 0) {
				assertEquals(i, cursor.getInt(0));
				cursor.moveToNext(); 
			} 
		}
		
		ImpexTask.doImpex(getActivity(), new String[]{fileName}, true);	//	Import
    };
    
    public void impexTest(boolean flag) throws InterruptedException {
		assertTrue(ImpexTask.isExternalStorageAvailable());
    	File directory = ImpexTask.directory(getActivity(), !flag);
		assertTrue(directory.exists());
    	directory = ImpexTask.directory(getActivity(), flag);
    	String fileName = "databases/plant_info.db";
		final File file = new File(directory, fileName);
		if (file.exists())
			file.delete();
//    	final CountDownLatch signal = new CountDownLatch(1);
    	ImpexTask.AsyncCallback callback = new ImpexTask.AsyncCallback() {
			public void onTaskCompleted() {
				assertTrue(file.exists());
//				signal.countDown();
			}
		};
    	if (flag) 
			ImpexTask.doImport(getActivity(), new String[]{fileName}, callback);
		else
			ImpexTask.doExport(getActivity(), new String[]{fileName}, callback);
//    	signal.await();
    }
}
