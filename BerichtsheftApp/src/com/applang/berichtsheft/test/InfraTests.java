package com.applang.berichtsheft.test;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.provider.NotePad;
import com.applang.provider.NotePadProvider;
import com.applang.provider.NotePad.NoteColumns;

import java.io.File;

import junit.framework.TestCase;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

public class InfraTests<T extends Activity> extends TestCase
{
    String mPackage, mDataDir;
    Class<?> mActivityClass;
    boolean mInitialTouchMode = false;
    Intent mActivityIntent = null;
    
	public InfraTests() {
		mPackage = getPackageNameByClass(R.class);
		mActivityClass = Activity.class;
	}

	public InfraTests(String pkg, Class<T> activityClass) {
		mPackage = pkg;
		mActivityClass = activityClass;
	}

    @Override
    protected void setUp() throws Exception {
        mDataDir = Environment.getDataDirectory().getPath();
        BerichtsheftApp.loadSettings();
        mActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
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
	protected int androidLevel = 0;

    public static boolean impex(Activity activity, String[] fileNames, boolean export) {
    	return true;
    };

	public void testContext() throws Exception {
		File dir = Environment.getDataDirectory();
		println(dir);
		assertTrue(dir.exists());
		Context context = new Context();
		context.setPackageInfo(mPackage, mDataDir);
		dir = context.getDataDirectory();
		assertTrue(dir.exists());

		String flavor = NotePad.AUTHORITY;
		String dbName = databaseName(flavor);
		File dbFile = context.getDatabasePath(dbName);
		makeSureExists(flavor, dbFile);
		String[] databaseList = context.databaseList();
		assertTrue(asList(databaseList).contains(dbName));
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
		contentProvider.setContext(context);
		Cursor cursor = contentProvider.query(uri, null, null, null, null);
		assertEquals(cursor.getCount() > 0, 
			traverse(cursor, new Job<Cursor>() {
				public void perform(Cursor c, Object[] params) throws Exception {
					println(getRow(c));
					params[0] = 1 + (Integer)params[0];
				}
			}, 
			params));
		contentProvider.close();
		assertEquals((Integer)params[0], (Integer)recordCount(context, uri));
		
		uri = contentUri(flavor, null);
		assertFalse(notNullOrEmpty(uri.getPath()));
		assertTrue(notNullOrEmpty(uri.getAuthority()));
		assertEquals("content://" + flavor, uri.toString());
		uri2 = contentUri(flavor, NotePadProvider.tableName(NotePadProvider.NOTES));
		assertEquals(NoteColumns.CONTENT_URI.toString(), uri2.toString());
		
		uri2 = Uri.parse(flavor);
		assertTrue(notNullOrEmpty(uri2.getPath()));
		assertFalse(notNullOrEmpty(uri2.getAuthority()));
		assertEquals(uri.toString(), "content://" + uri2.toString());
		
		uri2 = Uri.parse(dbPath);
		assertTrue(notNullOrEmpty(uri2.getPath()));
		assertFalse(notNullOrEmpty(uri2.getAuthority()));
		assertEquals(Uri.fromFile(new File(dbPath)).toString(), "file://" + uri2.toString());
    }
}
