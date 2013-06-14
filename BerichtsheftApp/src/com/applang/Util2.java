package com.applang;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.applang.Util.Function;
import com.applang.Util.ValList;
import com.applang.Util.ValMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class Util2
{
	public static class Settings 
	{
		static Properties properties = null;
		
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
	}

	/**
	 * @param <T>	the type of the value
	 * @param key	the name under which the setting item is known
	 * @param value	the value of the setting item
	 * @param params
	 * <table border="1"><tr><th>index</th><th>description</th></tr><tr><td>0</td><td>causes rounding of value if T is <code>Double</code></td></tr></table>
	 */
	@SuppressWarnings("unchecked")
	//	used in xsl scripts
	public static <T extends Object> void putSetting(String key, T value, Object... params) {
		Object decimalPlace = param(null, 0, params);
		if (value instanceof Double && decimalPlace instanceof Integer) {
			value = (T)Double.valueOf(round((Double)value, (Integer)decimalPlace));
		}
		Settings.properties.put(key, value);
	}

	/**
	 * @param <T>	the type of the value
	 * @param key	the name under which the setting item is known
	 * @param defaultValue	the value of the setting item in case key is unknown (null)
	 * @return	the value of the setting item
	 */
	@SuppressWarnings("unchecked")
	//	used in xsl scripts
	public static <T extends Object> T getSetting(String key, T defaultValue) {
		try {
			if (Settings.contains(key))
				return (T)Settings.properties.get(key);
		} catch (Exception e) {}
		
		return defaultValue;
	}

/*
	The format specifiers for general, character, and numeric types have the following syntax:
	 
	   %[argument_index$][flags][width][.precision]conversion
	   
	The optional argument_index is a decimal integer indicating the position of the argument 
	in the argument list. The first argument is referenced by "1$", the second by "2$", etc. 

	The optional flags is a set of characters that modify the output format. The set of valid flags 
	depends on the conversion. 

	The optional width is a non-negative decimal integer indicating the minimum number of characters 
	to be written to the output. 

	The optional precision is a non-negative decimal integer usually used to restrict the number of 
	characters. The specific behavior depends on the conversion. 

	The required conversion is a character indicating how the argument should be formatted. The set 
	of valid conversions for a given argument depends on the argument's data type. 

	The format specifiers for types which are used to represents dates and times have the following syntax:
	 
	   %[argument_index$][flags][width]conversion
	   
	The optional argument_index, flags and width are defined as above. 

	The required conversion is a two character sequence. The first character is 't' or 'T'. 
	The second character indicates the format to be used. These characters are similar to but not 
	completely identical to those defined by GNU date and POSIX strftime(3c). 

	The format specifiers which do not correspond to arguments have the following syntax: 
	
	   %[flags][width]conversion
	   
	The optional flags and width is defined as above. 

	The required conversion is a character indicating content to be inserted in the output.
*/

	public static final Pattern FORMAT_SPECIFIER_PATTERN = Pattern.compile("%(\\d+\\$)?([-#+ 0,(])?(\\d+(.\\d+)?)?[sfdbchox]");
	
	/**
	 * @param writer
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public static Writer format(Writer writer, Object... params) {
		String separator = "\t";
		
	    try {
			for (int i = 0; i < params.length; i++) {
				if (i > 0)
					writer.write(separator);
					
				Object o = params[i];
				String s = o instanceof String ? (String)o : "";
				
				MatchResult[] specifiers = findAllIn(s, FORMAT_SPECIFIER_PATTERN);
				int specs = specifiers.length;
				
				boolean useSpecifiers = specs > 0 && specs <= params.length - i - 1;
				if (useSpecifiers) {
					Object[] args = arrayreduce(params, i + 1, specs);
					for (int j = 0; j < args.length; j++)
						args[j] = 
							Util2.stringify(args[j], 
								specifiers[j].group().toLowerCase().endsWith("s"));
					s = String.format(s, args);
					i += specs;
				}
				else
					s = Util2.stringify(o, true).toString();
				
				writer.write(s);
			}
		} catch (Exception e) {}
		
		return writer;
	}

	public static Object stringify(Object o, boolean stringify) {
		if (!stringify || 
				o instanceof String || 
				o instanceof Short || 
				o instanceof Long || 
				o instanceof Integer || 
				o instanceof Float || 
				o instanceof Double || 
				o instanceof Character || 
				o instanceof Byte || 
				o instanceof Boolean)
			return o;
		
		String s;
		
		if (o instanceof Object[]) s = Arrays.toString((Object[])o);
		else if (o instanceof boolean[]) s = Arrays.toString((boolean[])o);
		else if (o instanceof byte[]) s = Arrays.toString((byte[])o);
		else if (o instanceof char[]) s = Arrays.toString((char[])o);
		else if (o instanceof double[]) s = Arrays.toString((double[])o);
		else if (o instanceof float[]) s = Arrays.toString((float[])o);
		else if (o instanceof int[]) s = Arrays.toString((int[])o);
		else if (o instanceof long[]) s = Arrays.toString((long[])o);
		else if (o instanceof short[]) s = Arrays.toString((short[])o);
		else if (o instanceof String[]) s = Arrays.toString((String[])o);
		else
			s = o.toString();
		
		return s;
	}

	@SuppressWarnings("resource")
	public static Writer formatAssociation(Writer writer, String key, Object value, int commaPos) {
		String commaString = ", ";
		if (commaPos > 0)
			writer = format(writer, commaString);
		
		if (value == null) 
			writer = format(writer, "%s=null", key);
		else
			writer = format(writer, "%s=%s", key, value);
		
		if (commaPos < 0)
			writer = format(writer, commaString);
		
		return writer;
	}

	public static void print(Object... params) {
		System.out.print(format(new StringWriter(), params).toString());
	}

	public static void println(Object... params) {
		System.out.println(format(new StringWriter(), params).toString());
	}

	public static void noprint(Object... params) {}

	public static void noprintln(Object... params) {}

	public static String toString(String description, Object o) {
		String value = o.toString();
		int brac = value.indexOf('[');
		return (notNullOrEmpty(description) ? description : "") + 
				value.substring(brac > -1 ? brac : 0);
	}
	
	public static class DataBaseConnect
	{
		public boolean open(String dbPath, Object... params) {
			boolean retval = false;
			ResultSet rs = null;
			try {
				close();
				
				String driver = Util.paramString("org.sqlite.JDBC", 2, params);
				Class.forName(driver);
				
				scheme = Util.paramString("sqlite", 1, params);
				boolean memoryDb = "sqlite".equals(scheme) && dbPath == null;
				
				preConnect(dbPath);
				
				String url = "jdbc:" + scheme + ":" + (memoryDb ? "" : dbPath);
				con = DriverManager.getConnection(url);
				stmt = con.createStatement();
				
				postConnect();
				
				String database = Util.paramString("sqlite_master", 3, params);
				if ("sqlite".equals(scheme))
					rs = stmt.executeQuery("select name from " + database + " where type = 'table'");
				else if ("mysql".equals(scheme)) {
					rs = stmt.executeQuery("show databases;");
					boolean exists = false;
				    while (rs.next()) 
				        if (rs.getString(1).equals(database)) {
				        	exists = true;
				        	break;
				        }
		        	rs.close();
		        	if (!exists)
		        		throw new Exception(String.format("database '%s' not found", database));
		        	else
		        		stmt.execute(String.format("use %s;", database));
		        	
					rs = stmt.executeQuery("show tables in " + database + ";");
				}
				
				String tableName = Util.paramString(null, 0, params);
				if (tableName == null)
					return true;
				
			    while (rs.next()) 
			        if (rs.getString(1).equals(tableName)) 
			        	return true;
			    
			    return false;
			} catch (Exception e) {
				handleException(e);
				return retval;
			} 
			finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						handleException(e);
						retval = false;
					}
				}
			}
		}
		
		public void preConnect(String path) throws Exception {
		}
		
		public void postConnect() throws Exception {
		}

		public void close() {
			try {
				if (con != null && !con.isClosed())
					con.close();
			} catch (SQLException e) {
				handleException(e);
			}
		}
		
		private String scheme = null;
		public String getScheme() {
			return scheme;
		}

		private Connection con = null;
		public Connection getCon() {
			return con;
		}

		private Statement stmt = null;
		public Statement getStmt() {
			return stmt;
		}
		
		public static boolean bulkSqlite(File db, File sql) {
        	try {
				ProcessBuilder builder = new ProcessBuilder(
					"sqlite3", 
					db.getCanonicalPath())
						.redirectErrorStream(true);
				
				Process process = builder.start();
				OutputStream outputStream = process.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(outputStream));
				osw.write(contentsFromFile(sql));
				osw.close();
				
				int exitcode = process.waitFor();
				return exitcode == 0;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}
		
		public static boolean dumpSqlite(File db, File dump) {
        	try {
				ProcessBuilder builder = new ProcessBuilder(
					"sqlite3", 
					db.getCanonicalPath(),
					".dump")
						.redirectErrorStream(true);
				
				Process process = builder.start();
				InputStream inputStream = process.getInputStream();
				InputStreamReader isw = new InputStreamReader(new BufferedInputStream(inputStream));
				contentsToFile(dump, readAll(isw));
				isw.close();
				
				int exitcode = process.waitFor();
				return exitcode == 0;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}
	}
	
	public static ValMap getResultMap(PreparedStatement ps, Object...params) {
		ValMap map = new ValMap();
		Function<String> keyConversion = param(null, 0, params);
		Function<Object> valueConversion = param(null, 1, params);
		try {
			int keyColumn = 1;
			int valueColumn = 2;
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String key = rs.getString(keyColumn);
				if (keyConversion != null) 
					key = keyConversion.apply(key, rs);
				Object value = rs.getObject(valueColumn);
				if (valueConversion != null) 
					value = valueConversion.apply(value, rs);
				map.put(key, value);
			}
			rs.close();
		} catch (Exception e) {
			return null;
		}
		return map;
	}

	public static List<ValMap> getResultMapList(PreparedStatement ps, Object...params) {
		List<ValMap> list = new ArrayList<ValMap>();
		try {
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			while (rs.next()) {
				ValMap map = new ValMap();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					Object value = rs.getObject(i);
					Function<Object> conversion = param(null, i-1, params);
					if (conversion != null) 
						value = conversion.apply(value, rs);
					map.put(rsmd.getColumnName(i), value);
				}
				list.add(map);
			}
			rs.close();
		} catch (Exception e) {
			return null;
		}
		return list;
	}
	
	public static void xmlTransform(String fileName, String styleSheet, String outFileName, Object... params) throws Exception {
	    StreamSource source = new StreamSource(fileName);
	    StreamSource stylesource = new StreamSource(styleSheet);
	
	    TransformerFactory factory = TransformerFactory.newInstance();
	    Transformer transformer = factory.newTransformer(stylesource);
	
	    for (int i = 0; i < params.length; i += 2) 
			transformer.setParameter(params[i].toString(), params[i + 1]);
	    
	    StreamResult result = new StreamResult(new File(outFileName));
	    transformer.transform(source, result);
	}

	public static void xmlNodeToFile(Node node, boolean omitXmlDeclaration, File file) {
	    try {
	    	DOMSource source = new DOMSource(node);
	        Result result = new StreamResult(file);
	
	        Transformer t = Util2.xmlTransformer(omitXmlDeclaration);
	        t.transform(source, result);
	    } catch (Exception e) {}
	}

	static Transformer xmlTransformer(boolean omitXmlDeclaration)
			throws TransformerConfigurationException, TransformerFactoryConfigurationError {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
		return t;
	}

	public static String xmlNodeToString(Node node, boolean omitXmlDeclaration) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = xmlTransformer(omitXmlDeclaration);
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			return "";
		}
		return sw.toString();
	}

	public static NodeList evaluateXPath(Object item, String path) {
	    try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(path);
			return (NodeList)expr.evaluate(item, XPathConstants.NODESET);
	    } catch (Exception e) {
	    	return null;
	    }
	}

	public static boolean osWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	public static String tempPath() {
		return System.getProperty("java.io.tmpdir");
	}

	//	used in xsl scripts
	public static String tempPath(String subdir, String name) {
		return Util2.pathCombine(Util2.tempDir(false, subdir).getPath(), name);
	}

	public static File tempDir(boolean deleteOnExistence, String... subdirs) {
		File tempDir = fileOf(arrayextend(subdirs, true, tempPath()));
		if (!tempDir.mkdirs()) {
	    	if (deleteOnExistence && deleteDirectory(tempDir))
	    		tempDir.mkdir();
		}
		return tempDir;
	}

	public static File tempFile(String nameWithExtension, String... subdirs) {
		try {
			File tempDir = fileOf(arrayextend(subdirs, true, tempPath()));
			String[] parts = nameWithExtension.split("\\.");
			return File.createTempFile(
					parts.length > 0 && parts[0].length() > 2 ? parts[0] : "tmp", 
					parts.length > 1 ? "." + parts[1] : null, 
					tempDir);
		} catch (IOException e) {
			return null;
		}
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
    
    public static ValList contentAuthorities(Context context, String startsWith) {
    	ValList list = new ValList();
//    	List<ProviderInfo> providers = context.getPackageManager().queryContentProviders(null, 0, 0);
//        for (ProviderInfo provider : providers) {
//            String authority = provider.authority;
//            if (authority.startsWith(startsWith))
//            	list.add(authority);
//        }
        return list;
    }
    
    public static String resourcePackageName(Activity activity) {
    	return null;
    }

	@SuppressWarnings("hiding")
	public static class Task<Result> extends SwingWorker<Result, Intent>
    {
		public Task(Activity activity, Job<Result> followUp, Object... params) {
			this.activity = activity;
			this.followUp = followUp;
			this.params = params;
		}

		protected Activity activity;
		protected Object[] params;
		
		protected void onPreExecute() {
		}
		
		protected void onPostExecute(Result result) {
		}
		
		protected Result doInBackground(Object...params) throws Exception {
			Integer millis = paramInteger(null, 0, params);
			if (millis != null)
				delay(millis);
			return null;
		}

		@Override
		protected Result doInBackground() throws Exception {
			return doInBackground(params);
		}

		protected void publishProgress(Intent... intents) {
			publish(intents);
		}

		@Override
		protected void process(List<Intent> chunks) {
			if (chunks.size() > 0 && activity != null) {
				Intent intent = chunks.get(0);
				if (notNullOrEmpty(intent.getAction()))
					activity.startActivity(intent);
			}
		}
 
		@Override
		protected void done() {
			if (followUp != null) 
				try {
					followUp.perform(get(), params);
					cancel(true);
				} catch (Exception e) {
					Log.e(TAG, "follow-up", e);
				}
		}
		
		private Job<Result> followUp;
    }
	
	public static ValMap getResultMap(Cursor cursor, Function<String> key, Function<Object> value) {
		ValMap map = new ValMap();
		try {
	    	if (cursor.moveToFirst()) 
	    		do {
					String k = key.apply(cursor);
					Object v = value.apply(cursor);
					map.put(k, v);
	    		} while (cursor.moveToNext());
		} catch (Exception e) {
            Log.e(TAG, "traversing cursor", e);
			return null;
		}
		finally {
			cursor.close();
		}
		return map;
	}

	public static ValList getRecord(Cursor cursor) {
		ValList list = new ValList();
		for (int i = 0; i < cursor.getColumnCount(); i++)
			list.add(cursor.getString(i));
		return list;
	}

	public static <T> List<T> excludeFromArray(int index, T[] array, List<T> list) {
		if (index > 0)
			list.addAll(Arrays.asList(Arrays.copyOfRange(array,	0, index)));
		if (index < array.length - 1)
			list.addAll(Arrays.asList(Arrays.copyOfRange(array,	index + 1, array.length)));
		return list;
	}
    
}
