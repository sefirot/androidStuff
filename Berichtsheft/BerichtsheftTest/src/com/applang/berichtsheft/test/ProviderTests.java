package com.applang.berichtsheft.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.Util2.ImpexTask;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.provider.NotePad.Notes;
import com.applang.provider.NotePadProvider;
import com.applang.provider.PlantInfo;
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

	static void generateNotePadData(Context context, boolean clear, Object[][] records) {
		ContentResolver contentResolver = context.getContentResolver();
		if (clear) {
			for (int i = 0; i < 3; i++)
				contentResolver.delete(NotePadProvider.contentUri(i), "", null);
		}
		Uri uri = null;
		long id = -1;
		for (int i = 0; i < records.length; i++) {
			Object[] record = records[i];
			
			int index = Arrays.asList(record).indexOf("ROWID");
			if (index > -1) 
				record[index] = id;
			
			ContentValues values = NotePadProvider.contentValues(record);
	        int tableIndex = 
	    		record[3] == null ? 1 :
	    		(record[2] == null ? 2 : 0);
			uri = contentResolver.insert(NotePadProvider.contentUri(tableIndex), values);
	        assertEquals(Notes.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
	        
	        if (index < 0)
	        	id = toLong(-1L, uri.getPathSegments().get(1));
		}
	}

	static void generateData(ContentResolver contentResolver, Uri uri, boolean clear, Object[][] records) {
		assertTrue(contentResolver.getType(uri).startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
		if (clear) {
			contentResolver.delete(uri, null, null);
		}
		try {
			Object name = BerichtsheftActivity.providers().get(uri.toString());
			Class<?> c = Class.forName(name.toString());
			Method contentValues = c.getDeclaredMethod("contentValues", Object[].class);
			for (int i = 0; i < records.length; i++) {
				Object[] record = records[i];
				ContentValues values = (ContentValues) contentValues.invoke(null, new Object[]{record});
				Uri urItem = contentResolver.insert(uri, values);
				assertTrue(contentResolver.getType(urItem).startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE));
			}
		} catch (Exception e) {
			Log.e(TAG, "generateData", e);
		};
	}

    public static void generateTestData(Context context) {
		long now = now();
		generateNotePadData(context, true, new Object[][] {
			{ 1L, "kein", "Kein", null, now }, 
			{ 2L, "fehler", "Fehler", null, now }, 
			{ 3L, "efhler", "eFhler", null, now }, 
			{ 4L, "ehfler", "ehFler", null, now }, 
			{ 5L, "im", "in dem", null, -now }, 
			{ 6L, "system", "System", null, now }, 
			{ 1L, "Velocity1", "$kein $fehler $im $system", now, now }, 
			{ 1L, "System", null, 1L, null }, 
			{ 2L, "Kein", null, 1L, null }, 
			{ 2L, "Velocity2", "$kein $efhler $im $system", now, now }, 	
			{ 3L, "Fehler", null, 2L, null }, 
			{ 4L, "Kein", null, 2L, null }, 
			{ 3L, "Velocity3", "$kein $ehfler $im $system", now, now }, 	
			{ 5L, "Fehler", null, 3L, null }, 
			{ 6L, "Kein", null, 3L, null }, 
//			{ 7L, null, null, 1L, 5L }, 
//			{ 8L, null, null, 2L, 5L }, 
//			{ 9L, null, null, 3L, 5L }, 
		});
	}
    
    public void testData() throws Exception {
		mActivity.deleteDatabase(NotePadProvider.DATABASE_NAME);
		
		generateTestData(mActivity);
		
    	ImpexTask.doImpex(mActivity, new String[] { NotePadProvider.DATABASE_NAME }, true);
    }
    
    public void testData3() throws Exception {
    	for (String database : BerichtsheftActivity.databases()) 
    		mActivity.deleteDatabase(database);
		
		generateNotePadData(mActivity, true, new Object[][] {
			{ 1L, "prompt1", "#set($var=\"\")" +
					"#prompt(\"name\" $var \"xxx\")#if($var)$var\n#end", null, now() }, 
			{ 2L, "prompt2", "#set($var=\"\")" +
					"#prompt(\"ja oder nein\",$var,[\"ja\",\"nein\"])#if($var)$var\n#end" +
					"#prompt(\"hello or world or cancel\",$var,[\"hello\",\"world\"] true)#if($var)$var\n#end", null, now() }, 
			{ 3L, "prompt3", "#set($var=[\"Kein\",\"Fehler\"])" +
					"#prompt(\"Mehrfachauswahl\" $var [\"Kein\",\"Fehler\",\"im\",\"System\"] true)#if($var)$var\n#end" +
					"#prompt(\"Einzelauswahl\",$var,[\"Kein\",\"Fehler\",\"im\",\"System\"])#if($var)$var\n#end", null, now() }, 
			{ 4L, "spinner", "#spinner(\"states\",$var)" +
					join("\n", getStateStrings()) +
					"#end#if($var)$var\n#end", null, now() }, 
			{ 5L, "cursor", "#cursor(\"plants\",$var," +
					quoted(PlantInfo.Plants.CONTENT_URI.toString()) + "," +
					"[" + quoted(PlantInfo.Plants.NAME) + 
					"," + quoted(PlantInfo.Plants._ID) + 
					"," + quoted(PlantInfo.Plants.BOTNAME) + 
					"," + quoted(PlantInfo.Plants.BOTFAMILY) + 
					"])#if($var)$var\n#end", null, now() }, 
		});
	    	
		generateData(mActivity.getContentResolver(), Plants.CONTENT_URI, true, new Object[][] {
				{ 1L, "Paradeiser", "Nachtschattengewächse", "Solanum lycopersicum", "Solanaceae", "xitomatl" }, 	
				{ 2L, "chili pepper", "nightshade", "Capsicum", "Solanaceae", "xilli" }, 	
				{ 3L, "Melanzani", "Nachtschattengewächse", "Solanum melongena", "Solanaceae", "ratatouille" }, 	
				{ 4L, "potato", "nightshade", "Solanum tuberosum", "Solanaceae", "tartufolo" }, 	
				{ 5L, "Tobak", "Nachtschattengewächse", "Nicotiana tabacum", "Solanaceae", "tobacco" }, 	
			});
		
		generateData(mActivity.getContentResolver(), Weathers.CONTENT_URI, true, new Object[][] {
				{ 1L, "here", "overcast", 11.1f, 1f, -1f, 0l, now() }, 	
			});
	        
    	ImpexTask.doImpex(mActivity, BerichtsheftActivity.databases(), true);
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
		mActivity.deleteDatabase(NotePadProvider.DATABASE_NAME);
		
		long now = now();
		generateNotePadData(mActivity, true, new Object[][] {
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
		
		com.applang.VelocityContext.setupVelocity(mActivity, true, "com.applang.berichtsheft");

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
		
		for (int i = 0; i < 2; i++) 
			assertEquals(
					new int[]{3,6,2}[i], 
					NotePadProvider.countNotes(contentResolver, i, "", null)[0].intValue());
		
    	ImpexTask.doImpex(mActivity, new String[] { "databases/" + NotePadProvider.DATABASE_NAME }, true);
    	
		assertEquals(1, contentResolver.delete(NotePadProvider.contentUri(0), Notes.TITLE + "=?", new String[]{"Velocity2"}));
		
		for (int i = 0; i < 2; i++) 
			assertEquals(
					new int[]{2,6,1}[i], 
					NotePadProvider.countNotes(contentResolver, i, "", null)[0].intValue());
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
		mActivity.deleteDatabase(WeatherInfoProvider.DATABASE_NAME);
		
        ContentResolver contentResolver = mActivity.getContentResolver();
		generateData(contentResolver, Weathers.CONTENT_URI, true, new Object[][] {
			{ 1L, "here", "overcast", 11.1f, 1f, -1f, 0l,  }, 	
		});
    	
		ContentValues values = new ContentValues();
        values.put(Weathers.MODIFIED_DATE, now());
        assertEquals(1, contentResolver.update(Weathers.CONTENT_URI, 
				values, 
				Weathers.LOCATION + "=?", new String[]{"here"}));
    	
        Cursor cursor = contentResolver.query(
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
		mActivity.deleteDatabase(PlantInfoProvider.DATABASE_NAME);
		
        ContentResolver contentResolver = mActivity.getContentResolver();
		generateData(contentResolver, Plants.CONTENT_URI, true, new Object[][] {
			{ 1L, "Paradeiser", "Nachtschattengewächse", "Solanum lycopersicum", "Solanaceae", "" }, 	
		});
        
        ContentValues values = new ContentValues();
        values.put(Plants.GROUP, "xitomatl");
        assertEquals(1, contentResolver.update(Plants.CONTENT_URI, 
				values, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"}));
    	
        Cursor cursor = contentResolver.query(
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

    public static String[] getStateStrings() {
        String[] stateStrings = {
            "Alabama (AL)",
            "Alaska (AK)",
            "Arizona (AZ)",
            "Arkansas (AR)",
            "California (CA)",
            "Colorado (CO)",
            "Connecticut (CT)",
            "Delaware (DE)",
            "District of Columbia (DC)",
            "Florida (FL)",
            "Georgia (GA)",
            "Hawaii (HI)",
            "Idaho (ID)",
            "Illinois (IL)",
            "Indiana (IN)",
            "Iowa (IA)",
            "Kansas (KS)",
            "Kentucky (KY)",
            "Louisiana (LA)",
            "Maine (ME)",
            "Maryland (MD)",
            "Massachusetts (MA)",
            "Michigan (MI)",
            "Minnesota (MN)",
            "Mississippi (MS)",
            "Missouri (MO)",
            "Montana (MT)",
            "Nebraska (NE)",
            "Nevada (NV)",
            "New Hampshire (NH)",
            "New Jersey (NJ)",
            "New Mexico (NM)",
            "New York (NY)",
            "North Carolina (NC)",
            "North Dakota (ND)",
            "Ohio (OH)",
            "Oklahoma (OK)",
            "Oregon (OR)",
            "Pennsylvania (PA)",
            "Rhode Island (RI)",
            "South Carolina (SC)",
            "South Dakota (SD)",
            "Tennessee (TN)",
            "Texas (TX)",
            "Utah (UT)",
            "Vermont (VT)",
            "Virginia (VA)",
            "Washington (WA)",
            "West Virginia (WV)",
            "Wisconsin (WI)",
            "Wyoming (WY)"
        };
        return stateStrings;
    }
}
