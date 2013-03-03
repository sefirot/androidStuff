package com.applang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class Util
{
	static {
		Locale.setDefault(Locale.GERMAN);
	}
	
    private static final int millisPerDay = 1000*60*60*24;
	private static Calendar calendar = Calendar.getInstance();
    private static Random random = new Random();

	static void setWeekDate(int year, int weekOfYear, int dayOfWeek) {
		while (dayOfWeek > 7) {
			dayOfWeek -= 7;
			weekOfYear += 1;
		}
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.WEEK_OF_YEAR, weekOfYear);
		calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
		setMidnight();
	}

	static void setMonthDate(int year, int month, int dayOfMonth) {
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		setMidnight();
	}

	static void setMidnight() {
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}
	/**
	 * calculates the milliseconds after 1970-01-01 for a given start of a day (midnight)
	 * @param year
	 * @param weekOrMonth week of year (1..53) or if negative number of month (-11..0)
	 * @param day day of week or day of month
	 * @return
	 */
	public static long timeInMillis(int year, int weekOrMonth, int day) {
		if (weekOrMonth < 1)
			setMonthDate(year, -weekOrMonth, day);
		else
			setWeekDate(year, weekOrMonth, day);
		return calendar.getTimeInMillis();
	}

	public static long timeInMillis(int year, int weekOrMonth, int day, int shift) {
		timeInMillis(year, weekOrMonth, day);
		calendar.add(Calendar.DATE, shift);
		return calendar.getTimeInMillis();
	}

	public static int daysToTodayFrom(int year, int weekOfYear, int dayOfWeek) {
		setWeekDate(year, weekOfYear, dayOfWeek);
	    Date today = new Date();
	    long diff = today.getTime() - calendar.getTimeInMillis();
	    return (int)(diff / millisPerDay);
	}

	public static long dateFromTodayInMillis(int days, Object... params) {
	    Date today = param(new Date(), 0, params);
	    boolean randomizeTimeOfDay = param(false, 1, params);
	    calendar.setTime(today);
		setMidnight();
		calendar.add(Calendar.DATE, days);
		long timeInMillis = calendar.getTimeInMillis();
		if (randomizeTimeOfDay)
			timeInMillis += random.nextInt(millisPerDay);
		return timeInMillis;
	}

	public static long getMillis(int days) {
		return days * millisPerDay;
	}

	public static long[] dayInterval(long time, int days) {
		long[] interval = new long[2];
		if (days < 0) {
			interval[0] = time - days * millisPerDay;
			interval[1] = time;
		}
		else {
			interval[0] = time;
			interval[1] = time + days * millisPerDay;
		}
		return interval;
	}

	public static long[] weekInterval(Date start, int weeks) {
		calendar.setTime(start);
		long millis = calendar.getTimeInMillis();
		calendar.add(Calendar.DATE, weeks * 7);
		long[] interval = new long[2];
		if (weeks < 0) {
			interval[0] = calendar.getTimeInMillis();
			interval[1] = millis;
		}
		else {
			interval[0] = millis;
			interval[1] = calendar.getTimeInMillis();
		}
		return interval;
	}

	public static long[] monthInterval(Date start, int months) {
		calendar.setTime(start);
		long millis = calendar.getTimeInMillis();
		calendar.add(Calendar.MONTH, months);
		long[] interval = new long[2];
		if (months < 0) {
			interval[0] = calendar.getTimeInMillis();
			interval[1] = millis;
		}
		else {
			interval[0] = millis;
			interval[1] = calendar.getTimeInMillis();
		}
		return interval;
	}

	public static String formatDate(long millis, Object...params) {
		String pattern = param("yyyy-MM-dd",0,params);
		SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
		return dateFormat.format(new Date(millis));
	}

	public static Date toDate(String dateString, Object...params) {
		try {
			String pattern = param("yyyy-MM-dd",0,params);
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			return dateFormat.parse(dateString);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static Long toTime(String dateString, Object...params) {
		Date date = toDate(dateString, params);
		if (date == null)
			return null;
		else
			return date.getTime();
	}

	public static long now() {
		return new Date().getTime();
	}

	public static boolean isType(Object prototype, Object o) {
		if (o == null)
			return prototype == null;
		else if (prototype == null)
			return o == null;
		else if (prototype instanceof Class<?>)
			return ((Class<?>)prototype).getName().equals(o.getClass().getName());
		else
			return prototype.getClass().getName().equals(o.getClass().getName());
	}
	
	public static boolean notNullOrEmpty(String value) {
		return value != null && value.length() > 0;
	}

	public static <T> boolean nullOrEmpty(T[] value) {
		return value == null || value.length < 1;
	}

	public static boolean nullOrEmpty(int[] value) {
		return value == null || value.length < 1;
	}

	public static boolean nullOrEmpty(long[] value) {
		return value == null || value.length < 1;
	}

	public static boolean nullOrEmpty(float[] value) {
		return value == null || value.length < 1;
	}

	public static boolean nullOrEmpty(double[] value) {
		return value == null || value.length < 1;
	}

	public static boolean isWhiteSpace(String s) {
		for (int i = 0; i < s.length(); i++) 
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		
		return true;
	}

    public static Integer toInt(Integer defaultValue, String value) {
    	Integer result;
        
        try {
        	result = Integer.parseInt(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
    }

    public static Long toLong(Long defaultValue, String value) {
    	Long result;
        
        try {
        	result = Long.parseLong(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
    }

    public static Float toFloat(Float defaultValue, String value) {
    	Float result;
        
        try {
        	result = Float.parseFloat(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
    }

    public static Double toDouble(Double defaultValue, String value) {
    	Double result;
        
        try {
        	result = Double.parseDouble(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
    }
    
    public static String stripUnits(String s) {
		int i = 0;
		while (i < s.length())
			if (Character.isDigit(s.charAt(i)) || "-+.,".contains(s.charAt(i) + "")) 
				i++;
			else
				break;
		return s.substring(0, i);
    }
	
	public static <T> boolean isAvailable(int index, T[] array) {
		return array != null && index > -1 && index < array.length && array[index] != null;
	}
	
	public static Object[] reduceDepth(Object[] params) {
		while (params != null && params.length == 1 && params[0] instanceof Object[])
			params = (Object[])params[0];

		return params;
	}
	/**
	 * @param <P>	type of the values in the parameter array
	 * @param <T>	genericized return type
	 * @param defaultParam	the value returned if the indexed value doesn't exist in the array
	 * @param index	indicating the value in the parameter array that is returned
	 * @param params	the parameter array
	 * @return	if the indicated value does not exist the value of defaultParam is returned
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <P extends Object, T extends P> T param(T defaultParam, int index, P... params) {
		if (params != null && index > -1 && params.length > index)
			try {
				T returnValue = (T)params[index];
				return returnValue;
			} catch (ClassCastException e) {}

		return defaultParam;
	}
	
	public static Boolean paramBoolean(Boolean defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof Boolean)
			return (Boolean)param;
		else
			return defaultParam;
	}
	
	public static Integer paramInteger(Integer defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof Integer)
			return (Integer)param;
		else
			return defaultParam;
	}
	
	public static Double paramDouble(Double defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof Double)
			return (Double)param;
		else
			return defaultParam;
	}
	
	public static String paramString(String defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof String)
			return (String)param;
		else
			return defaultParam;
	}
	
	public static File paramFile(File defaultParam, int index, Object... params) {
		if (params != null && index > -1 && params.length > index) {
			if (params[index] instanceof File)
				return (File)params[index];
			else if (params[index] instanceof String)
				return new File((String)params[index]);
		}

		return defaultParam;
	}
	
	public interface Function<T> {
		public T apply(Object...params);
	}
	
	public interface Callback {
		public void perform(Object...params);
	}
	
	public interface Job<T> {
		public void dispatch(T t, Object[] params) throws Exception;
	}
		 
	public static Object[] iterateFiles(boolean includeDirs, File dir, Job<Object> job, Object... params) throws Exception {
		params = reduceDepth(params);
		
		if (dir != null && dir.isDirectory()) {
			for (File file : dir.listFiles())
				if (file.isDirectory())
					iterateFiles(includeDirs, file, job, params);
				else if (file.isFile()) {
					job.dispatch(file, params);
					Integer n = paramInteger(null, 0, params);
					if (n != null)
						params[0] = n + 1;
				}
			
			if (includeDirs) {
				job.dispatch(dir, params);
				Integer n = paramInteger(null, 1, params);
				if (n != null)
					params[1] = n + 1;
			}
		}
		
		return params;
	} 
	
	public static MatchResult[] findAllIn(String input, Pattern pattern) {
		ArrayList<MatchResult> matches = new ArrayList<MatchResult>();
		
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) 
			matches.add(matcher.toMatchResult());
		
		return matches.toArray(new MatchResult[0]);
	}
	
	public static MatchResult findFirstIn(String input, Pattern pattern) {
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) 
			return matcher.toMatchResult();
		else
			return null;
	}

	public static boolean matches(String s, String regex) {
		return s.matches(regex);
	}
    /**
     * @param parts
     * @return	a <code>File</code> object constructed out of parts of the file path
     */
    public static File fileOf(String... parts) {
    	File file = null;
    	
    	for (int i = 0; i < parts.length; i++) {
    		String part = parts[i];
    		if (i == 0)
    			file = new File(part);
    		else if (part != null) {
    			part = part.replaceAll("(\\\\|/)+$", "");
    			file = new File(file, part);
    		}
    	}
    	
    	return file;
    }

	public static <T> String join(String delimiter, @SuppressWarnings("unchecked") T... params) {
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
    
    public static void copyContents(InputStream in, OutputStream out, Object... params) throws IOException {
		byte scoop[] = new byte[paramInteger(8192, 0, params)];
		
		int n;
		while ((n = in.read(scoop, 0, scoop.length)) > -1) 
			out.write(scoop, 0, n);
	}
	
	@SuppressWarnings("resource")
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if(!destFile.exists()) 
			destFile.createNewFile();
		
		FileChannel source = null;
		FileChannel destination = null;
		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
	        // previous code: destination.transferFrom(source, 0, source.size());
	        // to avoid infinite loops, should be:
	        long count = 0;
	        long size = source.size();              
	        while((count += destination.transferFrom(source, count, size-count))<size);
		}
		finally {
			if (source != null)
				source.close();
			if (destination != null)
				destination.close();
		}
	}
	
	public static boolean deleteDirectory(File dir) {
		try {
			iterateFiles(true, dir, new Job<Object>() {
				public void dispatch(Object f, Object[] parms) {
					((File)f).delete();
				}
			});
			
			return !dir.exists();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
	
	public static String readFromUrl(String url, String encoding) throws IOException {
		InputStream is = null;
		try {
			is = new URL(url).openStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName(encoding)));
			return readAll(rd);
		} finally {
			if (is != null)
				is.close();
		}
	}
	
	public static MatchResult[] excerptsFrom(InputStream is, Pattern pattern) throws IOException {
		String text = readAll(new BufferedReader(new InputStreamReader(is)));
		is.close();
		return findAllIn(text, pattern);
	}
	
	public static String contentsFromFile(File file) {
		Reader fr = null;
		try {
			fr = new InputStreamReader(new FileInputStream(file));
			return readAll(fr);
		} catch (Exception e) {
			return null;
		}
		finally {
			if (fr != null)
				try {
					fr.close();
				} catch (IOException e) {}
		}
	}
 
	public static File contentsToFile(File file, String s, Object... params) {
	    boolean append = paramBoolean(false, 0, params);
		Writer fw = null;
		try {
			fw = new OutputStreamWriter(new FileOutputStream(file, append));
			fw.write(s);
		} catch (Exception e) {
		}
		finally {
			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {}
		}
		return file;
	}

	/**
	 * @param params	optional parameters	
	 * <table border="1"><tr><th>index</th><th>description</th></tr><tr><td>0</td><td>a path as <code>String</code> to relativize against 'user.dir'</td></tr></table>
	 * @return	if path is null returns the absolute 'user.dir' system property otherwise the path relative to 'user.dir'.
	 */
	public static String relativePath(Object... params) {
		String base = paramString(System.getProperty("user.dir"), 1, params);
		String path = paramString(null, 0, params);
		if (path == null)
			return base;
		else
			return new File(base).toURI().relativize(new File(path).toURI()).getPath();
	}

	public static boolean fileExists(File file) {
		return file == null ? 
				false : 
				file.exists() && file.isFile();
	}
	
	public static class ValMap extends HashMap<String,Object>
	{
		public ValMap() {
			super();
		}

		public ValMap(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		public ValMap(int initialCapacity) {
			super(initialCapacity);
		}

		public ValMap(Map<? extends String, ? extends Object> m) {
			super(m);
		}
		
	}
	
	public static ValMap mappings = new ValMap();
	
	public static int clearMappings() {
		mappings = new ValMap();
		return 0;
	}
	
	public static String getMapping(String key) {
		Object value = mappings.get(key);
		return value == null ? "" : value.toString();
	}
	
	public static String setMapping(String key, String value) {
		mappings.put(key, value);
		return "";
	}
	
	@SuppressWarnings("unchecked")
	public static class BidiMap
	{
		@SuppressWarnings("rawtypes")
		Vector keys = new Vector();

		@SuppressWarnings("rawtypes")
		Vector values = new Vector();

		public Object[] getKeys() {
			return keys.toArray();
		}
		
		public Object[] getValues() {
			return values.toArray();
		}

		public void put(Object key, Object value) {
			keys.add(key);
			values.add(value);
		}

		public Object getKey(Object value) {
			int index = values.indexOf(value);
			return keys.get(index);
		}
		
		public Object getValue(Object key) {
			int index = keys.indexOf(key);
			return values.get(index);
		}

		public void removeKey(Object key) {
			int index = keys.indexOf(key);
			keys.remove(index);
			values.remove(index);
		}
		
		public void removeValue(Object value) {
			int index = values.indexOf(value);
			keys.remove(index);
			values.remove(index);
		}
		
	}
	
	/**
	 * @param d
	 * @param decimalPlace
	 * @return
	 */
	public static double round(double d, int decimalPlace) {
		// see the Javadoc about why we use a String in the constructor
		// http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
		java.math.BigDecimal bd = 
			new java.math.BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace, java.math.BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public static double absoluteZero = -273.15;

	@SuppressWarnings("unchecked")
	public static Object walkJSON(String prefix, Object o, Function<Object> function, Object...params) throws Exception {
		Object retval = o;
		
		if (o instanceof JSONObject) {
			JSONObject jo = (JSONObject) o;
			ValMap map = new ValMap();
			Iterator<String> it = jo.keys();
			while (it.hasNext()) {
				String key = it.next();
				String name = prefix + (prefix.length() > 0 ? "." : "") + key;
				Object value = jo.get(key);
				if (value.toString().startsWith("[")) 
					value = walkJSON(name, jo.getJSONArray(key), function, params);
				else if (value.toString().startsWith("{"))
					value = walkJSON(name, jo.getJSONObject(key), function, params);
				else if (function != null)
					value = function.apply(name, value, params);
				map.put(key, value);
			}
			retval = map;
		}
		else if (o instanceof JSONArray) {
			JSONArray ja = (JSONArray) o;
			ArrayList<Object> list = new ArrayList<Object>();
			for (int i = 0; i < ja.length(); i++) 
				list.add(walkJSON(prefix + String.format("(%d)", i), ja.get(i), function, params));
			retval = list;
		}
		else if (function != null)
			retval = function.apply(prefix, o, params);
		
		return retval;
	}

}
