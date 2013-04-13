package com.applang.berichtsheft.test;

import java.io.File;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeSingleton;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.VelocityUtil.*;

import com.applang.VelocityUtil;
import com.applang.berichtsheft.ui.components.DatePicker;

import junit.framework.TestCase;

public class VelocityTests extends TestCase {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
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
	}

	public void testCollection() throws Exception {
		String[] args = new String[] {"uno","dos","tres"};
		
		VelocityContext context = new VelocityContext();
		context.put("args", args);
		
		String template = "args = #foreach ($arg in $args) $arg #end";
		
		StringWriter writer = new StringWriter();
		
		Velocity.init();
		Velocity.evaluate(context,
		                  writer,
		                  "LOG",  // used for logging
		                  template);
		
		System.out.println(writer.getBuffer());
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
	String test_db = "/tmp/test.db", test_sql = "/tmp/test.sql";
	
	String sqlTemplate = "PRAGMA foreign_keys=OFF;" +
			"BEGIN TRANSACTION;" +
			"DROP TABLE if exists notes;" +
			"CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,note TEXT,created INTEGER,modified INTEGER);" +
			"%s" +
			"COMMIT;";
	String insertTemplate = 
			"INSERT INTO notes (title,note,created,modified) VALUES ('%s', '%s', %s, 1000*strftime('%%s','now'));";
	
	public void testNoteContext() throws SQLException {
		String[] args = {
				"ein", "kein", "null", 
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

		MapContext nc = new MapContext(map);
		assertEquals(args.length / 3 - 3, nc.getKeys().length);
		
		assertTrue(dbCon.open(test_db));
		ResultSet rs = dbCon.getStmt().executeQuery("select note from notes where created not null");
		while (rs.next()) {
			String s = rs.getString(1);
			while ((s = evaluation(nc, s, "notes")).contains("$")) ;
			println(s);
		}
		dbCon.close();
	}

	public void testWeatherContext() {
        RuntimeSingleton.init( new Properties() );

		ValMap map = null;
		assertTrue(dbCon.open("databases/weather_info.db"));
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
						return formatDate(Long.parseLong(params[0].toString()));
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

		MapContext wc = new MapContext(map);
		
		println(evaluation(wc, 
        		"Niederschlag am 2012-12-01 : $D2012-12-01", 
        		"weather"));

		VelocityContext vc = new VelocityContext();
		assertTrue(dbCon.open("databases/weather_info.db"));
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
		
		println(evaluation(vc, 
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
		
		Object openweather = VelocityUtil.walkJSON(null, json, new Function<Object>() {
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
		
		println(evaluation(vc, 
				"\n$weather.name, $weather.date\n" +
				"#foreach ($w in $weather.weather) " +
					"$w.description \n" +
				"#end", 
				"openweather"));
	}
	
	public void testOpenWeather2() throws Exception {
    	int days = 4;	//	Util.daysToTodayFrom(2012, 40, 2);
	    String url = String.format(
	    		"http://openweathermap.org/data/history?id=4885&cnt=%d&type=day", 
	    		days + 1);
	    String jsonText = readFromUrl(url, "UTF-8");
//		println(jsonText);
	    
		JSONObject json = new JSONObject(jsonText);
//		println(json.toString(4));
		
		Object openweather = VelocityUtil.walkJSON(null, json, new Function<Object>() {
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
		
		println(evaluation(vc, 
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
		
		VelocityUtil.walkJSON(null, new JSONObject(text), new Function<Object>() {
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
}
