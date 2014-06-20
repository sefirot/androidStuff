package com.applang.berichtsheft.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONObject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.VelocityUtil.*;

import android.util.Log;

import com.applang.UserContext;
import com.applang.UserContext.EvaluationTask;
import com.applang.Util.Function;
import com.applang.Util.Job;
import com.applang.Util.ValMap;
import com.applang.Util1;
import com.applang.VelocityUtil.CustomDirective;
import com.applang.VelocityUtil.Visitor;
import com.applang.components.ASTViewer;
import com.applang.components.DatePicker;

import junit.framework.TestCase;

public class VelocityTests extends TestCase
{
	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
		underTest = true;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSimpleContext() throws Exception {
	    Properties p = new Properties();
	    p.setProperty(
	    		"file.resource.loader.path", 
	    		"/home/lotharla/work/Niklas/androidStuff/BerichtsheftApp/src/com/applang/berichtsheft/test");
	    Velocity.init( p );

		VelocityContext context = new VelocityContext();
		context.put("vormittag", "sonnig");
		context.put("nachmittag", "stark bewölkt");
		
		StringWriter w;
		
		w = new StringWriter();
		String s = "v.m. $vormittag, n.m. $nachmittag";
        Velocity.evaluate( context, w, "mystring", s );
        String s1 = w.toString();
        
		w = new StringWriter();
		Velocity.mergeTemplate("description.vm", "UTF-8", context, w );
        String s2 = w.toString();
        
        assertEquals(s1, s2);
        
//        new TemplateNodeView("description.vm");
	}

	public void testBasics() {
		Velocity.init();
		StringWriter writer = new StringWriter();
		Velocity.evaluate(new VelocityContext(),
		                  writer,
		                  "LOG",  // used for logging
		                  "#set($x=(0.5+2)%2)$x\n$x+1");
		
		System.out.println(writer.getBuffer());
	}

	public void testCollection() throws Exception {
		String[] args = strings("uno","dos","tres");
		
		VelocityContext context = new VelocityContext();
		context.put("args", args);
		
		String template = "args = #foreach ($arg in $args) $foreach.count $arg #end" +
				"#set( $planets = [\"Earth\", \"Mars\", \"Neptune\"] )\n\n" +
				"$planets.isEmpty()\n" +
				"$planets.size()\n" +
				"$planets.get(2)\n" +
				"$planets.set(1, 'Venus')\n" +
				"$planets.add('Mercury')\n" +
				"$planets";
		
		StringWriter writer = new StringWriter();
		
		Velocity.init();
		Velocity.evaluate(context,
		                  writer,
		                  "LOG", 
		                  template);
		
		System.out.println(writer.getBuffer());
	}
	
	public void testRecognition() throws Exception {
		Object[] array = (Object[]) getDummies(0);
		for (Object symbol : array) {
			String string = symbol.toString();
			assertFalse(isReference(string));
			String repl = string.replaceFirst("\\.\\.\\.", "A1b2C3");
			assertTrue(isReference(repl));
		}
		assertFalse(isReference("$$"));
		assertTrue(isReference("$mud-Slinger_9"));
		assertFalse(isReference("$-Slinger_9"));
		assertTrue(isReference("$!mud-Slinger_9"));
		assertTrue(isReference("${mud-Slinger_9}"));
		assertTrue(isReference("$!{mud-Slinger_9}"));
		assertTrue(isReference("$customer.Address"));
		assertTrue(isReference("${purchase.Total}"));
		assertFalse(isReference("${purchase.Total.}"));
		
		assertTrue(isMethodCall("$customer.getAddress()"));
		assertTrue(isMethodCall("${purchase.getTotal()}"));
		bracketRecognition("$page.setTitle( \"My Home Page\", 3.141592, $ref )", METHOD_PATTERN);
		
		String string = "#set( $monkey.Say = [\"Not\", $my, \"fault\"] )";
		assertTrue(isDirective(string));
		bracketRecognition(string, DIRECTIVE_PATTERN);
		
		string = "#if($i<0)<#elseif($i>=$hello.length())>#{else}$hello.charAt($i)#end";
		assertNotNull(findFirstIn(string, directivePattern("else(?!if)")));
		assertNotNull(findFirstIn(string, directivePattern("elseif")));
		string = "#if($i<0)<#elseif($i>=$hello.length())>#end";
		assertNull(findFirstIn(string, directivePattern("else(?!if)")));
	}
	
