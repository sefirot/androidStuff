package com.applang.berichtsheft.test;

import java.io.File;
import java.io.IOException;
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
        
        assertEquals(0, contentResolver.query(
        		Plants.CONTENT_URI, 
        		PROJECTION_PLANTS, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER).getCount());
    	
    	String fileName = "databases/plant_info.db";
        ImpexTask.doImpex(getActivity(), fileName, false);	//	Export
        
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
    	
        ImpexTask.doImpex(getActivity(), fileName, true);	//	Import
        
        assertEquals(0, contentResolver.query(
        		Plants.CONTENT_URI, 
        		PROJECTION_PLANTS, 
        		Plants.NAME + "=?", new String[]{"Paradeiser"},
                Plants.DEFAULT_SORT_ORDER).getCount());
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
