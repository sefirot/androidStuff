package com.applang.berichtsheft.test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.TestCase;
import junit.framework.TestSuite;

@SuppressWarnings({"rawtypes","unchecked"})
public final class TestUtils {
    private static final String TEST_CASES = "tests";
    private static final String ANT_PROPERTY = "${tests}";
    private static final String DELIMITER = ",";

    /**
     * Check to see if the test cases property is set. Ignores Ant's
     * default setting for the property (or null to be on the safe side).
     **/
    public static boolean hasTestCases() {
		String prop = System.getProperty( TEST_CASES );
        return prop != null && !prop.equals( ANT_PROPERTY );
    }

    /**
     * Create a TestSuite using the TestCase subclass and the list
     * of test cases to run specified using the TEST_CASES JVM property.
     *
     * @param testClass the TestCase subclass to instantiate as tests in
     * the suite.
     *
     * @return a TestSuite with new instances of testClass for each
     * test case specified in the JVM property.
     *
     * @throws IllegalArgumentException if testClass is not a subclass or
     * implementation of junit.framework.TestCase.
     *
     * @throws RuntimeException if testClass is written incorrectly and does
     * not have the approriate constructor (It must take one String
     * argument).
     **/
	public static TestSuite getSuite( Class testClass ) {
        if ( ! TestCase.class.isAssignableFrom( testClass ) ) {
            throw new IllegalArgumentException( "Must pass in a subclass of TestCase" );
        }
        TestSuite suite = new TestSuite();
        try {
			Constructor constructor = testClass.getConstructor( new Class[] { String.class } );
            List testCaseNames = getTestCaseNames();
            for ( Iterator testCases = testCaseNames.iterator(); testCases.hasNext(); ) {
                String testCaseName = (String) testCases.next();
                suite.addTest( (TestCase) constructor.newInstance( new Object[] { testCaseName } ) );
            }
        } catch ( Exception e ) {
            throw new RuntimeException( testClass.getName() + " doesn't have the proper constructor" );
        }
        return suite;
    }

    /**
     * Create a List of String names of test cases specified in the
     * JVM property in comma-separated format.
     *
     * @return a List of String test case names
     *
     * @throws NullPointerException if the TEST_CASES property
     * isn't set
     **/
    private static List getTestCaseNames() {
        if ( System.getProperty( TEST_CASES ) == null ) {
            throw new NullPointerException( "Test case property is not set" );
        }
        List testCaseNames = new ArrayList();
        String testCases = System.getProperty( TEST_CASES );
        StringTokenizer tokenizer = new StringTokenizer( testCases, DELIMITER );
        while ( tokenizer.hasMoreTokens() ) {
            testCaseNames.add( tokenizer.nextToken() );
        }
        return testCaseNames;
    }
}