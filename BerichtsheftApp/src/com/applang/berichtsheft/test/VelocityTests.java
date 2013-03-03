package com.applang.berichtsheft.test;

import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.tools.VelocityFormatter;
import org.apache.velocity.context.AbstractContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeSingleton;

import com.applang.DataBaseConnection;
import com.applang.Util;

import static com.applang.Util.*;
import static com.applang.Util2.*;
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
		context.put("formatter", new VelocityFormatter(context));
		context.put("today", today);
		
		StringWriter writer = new StringWriter();
		Velocity.evaluate(context, writer, "TEST", "today is $formatter.formatShortDate($today)");
		
		DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
		String expected = "today is " + format.format(today);
		assertEquals(expected, writer.getBuffer().toString());
	}

	DataBaseConnection dbCon = new DataBaseConnection();
	
	public class WeatherContext extends AbstractContext
	{
		public WeatherContext(String dateString, String location, String itemName) {
			assertTrue(dbCon.open("databases/weather_info.db"));
			String sql = String.format("select created,%s from weathers", itemName);
			sql += " where created between ? and ? and location = ?";
			try {
				long[] interval = DatePicker.toInterval(dateString, 1);
				PreparedStatement ps = dbCon.con.prepareStatement(sql);
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
		}

		ValMap map = null;
		
		@Override
		public Object internalGet(String key) {
			return map == null ? null : map.get(key.substring(1));
		}

		@Override
		public Object internalPut(String key, Object value) {
			return map == null ? null : map.put(key, value);
		}

		@Override
		public boolean internalContainsKey(Object key) {
			return map == null ? false : map.containsKey(key);
		}

		@Override
		public Object[] internalGetKeys() {
			return map == null ? new Object[0] : map.keySet().toArray();
		}

		@Override
		public Object internalRemove(Object key) {
			return map == null ? null : map.remove(key);
		}
	}

	public void testWeatherContext() {
        RuntimeSingleton.init( new Properties() );
		
		WeatherContext wc = new WeatherContext(
				"2012-12-01", 
				"10519", 
				"precipitation");
		
        printEvaluatedTemplate(wc, 
        		"Niederschlag am 2012-12-01 : $D2012-12-01", 
        		"weather");

		VelocityContext vc = new VelocityContext();
		assertTrue(dbCon.open("databases/weather_info.db"));
		String sql = "select created,precipitation,maxtemp,mintemp,description from weathers";
		sql += " where created between ? and ? and location = ?";
		try {
			long[] interval = DatePicker.toInterval("50/2012", 1);
			PreparedStatement ps = dbCon.con.prepareStatement(sql);
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
		
		printEvaluatedTemplate(vc, 
				"#foreach ($result in $results) " +
				"am $result.created $result.description bei zwischen $result.mintemp und $result.maxtemp \n" +
				"#end", 
				"weather");
	}
	
	void printEvaluatedTemplate(Context vc, String template, String logTag) {
		StringWriter w = new StringWriter();
		Velocity.evaluate( vc, w, logTag, template );
		println(w.toString());
	}

	public void testOpenWeather() throws Exception {
		String url = "http://api.openweathermap.org/data/2.1/weather/city/2931361?type=json";
	    String jsonText = readFromUrl(url, "UTF-8");
//		println(jsonText);
	    
		JSONObject json = new JSONObject(jsonText);
//		println(json.toString(4));
		
		Object openweather = Util.walkJSON("", json, new Function<Object>() {
			public Object apply(Object...params) {
				String name = paramString(null, 0, params);
				Object value = param(null, 1, params);
				println("%s : %s", name, value);
				return value;
			}
		});
//		println(openweather);
		
		VelocityContext vc = new VelocityContext();
		vc.put("openweather", openweather);
		
		printEvaluatedTemplate(vc, 
				"\n$openweather.name, $openweather.date\n" +
				"#foreach ($w in $openweather.weather) " +
					"$w.description \n" +
				"#end", 
				"openweather");
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
		
		Object openweather = Util.walkJSON("", json, new Function<Object>() {
			public Object apply(Object...params) {
				String name = paramString(null, 0, params);
				Object value = param(null, 1, params);
				if (name.endsWith("dt"))
					value = formatDate(toLong(0L,value.toString()) * 1000);
				else if (name.contains("temp"))
					value = round(toDouble(0D, value.toString()) + absoluteZero, 2);
				println("%s : %s", name, value);
				return value;
			}
		});
//		println(openweather);
		
		VelocityContext vc = new VelocityContext();
		vc.put("openweather", openweather);
		
		printEvaluatedTemplate(vc, 
				"\n$openweather.station_id\n" +
				"#foreach ($day in $openweather.list) " +
					"$day.dt temperature between $day.main.temp.v and $day.main.temp_max °C\n" +
				"#end", 
				"openweather");
	}
}
