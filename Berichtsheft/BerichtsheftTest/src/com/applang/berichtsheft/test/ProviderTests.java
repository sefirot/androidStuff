package com.applang.berichtsheft.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;
import com.applang.provider.PlantInfo.Plants;
import com.applang.provider.PlantInfoProvider;
import com.applang.provider.WeatherInfo.Weathers;
import com.applang.provider.WeatherInfoProvider;

public class ProviderTests extends ActivityTests<BerichtsheftActivity>
{
    private static final String TAG = ProviderTests.class.getSimpleName();
    
	public ProviderTests() {
		super("com.applang.berichtsheft", BerichtsheftActivity.class);
	}
	
/*	public ProviderTests(String method) {
		this();
	}
*/
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(mActivity instanceof BerichtsheftActivity);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}

    private Cursor notesCursor(int tableIndex, String selection, String... selectionArgs) {
		Cursor cursor = mActivity.managedQuery(
        		NotePadProvider.contentUri(tableIndex), 
        		Notes.FULL_PROJECTION, 
        		selection, selectionArgs,
        		Notes.DEFAULT_SORT_ORDER);
        assertNotNull(cursor);
		return cursor;
	}

	private void fullRecord(Cursor cursor) {
		assertEquals(5, cursor.getColumnCount());
		Object[] values = new Object[5];
		values[0] = cursor.getLong(0);
		values[1] = cursor.getString(1);
		values[2] = cursor.getString(2);
		values[3] = cursor.getLong(3);
		values[4] = cursor.getLong(4);
		Log.i(TAG, Arrays.toString(values));
	}

    public ValMap noteMap(int tableIndex, String selection, String... selectionArgs) {
        Cursor cursor = notesCursor(tableIndex, selection, selectionArgs);
		
        ValMap map = getResultMap(cursor, 
        	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					assertTrue(cursor != null);
					fullRecord(cursor);
					return cursor.getString(1);
				}
	        }, 
        	new Function<Object>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					assertTrue(cursor != null);
					return cursor.getString(2);
				}
	        }
	    );
        
        return map;
	}
    
    public void testNotePadProvider() throws InterruptedException {
		long now = now();
		ActivityTests.generateNotePadData(mActivity, true, new Object[][] {
			{ 1L, "kein", "Kein", null, now }, 
			{ 2L, "fehler", "Fehler", null, now }, 
			{ 3L, "efhler", "eFhler", null, now }, 
			{ 4L, "ehfler", "ehFler", null, now }, 
			{ 5L, "im", "im", null, now }, 
			{ 6L, "system", "System", null, now }, 
			{ 1L, "Velocity1", "$kein $fehler $im $system", now, now }, 
			{ 2L, "Velocity2", "$kein $efhler $im $system", now, now }, 	
			{ 3L, "Velocity3", "$kein $ehfler $im $system", now, now }, 	
		});
		
		ValMap noteMap = noteMap(0, "title like ?", "Velocity%");
		assertEquals(3, noteMap.size());
		
		MapContext noteContext = new MapContext(noteMap(1, ""));
		assertEquals(6, noteContext.getKeys().length);
		
		setupVelocity4Android("com.applang.berichtsheft", mActivity.getResources());

		ContentResolver contentResolver = mActivity.getContentResolver();
		
		Cursor cursor = notesCursor(0, "title like ?", "Velocity%");
		assertTrue(cursor.moveToFirst());
		long id = 0;
		do {
			String note = cursor.getString(2);
			note = evaluation(noteContext, note, "notes");
			assertFalse(note.contains("$"));
			if (!note.contains("Fehler")) {
				long refId = cursor.getLong(0);
				ContentValues values = NotePadProvider.contentValues(++id, "Fehler", null, refId, null);
		        Uri uri = contentResolver.insert(NotePadProvider.contentUri(2), values);
		        assertEquals(Notes.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
			}
			System.out.println(note);
		} while (cursor.moveToNext());
		cursor.close();
		
		noteMap(2, "");

		assertEquals(6, NotePadProvider.countNotes(contentResolver, 1, "", null)[0].intValue());
		assertEquals(2, NotePadProvider.countNotes(contentResolver, 2, "", null)[0].intValue());
		assertEquals(3, NotePadProvider.countNotes(contentResolver, 0, "", null)[0].intValue());
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
        ContentResolver contentResolver = mActivity.getContentResolver();
        contentResolver.delete(Weathers.CONTENT_URI, Weathers.LOCATION + "=?", new String[]{"here"});
        
        Cursor cursor = contentResolver.query(
        		Weathers.CONTENT_URI, 
        		PROJECTION_WEATHERS, 
        		Weathers.LOCATION + "=?", new String[]{"here"},
        		Weathers.DEFAULT_SORT_ORDER);
		assertEquals(0, cursor.getCount());
        cursor.close();
        
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
        ContentResolver contentResolver = mActivity.getContentResolver();
        contentResolver.delete(Plants.CONTENT_URI, Plants.NAME + "=?", new String[]{"Paradeiser"});
        
        Cursor cursor = contentResolver.query(
        		Plants.CONTENT_URI, 
        		PROJECTION_PLANTS, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER);
        assertEquals(0, cursor.getCount());
        cursor.close();
        
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
		
//    	ImpexTask.doImpex(mActivity, new String[] { "databases/plant_info.db" }, true);
    };

    private String[] PROJECTION_ID = new String[] {
            Plants._ID, // 0
            
    };

    public void testgetAllEntries() throws IOException {
      	ContentResolver contentResolver = mActivity.getContentResolver();
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
    };
    
    public void _testImpex() throws InterruptedException {
		for (boolean flag : new boolean[]{true,false}) {
			assertTrue(isExternalStorageAvailable());
			File directory = ImpexTask.directory(mActivity, !flag);
			assertTrue(directory.exists());
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
			
			if (!flag)
				ImpexTask.doImport(mActivity, new String[] { fileName }, callback);
			else
				ImpexTask.doExport(mActivity, new String[] { fileName }, callback);
			
			//    	signal.await();
		}
    }
}
