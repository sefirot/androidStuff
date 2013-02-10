package com.applang;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Util
{
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
	 * @param weekOfYear (1..7) or -month (-11..0)
	 * @param dayOfWeek or dayOfMonth
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
	    Date today = Util.param(new Date(), 0, params);
	    boolean randomizeTimeOfDay = Util.param(false, 1, params);
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

	public static String formatDate(long millis, String pattern) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
		return dateFormat.format(new Date(millis));
	}

	public static Date parseDate(String dateString, String pattern) {
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			return dateFormat.parse(dateString);
		} catch (Exception e) {
			return null;
		}
	}

	public static long now() {
		return new Date().getTime();
	}

	public static boolean notNullOrEmpty(String value) {
		return value != null && !value.isEmpty();
	}

	public static <T> boolean nullOrEmpty(T[] value) {
		return value == null || value.length < 1;
	}

	public static boolean isWhiteSpace(String s) {
		for (int i = 0; i < s.length(); i++) 
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		
		return true;
	}

    public static int toInt(int defaultValue, String value) {
        int result;
        
        try {
        	result = Integer.parseInt(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
    }

    public static double toDouble(double defaultValue, String value) {
    	double result;
        
        try {
        	result = Double.parseDouble(value);
        } catch(NumberFormatException e) { result = defaultValue; }
        
        return result;
    }

	public static boolean matches(String s, String regex) {
		return s.matches(regex);
	}
	
	public static Document xmlDocument(File file) {
	    try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(file);
	    } catch (Exception e) {
	    	return null;
	    }
	}
	
	public static NodeList evaluateXPath(Document doc, String path) {
	    try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(path);
			return (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
	    } catch (Exception e) {
	    	return null;
	    }
	}
	
	public static String xmlContent(Reader reader) throws Exception {
		BufferedReader bufferedReader = new BufferedReader(reader);
		String line, text = "";
		while((line = bufferedReader.readLine()) != null)
			text += line;
		return text;
	}

	public static String xmlNodeToString(Node node, boolean omitXmlDeclaration) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			return "";
		}
		return sw.toString();
	}

	public static void xmlNodeToFile(Node node, boolean omitXmlDeclaration, File file) {
	    try {
	    	DOMSource source = new DOMSource(node);
	        Result result = new StreamResult(file);
	
	        Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
	        t.transform(source, result);
	    } catch (Exception e) {}
	}

	public static void xmlTransform(String fileName, String styleSheet, String outFileName, Object... params) throws Exception {
        StreamSource source = new StreamSource(fileName);
        StreamSource stylesource = new StreamSource(styleSheet);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(stylesource);

        for (int i = 0; i < params.length; i += 2) 
			transformer.setParameter(params[i].toString(), params[i + 1]);
        
        StreamResult result = new StreamResult(outFileName);
        transformer.transform(source, result);
	}
	
	/**
	 * @param <T>	type of the given array
	 * @param <U>	type of the cast array
	 * @param array	the given array
	 * @param a	prototype of the cast array
	 * @return	the cast array
	 */
	public static <T, U> U[] arraycast(T[] array, U[] a) {
		return Arrays.asList(array).toArray(a);
	}
	
	public static <T> T[] arrayreduce(T[] array, int start, int length) {
		if (start < 0 || 
			start > array.length || 
			start + length < 0 || 
			start + length > array.length)
			return Arrays.copyOfRange(array, 0, 0);
		else
			return Arrays.copyOfRange(array, start, start + length);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] arrayextend(T[] array, boolean prepend, T... params) {
		T[] a;
		if (prepend) {
			a = Arrays.copyOf(params, params.length + array.length);
			System.arraycopy(array, 0, a, params.length, array.length);
		}
		else {
			a = Arrays.copyOf(array, array.length + params.length);
			System.arraycopy(params, 0, a, array.length, params.length);
		}
		return a;
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
	
	public interface Job<T> {
		public void dispatch(T t, Object[] params) throws Exception;
	}
	
	public interface Callback {
		public void perform(Object... params);
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
	
	public static boolean osWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
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

	public static String tempPath() {
		return System.getProperty("java.io.tmpdir");
	}
	
	public static File tempDir(String... subdirs) {
    	File tempDir = fileOf(arrayextend(subdirs, true, tempPath()));
    	if (!tempDir.mkdirs()) {
        	if (deleteDirectory(tempDir))
        		tempDir.mkdir();
    	}
    	return tempDir;
	}
	
	public static File tempFile(String nameWithExtension, String... subdirs) {
    	try {
    		File tempDir = fileOf(arrayextend(subdirs, true, tempPath()));
    		String[] parts = nameWithExtension.split("\\.");
			return File.createTempFile(parts[0], parts.length > 1 ? "." + parts[1] : "", tempDir);
		} catch (IOException e) {
			return null;
		}
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
    
    public static String pathCombine(String... parts) {
    	File combined = fileOf(join(File.separator, parts));
    	if (combined == null)
    		return "";
    	
    	try {
			return combined.getCanonicalPath();
		} catch (IOException e) {
			return combined.getPath();
		}
    }
    
    public static String pathDivide(String path, String prefix) {
    	String pat = prefix.replaceAll(
    			osWindows() ? File.separator + File.separator : File.separator, 
    			"(\\\\\\\\|/)");
    	MatchResult m = findFirstIn(path, Pattern.compile("^" + pat + "(\\\\|/)"));
		if (m != null)
			return path.substring(m.group().length(), path.length());
		else
			return path;
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
			return Util.readAll(rd);
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

	public static int dialogResult = -1;

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
	
	/**
	 * @param parent
	 * @param title
	 * @param file
	 * @param fileFilter
	 * @return	the chosen <code>File</code>
	 */
	public static File chooseFile(boolean toOpen, Container parent, 
			String title, 
			File file, 
			FileFilter fileFilter)
	{
		File dir = new File(relativePath());
		try {
			dir = file.getParentFile();
		} catch (Exception e) {}
		
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(dir);
	    chooser.setDialogTitle(title);
	    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    if (fileFilter != null)
	    	setFileFilter(chooser, fileFilter);
		
	    if (fileExists(file))
	    	chooser.setSelectedFile(file);
	    
		parent = JOptionPane.getFrameForComponent(parent);
		dialogResult = toOpen ?
				chooser.showOpenDialog(parent) : 
				chooser.showSaveDialog(parent);
	    if (dialogResult == JFileChooser.APPROVE_OPTION)
	    	return chooser.getSelectedFile();
	    else
	    	return null;
	}
	
	static void setFileFilter(JFileChooser chooser, FileFilter fileFilter) {
	    if (fileFilter == null)
	    	chooser.resetChoosableFileFilters();
	    else {
	    	chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
	    	chooser.addChoosableFileFilter(fileFilter);
	    }
	    chooser.setAcceptAllFileFilterUsed(false);
	}

	/**
	 * @param parent
	 * @param title
	 * @param dir
	 * @return	the chosen directory <code>File</code>
	 */
	public static File chooseDirectory(Container parent, 
			String title, 
			File dir)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(dir);
	    chooser.setDialogTitle(title);
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
		
		parent = JOptionPane.getFrameForComponent(parent);
		dialogResult = chooser.showOpenDialog(parent);
	    if (dialogResult == JFileChooser.APPROVE_OPTION)
	    	return chooser.getSelectedFile();
	    else
	    	return null;
	}

	public static void addFocusObserver(JTextComponent jtc) {
	    jtc.addFocusListener(new FocusListener() {
	        public void focusGained(FocusEvent e) {
	            ((JTextComponent)e.getSource()).selectAll();
	        }
	        public void focusLost(FocusEvent arg0) {    
	        }
	    });
	}

	public static void addTabKeyForwarding(JComponent jc) {
	    jc.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				}
				if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
					e.consume();
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
				}
			}
		});
	}

	@SuppressWarnings("rawtypes")
	public static JTextField comboEdit(JComboBox combo) {
		return (JTextField)combo.getEditor().getEditorComponent();
	}
	
	public interface ComponentFunction<T> {
		public T apply(Component comp, Object[] parms);
	}
	
	public static <T> Object[] iterateComponents(Container container, ComponentFunction<T> func, Object... params) {
		params = reduceDepth(params);
		
		if (container != null) {
			Component[] components = params.length > 0 ? 
				container.getComponents() : 
				new Component[] {container};
				
			for (Component comp : components)
			{
				T t = func.apply(comp, params);
				
				if (comp instanceof Container)
					iterateComponents((Container)comp, func, t);
			}
		}
		
		return params;
	}
    
	/**
	 * @param container
	 * @param pattern
	 * @return	an array of those child <code>Component</code> objects of container which names match the given pattern
	 */
	public static Component[] findComponents(Container container, final String pattern) {
		final ArrayList<Component> al = new ArrayList<Component>();
		
		iterateComponents(container, new ComponentFunction<Object[]>() {
			public Object[] apply(Component comp, Object[] parms) {
				String name = comp.getName();
				if (name != null && name.matches(pattern))
					al.add(comp);
				
				return parms;
			}
		}, null, null);
		
		return al.toArray(new Component[al.size()]);
	}
    
	/**
	 * @param <C> the type of the return value (derived from <code>Component</code>)
	 * @param container	the containing <code>Component</code>
	 * @param key	the name of the <code>Component</code> searched for in the container including descending components
	 * @return	the <code>Component</code> found if unequal <code>null</code> otherwise nothing was found
	 */
	@SuppressWarnings("unchecked")
	public static <C extends Component> C findComponent(Container container, String key) {
		Component[] comps = findComponents(container, key);
		if (comps.length > 0) 
			try {
				return (C)comps[0];
			} catch (Exception e) {}
		
		return null;
	}
    
    public static class Timing
    {
    	public Timing(Component comp) {
    		this.comp = comp;
    		if (this.comp != null) {
        		this.curs = this.comp.getCursor();
        		this.comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        		this.comp.requestFocus();
    		}
    		this.millis = System.currentTimeMillis();
    	}
    	
		private long millis;
		
		public long current() {
			return System.currentTimeMillis() - millis;
		}
		
    	Component comp;
    	Cursor curs;
    	
    	/* (non-Javadoc)
    	 * @see java.lang.Object#finalize()
    	 */
    	@Override 
    	public void finalize() {
    		try {
        		this.millis -= System.currentTimeMillis();
        		if (this.comp != null) 
        			this.comp.setCursor(curs);
    		}
    		finally {
    			try {
					super.finalize();
				} catch (Throwable e) {}
    		}
    	}
    }
	
	public static long waiting(Component component, 
			ComponentFunction<Void> func, 
			Object... params)
	{
        final Timing timing = new Timing(component);
        
		try {
			func.apply(component, arrayextend(params, true, timing));
		}
		finally {
			timing.finalize();
		}
		
		return timing.millis;
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
	
	public static String getMapping(String key) {
		Object value = mappings.get(key);
		return value == null ? "" : value.toString();
	}
	
	public static String setMapping(String key, String value) {
		mappings.put(key, value);
		return "";
	}
	
	public static void posting2mappings(String posting) {
		String[] data = posting.split("&|=");
		
		try {
			for (int i = 0; i < data.length; i+=2) {
				String key = URLDecoder.decode(data[i], "UTF-8");
				String value = URLDecoder.decode(data[i+1], "UTF-8");
				setMapping(key, value);
			}
		} catch (UnsupportedEncodingException e) {}
	}
	
	public static ValMap getMapFromQuery(PreparedStatement ps, int keyColumn, int valueColumn) {
		ValMap map = new ValMap();
		try {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				map.put(rs.getString(keyColumn), rs.getObject(valueColumn));
			rs.close();
		} catch (Exception e) {
			return null;
		}
		return map;
	}
	
	public static Container container = null;

	public static boolean question(String text) {
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				container, text, 
				"Question", 
				JOptionPane.YES_NO_OPTION);
	}

	public static void message(String text) {
		JLabel mess = Util.findComponent(container, "mess");
		if (mess != null)
			mess.setText(text);
		else
			JOptionPane.showMessageDialog(container, text, "Message", JOptionPane.PLAIN_MESSAGE);
	}

	public static void handleException(Exception e) {
		if (e != null) 
        	message(e.getMessage());
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
	
	public static class Settings 
	{
		private static Properties properties = null;
		
		public static void clear() {
			properties = new Properties();
		}
		
		public static boolean contains(String key) {
			return properties.containsKey(key);
		}
		
		public static String defaultFilename() {
			String dir = relativePath();
			File[] array = new File(dir).listFiles();
	    	for (File file : array) {
	    		String path = file.getPath();
				if (file.isFile() && path.endsWith(".properties"))
	    			return path;
	    	}
	    	String[] parts = dir.split("\\\\|/");
	    	return parts[parts.length - 1] + ".properties";
		}
		
		/**
		 * @param params
		 * <table border="1"><tr><th>index</th><th>description</th></tr><tr><td>0</td><td>file path to settings</td></tr></table>
		 */
		public static void load(Object... params) {
			String fileName = paramString(defaultFilename(), 0, params);
			boolean decoding = paramBoolean(false, 1, params);
			
			if (properties == null)
				clear();
			
			XMLDecoder dec = null;
			try {
				if (decoding) {
					dec = new XMLDecoder(new FileInputStream(fileName));
					properties = (Properties) dec.readObject();
				} else {
					FileInputStream fis = new FileInputStream(fileName);
					properties.load(fis);
					fis.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (dec != null)
					dec.close();
			}
		}
		
		/**
		 * @param params
		 */
		public static void save(Object... params) {
			String fileName = paramString(defaultFilename(), 0, params);
			boolean encoding = paramBoolean(false, 1, params);
			
			if (properties == null)
				clear();
			
			XMLEncoder enc = null;
		    try {
				if (encoding) {
					enc = new XMLEncoder(new FileOutputStream(fileName));
					enc.writeObject(properties);
				}
		        else {
		            FileOutputStream fos = new FileOutputStream(fileName);
		            properties.store(fos, null);
		            fos.close();
		        }
			} 
		    catch (Exception e) {
				e.printStackTrace();
			} 
			finally {
				if (enc != null)
					enc.close();
			}
		}
		
		/**
		 * @param <T>	the type of the value
		 * @param key	the name under which the setting item is known
		 * @param value	the value of the setting item
		 * @param params
		 * <table border="1"><tr><th>index</th><th>description</th></tr><tr><td>0</td><td>causes rounding of value if T is <code>Double</code></td></tr></table>
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Object> void put(String key, T value, Object... params) {
			Object decimalPlace = param(null, 0, params);
			if (value instanceof Double && decimalPlace instanceof Integer) {
				value = (T)Double.valueOf(round((Double)value, (Integer)decimalPlace));
			}
			properties.put(key, value);
		}
		
		/**
		 * @param <T>	the type of the value
		 * @param key	the name under which the setting item is known
		 * @param defaultValue	the value of the setting item in case key is unknown (null)
		 * @return	the value of the setting item
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Object> T get(String key, T defaultValue) {
			try {
				if (Settings.contains(key))
					return (T)properties.get(key);
			} catch (Exception e) {}
	
			return defaultValue;
		}
	}

}
