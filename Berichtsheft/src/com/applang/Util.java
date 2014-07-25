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
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Array;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class Util
{
    static final String TAG = Util.class.getSimpleName();

	static {
		Locale.setDefault(Locale.GERMAN);
	}
	
	private static Random random = new Random();
	
	public static Random getRandom() {
		return random;
	}
	
    private static final int millisPerHour = 1000*60*60;
    private static final int millisPerDay = millisPerHour * 24;
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
		return ints(
			calendar.get(Calendar.DAY_OF_MONTH), 
			calendar.get(Calendar.MONTH), 
			calendar.get(Calendar.YEAR)
		);
	}
	/**
	 * calculates the milliseconds after 1970-01-01 for a given start of a day (midnight)
	 * @param year
	 * @param week week of year (1..53) or if zero or negative number of month (-11..0)
	 * @param day day of week or day of month
	 * @return
	 */
	// NOTE used in xsl scripts
	public static long timeInMillis(int year, int week, int day) {
		if (week < 1)
			return dateInMillis(year, -week, day);
		setWeekDate(year, week, day);
		return calendar.getTimeInMillis();
	}

	// NOTE used in xsl scripts
	public static long timeInMillis(int year, int week, int day, int days) {
		if (week < 1)
			return dateInMillis(year, -week, day, days);
		setWeekDate(year, week, day);
		calendar.add(Calendar.DATE, days);
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
	
	public static long hoursFromDate(long date, int hours) {
		return date + hours * millisPerHour;
	}

	public static long getMillis(int days) {
		return days * millisPerDay;
	}

	public static long[] dayInterval(Long time, int days) {
		if (time == null)
			return null;
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

	// NOTE used in xsl scripts
	public static String formatDate(long millis, String pattern) {
		return new SimpleDateFormat(pattern, Locale.US).format(new Date(millis));
	}

	// NOTE used in BeanShell scripts
	public static String formatDate(Long millis, Object...params) {
		if (millis == null)	return null;
//		return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL", millis);
		String pattern = param(timestampFormat,0,params);
		Locale locale = param(Locale.US,1,params);
		return new SimpleDateFormat(pattern, locale).format(new Date(millis));
	}

	public static String[] formatDates(Long[] millis, Object...params) {
		return formatDates(toLongArray(asList(millis)), params);
	}

	public static String[] formatDates(long[] millis, Object...params) {
		String[] dates = new String[millis.length];
		for (int i = 0; i < dates.length; i++) 
			dates[i] = formatDate(millis[i], params);
		return dates;
	}

	public static Date toDate(String dateString, Object...params) {
		try {
			String pattern = param("yyyy-MM-dd", 0, params);
			Locale locale = param(Locale.US,1,params);
			return new SimpleDateFormat(pattern, locale).parse(dateString);
		} catch (Exception e) {
//			Log.e(TAG, "toDate", e);
			return null;
		}
	}
	
	// NOTE used in scripts
	public static Long toTime(String dateString, Object...params) {
		Date date = toDate(dateString, params);
		if (date == null) {
			if (notNullOrEmpty(dateString) && isAvailable(0, params)) {
				Locale locale = param(Locale.US, 1, params);
				if (Locale.US.equals(locale))
					locale = Locale.getDefault();
				else 
					locale = Locale.US;
				date = toDate(dateString, params[0], locale);
			}
			if (date == null)
				return null;
		}
		return date.getTime();
	}

	// NOTE used in xsl scripts
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
	
	public static String identity(Object...o) {
		int len = o.length;
		if (len < 1)
			return "";
		else if (len < 2)
			return o[0] == null ? "null" : o[0].getClass().getSimpleName() + "@" + Integer.toHexString(o[0].hashCode());
		String[] s = new String[len];
		while (len > 0)
			s[--len] = identity(o[len]);
		return Arrays.toString(s);
	}
	
	public static String stringValueOf(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	public static boolean notNullOrEmpty(Object value) {
		return stringValueOf(value).length() > 0;
	}

	public static boolean nullOrEmpty(Object value) {
		return !notNullOrEmpty(value);
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

	// NOTE used in xsl scripts
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

    public static Long fromHex(Long defaultValue, String value) {
    	Long result;
        try {
        	result = Long.parseLong(value, 16);
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
    
    public static <T> boolean notAvailable(int index, T[] array) {
    	return !isAvailable(index, array);
    }
	
	public static <T> boolean isAvailable(int index, T[] array) {
		return array != null && index > -1 && index < array.length && array[index] != null;
	}
	
	public static boolean isAvailable(int index, int[] array) {
		return array != null && index > -1 && index < array.length;
	}
	
	public static boolean notAvailable(int index, List<?> list) {
		return !isAvailable(index, list.toArray());
	}
	
	public static boolean isAvailable(int index, List<?> list) {
		return isAvailable(index, list.toArray());
	}
	
	public static Object[] reduceDepth(Object...params) {
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

	public static <T extends Object> T functionValueOrElse(T elseValue, Function<T> value, Object...params) {
		if (value != null)
			try {
				return value.apply(params);
			} catch (Exception e) {}
		return elseValue;
	}
	/**
	 * retrieves the indicated varargs-parameter from a parameter array
	 * @param <P>	type of the values in the parameter array
	 * @param <T>	genericized return type
	 * @param defaultParam	the value returned if the indexed value doesn't exist in the array
	 * @param index	indicating the value in the parameter array that is returned
	 * @param params	the parameter array
	 * @return	if the indicated value does not exist the value of defaultParam is returned
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <P extends Object, T extends P> T param(T defaultParam, int index, P...params) {
		if (params != null && index > -1 && params.length > index)
			try {
				T returnValue = (T)params[index];
				return returnValue;
			} catch (ClassCastException e) {
				Log.e(TAG, "param", e);
			}
		return defaultParam;
	}
	
	public static int param(int defaultParam, int index, int...params) {
		if (params != null && index > -1 && params.length > index)
			return params[index];
		else
			return defaultParam;
	}
	
	public static boolean param(boolean defaultParam, int index, boolean...params) {
		if (params != null && index > -1 && params.length > index)
			return params[index];
		else
			return defaultParam;
	}
	
	public static <P extends Object, T extends P> T param_T(T defaultParam, int index, P...params) {
		T param = param(defaultParam, index, params);
		if (isType(defaultParam, param))
			return param;
		else
			return defaultParam;
	}
	
	public static Boolean param_Boolean(Boolean defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof Boolean)
			return (Boolean)param;
		else
			return defaultParam;
	}
	
	public static Integer param_Integer(Integer defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof Integer)
			return (Integer)param;
		else
			return defaultParam;
	}
	
	public static Double param_Double(Double defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof Double)
			return (Double)param;
		else
			return defaultParam;
	}
	
	public static String param_String(String defaultParam, int index, Object... params) {
		Object param = param(defaultParam, index, params);
		if (param instanceof String)
			return (String)param;
		else
			return defaultParam;
	}
	
	public static File param_File(File defaultParam, int index, Object... params) {
		if (params != null && index > -1 && params.length > index) {
			if (params[index] instanceof File)
				return (File)params[index];
			else if (params[index] instanceof String)
				return new File((String)params[index]);
		}
		return defaultParam;
	}

	public static String toString(Object[][] o) {
		ValList list = vlist();
		for (Object object : (Object[])o) 
			list.add(Arrays.toString((Object[])object));
		String s = Arrays.toString(list.toArray());
		return strip(Constraint.START, strip(Constraint.END, s , "]") , "[")
				.replaceAll("(\\],) (\\[)", "$1\n\n$2");
	}

	public static String toString(ValMap o) {
		return String.valueOf(o).replaceAll("\\], ", "\\],\n");
	}

	public static String toString(Object o) {
		String name = o.getClass().getName();
		if (name.startsWith("[") && name.endsWith(";")) {
			String s = "";
			for (int i = 0; i < Array.getLength(o); i++) 
				s += (s.length() > 0 ? NEWLINE : "") + toString(Array.get(o, i));
			return enclose("[", s, "]");
		}
		return String.valueOf(o);
	}
	
	public static Object object(Object o) {
		return o;
	}
	
	public static Object _null() {
		return object(null);
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
	
	public static <T> List<T> asList(T[] array) {
		return Arrays.asList(array);
	}
	
	public static <T> Set<T> set(Collection<T> collection) {
		return new HashSet<T>(collection);
	}
	
	public static <T> Set<T> sortedSet(Collection<T> collection) {
		return new TreeSet<T>(collection);
	}
	
	public static int[] ints(int...params) {
		return params;
	}
	
	public interface Function<T> {
		public T apply(Object...params);
	}
	
	public interface Job<T> {
		public void perform(T t, Object[] parms) throws Exception;
	}
	
	public static Pattern clippingPattern(String clipper1, String clipper2) {
		String clipped1 = "[^" + clipper1 + "]";
		String clipped2 = "[^" + clipper1 + clipper2 + "]";
		String clipped3 = "[^" + clipper2 + "]";
		return Pattern.compile(
				"(" + clipped1 + "*)" + clipper1 + "(" + clipped2 + "+)" + clipper2 + "(" + clipped3 + "*)");
	}
	
	public static MatchResult[] findAllIn(String input, Pattern pattern) {
		ArrayList<MatchResult> matches = alist();
		
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

	// NOTE used in xsl scripts
	public static boolean matches(String s, String regex) {
		return s.matches(regex);
	}
    /**
     * @param parts
     * @return	a <code>File</code> object constructed from parts of the file path
     */
    public static File fileOf(String...parts) {
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

	public static ValList split(String string, String regex, int...limit) {
		String[] parts = strings();
		if (notNullOrEmpty(string))
			parts = limit.length < 1 ? string.split(regex) : string.split(regex, limit[0]);
		return vlist(parts);
    }

	public static <T> String join(String delimiter, T...params) {
	    StringBuilder sb = new StringBuilder();
	    Iterator<T> iter = asList(params).iterator();
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

    public static String htmlize(String text) {
    	if (isHtml(text))
    		return text;
    	else
    		return enclose("<html>", text.replaceAll(NEWLINE_REGEX, "<br>"), "</html>");
	}

	private static boolean isHtml(String text) {
		return text.substring(0, Math.min(text.length(), 20)).trim().toLowerCase().startsWith("<html>");
	}

    public static String deHtmlize(String text) {
    	if (isHtml(text)) {
    		text = text.trim();
    		text = text.substring(6, text.length() - 7);
    		return text.replaceAll("<br>", NEWLINE);
    	}
    	else
    		return text;
	}

    public static String enclose(String decor, Object o, Object...params) {
    	decor = stringValueOf(decor);
    	String string = decor.concat(stringValueOf(o));
    	if (params.length < 1)
    		return string.concat(decor);
    	for (int i = 0; i < params.length; i++) 
    		string = string.concat(param("", i, params));
    	return string;
	}

    public static String strip(Constraint constraint, String string, Object...params) {
    	for (int i = 0; i < params.length; i++) {
    		Object param = param("", i, params);
			String pad = String.valueOf(param);
			if (check(string, constraint, pad))
				switch (constraint) {
				case START:
					string = string.substring(pad.length());
					break;
				case END:
					string = string.substring(0, string.length() - pad.length());
					break;
				default:
				}
    	}
    	return string;
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
		byte scoop[] = new byte[param_Integer(4096, 0, params).intValue()];
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
	
	public static boolean deleteDirectory(File dir, Object...params) {
		try {
			Job<File> deleter = new Job<File>() {
				public void perform(File f, Object[] params) throws Exception {
					if (f.delete()) {
						int no = (Integer) params[0];
						Integer n = param_Integer(null, no, (Object[]) params[1]);
						if (n != null)
							params[no] = n + 1;
					}
				}
			};
			if (dir != null && dir.isDirectory()) {
				for (File file : dir.listFiles())
					if (file.isDirectory()) {
						if (isSymlink(file))
							deleter.perform(file, new Object[]{1, params});
						else
							deleteDirectory(file, params);
					}
					else if (file.isFile()) {
						deleter.perform(file, new Object[]{0, params});
					}
				deleter.perform(dir, new Object[]{1, params});
			}
			return !dir.exists();
		} catch (Exception e) {
			Log.e(TAG, "deleteDirectory", e);
			return false;
		}
	}
	
	// NOTE used in xsl scripts
	public static String canonicalPath(String filePath) {
		try {
			return new File(filePath).getCanonicalPath();
		} catch (Exception e) {
			return filePath;
		}
	}
	
	public static boolean isSymlink(File file) throws IOException {
		if (file == null)
			throw new NullPointerException("File must not be null");
		File canon;
		if (file.getParent() == null) {
			canon = file;
		} 
		else {
			File canonDir = file.getParentFile().getCanonicalFile();
			canon = new File(canonDir, file.getName());
		}
		if (canon.getCanonicalFile().equals(canon.getAbsoluteFile()))
			return false;
		else
			return true;
	}
	
	public static String readAll(Reader rd, Object...params) throws IOException {
		Integer chars = param_Integer(null, 0, params);
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
	
	// NOTE used in scripts
	public static String contentsFromFile(File file) {
		return contentsFromFile(file, null);
	}
	
	public static String contentsFromFile(File file, Integer chars) {
		Reader fr = null;
		try {
			fr = new InputStreamReader(new FileInputStream(file));
			return readAll(fr, chars);
		} catch (Exception e) {
			Log.e(TAG, "contentsFromFile", e);
			return null;
		}
		finally {
			if (fr != null)
				try {
					fr.close();
				} catch (IOException e) {
					Log.e(TAG, "contentsFromFile", e);
				}
		}
	}
	 
	// NOTE used in scripts
	public static File contentsToFile(File file, String s) {
		return contentsToFile(file, s, false);
	}
	 
	public static File contentsToFile(File file, String s, boolean append) {
		Writer fw = null;
		try {
			fw = new OutputStreamWriter(new FileOutputStream(file, append));
			fw.write(s);
		} catch (Exception e) {
			Log.e(TAG, "contentsToFile", e);
		}
		finally {
			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
					Log.e(TAG, "contentsToFile", e);
				}
		}
		return file;
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
		public ValList sizeAtLeast(int size) {
			while (size() < size)
				add(null);
			return this;
		}
		@Override
		public Object get(int index) {
			if (index < 0)
				index = size() + index;
			return super.get(index);
		}
		@Override
		public void add(int index, Object element) {
			if (index < 0)
				index = size() + index;
			super.add(index, element);
		}
		@Override
		public Object remove(int index) {
			if (index < 0)
				index = size() + index;
			return super.remove(index);
		}
	}
	
	public static <T> ValList vlist(T...values) {
		return new ValList(asList(values));
	}
	
	public static ValMap vmap(Object...params) {
		ValMap map = new ValMap();
		for (int i = 0; i < params.length; i+=2) 
			map.put(param_String("", i, params), 
					param(null, i + 1, params));
		return map;
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
		public ValList getList(String key) {
			Object value = get(key);
			if (value == null) {
				ValList list = vlist();
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
	
	public static ValMap mappings = vmap();
	
	public static int clearMappings() {
		mappings = vmap();
		return 0;
	}
	
	// NOTE used in xsl scripts
	public static String getMapping(String key) {
		Object value = mappings.get(key);
		return stringValueOf(value);
	}
	
	// NOTE used in xsl scripts
	public static String setMapping(String key, String value) {
		mappings.put(key, value);
		return "";
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
		return asList(array).toArray(a);
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
		List<Integer> list = alist();
	    for (int i = 0; i < array.length; i++) {
	        list.add(array[i]);
	    }
	    return list;
	}

	public static long[] toLongArray(List<Long> list) {
		long[] primitives = new long[list.size()];
	    for (int i = 0; i < primitives.length; i++) {
			primitives[i] = list.get(i).longValue();
	    }
	    return primitives;
	}

	public static <T> T[] arrayappend(T[] array, T...elements) {
		ArrayList<T> list = alist(array);
		list.addAll(asList(elements));
		return list.toArray(array);
	}

	public static <T> int arrayindexof(T element, T[] array) {
		return asList(array).indexOf(element);
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
	
	public static <T> ArrayList<T> alist() {
		return new ArrayList<T>();
	}
	
	public static <T> ArrayList<T> alist(T[] elements) {
		return new ArrayList<T>(asList(elements));
	}

    public static boolean embedsLeft(String whole, String part) {
		return whole != null && whole.contains(part) && !whole.startsWith(part);
	}

    public static boolean embedsRight(String whole, String part) {
		return whole != null && whole.contains(part) && !whole.endsWith(part);
	}

    public static boolean embeds(String whole, String part) {
		return whole != null && embedsLeft(whole, part) && embedsRight(whole, part);
	}

	public static String addPart(String string, String part) {
		ValList list = split(string, GLUE_REGEX);
		if (list.add(part))
			return join(GLUE, set(list).toArray());
		else
			return string;
	}

	public static String removePart(String string, String part) {
		ValList list = split(string, GLUE_REGEX);
		if (list.remove(part))
			string = join(GLUE, list.toArray());
		return string;
	}
	
	public enum Constraint {
		AMONG(2), START(1), MIDDLE(0), END(-1);
		
		final int index;

		Constraint(int index) {
			this.index = index;
		}
	}
	
	public static Boolean among(String string, String regex) {
		ValList list = split(string, GLUE_REGEX);
		for (Object element : list) {
			String part = (String) element;
			if (part.matches(regex))
				return true;
		}
		return false;
	}
	
	public static Boolean check(String string, Constraint constraint, String part) {
		if (string == null)
			return null;
		switch (constraint) {
		case START:
			return string.startsWith(part);
		case MIDDLE:
			return string.contains(part);
		case AMONG:
			return among(string, part);
		case END:
			return string.endsWith(part);
		default:
			return null;
		}
	}
	
	public static String findFirstFile(File dir, Constraint constraint, String part) {
		File[] files = dir.listFiles();
    	if (files != null)
    		for (File file : files) {
	    		String path = file.getPath();
				if (file.isFile() && check(path, constraint, part))
	    			return path;
	    	}
		return null;
	}
    
    public interface Predicate<T> { boolean apply(T t); }

	public static <T> Collection<T> filter(Collection<T> target, boolean negate, Predicate<T> predicate) {
        Collection<T> result = alist();
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
		List<Integer> result = alist();
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
	    } catch (Exception e) {
			Log.e(TAG, "delay", e);
	    }
	}
	
	public static final String PATH_SEP = System.getProperty("file.separator");	//	slash
	public static final String TAB = "\t";
	public static final String NEWLINE = "\n";	//	System.getProperty("line.separator");
	public static final String DOT = ".";
	public static final String GLUE = "|";
	public static final String TAB_REGEX = "\\t";
	public static final String NEWLINE_REGEX = "\\n";
	public static final String DOT_REGEX = "\\.";
	public static final String GLUE_REGEX = "\\|";
	public static final String WHITESPACE_REGEX = "\\s+";
	public static final String WHITESPACE_OR_NOTHING_REGEX = "\\s*";
	public static final String SOMETHING_OR_NOTHING_REGEX = ".*";
	public static final String[] FOLD_MARKER = strings("{{{", "}}}");
	public static final String[] FOLD_MARKER_REGEX = strings("\\{\\{\\{", "\\}\\}\\}");
	
	public static String wildcardRegex(Object wildcard, String...specials) {
		String chars = join("\\", specials);
		if (chars.length() > 0)
			chars = "\\".concat(chars);
		chars = enclose("[", chars.concat("\\\\w"), "]");
		return stringValueOf(wildcard)
        		.replaceAll("\\*", chars.concat("+"))
        		.replaceAll("\\?", chars);
	}

	public static String indentedLine(String line, Object level, Object...params) {
	    StringBuffer sb = new StringBuffer();
	    int indents = param_Integer(0, 0, params);
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
	
	public static String flatten(String s) {
		return s.replaceAll(NEWLINE_REGEX, TAB);
	}
	
	public static boolean isSQLite(File file) {
		if (fileExists(file)) {
			String header16 = contentsFromFile(file, 16);
			return header16.startsWith("SQLite format 3");
		}
		return false;
	}
	
	public static void setPrivateField(Object cl, Object inst, String fieldName, Object value) throws Exception {
		Class<?> c = cl instanceof Class<?> ? 
				(Class<?>) cl :
				Class.forName(stringValueOf(cl));
		Field field = c.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(inst, value);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Object cl, Object inst, String fieldName) {
		try {
			Class<?> c = cl instanceof Class<?> ? 
					(Class<?>) cl :
					Class.forName(stringValueOf(cl));
			Field field = c.getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(inst);
		} catch (Exception e) {
			Log.e(TAG, String.format("getPrivateField '%s' in class '%s'", fieldName, cl), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T getConstantByName(String name, String className, String innerClassName) {
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
					if (name.equals(field.getName()))
						return (T)field.get(null);
		} catch (Exception e) {
			Log.e(TAG, "getConstantByName", e);
		}
		return null;
	}

	public static String getPackageNameByClass(Class<?> clazz) {
		String name = clazz.getName();
		return name.substring(0, Math.max(0, name.lastIndexOf(".")));
	}

	public static BidiMultiMap bmap(int count) {
		return new BidiMultiMap(new ValList[count]);
	}

	public static class BidiMultiMap implements Serializable
	{
		private static final long serialVersionUID =
				UUID.fromString("fe32a444-91e4-4ad2-8b1e-3d4c2f7a26f3").getLeastSignificantBits();
	
		private ValList[] mLists;
		
		public BidiMultiMap(ValList...lists) {
			setLists(lists);
		}
			
		public void setLists(ValList...lists) {
			if (lists == null)
				lists = new ValList[0];
			int size = 0;
			for (int i = lists.length - 1; i >= 0; i--) {
				lists[i] = param_T(vlist(), i, lists);
				size = Math.max(size, lists[i].size());
			}
			while (lists.length < 2)
				lists = arrayappend(lists, vlist());
			for (int i = 0; i < lists.length; i++) {
				ValList values = lists[i];
				values.sizeAtLeast(size);
			}
			mLists = lists;
		}
		
		public ValList[] getLists() {
			return mLists;
		}

		private int[] mIndex = new int[] {0,1};
	
		public int[] getIndex() {
			return mIndex;
		}

		public void setIndex(int...listIndex) {
			for (int i = 0; i < mIndex.length; i++) {
				int j = param(mIndex[i], i, listIndex);
				boolean retval = j > -1 && j < mLists.length;
				if (retval)
					mIndex[i] = j;
			}
		}
	
		public ValList getKeys() {
			return mLists[mIndex[0]];
		}
		
		public ValList getValues(int...listIndex) {
			return isAvailable(0, listIndex) ? 
					(isAvailable(listIndex[0], mLists) ? mLists[listIndex[0]] : null) : 
					mLists[mIndex[1]];
		}
		
		@Override
		public String toString() {
			String separator = GLUE;
			StringBuilder sb = new StringBuilder();
			int keysIndex = mIndex[0];
			mIndex[0] = 0;
			int size = getKeys().size();
			for (int i = 0; i < size; i++) {
				sb.append(String.format("%s", getKeys().get(i)));
				for (int j = 1; j < mLists.length; j++) {
					Object val = mLists[j].get(i);
					sb.append(String.format(separator + "%s", String.valueOf(val)));
				}
				if (i < size - 1)
					sb.append("," + NEWLINE);
			}
			mIndex[0] = keysIndex;
			return enclose("{", sb.toString(), "}");
		}
	
		public void add(Object...values) {
			for (int i = 0; i < mLists.length; i++) {
				Object value = param(null, i, values);
				mLists[i].add(value);
			}
		}
	
		public void insert(int index, Object...values) {
			for (int i = 0; i < mLists.length; i++) {
				Object value = param(null, i, values);
				mLists[i].add(index, value);
			}
		}
		
		public void remove(int index) {
			for (int i = 0; i < mLists.length; i++) 
				mLists[i].remove(index);
		}
		
		public void removeAll() {
			for (int i = 0; i < mLists.length; i++) 
				mLists[i].clear();
		}
		
		public ValList get(int index) {
			ValList list = vlist();
			for (int i = 0; i < mLists.length; i++) 
				list.add(mLists[i].get(index));
			return list;
		}
		
		public Integer[] get(Object key) {
			ValList list = vlist();
			List<Object> keys = getKeys();
			int index;
			do {
				index = keys.lastIndexOf(key);
				if (index > -1) {
					list.add(index);
					keys = keys.subList(0, index);
				}
			}
			while (index > -1);
			Collections.reverse(list);
			return list.toArray(new Integer[0]);
		}
	
		public Object getKey(Object value) {
			int index = getValues().indexOf(value);
			if (index > -1)
				return getKeys().get(index);
			else
				return null;
		}
		
		public boolean removeKey(Object key) {
			int index = getKeys().indexOf(key);
			boolean retval = index > -1;
			if (retval)
				remove(index);
			return retval;
		}
		
		@SuppressWarnings("unchecked")
		public <T> T getValue(Object key, int...listIndex) {
			ValList list = getValues(listIndex);
			if (list == null)
				return null;
			int index = getKeys().indexOf(key);
			if (index > -1)
				return (T) list.get(index);
			else
				return null;
		}
		
		public <T> void putValue(Object key, T value, int...listIndex) {
			if (getKeys().indexOf(key) < 0)
				add(key);
			ValList list = getValues(listIndex);
			if (list != null) {
				int index = getKeys().indexOf(key);
				list.set(index, value);
			}
		}
		
		public boolean isUnique(Object value, int listIndex) {
			ValList list = getValues(listIndex);
			if (list == null) 
				return false;
			int index = list.indexOf(value);
			return index > -1 && index == list.lastIndexOf(value);
		}
	}

	public static void redirectOutputToFile(String filename, boolean append) {
		try {
			PrintStream pout = new PrintStream(
				new FileOutputStream( filename, append ) );
			System.setOut( pout );
			System.setErr( pout );
		} catch ( IOException e ) {
			System.err.println("Can't redirect output to file: "+filename );
		}
	}

	public static ValMap namedParams(Object...params) {
		ValMap map = vmap();
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				Object param = params[i];
				if (param instanceof String
						&& embedsLeft(param.toString(), "=")) {
					String[] sides = param.toString().split("=", 2);
					if (sides.length > 0) {
						if (sides.length > 1)
							map.put(sides[0], sides[1]);
						else
							map.put(sides[0], "");
						continue;
					}
				}
				map.put("param" + i, param);
			}
		}
		return map;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Object> T namedValue(T defaultValue, String name, Map<String, Object> map) throws ClassCastException {
		if (map.containsKey(name)) 
			try {
				T returnValue = (T)map.get(name);
				return returnValue;
			} catch (ClassCastException e) {}
		return defaultValue;
	}
	
	public static Boolean namedBoolean(Boolean defaultValue, String name, Map<String, Object> map) {
		if (map.containsKey(name)) {
			Object value = map.get(name);
			if (value instanceof Boolean)
				return (Boolean) value;
			else
				return toBool(defaultValue, "" + value);
		}
		else
			return defaultValue;
	}
	
	public static Integer namedInteger(Integer defaultValue, String name, Map<String, Object> map) {
		if (map.containsKey(name)) {
			Object value = map.get(name);
			if (value instanceof Integer)
				return (Integer) value;
			else
				return toInt(defaultValue, "" + value);
		}
		else
			return defaultValue;
	}
	
	public static Double namedDouble(Double defaultValue, String name, Map<String, Object> map) {
		if (map.containsKey(name)) {
			Object value = map.get(name);
			if (value instanceof Double)
				return (Double) value;
			else
				return toDouble(defaultValue, "" + value);
		}
		else
			return defaultValue;
	}

}
