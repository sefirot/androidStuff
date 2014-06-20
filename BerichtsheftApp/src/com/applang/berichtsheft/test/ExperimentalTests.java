package com.applang.berichtsheft.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.swing.ProgressMonitorInputStream;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import android.net.Uri;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.components.DatePicker;
import com.applang.components.WeatherManager;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

import junit.framework.TestCase;

public class ExperimentalTests extends TestCase
{
	public void setUp() throws Exception {
		BerichtsheftApp.loadSettings();
		messRedirection = new Function<String>() {
			public String apply(Object... params) {
				String message = param("", 0, params);
				return message;
			}
		};
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testPeriod() throws Exception {
		assertTrue(DatePicker.Period.pick(0));
		println(getSetting("weather.period", ""));
		println(DatePicker.Period.getDescription(0));
		String dateString = DatePicker.Period.weekDate();
		int[] weekDate = DatePicker.parseWeekDate(dateString);
		println(dateString, weekDate);
	}

	public void testEvaluation() throws Exception {
		int[] dateParts = ints(2013, 1, 1, 2);
//		dateParts = DatePicker.pickAPeriod(dateParts, "pick day, week or month");
		assertTrue(dateParts != null);
		DatePicker.Period.setParts(dateParts);
		WeatherManager wm = new WeatherManager();
		wm.parseAndEvaluate("10519", DatePicker.Period.getParts(), true, null);
		startFrame(null);
    }
	
	String home = System.getProperty("user.home");

	public void testDescriptions() {
		int exitVal = -1;
        try
        {
        	ProcessBuilder builder = new ProcessBuilder(
            		home + "/gawk-4.0.0/gawk", 
            		"-f", BerichtsheftApp.applicationDataPath("Skripte/descriptions.awk"));
//            		"-f", home + "/work/awk/letter-count.awk", "-v", "letters=O V C *");
            builder.directory(new File(System.getProperty("user.dir")));
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            OutputStream os = process.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
//			InputStream is = new URL(NOAA_URL).openStream();
			@SuppressWarnings("resource")
			InputStream is = new FileInputStream(home + "/work/Niklas/www1.ncdc.noaa.gov/553356121374dat.txt");	
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            
            String line;
            while ((line = br.readLine()) != null) 
            	osw.write(line + "\n");
            
            br.close();
            is.close();
            osw.close();
            
            WeatherManager wm = new WeatherManager();
            assertTrue(wm.openConnection("databases/weather_info.db"));
           
            is = process.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            
            int unfound = 0;
            ValMap values = vmap();
            while ((line = br.readLine()) != null) {
//            	System.out.println(line);
                String[] parts = line.split("\t", 3);
                String location = parts[0].substring(0, 5);
                long time = Long.parseLong(parts[1]);
                values.put("description", parts[2]);
    			long id = wm.getIdOfDay(location, time);
				if (id > -1)
    				assertEquals(1, wm.update(id, values));
    			else
    				unfound++;
            }
            System.out.printf("%d unfound", unfound);
            
            br.close();
            
			exitVal = process.waitFor();
        } catch (Throwable t) {
			t.printStackTrace();
        }
		assertEquals("ExitValue: " + exitVal, 0, exitVal);
	}

	public void testStations() throws Exception {
		Uri uri = WeatherManager.siteUri("DE", "10519", "18Z", 2012, 12, 31, 1);
		println(uri);
    	Document doc = Jsoup.connect(uri.toString())
    			.timeout(10000)
    			.get();
    	
    	Elements elements = doc.select("select[name=ind] > option");
		
		final ValMap stations = vmap();
		for (Element element : elements) 
			stations.put(element.text().trim(), element.attr("value"));
		
		assertEquals("10519", stations.get("BONN-ROLEBER"));
		
		jsonTest(stations);
		
		println("%d stations", stations.size());
	}
	
	String DETAIL_URL = "http://www.mundomanz.com/meteo_p/byind?" +
			"countr=GERMANY&" +
			"ind=10519&year=2012&" +
			"month=12&" +
			"day=31&" +
			"n_days=2&" +
			"time=18Z&" +
			"trans=DE&" +
			"l=1&" +
			"action=display";
	
	public void testDetails() throws Exception {
		Document doc = getJsoup(DETAIL_URL);
		Element partes = doc.getElementById("partes");
		assertNotNull(partes);
		Elements elements = partes.select("b");
		ValMap details = vmap();
		for (Element element : elements) 
			details.put(element.text().trim(), element.parent().nextSibling().toString());
		println(details);
		jsonTest(details);
	}
	
	Document getJsoup(String urlString) throws Exception {
		InputStream is = new URL(urlString).openStream();
		ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(null, urlString, is);
		return Jsoup.parse(pmis, "UTF-8", "", Parser.htmlParser());
//    	Connection connection = Jsoup.connect(url);
//		return connection
//    			.timeout(10000)
//    			.get();
	}
/*
	D: observation day.
	h: UTC observation time.
	T: air temperature (ºC).
	RH: air relative humidity (%).
	P/Gh: sea level pressure (hpa) or geopotential height (m).
	WI: wind direction and speed (km/h).
	CC: total cloud cover(eighths).
	LC: cover and type of low clouds.
	MC: cover and type of middle clouds.
	HC: type of high clouds.
	PR: amount of precipitation and measuring period.
	MT: maximun temperature.
	mT: minimum temperature.
	WE: weather.
*/
	String SUMMARY_URL = "http://www.mundomanz.com/meteo_p/byind?" +
			"countr=GERMANY&" +
			"ind=10519&" +
			"year=2012&" +
			"month=12&" +
			"day=31&" +
			"n_days=2&" +
			"trans=PA&" +
			"time=all&" +
			"l=1&action=display";

	String MONTHREP_URL = "http://www.mundomanz.com/meteo_p/monthrep?" +
			"countr=GERMANY&" +
			"ind=10519&" +
			"year=2012&" +
			"month=08&" +
			"l=1&action=display";

    public void testMonthrep_1() throws Exception {
    	Document doc = getJsoup(MONTHREP_URL);
    	
    	Elements tables = doc.select("table:contains(Daily extreme temperatures)");
    	Element table = null, el = null;
    	int length = Integer.MAX_VALUE;
    	for (Element t : tables)
    		if (length > t.html().length()) {
    			length = t.html().length();
    			el = t;
    		}
    	if (el != null) {
    		while (el != el.lastElementSibling()) {
    			el = el.nextElementSibling();
    			tables = el.select("table");
				if (tables.size() > 0) {
					table = tables.first();
    				break;
    			}
    		}
    	}
    	
		println(table);
	}

    public void testMonthrep_2() throws Exception {
     	String fragment =
    		"<table align=center>" +
    		"    	<tr align='center' bgcolor='#cccccc'>    	<td>Day</td>    	<td>1</td>    	<td>2</td>    	<td>3</td>    	<td>4</td>    	<td>5</td>    	<td>6</td>    	<td>7</td>    	<td>8</td>    	<td>9</td>    	<td>10</td>    	<td>11</td>    	<td>12</td>    	<td>13</td>    	<td>14</td>    	<td>15</td>    	</tr>" +
    		"    	<tr align='center' bgcolor='#dddddd'>    	<td bgcolor='#cccccc'>Max.</td>    	<td>28.4</td>    	<td>25.6</td>    	<td>23.7</td>    	<td>25.1</td>    	<td>24.7</td>    	<td>21.5</td>    	<td>19.9</td>    	<td>21.0</td>    	<td>22.5</td>    	<td>21.0</td>    	<td>22.3</td>    	<td>23.8</td>    	<td>26.4</td>    	<td>26.8</td>    	<td>30.4</td>    	</tr>" +
    		"    	<tr align='center' bgcolor='#dddddd'>    	<td bgcolor='#cccccc'>Min.</td>    	<td>11.5</td>    	<td>20.1</td>    	<td>14.4</td>    	<td>14.6</td>    	<td>14.8</td>    	<td>14.5</td>    	<td>13.2</td>    	<td>11.6</td>    	<td>14.2</td>    	<td>9.9</td>    	<td>8.5</td>    	<td>10.5</td>    	<td>12.9</td>    	<td>14.2</td>    	<td>16.3</td>    	</tr>" +
    		"    	</table>";
    	
     	Document doc = Jsoup.parseBodyFragment(fragment);
    	Elements column = doc.select("td:eq(5)");
    	for (Element cell : column)
    		println(cell.html());
	}

    @SuppressWarnings("unused")
	public void testJsoup() throws Exception {
    	org.jsoup.nodes.Document doc;
//    	doc = Jsoup.connect("http://en.wikipedia.org/wiki/Main_Page").get();
//    	org.jsoup.select.Elements newsHeadlines = doc.select("#mp-itn b a");
//    	for (int i = 0; i < newsHeadlines.size(); i++) {
//    		org.jsoup.nodes.Element headline = newsHeadlines.get(i);
//			System.out.println(headline.text());
//		}
    	
//    	doc = Jsoup.connect("http://espn.go.com/mens-college-basketball/conferences/standings/_/id/2/year/2012/acc-conference").get();
//        for (org.jsoup.nodes.Element table : doc.select("table.tablehead")) {
//            for (org.jsoup.nodes.Element row : table.select("tr")) {
//            	org.jsoup.select.Elements tds = row.select("td");
//                if (tds.size() > 6) {
//					System.out.println(tds.get(0).text() + ":" + tds.get(1).text());
//                }
//            }
//        }
    	
        String fragment =
    			"<div id='div1'>" +
    					"<p id='para1'>This is the first paragraph</p>" +
    					"<p id='para2'>Second paragraph here!" +
    					"</div>";
    	/*org.jsoup.nodes.Document */doc = Jsoup.parseBodyFragment(fragment);
//    	System.out.println(doc.toString());
    	doc.select("p").last().after("<p id='para3'>Third paragraph I just added</p>");
//    	System.out.println(doc.body().children().toString());
//    	System.out.println(doc.select("#para1").toString());
    	org.jsoup.select.Elements elements = doc.select("p");
//    	System.out.println(elements.toString());
    	elements = doc.select("#para1").remove();
//    	System.out.println(doc.body().children().toString());
//    	System.out.println("---------------------------------");
//    	System.out.println(elements.toString());
//    	System.out.println(Jsoup.clean(fragment, org.jsoup.safety.Whitelist.basic()));
    	org.jsoup.safety.Whitelist myWhitelist = new org.jsoup.safety.Whitelist();
    	myWhitelist.addTags("div", "p");
    	myWhitelist.addAttributes("div", "class");
    	myWhitelist.addAttributes("p", "id");
//    	System.out.println(Jsoup.clean(fragment, myWhitelist));
    	
//    	URL url = new URL("http://gosmarter.net?query=cars");
//    	doc = Jsoup.parse(url, 3000);
//    	Iterator<Element> productList = doc.select("div[class=productList]").iterator();
//    	assertTrue(productList.hasNext());
//    	Element product = productList.next();
//		Element productLink = product.select("a").first();
//    	String href = productLink.attr("abs:href");
//    	System.out.println(href);
    }

/*    public void testJTidy() throws Exception {
    	org.w3c.dom.Document doc = getDOM();
    	org.w3c.dom.NodeList a_tags = doc.getElementsByTagName("a");
    	for (int i = 0; i < a_tags.getLength(); i++) {
    		org.w3c.dom.Node node = a_tags.item(i);
    		org.w3c.dom.NamedNodeMap attributes = node.getAttributes();
    		org.w3c.dom.Node attribute = attributes.getNamedItem("name");
			if (attribute != null)
				System.out.println(attribute.getNodeValue());
    	}
    }

    org.w3c.dom.Document getDOM() {
		InputStream is = null;
    	try {
			org.w3c.tidy.Tidy tidy = new org.w3c.tidy.Tidy();
			is = new URL(SYNOP_URL).openStream();
			return tidy.parseDOM(is, System.out);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    	finally {
    		if (is != null)
				try {
					is.close();
				} catch (IOException e) {}
    	}
	}
*/
    javax.swing.text.html.HTMLEditorKit kit = new javax.swing.text.html.HTMLEditorKit();
	
    javax.swing.text.html.HTMLDocument getDoc(String spec) {
        try {
			URL url = new URL( spec ); 
			javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) kit.createDefaultDocument(); 
			doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
			Reader reader = new InputStreamReader(url.openConnection().getInputStream()); 
			kit.read(reader, doc, 0);
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}
    
    String getString(javax.swing.text.html.HTMLDocument doc) {
        try {
			StringWriter writer = new StringWriter();
			kit.write(writer, doc, 0, doc.getLength());
			return (writer.toString());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }

    public void testHTMLParser() throws Exception {
    	javax.swing.text.html.HTMLDocument doc = getDoc(MONTHREP_URL);
		contentsToFile(new File(tempPath() + "/synop.html"), getString(doc));
    	
		javax.swing.text.ElementIterator it = new javax.swing.text.ElementIterator(doc); 
		javax.swing.text.Element elem; 
        
        while( (elem = it.next()) != null  ) { 
        	String name = elem.getName();
			System.out.println(name);
	        if( name.equals("p-implied") || name.equals("content") ) { 
		        System.out.println (elem);
	        } 
        }
	}

	public void testOpenWeather() {
        try
        {
        	int days = 100;	//	Util.daysToTodayFrom(2012, 40, 2);
    	    String url = String.format(
    	    		"http://api.openweathermap.org/data/2.1/history/station/4885?cnt=%d&type=hour", 
    	    		days + 1);
            
    	    String jsonText = readFromUrlWithProgress(url, "UTF-8");
//    		println(jsonText);
    	    
			Object openWeather = walkJSON(null, new JSONObject(jsonText), new Function<Object>() {
				public Object apply(Object...params) {
					Object[] path = param(null, 0, params);
					Object value = param(null, 1, params);
					println("%s : %s", Arrays.toString(path), value);
					return value;
				}
			});
			jsonTest(openWeather);

//			final JSONParser parser = new JSONParser(new URL(url).openStream());
//			JSONValue val = parser.nextValue();
//			Util.contentsToFile(
//				new File("/home/lotharla/work/Niklas/openweather/2012-W32-1-weather-4885-station-2012-W49-4.json"), 
//				val.render(true));
        }
        catch (Exception e) {
        	e.printStackTrace();
        	fail(e.getMessage());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	void jsonTest(Object object) throws Exception {
		File file = new File(tempPath() + "/test.json");

		JSONStringer jsonWriter = new JSONStringer();
		toJSON(null, jsonWriter, "", object, null);
		contentsToFile(file, jsonWriter.toString());
		
		assertTrue(file.length() > 10);
		
		Object o = walkJSON(null, new JSONObject(contentsFromFile(file)), new Function() {
			public Object apply(Object... params) {
				Object[] path = param(null, 0, params);
				Object value = param(null, 1, params);
				
				assertTrue(isAvailable(2, params));
				assertTrue(params[2] instanceof Object[]);
				Object[] p = param(null, 2, params);
				Object o = param(null, 0, p);
				assertNotNull(o);
				assertTrue(o instanceof ValMap);
				
				assertEquals(member(path, o), value);
				return value;
			}
		}, object);
		assertTrue(o instanceof ValMap);
		assertEquals(object.toString(), o.toString());
	}

	Object member(Object[] path, Object object) {
		for (int i = 0; i < path.length; i++) {
			if (path[i] instanceof Integer) 
				object = ((ValList)object).get((Integer) path[i]);
			else
				object = ((ValMap)object).get(path[i].toString());
		}
		return object;
	}

	public void testBookmarks() throws Exception {
		Document doc = getJsoup(fileUri("/home/lotharla/backups/bookmarks-2014-02-24.html", null).toString());
    	Elements folders = doc.select("DL");
    	for (Element folder : folders) {
			String html = folder.outerHtml();
			no_println(html);
		}
	}

}