	public void bracketRecognition(String sample, Pattern pattern) {
		MatchResult[] m = findAllIn(sample, pattern);
		assertEquals(1, m.length);
		println(pattern);
/*		for (int i = 0; i <= m[0].groupCount(); i++) {
			println(m[0].group(i));
		}
*/		println(m[0].group(m[0].groupCount()));
	}
	
	public void testNodes() {
		String resName = "hello.vm";
		final String vm = resourceFrom("/assets/" + resName, "UTF-8");
		SimpleNode doc = parse(new StringReader(vm), resName);
		assertNotNull(doc);
		Visitor.walk(doc, new Function<Object>() {
			public Object apply(Object... params) {
				Node node = param(null, 0, params);
				Object[] data = param(null, 2, params);
				if (!Visitor.isProcessNode(node)) {
					int[] lc = Visitor.beginLC(node);
					int[] span = Visitor.span(node);
					int[] offsets = getTextOffsets(vm, span);
					String string = vm.substring(offsets[0], offsets[1]);
					println(string);
					assertEquals(Visitor.tokens(node), string);
					switch (lc[0] + 100 * lc[1]) {
					case 101:	assertEquals(0, offsets[0]);	assertEquals(21, offsets[1]);	break;
					case 601:	assertEquals(5, offsets[0]);	assertEquals(11, offsets[1]);	break;
					}
				}
				return data;
			}
		});
	}
	
	public void testLineColumn() {
		Log.changeLogLevel(Log.DEBUG);
		
		String string = "#set($hello='Hello')\n" +
				"#foreach($i in [-5..9])\n" +
					"#if($i<0)<#elseif($i>=$hello.length())>#{else}$hello.charAt($i)#end\n" +
				"#{end}";
		lineColumnTest(string, -1);
		
		lineColumnTest("world", 5);
		lineColumnTest("wo\trld", 6);
		lineColumnTest("\tworld", 6);
		lineColumnTest("\t\tworld", 7);
//		lineColumnTest("\tthis\tworld", 11);
	}
	
	void lineColumnTest(String string, int endColumn) {
		printAST(string, true, false);

		SimpleNode doc = parse(new StringReader(string), string);
		if (endColumn < 0)
			return;
		
		Node n = Visitor.find(doc, ints(0,0), "wo");
		assertNotNull(n);
		int[] beginLC = Visitor.beginLC(n);
		assertEquals(1, beginLC[0]);
		assertEquals(1, beginLC[1]);
		int[] endLC = Visitor.endLC(n);
		assertEquals(1, endLC[0]);
		assertEquals(endColumn, endLC[1]);
	}
	
	private void printTail(Node node) {
		println(Visitor.tail(node));
	}

