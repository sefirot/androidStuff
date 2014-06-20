package com.applang.berichtsheft.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.json.JSONArray;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.applang.UserContext;
import com.applang.Util.ValList;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.provider.NotePad.NoteColumns;
import com.applang.provider.NotePad;
import com.applang.provider.NotePadProvider;
import com.applang.provider.PlantInfo;
import com.applang.provider.PlantInfo.Plants;
import com.applang.provider.PlantInfoProvider;
import com.applang.provider.WeatherInfo.Weathers;
import com.applang.provider.WeatherInfoProvider;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

public class ProviderTests extends InfraTests<BerichtsheftActivity>
{
    private static final String TAG = ProviderTests.class.getSimpleName();
    
    private SharedPreferences prefs = null;
    
	public ProviderTests() {
		super(getPackageNameByClass(R.class), BerichtsheftActivity.class);
	}
	
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(mActivity instanceof BerichtsheftActivity);
        prefs = mActivity.getSharedPreferences(null, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("keepData", false).commit();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}
	
	private Handler notifyHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			System.out.println(msg);
			super.handleMessage(msg);
		}
	};
	
	int contentObservations = 0;

	private void setContentObserver(ContentResolver contentResolver, final Uri notificationUri) {
		ContentObserver contentObserver = new ContentObserver(notifyHandler) {
			@Override
			public void onChange(boolean selfChange) {
				contentObservations++;
			}
		};
		contentResolver.registerContentObserver(notificationUri, true, contentObserver);
	}

    void keepTestData(final String[] fileNames) {
    	if (prefs.getBoolean("keepData", false)) {
			impex(mActivity, fileNames, true);
		}
	}
    
	public int generateNotes(boolean clear, 
			Pattern expat, int grp, 
			int[][] dates, String[] titles,
			boolean randomizeTimeOfDay, 
			Integer... params) throws Exception 
	{
		InputStream is = mActivity.getResources().getAssets().open("Kein Fehler im System.txt");
		MatchResult[] excerpts = excerptsFrom(is, expat);
		Integer start = param(0, 0, params);
		Integer length = param(dates != null ? dates.length : 0, 1, params);
		Uri uri = NotePadProvider.contentUri(0);
		ContentResolver contentResolver = mActivity.getContentResolver();
		if (clear) {
			contentResolver.delete(uri, "", null);
		}
		int cnt = 0;
		long time = dateFromTodayInMillis(0);
		String title = "";
		for (int i = start; i < excerpts.length; i++) {
			MatchResult m = excerpts[i];
			int j = i - start;
			if (isAvailable(j, titles))
				title = titles[j];
			time = dates != null && j < dates.length ?
					timeInMillis(dates[j][0], dates[j][1], dates[j][2]) : 
					dateFromTodayInMillis(1, new Date(time), randomizeTimeOfDay);
			long[] interval = dayInterval(time, 1);
			ContentValues values = new ContentValues();
			values.put("note", m.group(grp));
			values.put("title", title);
			values.put("created", time);
			values.put("modified", now());
			Cursor c = contentResolver.query(uri, strings("_id"), 
					"created between ? and ? and title = ?", 
					strings("" + interval[0], "" + (interval[1] - 1), title),
					null);
			boolean update = c != null && c.moveToFirst();
			if (update)
				assertEquals(1, contentResolver.update(uri, values, "_id=?", strings("" + c.getLong(0))));
			else
				assertTrue(contentResolver.insert(uri, values) instanceof Uri);
			cnt++;
			if (j >= length - 1)
				break;
		}
		return cnt;
	}

	public ValMap results(final int kind) {
		String sql;
		switch (kind) {
		case 1:
			sql = "select count(*) from notes";
			break;
		case 0:
			sql = "select title,count(_id) from notes group by title";
			break;
		default:
			return null;
		}
		ContentResolver contentResolver = mActivity.getContentResolver();
	    return getResults(contentResolver.query(contentUri(NotePad.AUTHORITY, null), null, sql, null, null), 
		    	new Function<String>() {
					public String apply(Object... params) {
						Cursor cursor = param(null, 0, params);
						switch (kind) {
						case 1:
							return "count";
						case 0:
							return cursor.getString(0);
						default:
							return null;
						}
					}
		        }, 
		    	new Function<Object>() {
					public Object apply(Object... params) {
						Cursor cursor = param(null, 0, params);
						switch (kind) {
						case 1:
							return cursor.getInt(0);
						case 0:
							return cursor.getInt(1);
						default:
							return null;
						}
					}
		        }
		    );
	}
		
	Pattern expat = Pattern.compile("(?s)\\n([^\\{\\}]+?)(?=\\n)");
	int[] date = ints(2012, -Calendar.DECEMBER, 24);
	
	public void keinFehlerImSystemm() throws Exception {
		int[][] dates = new int[][] {date};
		
		int length = 19;
		assertEquals(length, 
			generateNotes(true, expat, 1, 
				dates, 
				strings("1."), 
				false, 
				2, length));
		length = 7;
		assertEquals(length, 
			generateNotes(false, expat, 1, 
				dates, 
				strings("2."), 
				false, 
				21, length));
		length = 10;
		assertEquals(length, 
			generateNotes(false, expat, 1, 
				dates, 
				strings("3."), 
				false, 
				28, length));
		
		assertEquals(36, results(1).get("count"));
	}

	public static void generateNotePadData(Context context, boolean clear, Object[][] records) {
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
	        int tableIndex = 
	    		record[3] == null ? 1 :
	    		(record[2] == null ? 2 : 0);
	        ContentValues values = NotePadProvider.contentValues(tableIndex, record);
			uri = contentResolver.insert(NotePadProvider.contentUri(tableIndex), values);
	        assertEquals(NoteColumns.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
	        if (index < 0)
	        	id = toLong(-1L, uri.getPathSegments().get(1));
		}
	}

	public static void generateData(ContentResolver contentResolver, Uri uri, boolean clear, Object[][] records) {
		assertTrue(contentResolver.getType(uri).startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
		if (clear) {
			contentResolver.delete(uri, null, null);
		}
		try {
			Class<?> c = Class.forName(uri.getAuthority() + "Provider");
			Method contentValues = c.getDeclaredMethod("contentValues", Integer.TYPE, Object[].class);
			for (int i = 0; i < records.length; i++) {
				Object[] record = records[i];
				ContentValues values = (ContentValues) contentValues.invoke(null, 0, record);
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
		});
	}
    
    public void testData() throws Exception {
		String dbName = NotePadProvider.DATABASE_NAME;
		mActivity.deleteDatabase(dbName);
		generateTestData(mActivity);
		keepTestData(strings(dbName));
    }
    
    public void testData3() throws Exception {
    	for (String database : databases(mActivity)) 
    		mActivity.deleteDatabase(database);
    	String helloVm = readAsset(mActivity, "hello.vm");
    	String[] states = getStateStrings(mActivity);
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
					join("\n", states) +
					"#end\n#if($var)$var\n#end", null, now() }, 
			{ 5L, "cursor", "#cursor(\"plants\",$var," +
					quoted(PlantInfo.Plants.CONTENT_URI.toString()) + "," +
					"[" + quoted(PlantInfo.Plants.NAME) + 
					"," + quoted(PlantInfo.Plants._ID) + 
					"," + quoted(PlantInfo.Plants.BOTNAME) + 
					"," + quoted(PlantInfo.Plants.BOTFAMILY) + 
					"])#if($var)$var\n#end", null, now() }, 
			{ 6L, "random", "$math.random is a random number", null, now() }, 
			{ 7L, "random2", "#set($lower=1)#set($upper=20)" +
					"$math.random($lower, $upper) is a random number between $lower and $upper", null, now() }, 
			{ 8L, "planets", "#set( $Planets = ['Mercury', 'Earth', 'Mars', 'Venus'] )$Planets\n" +
					"#set( $pLanets = ['Jupiter', 'Saturn', 'Neptune'] )$pLanets\n" +
					"#set( $plAnets = ['Uranus', 'Pluto', 'Neptune'] )$plAnets\n" +
					"#set( $plaNets = ['Jupiter', 'Saturn', 'Mars'] )$plaNets\n" +
					"#set( $planEts = ['Jupiter', 'Venus', 'Neptune'] )$planEts\n" +
					"#set( $planeTs = ['Earth', 'Saturn', 'Neptune'] )$planeTs\n" +
					"#set( $planetS = ['Mercury', 'Saturn', 'Neptune'] )$planetS\n" +
					"#set( $Planets = ['Jupiter', 'Earth', 'Neptune'] )$Planets\n" +
					"#set( $pLanets = ['Jupiter', 'Saturn', 'Mars'] )$pLanets\n" +
					"#set( $planets = ['Venus', 'Saturn', 'Neptune'] )\n" +
					"$planets", null, now() }, 
			{ 9L, "hello", helloVm, null, now() }, 
		});
		assertEquals(9, recordCount(mActivity, NotePadProvider.contentUri(1)));
		
		keinFehlerImSystemm();
	    	
		generateData(mActivity.getContentResolver(), Plants.CONTENT_URI, true, new Object[][] {
			{ 1L, "Paradeiser", "Nachtschattengewächse", "Solanum lycopersicum", "Solanaceae", "xitomatl" }, 	
			{ 2L, "chili pepper", "nightshade", "Capsicum", "Solanaceae", "xilli" }, 	
			{ 3L, "Melanzani", "Nachtschattengewächse", "Solanum melongena", "Solanaceae", "ratatouille" }, 	
			{ 4L, "potato", "nightshade", "Solanum tuberosum", "Solanaceae", "tartufolo" }, 	
			{ 5L, "Tobak", "Nachtschattengewächse", "Nicotiana tabacum", "Solanaceae", "tobacco" }, 	
		});
		assertEquals(5, recordCount(mActivity, Plants.CONTENT_URI));
		
		generateData(mActivity.getContentResolver(), Weathers.CONTENT_URI, true, new Object[][] {
			{ 1L, "here", "overcast", 11.1f, 1f, -1f, 0l, now() }, 	
		});
		assertEquals(1, recordCount(mActivity, Weathers.CONTENT_URI));
	        
		keepTestData(databases(mActivity));
    }

    public static String[] getStateStrings(Context context) {
    	try {
			InputStream is = context.getResources().openRawResource(R.raw.states);
			String res = readAll(new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8"))));
			return toStrings(((ValList) walkJSON(null, new JSONArray(res), null)));
		} catch (Exception e) {
			fail(e.getMessage());
			return null;
		}
    }

    private Cursor notesCursor(int tableIndex, String selection, String... selectionArgs) {
		Cursor cursor = mActivity.managedQuery(
        		NotePadProvider.contentUri(tableIndex), 
        		NotePadProvider.FULL_PROJECTION, 
        		selection, selectionArgs,
        		NoteColumns.DEFAULT_SORT_ORDER);
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
		
        ValMap map = getResults(cursor, 
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
		
		ContentResolver contentResolver = mActivity.getContentResolver();
        setContentObserver(contentResolver, NoteColumns.CONTENT_URI);
		
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
		
		CustomContext noteContext = new CustomContext(noteMap(1, ""));
		assertEquals(6, noteContext.getKeys().length);
		
		com.applang.UserContext.setupVelocity(mActivity, true, getPackageNameByClass(R.class));

		ValList list = vlist();
        Cursor cursor = notesCursor(0, "title like ?", "Velocity%");
        assertTrue(cursor.moveToFirst());
        long id = 0;
        do {
                String note = cursor.getString(2);
                note = evaluate(noteContext, note, "notes");
                assertFalse(note.contains("$"));
                if (!note.contains("Fehler")) {
                        long refId = cursor.getLong(0);
                        ContentValues values = NotePadProvider.contentValues(0, ++id, "Fehler", null, refId, null);
                        list.add(values);
                }
                System.out.println(note);
        } while (cursor.moveToNext());
        cursor.close();
        for (Object item : list) {
                ContentValues values = (ContentValues) item;
                Uri uri = contentResolver.insert(NotePadProvider.contentUri(2), values);
                assertEquals(NoteColumns.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
        }
		
		noteMap(2, "");
		
		for (int i = 0; i < 2; i++) 
			assertEquals(
					new int[]{3,6,2}[i], 
					NotePadProvider.countNotes(contentResolver, i, "", null)[0].intValue());
		
    	keepTestData(strings(NotePadProvider.DATABASE_NAME));
    	
		assertEquals(1, contentResolver.delete(NotePadProvider.contentUri(0), NoteColumns.TITLE + "=?", new String[]{"Velocity2"}));
		
		for (int i = 0; i < 2; i++) 
			assertEquals(
					new int[]{2,6,1}[i], 
					NotePadProvider.countNotes(contentResolver, i, "", null)[0].intValue());
        
        assertEquals(androidLevel < 1 ? 15 : 5, contentObservations);
    }

	public void testNotePadProvider2() throws Exception {
		mActivity.deleteDatabase(NotePadProvider.DATABASE_NAME);
		long now = now();
		Object[][] records = new Object[][] {
			{ 1L, "kein", "Kein", null, now }, 
			{ 2L, "fehler", "Fehler", null, now }, 
			{ 3L, "efhler", "eFhler", null, now }, 
			{ 4L, "ehfler", "ehFler", null, now }, 
			{ 5L, "im", "im", null, now }, 
			{ 6L, "system", "System", null, now }, 
			{ 1L, "Velocity1", "$kein $fehler $im $system", now, now }, 
			{ 2L, "Velocity2", "$kein $efhler $im $system", now, now }, 	
			{ 3L, "Velocity3", "$kein $ehfler $im $system", now, now }, 	
		};
		generateNotePadData(mActivity, true, records);
		ContentResolver contentResolver = mActivity.getContentResolver();
        for (int i = 0; i < 2; i++) 
        	switch (i) {
			case 0:
				assertEquals(3, NotePadProvider.getTitles(contentResolver, i, null, null).length);
				for (int j = 6; j < 9; j++) {
					long id = (Long) records[j][0];
					assertEquals(id, NotePadProvider.getIdOfNote(contentResolver, i, 
							NoteColumns.CREATED_DATE + "=? and " + NoteColumns.TITLE + "=?", 
							strings("" + records[j][3], "" + records[j][1])));
					assertTrue(NotePadProvider.fetchNoteById(id, contentResolver, i, null));
				}
				break;
			case 1:
				assertEquals(6, NotePadProvider.getTitles(contentResolver, i, null, null).length);
				for (int j = 0; j < 6; j++) {
					long id = (Long) records[j][0];
					assertEquals(id, NotePadProvider.getIdOfNote(contentResolver, i, 
							NoteColumns.CREATED_DATE + " is null and " + NoteColumns.TITLE + "=?", 
							strings("" + records[j][1])));
					assertTrue(NotePadProvider.fetchNoteById(id, contentResolver, i, null));
				}
				break;
			}
    }

    public void testWeatherInfoProvider() throws IOException {
		mActivity.deleteDatabase(WeatherInfoProvider.DATABASE_NAME);
		
        ContentResolver contentResolver = mActivity.getContentResolver();
        setContentObserver(contentResolver, Weathers.CONTENT_URI);
        
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
        		WeatherInfoProvider.FULL_PROJECTION, 
        		Weathers.LOCATION + "=?", new String[]{"here"},
        		Weathers.DEFAULT_SORT_ORDER);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals("here", cursor.getString(2));
        assertEquals("overcast", cursor.getString(1));
        assertEquals(11.1f, cursor.getFloat(3));
        cursor.close();
        
        assertEquals(3, contentObservations);
    };

    public void testPlantInfoProvider() throws IOException {
		mActivity.deleteDatabase(PlantInfoProvider.DATABASE_NAME);
        ContentResolver contentResolver = mActivity.getContentResolver();
        setContentObserver(contentResolver, PlantInfoProvider.contentUri(0));
		generateData(contentResolver, PlantInfoProvider.contentUri(0), false, new Object[][] {
			{ 1L, "Paradeiser", "Nachtschattengewächse", "Solanum lycopersicum", "Solanaceae", "" }, 	
		});
        ContentValues values = new ContentValues();
        values.put(Plants.GROUP, "xitomatl");
        assertEquals(1, contentResolver.update(Plants.CONTENT_URI, 
				values, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"}));
    	
        Cursor cursor = contentResolver.query(
        		Plants.CONTENT_URI, 
        		PlantInfoProvider.FULL_PROJECTION, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals("Paradeiser", cursor.getString(1));
        assertEquals("Solanum lycopersicum", cursor.getString(3));
        assertEquals("xitomatl", cursor.getString(5));
        cursor.close();
        assertEquals(2, contentObservations);	//	1 insert, 1 update
    };

    private String[] PROJECTION_ID = new String[] {
            Plants._ID, // 0
            
    };

    public void testgetAllEntries() throws IOException {
      	ContentResolver contentResolver = mActivity.getContentResolver();
		contentResolver.delete(Plants.CONTENT_URI, null, null);
		
		assertEquals(0, contentResolver.query(
			Plants.CONTENT_URI, 
			PlantInfoProvider.FULL_PROJECTION, 
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
    
	@SuppressWarnings("rawtypes")
	public void testMisc() throws Exception {
		File filesDir = mActivity.getFilesDir();
		System.out.printf("filesDir : %s\n", filesDir.getPath());
		FileOutputStream outStream = mActivity.openFileOutput("hello", Context.MODE_PRIVATE);
		outStream.write("hello".getBytes());
		outStream.close();
		System.out.printf("fileList : %s\n", asList(mActivity.fileList()));
		System.out.printf("dir 'xxx' : %s\n", mActivity.getDir("xxx", Context.MODE_PRIVATE));
		
		for (String pkg : providerPackages) {
			Class[] cls = getLocalClasses(pkg, mActivity);
			System.out.println(com.applang.Util.toString(cls));
			for (Class cl : filter(asList(cls), false, new Predicate<Class>() {
				public boolean apply(Class c) {
					String name = c.getName();
					return !name.contains("$") && !name.endsWith("Provider");
				}
			}))
				System.out.println(cl.toString());
		}
    	ValList list = contentAuthorities(providerPackages, mActivity);
		System.out.println(list);
		System.out.println(asList(databases(mActivity)));
		
    	ContentResolver contentResolver = mActivity.getContentResolver();
    	list = vlist();
		Uri uri = NotePadProvider.contentUri(null);
		Cursor cursor = contentResolver.query(uri, 
				null, 
				"select name from sqlite_master where type = 'table'", 
				null, 
				null);
		if (cursor.moveToFirst())
			do {
				list.add(cursor.getString(0));
			} while (cursor.moveToNext());
		cursor.close();
		System.out.println(list.toString());
		
    	Map<String,String> anweisungen = UserContext.directives();
		for (String key : anweisungen.keySet()) {
			System.out.println(anweisungen.get(key));
		}
	}
}
