package com.applang.berichtsheft.test;

import static com.applang.Util1.*;
import static com.applang.Util2.*;

import java.io.File;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.test.*;

public class InfraTests<T extends Activity> extends ActivityInstrumentationTestCase2<T>
{
	public InfraTests(String pkg, Class<T> activityClass) {
		super(pkg, activityClass);
	}

	protected Solo solo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
		
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        dbFileNames = databases(mActivity);
		
        assertTrue(impex(mActivity, dbFileNames, true));		//	Export
        
		solo = new Solo(getInstrumentation(), mActivity);
    }
    
    @Override
    protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
		
		assertTrue(impex(mActivity, dbFileNames, false));		//	Import
        super.tearDown();
	}

	protected T mActivity;
	protected Instrumentation mInstrumentation;
	protected int androidLevel = 2;
	protected String[] dbFileNames;

    public static boolean impex(Activity activity, String[] fileNames, boolean export) {
		return ImpexTask.doImpex(activity, fileNames, export);
    };
    
    public void _testImpex() throws InterruptedException {
		for (boolean flag : new boolean[]{true,false}) {
			assertTrue(isExternalStorageAvailable());
			File directory = ImpexTask.directory(ImpexTask.getDatabasesPath(mActivity), !flag);
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
