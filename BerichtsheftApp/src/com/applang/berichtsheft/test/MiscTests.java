package com.applang.berichtsheft.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONTokener;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

//import antlr.RecognitionException;
//import antlr.TokenStreamException;

import com.applang.JsonUtil;
import com.applang.Util;
import com.applang.ZipUtil;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;
import com.applang.berichtsheft.ui.components.DatePicker;
import com.applang.berichtsheft.ui.components.NotePicker;

//import com.sdicons.json.model.JSONValue;
//import com.sdicons.json.parser.JSONParser;

public class MiscTests extends XMLTestCase
{
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		if (tempfile.exists())
			tempfile.delete();
	}

	File tempfile = new File("/tmp/temp.xml");
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testDateTime() throws Exception {
		int year = 2012;
		int weekInYear = 53;
		int dayInWeek = 2;
		Long millis = Util.dateInMillis(year, weekInYear, dayInWeek);
		assertEquals(Util.dateInMillis(2012, -Calendar.DECEMBER, 31), millis);
		millis = Util.dateInMillis(2013, -Calendar.JANUARY, 1);
		String kalenderWoche = String.format("%d/%d", weekInYear % 52, (year + 1) % 100);
		assertEquals(kalenderWoche, Util.formatDate(millis, DatePicker.weekFormat));
		
		long[] week = DatePicker.weekInterval("53/12", 1);
		assertEquals("2/13", Util.formatDate(week[1], DatePicker.weekFormat));
		String dateString = "1/13";
		assertEquals(dateString, DatePicker.weekDate(week));
		week = DatePicker.nextWeekInterval(dateString);
		dateString = Util.formatDate(week[0], DatePicker.weekFormat);
		assertEquals("2/13", dateString);
		week = DatePicker.previousWeekInterval(dateString);
		assertEquals("1/13", DatePicker.weekDate(week));
		
		millis = Util.dateInMillis(1954, -Calendar.JANUARY, 27);
		dateString = Util.formatDate(millis, DatePicker.weekFormat);
		assertFalse(DatePicker.isCalendarDate(dateString));
		assertTrue(DatePicker.isWeekDate(dateString));
		int[] weekDate = DatePicker.parseWeekDate(dateString);
		assertEquals(5, weekDate[0]);
		assertEquals(1954, weekDate[1]);
		dateString = Util.formatDate(Util.now(), DatePicker.weekFormat);
		weekDate = DatePicker.parseWeekDate(dateString);
		assertTrue(weekDate[1] > 2000);
		
		assertThat(Util.parseDate("", DatePicker.dateFormat), is(equalTo(null)));
	}

	NotePicker np = new NotePicker(null);
	String note_pad = Util.relativePath("databases/note_pad_2012-08.db");
	long[] interval;

	void setupNotesDb(String db, Object... params) throws ParseException {
		assertTrue("openConnection failed", np.openConnection(db, params));
		interval = DatePicker.weekInterval("33/12", 1);
		assertEquals(2, interval.length);
	}
	
	int test_data(boolean empty, 
			Pattern expat, int grp, 
			int[][] dates, String[] categories, 
			Integer... params) throws Exception 
	{
		Integer start = Util.param(0, 0, params);
		Integer length = Util.param(dates != null ? dates.length : 0, 1, params);
		InputStream is = MiscTests.class.getResourceAsStream("Kein Fehler im System.txt");
		
		if (empty)
			np.delete(NotePicker.allCategories, NotePicker.allDates);
		
		MatchResult[] excerpts = Util.excerptsFrom(is, expat);
		int cnt = 0;
		long time = Util.dateFromTodayInMillis(0);
		for (int i = start; i < excerpts.length; i++) {
			MatchResult m = excerpts[i];
			int j = i - start;
			if (Util.isAvailable(j, categories))
				np.setCategory(categories[j]);
			time = dates != null && j < dates.length ?
					Util.dateInMillis(dates[j][0], dates[j][1], dates[j][2]) : 
					Util.dateFromTodayInMillis(1, new Date(time));
			String dateString = np.formatDate(time);
			String note = m.group(grp);
			if (empty)
				assertThat(
						np.insert(1+i-start, note, np.getCategory(), time), 
						is(greaterThan(-1)));
			else
				assertThat(
						np.updateOrInsert(np.getPattern(), dateString, note), 
						is(greaterThan(-1L)));
			cnt++;
			if (j >= length - 1)
				break;
		}
		return cnt;
	}

	int[][] dates = new int[][] {
			{2012, 52, Calendar.SUNDAY}, 
			{2012, 52, Calendar.SUNDAY}, 
			{2013, 1, Calendar.SUNDAY}, 
	};
	
	String[] categories = new String[] {
			"Bemerkung", 
			"Bericht", 
			"Bericht", 
	};
	
	public void testData() throws Exception {
		assertTrue(new File(test_db).delete());
		assertTrue(np.openConnection(test_db));
		
		Pattern expat = Pattern.compile("(?s)\\n([^\\{\\}]+?)(?=\\n)");
		
		int cnt = test_data(true, expat, 1, dates, categories, 2);
		assertEquals(dates.length, cnt);
		
		try {
			assertThat(
					np.insert(4, "", np.getCategory(), Util.dateInMillis(dates[2][0], dates[2][1], dates[2][2])), 
					is(greaterThan(-1)));
			fail("expected to fail on UNIQUE constraint in the notes table");
		} catch (Exception e) {}
	}

	String test_db = "/tmp/test.db";
	
	public void testSystemData() throws Exception {
		assertTrue(new File(test_db).delete());
		assertTrue(np.openConnection(test_db));
		
		int[][] dates = new int[][] {{2012, -Calendar.DECEMBER, 24}};
		Pattern expat = Pattern.compile("(?s)\\n([^\\{\\}]+?)(?=\\n)");
		
		int length = 19;
		assertThat(test_data(true, expat, 1, 
				dates, 
				new String[] {"1."}, 
				2, length), is(equalTo(length)));
		length = 7;
		assertThat(test_data(false, expat, 1, 
				dates, 
				new String[] {"2."}, 
				21, length), is(equalTo(length)));
		length = 10;
		assertThat(test_data(false, expat, 1, 
				dates, 
				new String[] {"3."}, 
				28, length), is(equalTo(length)));
		
		assertThat(np.keyLine(NotePicker.allCategories).length, is(equalTo(36)));
	}
	
	public void testMysql() throws Exception {
//		BerichtsheftTextArea textArea = new BerichtsheftTextArea();
//		np = new NotePicker(textArea);
		setupNotesDb("//localhost/note_pad?user=lotharla&password=gnalppA", 
				"mysql", 
				"com.mysql.jdbc.Driver", 
				"note_pad");
		
		int cnt = test_data(true, NotePicker.notePattern1, 2, dates, null);
		
		PreparedStatement ps = np.getCon().prepareStatement("select _id from notes");
		assertThat(np.registerNotes(ps.executeQuery()), is(equalTo(cnt)));
		
		try {
			assertThat(
					np.insert(4, "", np.getCategory(), Util.dateInMillis(dates[2][0], dates[2][1], dates[2][2])), 
					is(greaterThan(-1)));
			fail("expected to fail on UNIQUE constraint in the notes table");
		} catch (Exception e) {}
	}

	public void testKeyLine() throws Exception {
		testData();
		
		String[] keys = np.keyLine();
		assertThat(Arrays.asList(keys).toString(), keys.length, is(equalTo(dates.length)));
		
		int i = 1;
		long time = Util.dateInMillis(dates[i][0], dates[i][1], dates[i][2]);
		np.setPattern(categories[i]);
		
		assertThat(np.previousNoteAvailable(time),	is(true));
		assertThat(np.noteAvailable(time - 1),		is(false));
		assertThat(np.noteAvailable(time),			is(true));
		assertThat(np.noteAvailable(time + 1),		is(false));
		assertThat(np.nextNoteAvailable(time),		is(true));
		
		long after = Util.dateInMillis(dates[2][0], dates[2][1], dates[2][2]);
		long before = Util.dateInMillis(dates[0][0], dates[0][1], dates[0][2]);
		assertThat(np.find(true, new long[]{time}),	is(equalTo(np.keyValue(after, categories[2]))));
		assertThat(np.find(false, new long[]{time}), is(equalTo(np.keyValue(before, categories[0]))));
		
		interval = DatePicker.weekInterval("52/12", 1);
		time = np.timeFromKey(np.find(true, interval));
		assertThat(np.formatWeek(time), is(equalTo("1/13")));
		
		np.setPattern("Bemerkung");
		assertThat(np.previousNoteAvailable(interval[0]),	is(false));
		assertThat(np.noteAvailable(interval[0]),			is(true));
		assertThat(np.nextNoteAvailable(interval[0]),		is(true));
		
		np.setPattern("Bericht");
		assertThat(np.previousNoteAvailable(interval[0]),	is(true));
		assertThat(np.noteAvailable(interval[0]),			is(true));
		assertThat(np.nextNoteAvailable(interval[0]),		is(true));
		
		np.setPattern("Bemerkung");
		assertThat(np.previousNoteAvailable(interval[1]),	is(true));
		assertThat(np.noteAvailable(interval[1]),			is(false));
		assertThat(np.nextNoteAvailable(interval[1]),		is(false));
		
		np.setPattern("Bericht");
		assertThat(np.previousNoteAvailable(interval[1]),	is(true));
		assertThat(np.noteAvailable(interval[1]),			is(true));
		assertThat(np.nextNoteAvailable(interval[1]),		is(false));
		
		np.setPattern(NotePicker.allCategories);
		assertThat(np.noteAvailable(interval[0]),			is(true));
		assertThat(np.previousNoteAvailable(interval),	is(false));
		assertThat(np.noteAvailable(interval),			is(true));
		assertThat(np.nextNoteAvailable(interval),		is(true));
		
		interval = DatePicker.weekInterval("1/13", 1);
		time = np.timeFromKey(np.find(false, interval));
		assertThat(np.formatWeek(time), is(equalTo("52/12")));
		
		assertThat(np.previousNoteAvailable(interval),	is(true));
		assertThat(np.noteAvailable(interval),			is(true));
		assertThat(np.nextNoteAvailable(interval),		is(false));
	}

	public void testNotePicking() throws Exception {
		setupNotesDb(note_pad);

		PreparedStatement ps = np.getCon().prepareStatement("SELECT _id FROM notes where created between ? and ?");
		ps.setLong(1, interval[0]);
		ps.setLong(2, interval[1]);
		assertTrue(0 < np.registerNotes(ps.executeQuery()));
		
		np.setCategory("Bericht");
		String pattern = np.getPattern();
		assertTrue(Util.matches("Bericht", pattern));
		
		ps = np.getCon().prepareStatement("SELECT _id FROM notes where title regexp ?");
		ps.setString(1, pattern);
		assertEquals(17, np.registerNotes(ps.executeQuery()));
		
		ps = np.getCon().prepareStatement("SELECT _id FROM notes where created between ? and ? and title regexp ?");
		ps.setLong(1, interval[0]);
		ps.setLong(2, interval[1]);
		ps.setString(3, pattern);
		assertEquals(5, np.registerNotes(ps.executeQuery()));
		
		np.setCategory("Bemerk");
		ps.setString(3, np.getPattern());
		assertTrue(4 <= np.registerNotes(ps.executeQuery()));
	}

	public void testNoteWrapping() throws Exception {
		setupNotesDb(note_pad);
		
		String text = np.wrapNote("foo", "bar");
		assertEquals("{{{ foo\nbar\n}}}", text);
		MatchResult m = Util.findFirstIn(text, NotePicker.notePattern1);
		assertEquals(2, m.groupCount());
		assertEquals("foo", m.group(1));
		assertEquals("bar", m.group(2));
		text = np.wrapNote(Util.now(), "", "");
		m = Util.findFirstIn(text, NotePicker.notePattern2);
		assertEquals(3, m.groupCount());
		assertEquals("", m.group(2));
		assertEquals("", m.group(3));

		PreparedStatement ps = np.preparePicking(false, np.getPattern(), interval);
		assertEquals(11, np.registerNotes(ps.executeQuery()));
		
		text = np.all();
		assertTrue(np.isWrapped(text));
		String[][] notes = np.getRecords(text);
		for (int i = 0; i < notes.length; i++) {
			for (int j = 0; j < notes[i].length; j++) 
				if (j == 0)
					assertEquals(np.formatDate((Long)np.records[i][j]), notes[i][j]);
				else
					assertEquals(np.records[i][j], notes[i][j]);
		}
		
		np.pickNote(DatePicker.weekDate(interval), np.getPattern());
		assertFalse(np.isWrapped(np.getText()));
	}

	public void testRegexpInSqlite() throws Exception {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:/tmp/temp.db");
		Statement stat = conn.createStatement();
		stat.executeUpdate("DROP TABLE if exists try;");
		stat.executeUpdate("CREATE TABLE try (a TEXT);");
		stat.executeUpdate("insert into try (a) values ('foo');");
		stat.executeUpdate("insert into try (a) values ('bar');");
		stat.executeUpdate("insert into try (a) values ('bat');");
		stat.executeUpdate("insert into try (a) values ('woo');");
		stat.executeUpdate("insert into try (a) values ('oop');");
		stat.executeUpdate("insert into try (a) values ('craw');");
		stat.close();
		conn.close();
		
		String ctrlFileName = "/tmp/temp.xml";
		Util.contentsToFile(new File(ctrlFileName), 
				"<control>" +
					"<QUERY " + 
						"statement=\"SELECT a FROM try WHERE a REGEXP ?\" typeinfo=\"string\" />" + 
					"<debug>" + 
						"<place_value ><a><![CDATA[^b.*]]></a></place_value>" + 
					"</debug>" +
				"</control>");
		
		String styleSheet = "scripts/content.xsl";
		String outputFile = "/tmp/debug.out";
		Util.xmlTransform(ctrlFileName, styleSheet, outputFile, 
				"debug", "yes", 
				"dbfile", "/tmp/temp.db", 
				"inputfile", "/tmp/temp.xml"
		);
		System.out.println(Util.contentsFromFile(new File(outputFile)));
	}
	
	File contentfile = new File("scripts/content.xml");
	String paramsFilename = "scripts/params.xml";

	public void _testXMLFilters() throws Exception {
	    // Instantiate  a TransformerFactory.
	  	TransformerFactory tFactory = TransformerFactory.newInstance();
	    // Determine whether the TransformerFactory supports The use uf SAXSource 
	    // and SAXResult
	    if (tFactory.getFeature(SAXSource.FEATURE) && tFactory.getFeature(SAXResult.FEATURE))
	    { 
		    // Cast the TransformerFactory to SAXTransformerFactory.
		    SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);
		  	// Create an XMLFilter for each stylesheet.
		    XMLFilter xmlFilter1 = saxTFactory.newXMLFilter(new StreamSource("/home/lotharla/xalan-j_2_7_1/samples/UseXMLFilters/foo1.xsl"));
		    XMLFilter xmlFilter2 = saxTFactory.newXMLFilter(new StreamSource("/home/lotharla/xalan-j_2_7_1/samples/UseXMLFilters/foo2.xsl"));
		    XMLFilter xmlFilter3 = saxTFactory.newXMLFilter(new StreamSource("/home/lotharla/xalan-j_2_7_1/samples/UseXMLFilters/foo3.xsl"));
		    
		    // Create an XMLReader.
			XMLReader reader = XMLReaderFactory.createXMLReader();
		    
		    // xmlFilter1 uses the XMLReader as its reader.
		    xmlFilter1.setParent(reader);
		    
		    // xmlFilter2 uses xmlFilter1 as its reader.
		    xmlFilter2.setParent(xmlFilter1);
		    
		    // xmlFilter3 uses xmlFilter2 as its reader.
		    xmlFilter3.setParent(xmlFilter2);
		    
		    // xmlFilter3 outputs SAX events to the serializer.
		    java.util.Properties xmlProps = OutputPropertiesFactory.getDefaultMethodProperties("xml");
		    xmlProps.setProperty("indent", "no");
		    xmlProps.setProperty("standalone", "no"); 
		    Serializer serializer = SerializerFactory.getSerializer(xmlProps);                      
		    serializer.setOutputStream(System.out);
		    xmlFilter3.setContentHandler(serializer.asContentHandler());

		    xmlFilter3.parse(new InputSource("/home/lotharla/xalan-j_2_7_1/samples/UseXMLFilters/foo.xml"));
	    }
	}
	
	public void testPiping() throws Exception {
		// Instantiate  a TransformerFactory.
	  	TransformerFactory tFactory = TransformerFactory.newInstance();
	    // Determine whether the TransformerFactory supports The use uf SAXSource 
	    // and SAXResult
	    if (tFactory.getFeature(SAXSource.FEATURE) && tFactory.getFeature(SAXResult.FEATURE))
	    { 
			// Cast the TransformerFactory to SAXTransformerFactory.
			SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);	  
			// Create a TransformerHandler for each stylesheet.
			TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource("scripts/control.xsl"));
			TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource("scripts/content.xsl"));
			tHandler2.getTransformer().setParameter("inputfile", "content.xml");
