package com.applang;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class Util 
{
	/**
	 * @param <T>	genericized return type
	 * @param defaultParam	the value returned if the indexed value doesn't exist in the <code>Object</code> array
	 * @param index	the <code>int</code> indicating the value in the <code>Object</code> array
	 * @param params	the optional array of <code>Object</code> values
	 * @return	if the value is not existing <code>null</code> is returned
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T param(T defaultParam, int index, Object... params) throws ClassCastException {
		if (params != null && index > -1 && params.length > index)
			try {
				T returnValue = (T)params[index];
				return returnValue;
			} catch (ClassCastException e) {}

		return defaultParam;
	}

	public static <T> boolean isAvailable(int i, T[] array) {
		return array != null && 
				i > -1 && 
				i < array.length && 
				array[i] != null;
	}

	public static <T> boolean isNullOrEmpty(T[] array) {
		return array == null || array.length < 1;
	}

	public static boolean notNullOrEmpty(String value) {
		return value != null && value.length() > 0;
	}

	public static <T> String join(String delimiter, T... params) {
	    StringBuilder sb = new StringBuilder();
	    Iterator<T> iter = new ArrayList<T>(Arrays.asList(params)).iterator();
	    if (iter.hasNext())
	        do {
		        sb.append(String.valueOf(iter.next()))
		        	.append(iter.hasNext() ? delimiter : "");
		    }
		    while (iter.hasNext());
	    return sb.toString();
	}

    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return new String[]{};
        } else {
            return text.split(expression, -1);
        }
    }

	public static double delta = 0.00001;

	public static String formatAmount(double value) {
		return String.format("%.2f", value);
	}

	public static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static String timestamp(Date date) {
		return timestampFormat.format(date);
	}
    
    public static String timestampNow() {
		return timestamp(new Date());
	}
 
	public static String packageName() {
		return "com.applang";
	}

	public static String pathToDatabases() {
		return "data/" + packageName() + "/databases";
	}

	public static String databaseName() {
		return "data";
	}

	public static File databasesDir() {
		return new File(new File(getDataDirectory()), pathToDatabases());
	}

	public static File databaseFile() {
		return new File(databasesDir(), databaseName());
	}

	public static String getDataDirectory() {
		String dir = System.getProperty("data.dir");
		if (notNullOrEmpty(dir))
			return dir;
		else
			return System.getProperty("user.dir");
	}

	public static void setDataDirectory(String dir) {
		System.setProperty("data.dir", dir);
	}
}
