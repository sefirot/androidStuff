package com.applang.berichtsheft.test;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.provider.NotePad;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.TestCase;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class InfraTests extends TestCase
{
    private static final String TAG = InfraTests.class.getSimpleName();

    String mPackage, mDataDir;
    Class<?> mActivityClass;
    boolean mInitialTouchMode = false;
    Intent mActivityIntent = null;
    
	public InfraTests() {
        mPackage = "com.applang.berichtsheft";
        mDataDir = "../Berichtsheft";
        mActivityClass = Activity.class;
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();
		
        mActivity = getActivity();
//        mInstrumentation = getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}

	public Activity getActivity() {
        Activity a;
		try {
			a = (Activity) mActivityClass.newInstance();
			a.setPackageInfo(mPackage, mDataDir);
		} catch (Exception e) {
			return null;
		}
        return (Activity) a;
    }

	protected Activity mActivity;
	protected Instrumentation mInstrumentation;

	public static void generateData(ContentResolver contentResolver, Uri uri, boolean clear, Object[][] records) {
		assertTrue(contentResolver.getType(uri).startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE));
		if (clear) {
			contentResolver.delete(uri, null, null);
		}
		try {
			Class<?> c = Class.forName(uri.getAuthority() + "Provider");
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
			
			int index = asList(record).indexOf("ROWID");
			if (index > -1) 
				record[index] = id;
			
			ContentValues values = NotePadProvider.contentValues(record);
	        int tableIndex = 
	    		record[3] == null ? 1 :
	    		(record[2] == null ? 2 : 0);
			uri = contentResolver.insert(NotePadProvider.contentUri(tableIndex), values);
	        assertEquals(NoteColumns.CONTENT_ITEM_TYPE, contentResolver.getType(uri));
	        
	        if (index < 0)
	        	id = toLong(-1L, uri.getPathSegments().get(1));
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

	public void testNotePadProvider() throws Exception {
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
		
		CustomContext noteContext = new CustomContext(noteMap(1, ""));
		assertEquals(6, noteContext.getKeys().length);
		
		com.applang.UserContext.setupVelocity(mActivity, true, "com.applang.berichtsheft");

		ContentResolver contentResolver = mActivity.getContentResolver();
		
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
				ContentValues values = NotePadProvider.contentValues(++id, "Fehler", null, refId, null);
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
    	
		assertEquals(1, contentResolver.delete(NotePadProvider.contentUri(0), NoteColumns.TITLE + "=?", strings("Velocity2")));
		
		for (int i = 0; i < 2; i++) 
			assertEquals(
					new int[]{2,6,1}[i], 
					NotePadProvider.countNotes(contentResolver, i, "", null)[0].intValue());
    }
	
	public void testContext() throws Exception {
		Context context = new Context();
		context.setPackageInfo(mPackage, mDataDir);
		String[] databaseList = context.databaseList();
		File dbFile = context.getDatabasePath(databaseList[0]);
		String dbPath = dbFile.getPath();
		assertEquals(fileOf(mDataDir, 
				"data",
				mPackage,
				"databases",
				NotePadProvider.DATABASE_NAME).getPath(), dbPath);
		
		dbPath = new File(dbPath).getCanonicalPath();
		Uri uri = new Uri.Builder()
			.scheme("file")
			.path(dbPath)
			.build();
		assertFalse(notNullOrEmpty(uri.getAuthority()));
		assertTrue(notNullOrEmpty(uri.getPath()));
		
		Uri uri2 = Uri.fromFile(dbFile);
		ValList tables = tables(context, uri2);
		println(tables);
		for (String table : NotePadProvider.DATABASE_TABLES)
			assertTrue(tables.contains(table));
		
		uri = uri.buildUpon()
			.fragment("notes")
			.query("" + 1)
			.build();
		uri = Uri.parse(uri.toString());
		println(uri);
		println(uri.getPath(), uri.getFragment(), uri.getQuery());
		uri = uri.buildUpon().query("").build();
		
    	Object[] params = new Object[]{0};
		ContentProvider contentProvider = new ContentProvider();
		Cursor cursor = contentProvider.query(uri, null, null, null, null);
		assertTrue(traverse(cursor, new Job<Cursor>() {
			public void perform(Cursor c, Object[] params) throws Exception {
				println(getRow(c));
				params[0] = 1 + (int)params[0];
			}
		}, params));
		contentProvider.close();
		assertEquals((int)params[0], recordCount(context, uri));
		
		uri = contentUri(NotePad.AUTHORITY, null);
		assertFalse(notNullOrEmpty(uri.getPath()));
		assertTrue(notNullOrEmpty(uri.getAuthority()));
		assertEquals("content://" + NotePad.AUTHORITY, uri.toString());
		uri2 = contentUri(NotePad.AUTHORITY, NotePadProvider.tableName(NotePadProvider.NOTES));
		assertEquals(NoteColumns.CONTENT_URI.toString(), uri2.toString());
		
		uri2 = Uri.parse(NotePad.AUTHORITY);
		assertTrue(notNullOrEmpty(uri2.getPath()));
		assertFalse(notNullOrEmpty(uri2.getAuthority()));
		assertEquals(uri.toString(), "content://" + uri2.toString());
		
		uri2 = Uri.parse(dbPath);
		assertTrue(notNullOrEmpty(uri2.getPath()));
		assertFalse(notNullOrEmpty(uri2.getAuthority()));
		assertEquals(Uri.fromFile(new File(dbPath)).toString(), "file://" + uri2.toString());
    }
}
