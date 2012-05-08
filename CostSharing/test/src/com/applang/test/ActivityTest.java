package com.applang.test;

import java.io.File;

import junit.framework.Test;

import com.applang.*;

import android.os.Environment;
import android.test.*;
import android.test.suitebuilder.TestSuiteBuilder;

public class ActivityTest extends ActivityInstrumentationTestCase2<CostSharingActivity> 
{
	public static void main(String[] args) {
//		TestSuite suite = new TestSuite(CashPointTest.class);
//		InstrumentationTestRunner.run(suite);
	}
	
	public static Test suite() {
	    return new TestSuiteBuilder(CostSharingTest.class).
	    		includePackages("com.applang.test.CostSharingTest").build();
	}

	public ActivityTest() {
		super("com.applang", CostSharingActivity.class);
	}
	
	public ActivityTest(String method) {
		this();
	}

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    	
    	File dbDir = new File(Environment.getDataDirectory(), Util.pathToDatabases());
    	assertTrue(new File(dbDir, Util.databaseName()).exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
	}
}