	void printAST(String text, Object...params) {
		final boolean detailed = param_Boolean(true, 0, params);
		final boolean essentials = param_Boolean(true, 1, params);
        try {
			SimpleNode document = RuntimeSingleton.parse(new StringReader(text), "");
			
			final PrintWriter writer = new PrintWriter(System.out);
			Visitor.walk(document, new Function<Object>() {
				public Object apply(Object...params) {
					Node node = param(null, 0, params);
					int indents = param_Integer(0, 1, params);
					if (Visitor.isProcessNode(node)) {
						String string = node.toString();
						int offset = string.indexOf("tokens=") + 7;
						String substring = string.substring(offset, string.length() - 1)
								.replaceAll(NEWLINE_REGEX, "\\" + NEWLINE_REGEX);
						writer.append(indentedLine(substring, null, indents));
					}
					else {
						if (!essentials || Visitor.isEssential.apply(node)) {
							String info = Visitor.nodeInfo(node, detailed ? 1 : 0);
							writer.append(indents + TAB + indentedLine(info, detailed ? null : TAB, indents));
						}
					}
					return null;
				}
			});
			writer.flush();
			
			printTail(document);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	void askParams(Object...params) {
		if (params.length > 1) {
			JCheckBox[] checks = new JCheckBox[2];
			checks[0] = new JCheckBox();
			checks[0].setText("detailed");
			checks[0].setSelected(param_Boolean(true, 0, params));
			checks[1] = new JCheckBox();
			checks[1].setText("essentials");
			checks[1].setSelected(param_Boolean(true, 1, params));
			JOptionPane.showMessageDialog(null, checks, "Visit", 
					JOptionPane.PLAIN_MESSAGE, 
					null);
			params[0]=checks[0].isSelected();
			params[1]=checks[1].isSelected();
		}
	}
	
	public void testVisitor() {
		File vm = ASTViewer.getVm();
		Object[] params = new Object[]{true,true};
		askParams(params);
		printAST(contentsFromFile(vm), params);
	}
	
	public void testUpdateAST() throws Exception {
		String string = "#set($hello='Hello')" +
				"#foreach($i in [-5..9])" +
					"#if($i<0)<#elseif($i>=$hello.length())>#{else}$hello.charAt($i)#end" +
				"#{end}";
		SimpleNode document = parse(new StringReader(string), "");
		
		Node node; 
		int[] startLC = ints(0,0);
		while ((node = Visitor.find(document, startLC, "\\<")) != null) {
			printTokens(node);
			startLC = Visitor.beginLC(node);
		}
		
		node = Visitor.find(document, ints(0,0), "^\\<");
		Visitor.update(node, "<\n>");
		printTokens(node);
		printTail(document);
	}
	
	private void printTokens(Node node) {
		println(enclose("'", Visitor.tokens(node)));
	}
	
	public void testProblems() throws Exception {
		String string = "#foreach( $ref_R in $arg_L )...#end";
		
		SimpleNode document = parse(new StringReader(string), "");
		String[] array = VelocityTests.listReferences(document);
		assertTrue(asList(array).contains("$ref_R"));
		assertTrue(asList(array).contains("$arg_L"));
		String arg1 = "$xxx";
		String arg2 = "zzz";
		println(string = VelocityTests.updateReferences(document, strings(arg1,arg2)));
		assertTrue(string.contains(arg1));
		assertTrue(string.contains(arg2));
		assertNull(parse(new StringReader(string), ""));
		println(getMessage());
		println(getProblemCoordinates());
		println(getSuggestionsFromProblem());
		assertNotNull(getProblemArgumentInfo());
		println(getProblemMacroInfo());
		println(getProblemArgumentInfo());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testMap() throws Exception {
		Map person = new HashMap();
		person.put("name", "Duke");
		
		VelocityContext context = new VelocityContext();
		context.put("person", person);
		
		StringWriter writer = new StringWriter();
		Velocity.evaluate(context, writer, "TEST", "${person.name}");
		assertEquals("Duke", writer.getBuffer().toString());
	}

	@SuppressWarnings("deprecation")
	public void testFormatter() throws Exception {
		Date today = new Date();
		
		VelocityContext context = new VelocityContext();
		context.put("formatter", new org.apache.velocity.app.tools.VelocityFormatter(context));
		context.put("today", today);
		
		StringWriter writer = new StringWriter();
		Velocity.evaluate(context, writer, "TEST", "today is $formatter.formatShortDate($today)");
		
		DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
		String expected = "today is " + format.format(today);
		assertEquals(expected, writer.getBuffer().toString());
	}

	DataBaseConnect dbCon = new DataBaseConnect();
	String test_db = tempPath() + "/test.db", test_sql = tempPath() + "/test.sql";
	
	String sqlTemplate = "PRAGMA foreign_keys=OFF;" +
			"BEGIN TRANSACTION;" +
			"DROP TABLE if exists notes;" +
			"CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,note TEXT,created INTEGER,modified INTEGER);" +
			"%s" +
			"COMMIT;";
	String insertTemplate = 
			"INSERT INTO notes (title,note,created,modified) VALUES ('%s', '%s', %s, 1000*strftime('%%s','now'));";
	
	public void testNoteContext() throws Exception {
		String[] args = {
				"ein", "$kein", "null", 
				"Kein", "$ein", "null", 
				"Fehler", "efhler", "null", 
				"eFhler", "ehfler", "null", 
				"ehFler", "ehlfer", "null", 
				"Im", "im", "null", 
				"System", "system", "null", 
				"Bemerkung", "$Kein $Fehler $Im $System", "" + now(), 	
				"Bemerkung", "$Kein $eFhler $Im $System", "" + now(), 	
				"Bemerkung", "$Kein $ehFler $Im $System", "" + now(), 	
		};
		String insert = "";
		for (int i = 0; i < args.length - 2; i+=3) 
			insert += String.format(insertTemplate, args[i], args[i+1], args[i+2]);
		
		File db = new File(test_db);
		File sql = new File(test_sql);
		contentsToFile(sql, String.format(sqlTemplate, insert));
		assertTrue(DataBaseConnect.bulkSqlite(db, sql));
		assertTrue(DataBaseConnect.dumpSqlite(db, sql));
//		println(contentsFromFile(sql));
		
		ValMap map = null;
		assertTrue(dbCon.open(test_db));
		try {
			String s = "select title,note from notes where created is null group by title order by modified desc";
			PreparedStatement ps = dbCon.getCon().prepareStatement(s);
			map = getResultMap(ps);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		dbCon.close();

		CustomContext nc = new CustomContext(map);
		assertEquals(args.length / 3 - 3, nc.getKeys().length);
		
		nc.put("kein", -0.00003141592);
		
		String fileName = tempPath() + "/context.ser";
		new File(fileName).delete();
		
		FileOutputStream fileOut = new FileOutputStream(fileName);
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		out.writeObject(nc);
		out.close();
		fileOut.close();
		
		FileInputStream fileIn = new FileInputStream(fileName);
		ObjectInputStream in = new ObjectInputStream(fileIn);
		CustomContext nc2 = (CustomContext) in.readObject();
		in.close();
		fileIn.close();
		
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			assertTrue(nc2.containsKey(key));
			assertEquals(nc.get(key), nc2.get(key));
		}
		
		assertTrue(dbCon.open(test_db));
		try {
			ResultSet rs = dbCon.getStmt().executeQuery("select note from notes where created not null");
			while (rs.next()) {
				String s = rs.getString(1);
				int loop = 0;
				while (loop < 10 && (s = evaluate(nc, s, "notes")).contains("$")) 
					loop++;
				println(s);
				assertFalse("error in context", s.contains("$"));
			}
		}
		finally {
			dbCon.close();
		}
	}

	public void testWeatherContext() {
        RuntimeSingleton.init( new Properties() );

		ValMap map = null;
		String dbName = "databases/weather_info.db";
		assertTrue(dbCon.open(dbName));
		String sql = "select created,precipitation from weathers";
		sql += " where created between ? and ? and location = ?";
		try {
			String dateString = "2012-12-01", location = "10519";
			long[] interval = DatePicker.toInterval(dateString, 1);
			PreparedStatement ps = dbCon.getCon().prepareStatement(sql);
			ps.setLong(1, interval[0]);
			ps.setLong(2, interval[1] - 1);
			ps.setString(3, location);
			map = getResultMap(ps,
				new Function<String>() {
					public String apply(Object... params) {
						String f = "D" + formatDate(Long.parseLong(params[0].toString()), "yyyy-MM-dd");
						return f;
					}
				}, 
				new Function<Object>() {
					public Object apply(Object... params) {
						return params[0] + " mm";
					}
				}
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		dbCon.close();

		CustomContext wc = new CustomContext(map);
		
		println(evaluate(wc, 
        		"Niederschlag am 2012-12-01 : $D2012-12-01", 
        		"weather"));

		VelocityContext vc = new VelocityContext();
		assertTrue(dbCon.open(dbName));
		sql = "select created,precipitation,maxtemp,mintemp,description from weathers";
		sql += " where created between ? and ? and location = ?";
		try {
			long[] interval = DatePicker.toInterval("50/2012", 1);
			PreparedStatement ps = dbCon.getCon().prepareStatement(sql);
			ps.setLong(1, interval[0]);
			ps.setLong(2, interval[1] - 1);
			ps.setString(3, "10519");
			List<ValMap> list = getResultMapList(ps,
				new Function<String>() {
					public String apply(Object... params) {
						return formatDate(Long.parseLong(params[0].toString()), DatePicker.calendarFormat);
					}
				}, 
				new Function<Object>() {
					public Object apply(Object... params) {
						return params[0] + " mm";
					}
				}, 
				new Function<Object>() {
					public Object apply(Object... params) {
						return params[0] + " °C";
					}
				}, 
				new Function<Object>() {
					public Object apply(Object... params) {
						return params[0] + " °C";
					}
				}
			);
			vc.put("results", list);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		dbCon.close();
		
		println(evaluate(vc, 
				"#foreach ($result in $results) " +
				"am $result.created $result.description bei zwischen $result.mintemp und $result.maxtemp \n" +
				"#end", 
				"weather"));
	}

	public void testOpenWeather1() throws Exception {
		String url = "http://api.openweathermap.org/data/2.1/weather/city/2931361?type=json";
	    String jsonText = readFromUrl(url, "UTF-8");
//		println(jsonText);
	    
		JSONObject json = new JSONObject(jsonText);
//		println(json.toString(4));
		
		Object openweather = Util1.walkJSON(null, json, new Function<Object>() {
			public Object apply(Object...params) {
				Object[] path = param(null, 0, params);
				Object value = param(null, 1, params);
				println("%s : %s", Arrays.toString(path), value);
				return value;
			}
		});
//		println(openweather);
		
		VelocityContext vc = new VelocityContext();
		vc.put("weather", openweather);
		
		println(evaluate(vc, 
				"\n$weather.name, $weather.date\n" +
				"#foreach ($w in $weather.weather) " +
					"$w.description \n" +
				"#end", 
				"openweather"));
	}
	
	public void testOpenWeather2() throws Exception {
    	int days = 4;	//	Util.daysToTodayFrom(2012, 40, 2);
	    String url = String.format(
	    		"http://api.openweathermap.org/data/2.1/history/station/4885?cnt=%d&type=day", 
	    		days + 1);
	    String jsonText = readFromUrl(url, "UTF-8");
//		println(jsonText);
	    
		JSONObject json = new JSONObject(jsonText);
//		println(json.toString(4));
		
		Object openweather = Util1.walkJSON(null, json, new Function<Object>() {
			public Object apply(Object...params) {
				Object[] path = param(null, 0, params);
				Object value = param(null, 1, params);
				String name = Arrays.toString(path);
				if ("dt".equals(path[path.length - 1])) 
					value = formatDate(toLong(0L,value.toString()) * 1000);
				else if (name.contains("temp"))
					value = kelvin2celsius(value);
//				println("%s : %s", name, value);
				return value;
			}
		});
//		println(openweather);
		
		VelocityContext vc = new VelocityContext();
		vc.put("openweather", openweather);
		
		println(evaluate(vc, 
				"\n$openweather.station_id\n" +
				"#foreach ($day in $openweather.list) " +
					"$day.dt temperature between $day.main.temp.v and $day.main.temp_max °C\n" +
				"#end", 
				"openweather"));
	}
	
	public void testOpenWeather3() throws Exception {
    	int cnt = 2;	//	Util.daysToTodayFrom(2012, 40, 2);
	    String url = String.format(
	    		"http://api.openweathermap.org/data/2.1/history/station/4885?cnt=%d&type=hour", 
	    		1 + cnt);
	    String text = readFromUrl(url, "UTF-8");
		println(text);
		
		Util1.walkJSON(null, new JSONObject(text), new Function<Object>() {
			public Object apply(Object...params) {
				Object[] path = param(null, 0, params);
				Object value = param(null, 1, params);
				String name = Arrays.toString(path);
				if ("dt".equals(path[path.length - 1])) {
					value = formatDate(toLong(0L,value.toString()) * 1000);
					println("%s : %s", name, value);
				}
				else if (name.contains("temp"))
					value = kelvin2celsius(value);
				
				if ("list".equals(path[0]) && name.contains("rain")) {
					println("%s : %s", name, value);
				}
				
				return value;
			}
		});
	}

	public void testReferences() throws Exception {
		InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("ToJSON.vm"));
		int cnt = 0;
		for (MatchResult m : findAllIn(readAll(isr), REFERENCE_PATTERN)) {
			println(m.group());
			cnt++;
		}
		assertEquals(29, cnt);
		isr.close();
	}

	public void testDirectives() throws Exception {
		for (Map.Entry<String, String> entry : signatures().entrySet()) {
			println(entry);
			String signature = entry.getValue();
			SimpleNode document = parse(new StringReader(signature), "");
			if (document == null)
				continue;
			
			String[] array = VelocityTests.listReferences(document);
			assertEquals(findAllIn(signature, VRI_PATTERN).length, array.length);
		}
	}
	
	class CursorDirective extends CustomDirective
	{
	    public CursorDirective() {
			super();
	        arguments = strings("message_S","variable_R",
	        		"uri_U",
	        		"projection_L",
	        		optionalize("selection_S"),
	        		optionalize("selectionArgs_L"),
	        		optionalize("sortOrder_S"),
	        		optionalize("flag_B"));
		}
		@Override
		public String getName() {
			return "cursor";
		}
		@Override
		public int getType() {
			return LINE;
		}
		@Override
		public boolean render(InternalContextAdapter context, Writer writer,
				Node node) throws IOException, ResourceNotFoundException,
				ParseErrorException, MethodInvocationException {
			return false;
		}
	}
	
	/**
	 * A directive to prompt the user for a value.
	 */
	class Prompt extends CustomDirective
	//    implements VelocityConstants
	{
	   /**
	    * Return name of this directive.
	    */
	   public String getName()
	   {
	      return "prompt";
	   }
	
	   /**
	    * Return type of this directive.
	    */
	   public int getType()
	   {
	      return LINE;
	   }
	
	   /**
	    * Prompt the user for a value.
	    */
	   public boolean render(InternalContextAdapter context, Writer writer, Node node) throws MethodInvocationException
	   {
	      Object prompt = getRequiredValue(node, 0, "label", context);
	      if (prompt == null) {
	         return false;
	      }
	      String key = getRequiredVariable(node, 1, "key");
	      Object defaultValue = getOptionalValue(node, 2, context);
	      boolean overrideContext = getOptionalBoolean(node, 3, context);
	
	      if (!overrideContext && context.getInternalUserContext().get(key) != null) {
	         return true;
	      }
	
//	      JEditTextArea textArea = (JEditTextArea) context.get(TEXT_AREA);
	      Object value = JOptionPane.showInputDialog(null, prompt,
	                                                 "Velocity Prompt",
	                                                 JOptionPane.QUESTION_MESSAGE,
	                                                 null, null, defaultValue);
	      if (value != null) {
	         context.getInternalUserContext().put(key, value);
	      }
	      return true;
	   }
	}
	
	public void testAssemble() throws Exception {
//		assembleTest(signatures().get("macro"));
		assembleTest(new CursorDirective().signature(true));
		for (String value : signatures().values()) {
			assembleTest(value);
		}
	}
	
	private void assembleTest(String value) {
		String name = firstIdentifierFrom(value);
		ValList args = argumentsFrom(value);
		ValList list = vlist();
		for (Object arg : args) {
			String type = argumentType(arg.toString());
			Object[] dummies = (Object[]) getDummies(type);
			if (isAvailable(0, dummies))
				list.add(dummies[0]);
		}
		int size = args.size();
		if (size > 0 && UNKNOWN.equals(args.get(-1)))
			list.add(list.get(-1));
		String directive = assemble(value, list);
		println(/*name, args, list, */directive);
		if ("#cursor".equals(name)) {
			for (int i = -1; isOptionalArgument(args.get(i).toString()); i--) {
				assertEquals(size + i, findAllIn(directive, Pattern.compile(ARGUMENT_SEPARATORS[0])).length);
				list.remove(-1);
				directive = assemble(value, list);
				println(directive);
			}
		}
	}

	public void testUserDirectives() throws Exception {
		Properties props = new Properties();
		//	InputStream in = getClass().getResourceAsStream("velocity.properties");
		//	props.load(in);
		props.setProperty("userdirective", 
//				"com.applang.berichtsheft.test.TruncateDirective," +
//				"com.applang.berichtsheft.test.TruncateBlockDirective," +
				"com.applang.Prompt");
		UserContext.setupVelocity(null, true);
		
		String t = 
//				"#set($test = \"long line that should be truncated\")\n" +
//				"#truncate(\"Testing $test\", 20, \"...\", true)\n" +
//				"#truncateBlock(15,\"...\",true)" +
//				"    Long block" +
//				"    that will be" +
//				"    truncated" +
//				"#end\n" +
				"#set($key=\"\")\n" +
				"#prompt(\"label\",$key,\"value\")\n\n" +
				"$key";
		
		new EvaluationTask(null, null, null, null, new Job<Object>() {
			public void perform(Object s, Object[] params) throws Exception {
				println(s);
				assertEquals("value", s);
			}
		}, t).execute();
//		assertEquals("Testing long line...    Long...\n", s);
	}
	
	public class Product {
	    private String name;
	    private double price;
	     
	    public Product(String aName, double aPrice) {
	        name = aName;
	        price = aPrice;
	    }
	     
	    public String getName() {
	        return name;
	    }
	    public void setName(String name) {
	        this.name = name;
	    }
	    public double getPrice() {
	        return price;
	    }
	    public void setPrice(double price) {
	        this.price = price;
	    }
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testMathTool() throws Exception {
	    Properties p = new Properties();
	    p.setProperty(
	    		"file.resource.loader.path", 
	    		"/home/lotharla/work/Niklas/androidStuff/BerichtsheftApp/src/com/applang/berichtsheft/test");
	    Velocity.init( p );
	    
	    VelocityContext ctx = new VelocityContext();
	    ctx.put("math", new MathTool());
	    ctx.put("aNumber", new Double(5.5));
	    System.out.println(evaluate(ctx, "$math.random is a random number", "mathtool"));
	    System.out.println(evaluate(ctx, "$math.random(1, 20) is a random number between 1 and 20", "mathtool"));
	    System.out.println(evaluate(ctx, "4.45678 rounded to 3 places is $math.roundTo(3, \"4.45678\")", "mathtool"));
	    System.out.println(evaluate(ctx, "4.45678 rounded to the nearest integer is $math.roundToInt(\"4.45678\")", "mathtool"));
	    System.out.println(evaluate(ctx, "$aNumber ^ 3.2 = $math.pow($aNumber, \"3.2\")", "mathtool"));
	    System.out.println(evaluate(ctx, "$aNumber / 3.2 = $math.div($aNumber, \"3.2\")", "mathtool"));
	    System.out.println(evaluate(ctx, "$aNumber * 3.2 = $math.mul($aNumber, \"3.2\")", "mathtool"));
	    System.out.println(evaluate(ctx, "$aNumber - 3.2 = $math.sub($aNumber, \"3.2\")", "mathtool"));
	    System.out.println(evaluate(ctx, "$aNumber + 3.2 = $math.add($aNumber, \"3.2\")", "mathtool"));
	    System.out.println(evaluate(ctx, "The maximum of $aNumber and 3.2 is $math.max($aNumber, \"3.2\")", "mathtool"));
	    System.out.println(evaluate(ctx, "The minimum of $aNumber and 3.2 is $math.min($aNumber, \"3.2\")", "mathtool"));
        
        Collection products = new ArrayList();
        products.add(new Product("Product 1", 112.199));
        products.add(new Product("Product 2", 113.991));
        products.add(new Product("Product 3", 111.919));
        ctx.put("productList", products);
        System.out.println(merge(ctx, Velocity.getTemplate("calculation.vm")));
//	    System.out.println(evaluate(ctx, 
//	    		"#set($totalPrice = 0)" 
//	    		+"#foreach($product in $productList)" 
//	    		+"  $product.Name    $$product.Price\n" 
//	    		+"#set($totalPrice = $math.add($totalPrice, $product.Price))" 
//	    		+"#end" 
//	    		+"Total Price: $$totalPrice\n"
//	    		, "mathtool"
//	    ));
        ctx.put("date", new DateTool());
        Calendar aDate = Calendar.getInstance(TimeZone.getTimeZone("MET"));
        aDate.set(200, 11, 25);
        ctx.put("aDate", aDate);
        System.out.println(merge(ctx, Velocity.getTemplate("datetool.vm")));
        ctx.put("number", new NumberTool());
        ctx.put("aNumber", new Double(0.95));
        ctx.put("aLocale", Locale.UK);
	    System.out.println(evaluate(ctx, "Currency (with Locale): $number.format(\"currency\", $aNumber, $aLocale)", "numbertool"));
	    System.out.println(evaluate(ctx, "Integer Formatting:     $number.format(\"integer\", $aNumber)", "numbertool"));
	    System.out.println(evaluate(ctx, "Percentage Formatting:  $number.format(\"percent\", $aNumber)", "numbertool"));
	    System.out.println(evaluate(ctx, "Currency Formatting:    $number.format(\"currency\", $aNumber)", "numbertool"));
	}

	public static String updateReferences(SimpleNode document, final String[] array) {
		Visitor.walk(document, new Function<Object>() {
			@Override
			public Object apply(Object...params) {
				Node node = param(null, 0, params);
				Object[] data = param(null, 2, params);
				Integer i = param(-1, 0, data);
				Token t = node.getFirstToken();
				if (t.image.startsWith(VRI.toString()) && i < array.length) {
					t.image = array[i];
					data[0] = ++i;
				}
				return data;
			}
	    }, new Object[] {0});
		return Visitor.tail(document);
	}

	public static String[] listReferences(SimpleNode document) {
	    ArrayList<String> list = alist();
	    
		Visitor.walk(document, new Function<Object>() {
			@Override
			public Object apply(Object...params) {
				Node node = param(null, 0, params);
				Object[] data = param(null, 2, params);
				ArrayList<String> list = param(null, 0, data);
				Token t = node.getFirstToken();
				if (t.image.startsWith(VRI.toString()))
					list.add(t.image);
				return data;
			}
	    }, list);
	    
	    return toStrings(list);
	}
}
