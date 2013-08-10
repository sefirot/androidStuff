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
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.util.Log;

public class Util
{
    static final String TAG = Util.class.getSimpleName();

	static {
		Locale.setDefault(Locale.GERMAN);
	}
	
	private static Random random = new Random();
    private static final int millisPerDay = 1000*60*60*24;
	private static Calendar calendar = Calendar.getInstance(Locale.US);

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

	public static Calendar getCalendar() {
		return calendar;
	}

	public static int[] getCalendarDate(long time) {
		calendar.setTimeInMillis(time);
		return new int[] {calendar.get(Calendar.DAY_OF_MONTH), 
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.YEAR)};
	}
	/**
	 * calculates the milliseconds after 1970-01-01 for a given start of a day (midnight)
	 * @param year
	 * @param week week of year (1..53) or if zero or negative number of month (-11..0)
	 * @param day day of week or day of month
	 * @return
	 */
	public static long timeInMillis(int year, int week, int day) {
		if (week < 1)
			return dateInMillis(year, -week, day);
		setWeekDate(year, week, day);
		return calendar.getTimeInMillis();
	}

	public static long dateInMillis(int year, int month, int day) {
		setMonthDate(year, month, day);
		return calendar.getTimeInMillis();
	}

	public static long dateInMillis(int year, int month, int day, int days) {
		setMonthDate(year, month, day);
		calendar.add(Calendar.DATE, days);
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

	public static String timestampFormat = "yyyy-MM-dd HH:mm:ss.SSS";

	//	used in xsl scripts
	public static String formatDate(long millis, String pattern) {
		return new SimpleDateFormat(pattern, Locale.US).format(new Date(millis));
	}

	public static String formatDate(long millis, Object...params) {
//		return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL", millis);
		String pattern = param(timestampFormat,0,params);
		Locale locale = param(Locale.US,1,params);
		return new SimpleDateFormat(pattern, locale).format(new Date(millis));
	}

	public static Date toDate(String dateString, Object...params) {
		try {
			String pattern = param("yyyy-MM-dd", 0, params);
			Locale locale = param(Locale.US,1,params);
			return new SimpleDateFormat(pattern, locale).parse(dateString);
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

	//	used in xsl scripts
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
	
	public static String stringValueOf(Object value) {
		return value != null ? String.valueOf(value) : "";
	}

	public static boolean notNullOrEmpty(Object value) {
		return stringValueOf(value).length() > 0;
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

	//	used in xsl scripts
	public static boolean isWhiteSpace(String s) {
		for (int i = 0; i < s.length(); i++) 
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		
		return true;
	}

    public static Boolean toBool(Boolean defaultValue, String value) {
    	Boolean result;
        
        try {
        	result = Boolean.parseBoolean(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
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
			if (Character.isDigit(s.charAt(i)) || "-+.,".contains(String.valueOf(s.charAt(i)))) 
				i++;
			else
				break;
		return s.substring(0, i);
    }
	
	public static boolean isAvailable(int index, List<?> list) {
		return list != null && index > -1 && index < list.size() && list.get(index) != null;
	}
	
	public static <T> boolean isAvailable(int index, T[] array) {
		return array != null && index > -1 && index < array.length && array[index] != null;
	}
	
	public static Object[] reduceDepth(Object[] params) {
		while (params != null && params.length == 1 && params[0] instanceof Object[])
			params = (Object[])params[0];
		return params;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T valueOrElse(T elseValue, Object value) {
		if (value != null)
			try {
				return (T)value;
			} catch (Exception e) {}
		return elseValue;
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
	
	public static int param(int defaultParam, int index, int... params) {
		if (params != null && index > -1 && params.length > index)
			return params[index];
		else
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
	
	public static Object[] objects(Object...params) {
		return params;
	}
	
	public static String[] strings(String...params) {
		return params;
	}
	
	public static String[] toStrings(Object...params) {
		return arraycast(params, strings());
	}
	
	public static <T> String[] toStrings(Collection<T> collection) {
		return collection.toArray(strings());
	}
	
	public static <T> List<T> list(T[] array) {
		return Arrays.asList(array);
	}
	
	public static <T> Set<T> sorted(Collection<T> collection) {
		return new TreeSet<T>(collection);
	}
	
	public interface Function<T> {
		public T apply(Object...params);
	}
	
	public interface Job<T> {
		public void perform(T t, Object[] params) throws Exception;
	}
		 
	public static Object[] iterateFiles(boolean includeDirs, File dir, Job<Object> job, Object... params) throws Exception {
		params = reduceDepth(params);
		
		if (dir != null && dir.isDirectory()) {
			for (File file : dir.listFiles())
				if (file.isDirectory())
					iterateFiles(includeDirs, file, job, params);
				else if (file.isFile()) {
					job.perform(file, params);
					Integer n = paramInteger(null, 0, params);
					if (n != null)
						params[0] = n + 1;
				}
			
			if (includeDirs) {
				job.perform(dir, params);
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

	//	used in xsl scripts
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

	public static ValList split(String string, String regex) {
		String[] parts = string.split(regex);
		return new ValList(list(parts));
    }

	public static <T> String join(String delimiter, @SuppressWarnings("unchecked") T...params) {
	    StringBuilder sb = new StringBuilder();
	    Iterator<T> iter = new ArrayList<T>(list(params)).iterator();
	    if (iter.hasNext())
	        do {
		        sb.append(String.valueOf(iter.next()))
		        	.append(iter.hasNext() ? delimiter : "");
		    }
		    while (iter.hasNext());
	    return sb.toString();
	}

    public static String quoted(String string) {
    	return enclose("\"", string, "\"");
	}

    public static String enclose(String decor, String string, Object...params) {
    	decor = valueOrElse("", decor);
    	string = decor.concat(valueOrElse("", string));
    	if (params.length < 1)
    		return string.concat(decor);
    	
    	for (int i = 0; i < params.length; i++) 
    		string = string.concat(param("", i, params));
		
    	return string;
	}

    public static String strip(boolean atStart, String string, Object...params) {
    	for (int i = 0; i < params.length; i++) {
    		Object param = param("", i, params);
			String pad = String.valueOf(param);
	    	if (atStart && string.startsWith(pad))
	    		string = string.substring(pad.length());
	    	if (!atStart && string.endsWith(pad))
	    		string = string.substring(0, string.length() - pad.length());
    	}
    	return string;
	}

	public static String trim(boolean left, String s, String...regex) {
		for (String rgx : regex) {
			Matcher m = Pattern.compile(left ? "^" + rgx : rgx + "$").matcher(s);
		    StringBuffer sb = new StringBuffer();
		    while (m.find())
		    	m.appendReplacement(sb, "");
		    m.appendTail(sb);
		    s = sb.toString();
		}
		return s;
	}
    
    public static void copyContents(InputStream in, OutputStream out, Object...params) throws IOException {
		byte scoop[] = new byte[paramInteger(4096, 0, params).intValue()];
		
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
				public void perform(Object f, Object[] parms) {
					((File)f).delete();
				}
			});
			
			return !dir.exists();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String readAll(Reader rd, Object...params) throws IOException {
		Integer chars = paramInteger(null, 0, params);
		StringBuilder sb = new StringBuilder();
		int cp, i = 0;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
			if (chars != null && ++i >= chars)
				break;
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
	
	//	used in scripts
	public static String contentsFromFile(File file) {
		return contentsFromFile(file, null);
	}
	
	public static String contentsFromFile(File file, Integer chars) {
		Reader fr = null;
		try {
			fr = new InputStreamReader(new FileInputStream(file));
			return readAll(fr, chars);
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
	 
	//	used in scripts
	public static File contentsToFile(File file, String s) {
		return contentsToFile(file, s, false);
	}
	 
	public static File contentsToFile(File file, String s, boolean append) {
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
	public static String relativePath(Object...params) {
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

	public static boolean fileExists(String path) {
		return notNullOrEmpty(path) ? 
				fileExists(new File(path)) : 
				false;
	}
	
	public static class ValList extends ArrayList<Object>
	{
		public ValList() {
			super();
		}
		public ValList(Collection<? extends Object> c) {
			super(c);
		}
		public ValList(int initialCapacity) {
			super(initialCapacity);
		}
		@Override
		public Object get(int index) {
			if (index < 0)
				index = size() + index;
			return super.get(index);
		}
		@Override
		public Object remove(int index) {
			if (index < 0)
				index = size() + index;
			return super.remove(index);
		}
	}
	
	public static ValList list() {	return new ValList();	}
	public static ValMap map() {	return new ValMap();	}
	
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
		
		public ValList getList(String key) {
			Object value = get(key);
			if (value == null) {
				ValList list = list();
				put(key, list);
				return list;
			}
			else if (value instanceof ValList)
				return (ValList) value;
			else
				return null;
		}
		
		public Object getListValue(String key, int index) {
			return getList(key).get(index);
		}
	}
	
	public static ValMap mappings = new ValMap();
	
	public static int clearMappings() {
		mappings = new ValMap();
		return 0;
	}
	
	//	used in xsl scripts
	public static String getMapping(String key) {
		Object value = mappings.get(key);
		return value == null ? "" : value.toString();
	}
	
	//	used in xsl scripts
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
	
	public static double kelvin2celsius(Object value) {
		double d;
		if (value instanceof Double)
			d = (Double) value;
		else 
			d = toDouble(Double.NaN, value.toString());
		return round(d + absoluteZero, 2);
	}

	/**
	 * @param <T>	type of the given array
	 * @param <U>	type of the cast array
	 * @param array	the given array
	 * @param a	prototype for the cast array
	 * @return	the cast array
	 */
	public static <T, U> U[] arraycast(T[] array, U[] a) {
		return list(array).toArray(a);
	}

	public static boolean[] toBooleanArray(List<Boolean> list) {
	    boolean[] primitives = new boolean[list.size()];
	    for (int i = 0; i < primitives.length; i++) {
	        primitives[i] = list.get(i).booleanValue();
	    }
	    return primitives;
	}

	public static int[] toIntArray(List<Integer> list) {
		int[] primitives = new int[list.size()];
	    for (int i = 0; i < primitives.length; i++) {
	        primitives[i] = list.get(i).intValue();
	    }
	    return primitives;
	}

	public static List<Integer> fromIntArray(int[] array) {
		List<Integer> list = new ArrayList<Integer>();
	    for (int i = 0; i < array.length; i++) {
	        list.add(array[i]);
	    }
	    return list;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] arrayappend(T[] array, T... elements) {
		ArrayList<T> list = new ArrayList<T>(list(array));
		list.addAll(new ArrayList<T>(list(elements)));
		return list.toArray(array);
	}

	public static <T> int arrayindexof(T element, T[] array) {
		return list(array).indexOf(element);
	}

	public static <T> void arrayreverse(T[] array) {
		int len = array.length;
		for (int i = 0; i < len / 2; i++) {
			T element = array[i];
			array[i] = array[len - i - 1];
			array[len - i - 1] = element;
		}
	}

	public static void arrayreverse(int[] array) {
		int len = array.length;
		for (int i = 0; i < len / 2; i++) {
			int element = array[i];
			array[i] = array[len - i - 1];
			array[len - i - 1] = element;
		}
	}

	public static Document xmlDocument(File file, Object...params) {
		FileInputStream fis = null;
	    try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			String systemId = param(null, 0, params);
			if (systemId == null)
				return db.parse(file);
			else {
				fis = new FileInputStream(file);
				return db.parse(fis, systemId);
			}
	    } catch (Exception e) {
			Log.e(TAG, "xmlDocument", e);
	    	return null;
	    }
	    finally {
	    	if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					Log.e(TAG, "xmlDocument", e);
				}
	    }
	}
    
    public interface Predicate<T> { boolean apply(T t); }

	public static <T> Collection<T> filter(Collection<T> target, boolean negate, Predicate<T> predicate) {
        Collection<T> result = new ArrayList<T>();
        for (T element: target) {
			if (predicate.apply(element)) {
				if (!negate)
	                result.add(element);
			}
			else if (negate)
                result.add(element);
        }
        return result;
    }

	public static <T> List<Integer> filterIndex(List<T> target, boolean negate, Predicate<T> predicate) {
		List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < target.size(); i++) {
        	T element = target.get(i);
			if (predicate.apply(element)) {
				if (!negate)
	                result.add(i);
			}
			else if (negate)
                result.add(i);
        }
        return result;
    }

	public static char decimalSeparator() {
		NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
		if (f instanceof DecimalFormat) {
			DecimalFormatSymbols symbols = ((DecimalFormat) f).getDecimalFormatSymbols();
			return symbols.getDecimalSeparator();
		}
		else
			return '.';
	}

	public static void delay(long millis) {
	    try {
	        millis += System.currentTimeMillis();
			while (System.currentTimeMillis() < millis) 
				Thread.yield();
	    } catch (Exception e) {}
	}
	
	public static final String TAB = "\t";
	public static final String NEWLINE = "\n";	//	System.getProperty("line.separator");
	public static final String TAB_REGEX = "\\t";
	public static final String NEWLINE_REGEX = "\\n";
	public static final String[] FOLD_MARKER = strings("{{{", "}}}");
	public static final String[] FOLD_MARKER_REGEX = strings("\\{\\{\\{", "\\}\\}\\}");

	public static String indentedLine(String line, Object level, Object...params) {
	    StringBuffer sb = new StringBuffer();
	    int indents = paramInteger(0, 0, params);
	    if (level instanceof String) {
	    	String indentString = String.valueOf(level);
		    if (notNullOrEmpty(indentString))
			    for (int i = 0; i < indents; ++i) 
			        sb.append(indentString);
		    else
		    	sb.append(indents + TAB);
	    }
	    else if (level instanceof Function<?>) 
	    	sb.append(((Function<?>) level).apply(params));
	    sb.append(line);
	    sb.append(NEWLINE);
	    return sb.toString();
	}
	
	public static boolean isSQLite(File file) {
		if (fileExists(file)) {
			String header16 = contentsFromFile(file, 16);
			return header16.startsWith("SQLite format 3");
		}
		return false;
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("rawtypes")
	public static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
	    List<Class> classes = new ArrayList<Class>();
	    if (directory.exists()) {
	    	File[] files = directory.listFiles();
	    	for (File file : files) {
	    		if (file.isDirectory()) {
	    			assert !file.getName().contains(".");
	    			classes.addAll(findClasses(file, packageName + "." + file.getName()));
	    		} else if (file.getName().endsWith(".class")) {
	    			classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
	    		}
	    	}
	    }
	    return classes;
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public static Class[] getLocalClasses(String packageName) throws ClassNotFoundException, IOException {
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    assert classLoader != null;
	    String path = packageName.replace('.', '/');
	    Enumeration<URL> resources = classLoader.getResources(path);
	    List<File> dirs = new ArrayList<File>();
	    while (resources.hasMoreElements()) {
	        URL resource = resources.nextElement();
	        dirs.add(new File(resource.getFile()));
	    }
	    ArrayList<Class> classes = new ArrayList<Class>();
	    for (File directory : dirs) {
	        classes.addAll(findClasses(directory, packageName));
	    }
	    return classes.toArray(new Class[classes.size()]);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T getConstantByName(String className, String innerClassName, String fieldName) {
		try {
			Class<?> c = Class.forName(className);
			if (notNullOrEmpty(innerClassName)) {
				Class<?>[] declaredClasses = c.getDeclaredClasses();
				c = null;
				for (Class<?> inner : declaredClasses) 
					if (innerClassName.equals(inner.getSimpleName())) 
						c = inner;
			}
			if (c != null)
				for (Field field : c.getDeclaredFields()) 
					if (fieldName.equals(field.getName()))
						return (T)field.get(null);
		} catch (Exception e) {
			Log.e(TAG, "getConstant", e);
		}
		return null;
	}

}
