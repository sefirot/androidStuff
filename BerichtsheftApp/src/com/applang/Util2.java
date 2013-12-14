package com.applang;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.applang.Util.ValMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.ResourceURLFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.SwingUtil.*;

public class Util2
{
	/**
	 * @param params	optional parameters	
	 * <table border="1"><tr><th>index</th><th>description</th></tr><tr><td>0</td><td>a path as <code>String</code> to relativize against 'user.dir'</td></tr></table>
	 * @return	if path is null returns the absolute 'user.dir' system property otherwise the path relative to 'user.dir'.
	 */
	public static String relativePath(Object...params) {
		String base = param_String(System.getProperty("user.dir"), 1, params);
		String path = param_String(null, 0, params);
		if (path == null)
			return base;
		else
			return new File(base).toURI().relativize(new File(path).toURI()).getPath();
	}
	
	public static String absolutePath(String className) throws Exception {
		final String name = className.replaceAll("\\.", "/") + ".class";
		Set<URL> res = Resources.getResourceURLs(
			new ResourceURLFilter() {
				public boolean accept(URL resourceUrl) {
					String url = resourceUrl.getFile();
					return url.endsWith(name);
				}
			});
		return res.iterator().next().getFile();
	}
	
	public static class Settings 
	{
		public static Properties properties = null;
		
		public static void clear() {
			properties = new Properties();
		}
		
		public static boolean contains(String key) {
			return properties.containsKey(key);
		}
		
		public static void remove(String key) {
			properties.remove(key);
		}
		
		public static String defaultFilename() {
			String dir = System.getProperty("settings.dir", "");
			if (nullOrEmpty(dir))
				dir = relativePath();
			File[] array = new File(dir).listFiles();
	    	for (File file : array) {
	    		String path = file.getPath();
				if (file.isFile() && path.endsWith(".properties"))
	    			return path;
	    	}
	    	String[] parts = dir.split("\\\\|/");
	    	return pathCombine(dir, parts[parts.length - 1] + ".properties");
		}
		
		/**
		 * @param params
		 * <table border="1"><tr><th>index</th><th>description</th></tr><tr><td>0</td><td>file path to settings</td></tr></table>
		 */
		public static void load(Object... params) {
			String fileName = param_String(defaultFilename(), 0, params);
			boolean decoding = param_Boolean(false, 1, params);
			
			if (properties == null)
				clear();
			
			if (!fileExists(fileName))
				return;
			
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
			} catch (FileNotFoundException e) {
			} catch (Exception e) {
				Log.e(TAG, "Settings.load", e);
			} finally {
				if (dec != null)
					dec.close();
			}
		}
		
		/**
		 * @param params
		 */
		public static void save(Object... params) {
			String fileName = param_String(defaultFilename(), 0, params);
			boolean encoding = param_Boolean(false, 1, params);
			
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
				Log.e(TAG, "Settings.save", e);
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
		if (value instanceof Double && isAvailable(0, params)) {
			Integer decimalPlace = param_Integer(0, 0, params);
			value = (T)Double.valueOf(round((Double)value, decimalPlace));
		}
		String string = stringValueOf(value);
		Log.d(TAG, String.format("putSetting '%s' : %s", key, string));
		Settings.properties.put(key, string);
	}