//			TransformerHandler tHandler3 = saxTFactory.newTransformerHandler(new StreamSource("/tmp/foo3.xsl"));
			
			// Create an XMLReader.
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(tHandler1);
			reader.setProperty("http://xml.org/sax/properties/lexical-handler", tHandler1);
			
			tHandler1.setResult(new SAXResult(tHandler2));
//			tHandler2.setResult(new SAXResult(tHandler3));
			
			// transformer3 outputs SAX events to the serializer.
			java.util.Properties xmlProps = OutputPropertiesFactory.getDefaultMethodProperties("xml");
			xmlProps.setProperty("indent", "no");
			xmlProps.setProperty("standalone", "no");
			Serializer serializer = SerializerFactory.getSerializer(xmlProps);
			OutputStream out = new FileOutputStream(tempfile);
			serializer.setOutputStream(out);
			tHandler2.setResult(new SAXResult(serializer.asContentHandler()));
			
			// Parse the XML input document. The input ContentHandler and output ContentHandler
			// work in separate threads to optimize performance.   
			reader.parse(paramsFilename);
	    }
	    
		compare_input_output(contentfile.getPath(), tempfile.getPath());
	}

	void compare_input_output(String inputFilename, String outputFilename) throws Exception {
		assertTrue(String.format("'%s' doesn't exist", inputFilename), new File(inputFilename).exists());
		assertTrue(String.format("'%s' doesn't exist", outputFilename), new File(outputFilename).exists());
		Diff diff = compareXML(new FileReader(inputFilename), new FileReader(outputFilename));
		if (!diff.similar()) {
	        DetailedDiff detailedDiff = new DetailedDiff(diff);
			List<?> allDifferences = detailedDiff.getAllDifferences();
			for (Object detail : allDifferences) {
				Difference df = (Difference) detail;
				System.out.println(df.getDescription());
			}
		}
	}

	public void testContent() throws Exception {
    	File tempDir = Util.tempDir("berichtsheft");
    	
		File source = new File("Vorlagen/Tagesberichte.odt");
		assertTrue(source.exists());
		File archive = new File(tempDir, "Vorlage.zip");
		Util.copyFile(source, archive);
		assertTrue(archive.exists());
    	int unzipped = ZipUtil.unzipArchive(archive, 
    			new ZipUtil.UnzipJob(tempDir.getPath()), 
    			false);
    	assertTrue(archive.delete());

    	String content = Util.pathCombine(tempDir.getPath(), "content.xml");
		String _content = Util.pathCombine(tempDir.getPath(), "_content.xml");
		assertTrue(new File(content).renameTo(new File(_content)));
		
		assertTrue(BerichtsheftApp.pipe(_content, content, new FileReader(paramsFilename)));
//		Util.xmlTransform("scripts/control.xml", "scripts/content.xsl", content, "inputfile", _content);
		
		compare_input_output(_content, content);
		assertTrue(new File(_content).delete());
		
		File destination = new File("Dokumente/Tagesberichte.odt");
		if (destination.exists())
			destination.delete();
		int zipped = ZipUtil.zipArchive(destination, 
				tempDir.getPath(), 
				tempDir.getPath());
		assertTrue(destination.exists());
		assertEquals(unzipped, zipped);
		
		int updated = ZipUtil.updateArchive(destination, 
    			tempDir.getPath(), 
    			1, 
    			content, content);
		assertEquals(unzipped, updated);
		
		assertTrue(Util.deleteDirectory(tempDir));
	}

	public void testMerge() {
		assertTrue(BerichtsheftApp.merge(
				"Vorlagen/Tagesberichte.odt", 
				"Dokumente/Tagesberichte.odt", 
				"/home/lotharla/work/Niklas/note_pad.db", 
				2012, 32));
	}

    public void testWeather() {
        try
        {
        	int days = 0;	//	Util.daysToTodayFrom(2012, 40, 2);
    	    String url = String.format(
    	    		"http://openweathermap.org/data/history?id=4885&cnt=%d&type=day", 
    	    		days + 1);
		    
    	    long dt = Util.dateFromTodayInMillis(-days) / 1000;
//			System.out.println(dt);
            
//			JSONTokener tokener = new JSONTokener(reader);
            
			JSONObject json = JsonUtil.readFromUrl(url);
//			System.out.println(json.toString());
			Iterator<String> it = json.keys();
			while (it.hasNext()) {
				String key = it.next();
				String value = json.getString(key);
				System.out.println(String.format("%s : %s", key, value));
			}
			JSONArray list = json.getJSONArray("list");
//		    System.out.println(list.toString(4));
		    for (int i = 0; i < list.length(); i++) {
				String value = list.getString(i);
				System.out.println(value);
			}
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

}
