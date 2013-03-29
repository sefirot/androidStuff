package com.applang.berichtsheft.test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.ImpexTask;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;
import com.applang.provider.PlantInfo.Plants;
import com.applang.provider.WeatherInfo.Weathers;

@SuppressWarnings("deprecation")
public class ProviderTests extends android.test.ActivityInstrumentationTestCase<BerichtsheftActivity>
{
	public ProviderTests() {
		super("com.applang.berichtsheft", BerichtsheftActivity.class);
	}
	
	public ProviderTests(String method) {
		this();
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(getActivity() instanceof BerichtsheftActivity);
        impex(getActivity(), false);	//	Export
    }

    @Override
    protected void tearDown() throws Exception {
    	impex(getActivity(), true);	//	Import
        super.tearDown();
	}
    
    public static void impex(Context context, boolean flag) {
    	String[] dbNames = new String[] { 
    		"databases/note_pad.db",
    		"databases/weather_info.db",
    		"databases/plant_info.db",
    	};
		ImpexTask.doImpex(context, dbNames, flag);
    };

    private Cursor notesCursor(String selection, String... selectionArgs) {
		Cursor cursor = getActivity().managedQuery(
        		Notes.CONTENT_URI, 
        		Notes.FULL_PROJECTION, 
        		selection, selectionArgs,
        		Notes.DEFAULT_SORT_ORDER);
        assertNotNull(cursor);
		return cursor;
	}

    public ValMap noteMap(String selection, String... selectionArgs) {
        Cursor cursor = notesCursor(selection, selectionArgs);
		
        final boolean bausteine = NotePadProvider.tableIndex(selection) == 1;

        ValMap map = getResultMap(cursor, 
        	new Function<String>() {
				public String apply(Object... params) {
					Cursor cursor = param(null, 0, params);
					assertTrue(cursor != null);
					String s = "";
					for (int i = 0; i < cursor.getColumnCount(); i++) {
						if (Notes.CREATED_DATE.equals(cursor.getColumnName(i)))
							assertTrue(bausteine ? cursor.isNull(i) : !cursor.isNull(i));
						s += cursor.getString(i) + "\t";
					}
					System.out.println(s);
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

    public void testNotepadProvider() throws InterruptedException {
		for (int i = 0; i < NotePadProvider.NOTES_TABLE_NAMES.length; i++) {
    		assertEquals(i, NotePadProvider.tableIndex(NotePadProvider.selection(i, "")));
    		assertEquals(i, NotePadProvider.tableIndex(NotePadProvider.selection(i, new ContentValues())));
    	}
		ContentValues contentValues = new ContentValues();
		assertEquals(0, NotePadProvider.tableIndex(contentValues));
		assertEquals(1, NotePadProvider.tableIndex("Created isnull"));
		contentValues.put(Notes.MODIFIED_DATE, (Long)null);
		assertEquals(2, NotePadProvider.tableIndex(contentValues));
    	
    		
    	ContentResolver contentResolver = getActivity().getContentResolver();
		contentResolver.delete(Notes.CONTENT_URI, "created is null", null);
		contentResolver.delete(Notes.CONTENT_URI, "title like 'Velocity%'", null);
		contentResolver.delete(Notes.CONTENT_URI, "modified is null", null);
   	
    	int cnt = noteMap(null).size();
    	
		long now = now();
		Object[] args = {
				"kein", "Kein", null, now, 
				"fehler", "Fehler", null, now, 
				"efhler", "eFhler", null, now, 
				"ehfler", "ehFler", null, now, 
				"im", "im", null, now, 
				"system", "System", null, now, 
				"Velocity1", "$kein $fehler $im $system", now, now, 
				"Velocity2", "$kein $efhler $im $system", now, now, 	
				"Velocity3", "$kein $ehfler $im $system", now, now, 	
		};
		for (int i = 0; i < args.length - 3; i+=4) {
			ContentValues values = NotePadProvider.contentValues(i, args);
	        Uri uri = contentResolver.insert(Notes.CONTENT_URI, values);
	        assertEquals(Notes.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
		}
		
		ValMap noteMap = noteMap("title like ?", "Velocity%");
		assertEquals(3, noteMap.size());
		
		MapContext noteContext = new MapContext(noteMap("created isnull"));
		assertEquals(6, noteContext.getKeys().length);
		
		setupVelocity4Android("com.applang.berichtsheft", getActivity().getResources());
		
		Cursor cursor = notesCursor("title like ?", "Velocity%");
		assertTrue(cursor.moveToFirst());
		do {
			String note = cursor.getString(2);
			note = evaluation(noteContext, note, "notes");
			assertFalse(note.contains("$"));
			if (!note.contains("Fehler")) {
				long id = cursor.getLong(0);
				ContentValues values = NotePadProvider.contentValues(0, "Fehler", "", id, null);
		        Uri uri = contentResolver.insert(Notes.CONTENT_URI, values);
		        assertEquals(Notes.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
			}
			System.out.println(note);
		} while (cursor.moveToNext());
		cursor.close();

		assertEquals(6, contentResolver.delete(Notes.CONTENT_URI, "created is null", null));
		assertEquals(2, contentResolver.delete(Notes.CONTENT_URI, "modified is null", null));
		assertEquals(3, contentResolver.delete(Notes.CONTENT_URI, "title like 'Velocity%'", null));
		assertEquals(cnt, noteMap(null).size());
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
        ContentResolver contentResolver = getActivity().getContentResolver();
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
        ContentResolver contentResolver = getActivity().getContentResolver();
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
    };

    private String[] PROJECTION_ID = new String[] {
            Plants._ID, // 0
            
    };

    public void testgetAllEntries() throws IOException {
      	ContentResolver contentResolver = getActivity().getContentResolver();
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