	@SuppressWarnings("rawtypes")
	public static void putListSetting(String key, Collection value) {
		try {
			JSONStringer jsonWriter = new JSONStringer();
			toJSON(null, jsonWriter, "", value, null);
			putSetting(key, jsonWriter.toString());
		} catch (Exception e) {
			Log.e(TAG, "putListSetting", e);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void putMapSetting(String key, Map value) {
		try {
			JSONStringer jsonWriter = new JSONStringer();
			toJSON(null, jsonWriter, "", value, null);
			putSetting(key, jsonWriter.toString());
		} catch (Exception e) {
			Log.e(TAG, "putMapSetting", e);
		}
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
		Object value = defaultValue;
		try {
			if (Settings.contains(key)) {
				value = Settings.properties.get(key);
				if (value != null) {
					String string = String.valueOf(value);
					if (string.length() < 1)
						value = null;
					else if (defaultValue instanceof String) 
						value = string;
					else if (defaultValue instanceof Boolean)
						value = Boolean.parseBoolean(string);
					else if (defaultValue instanceof Integer)
						value = Integer.parseInt(string);
					else if (defaultValue instanceof Long)
						value = Long.parseLong(string);
					else if (defaultValue instanceof Float)
						value = Float.parseFloat(string);
					else if (defaultValue instanceof Double)
						value = Double.parseDouble(string);
				}
				Log.d(TAG, String.format("getSetting '%s' : %s", key, String.valueOf(value)));
			}
		} catch (Exception e) {
			Log.d(TAG, "getSetting", e);
		}
		return (T)value;
	}

	@SuppressWarnings("rawtypes")
	public static Collection getListSetting(String key, Collection defaultValue) {
		try {
			if (Settings.contains(key)) {
				String s = getSetting(key, null);
				return (ValList) walkJSON(null, new JSONArray(s), null);
			}
		} catch (Exception e) {
			Log.d(TAG, "getListSetting", e);
		}
		
		return defaultValue;
	}

	@SuppressWarnings("rawtypes")
	public static Map getMapSetting(String key, Map defaultValue) {
		try {
			if (Settings.contains(key)) {
				String s = getSetting(key, null);
				return (ValMap) walkJSON(null, new JSONObject(s), null);
			}
		} catch (Exception e) {
			Log.d(TAG, "getMapSetting", e);
		}
		
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
	public static Writer write(Writer writer, Object... params) {
		String separator = "\t";
		
	    try {
			for (int i = 0; i < params.length; i++) {
				if (i > 0)
					writer.write(separator);
					
				Object o = params[i];
				String s = String.valueOf(o);
				
				MatchResult[] specifiers = findAllIn(s, FORMAT_SPECIFIER_PATTERN);
				int specs = specifiers.length;
				
				boolean useSpecifiers = specs > 0 && specs <= params.length - i - 1;
				if (useSpecifiers) {
					Object[] args = arrayreduce(params, i + 1, specs);
					for (int j = 0; j < args.length; j++)
						args[j] = 
							stringify(
								args[j], 
								specifiers[j].group().toLowerCase().endsWith("s"));
					s = String.format(s, args);
					i += specs;
				}
				else
					s = stringify(o, true).toString();
				
				writer.write(s);
			}
		} catch (Exception e) {}
		
		return writer;
	}

	private static Object stringify(Object o, boolean stringify) {
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
			s = String.valueOf(o);
		return s;
	}

	@SuppressWarnings("resource")
	public static Writer writeAssoc(Writer writer, String key, Object value, int...pos) {
		String commaString = ", ";
		int commaPos = param(0,0,pos);
		if (commaPos > 0)
			writer = write(writer, commaString);
		if (value == null) 
			writer = write(writer, "%s=null", key);
		else
			writer = write(writer, "%s=%s", key, value);
		if (commaPos < 0)
			writer = write(writer, commaString);
		return writer;
	}

	public static void print(Object... params) {
		Object out = param(null, 0, params);
		if (out instanceof Writer) {
			Writer writer = (Writer) out;
			write(writer, arrayreduce(params, 1, params.length - 1));
		}
		else
			System.out.print(write(new StringWriter(), params).toString());
	}

	public static void println(Object... params) {
		print(params);
		Object out = param(null, 0, params);
		if (out instanceof Writer) 
			write((Writer) out, NEWLINE);
		else
			System.out.print(NEWLINE);
	}

	public static void noprint(Object... params) {}

	public static void noprintln(Object... params) {}

	public static String toString(Object[][] o) {
		ValList list = vlist();
		for (Object object : (Object[])o) 
			list.add(Arrays.toString((Object[])object));
		String s = Arrays.toString(list.toArray());
		return strip(true, strip(false, s , "]") , "[")
				.replaceAll("(\\],) (\\[)", "$1\n\n$2");
	}

	public static String toString(ValMap o) {
		return String.valueOf(o).replaceAll("\\], ", "\\],\n");
	}

	public static String toString(Object o) {
		String value = String.valueOf(o);
		int brac = value.indexOf('[');
		return value.substring(brac > -1 ? brac : 0);
	}

	public static void printMatchResults(String string, Pattern pattern) {
		MatchResult[] mr = findAllIn(string, pattern);
    	for (int i = 0; i < mr.length; i++) {
    		MatchResult m = mr[i];
    		println("%d-%d(%d,%d)%s", i, m.groupCount(), m.start(), m.end(), m.group());
    		for (int j = 1; j <= m.groupCount(); j++) {
    			println(m.group(j));
    		}
		}
    	println("(%d)%s", string.length(), string);
	}
	
	public static class DataBaseConnect
	{
		public boolean open(String dbPath, Object... params) {
			boolean retval = false;
			ResultSet rs = null;
			try {
				close();
				
				String driver = param_String("org.sqlite.JDBC", 2, params);
				Class.forName(driver);
				
				scheme = param_String("sqlite", 1, params);
				boolean memoryDb = "sqlite".equals(scheme) && dbPath == null;
				
				preConnect(dbPath);
				
				String url = "jdbc:" + scheme + ":" + (memoryDb ? "" : dbPath);
				con = DriverManager.getConnection(url);
				stmt = con.createStatement();
				
				postConnect();
				
				String database = param_String("sqlite_master", 3, params);
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
				
				String tableName = param_String(null, 0, params);
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
		ValMap map = vmap();
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
				ValMap map = vmap();
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

	public static BidiMultiMap getResultMultiMap(PreparedStatement ps, Object...params) {
		BidiMultiMap bidi = bmap(params.length);
		try {
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = Math.min(rsmd.getColumnCount(), params.length);
			while (rs.next()) {
				ValList list = vlist();
				for (int i = 1; i <= columnCount; i++) {
					Object value = rs.getObject(i);
					Function<Object> conversion = param(null, i-1, params);
					if (conversion != null) 
						value = conversion.apply(value, rs);
					list.add(value);
				}
				bidi.add(list.toArray());
			}
			rs.close();
		} catch (Exception e) {
			return null;
		}
		return bidi;
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
	
	        Transformer t = xmlTransformer(omitXmlDeclaration);
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
	
	public static Element xmlSerialize(BidiMultiMap bidi, Element element, 
			Function<Object> filter, Object...params) throws Exception
	{
		JSONStringer jsonWriter = new JSONStringer();
		toJSON(null, jsonWriter, "", asList(bidi.getLists()), filter, params);
		CDATASection cdata = element.getOwnerDocument().createCDATASection(jsonWriter.toString());
		element.appendChild(cdata);
		return element;
	}
	
	public static BidiMultiMap xmlDeserialize(Element element, 
			Function<Object> filter, Object...params) throws Exception
	{
		String string = element.getTextContent();
		ValList list = (ValList) walkJSON(null, new JSONArray(string), filter, params);
		return new BidiMultiMap(list.toArray(new ValList[0]));
	}

	public static Element selectElement(Object item, String xpath) {
		if (item == null)
			return null;
		NodeList nodes = evaluateXPath(item, xpath);
		if (nodes.getLength() > 0)
			return (Element) nodes.item(0);
		else
			return null;
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
		return pathCombine(tempDir(false, subdir).getPath(), name);
	}

	public static File tempDir(boolean deleteOnExistence, String...subdirs) {
		File tempDir = fileOf(arrayextend(subdirs, true, tempPath()));
		if (!tempDir.mkdirs()) {
	    	if (deleteOnExistence && deleteDirectory(tempDir))
	    		tempDir.mkdir();
		}
		return tempDir;
	}

	public static File tempFile(String nameWithExtension, String...subdirs) {
		try {
			File tempDir = tempDir(false, subdirs);
			String[] parts = nameWithExtension.split("\\.");
			return File.createTempFile(
					parts.length > 0 && parts[0].length() > 2 ? parts[0] : "tmp", 
					parts.length > 1 ? "." + parts[1] : null, 
					tempDir);
		} catch (IOException e) {
			return null;
		}
	}

	public static String pathCombine(String...parts) {
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
	
	public static <T> List<T> arrayexclude(int index, T[] array, List<T> list) {
		if (index > 0)
			list.addAll(asList(Arrays.copyOfRange(array,	0, index)));
		if (index < array.length - 1)
			list.addAll(asList(Arrays.copyOfRange(array,	index + 1, array.length)));
		return list;
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
		
		public Task<Result> execute(Object...params) {
			this.params = params;
			super.execute();
			return this;
		}
		
		protected void onPreExecute() {
		}
		
		protected void onPostExecute(Result result) {
		}
		
		protected Result doInBackground(Object...params) throws Exception {
			Integer millis = param_Integer(null, 0, params);
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
		
		protected Job<Result> followUp;
    }

	public static BufferedImage verticalflip(Image img) {
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
		g.dispose();
		return bimg;
	}
	public static BufferedImage horizontalflip(Image img) {
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
		g.dispose();
		return bimg;
	}
	public static BufferedImage rotate(Image img, int angle) {
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bimg.createGraphics();
		g.rotate(Math.toRadians(angle), w/2, h/2);
		g.drawImage(img, 0, 0, null);
		return bimg;
	}
	public static BufferedImage resize(Image img, int newW, int newH) {
		int w = img.getWidth(null);
		int h = img.getHeight(null);
		BufferedImage bimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bimg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
		g.dispose();
		return bimg;
	}
	
    public static final int S_IRWXU = 00700;
    public static final int S_IRUSR = 00400;
    public static final int S_IWUSR = 00200;
    public static final int S_IXUSR = 00100;

    public static final int S_IRWXG = 00070;
    public static final int S_IRGRP = 00040;
    public static final int S_IWGRP = 00020;
    public static final int S_IXGRP = 00010;

    public static final int S_IRWXO = 00007;
    public static final int S_IROTH = 00004;
    public static final int S_IWOTH = 00002;
    public static final int S_IXOTH = 00001;

	public static void setPermissions(String fileName, int mode) {
		try {
			String chmod = String.format("chmod %s %s", Integer.toOctalString(mode), fileName);
			Runtime.getRuntime().exec(chmod);
		} catch (Exception e) {
			Log.e(TAG, fileName, e);
		}
	}

	public static String runShellScript(String name, String script) {
		String fileName = pathCombine(new String[]{tempPath(), name});
		String path = fileName + ".sh";
		String fn = "\"" + fileName + "\"";
		script = enclose("[ -e " + fn + " ] && rm " + fn + "\n", script, " > " + fn) + " 2>&1";
		File file = new File(path);
		contentsToFile(file, "#!/bin/bash\n" + script);
		Process proc = null;
		try {
			file.setExecutable(true);
			proc = Runtime.getRuntime().exec(path);
			proc.waitFor();
		} catch (Exception e) {
			Log.e(TAG, "runShellScript", e);
		} finally {
			if (proc != null)
				proc.destroy();
		}
		String contents = contentsFromFile(new File(fileName));
		return contents != null ? contents.trim() : "";
	}
	
	public static Object getCellValue(Cursor cursor, int rowIndex, int columnIndex) {
		if (rowIndex < 0 || cursor.moveToPosition(rowIndex))
			switch (cursor.getType(columnIndex)) {
			case Cursor.FIELD_TYPE_FLOAT:
				return cursor.getDouble(columnIndex);
			case Cursor.FIELD_TYPE_INTEGER:
				return cursor.getLong(columnIndex);
			case Cursor.FIELD_TYPE_STRING:
				return cursor.getString(columnIndex);
			case Cursor.FIELD_TYPE_BLOB:
				return cursor.getBlob(columnIndex);
			case Cursor.FIELD_TYPE_NULL:
				break;
			}
		return null;
	}

	public static ValList getRow(Cursor cursor) {
		ValList list = vlist();
		for (int c = 0; c < cursor.getColumnCount(); c++) 
			list.add(getCellValue(cursor, -1, c));
		return list;
	}

	public static ValMap table_info2(Context context, Uri uri, String tableName, String flavor) {
		ValMap info = table_info(context, uri, tableName);
		if (flavor != null) {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(
					getDatabaseFile(context, uri).getPath(), 
					null,
					SQLiteDatabase.OPEN_READONLY);
			if (db != null) {
				info.put("VERSION", getFlavorVersion(flavor, db));
				db.close();
			}
		}
		return info;
	}

	public static boolean makeSureExists(String flavor, File dbFile) {
		try {
			Class<?> c = Class.forName(flavor + "Provider");
			for (Class<?> cl : c.getDeclaredClasses()) {
				if ("DatabaseHelper".equals(cl.getSimpleName())) {
					Context context = Context.contextForFlavor("", flavor, dbFile);
					String dbName = c.getDeclaredField("DATABASE_NAME").get(null).toString();
					Object inst = cl.getConstructor(Context.class, String.class)
							.newInstance(context, dbName);
					Method method = cl.getMethod("getWritableDatabase");
					method.invoke(inst);
					cl.getMethod("close").invoke(inst);
				}
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "makeSureExists", e);
			return false;
		}
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
//	NOTE	there is a different method with the same signature in Util2 for Android
	@SuppressWarnings("rawtypes")
	public static Class[] getLocalClasses(String packageName, Object...params) throws Exception {
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    assert classLoader != null;
	    String path = packageName.replace('.', '/');
	    Enumeration<URL> resources = ClassLoader.getSystemResources(path);
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
	
	public static String readAsset(Activity activity, String fileName) {
        StringBuffer sb = new StringBuffer();
        try {
			String path = pathCombine("/assets", fileName);
			InputStream is = Resources.class.getResourceAsStream( path );
			while( true ) {
			    int c = is.read();
	            if( c < 0 )
	                break;
			    sb.append( (char)c );
			}
		} catch (Exception e) {
			Log.e(TAG, "readAsset", e);
		}
        return sb.toString();
    }

}
