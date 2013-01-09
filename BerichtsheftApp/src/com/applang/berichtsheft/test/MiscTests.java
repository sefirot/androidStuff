package com.applang.berichtsheft.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import junit.framework.*;

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
		long millis = Util.dateInMillis(year, weekInYear, dayInWeek);
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
		dateString = Util.formatDate(new Date().getTime(), DatePicker.weekFormat);
		weekDate = DatePicker.parseWeekDate(dateString);
		assertTrue(weekDate[1] > 2000);
	}

	String dbName = Util.relativePath("databases/note_pad_2012-08.db");
	NotePicker np = new NotePicker(null);
	long[] interval;

	void setupNotePicking() throws ParseException {
		assertTrue(np.openConnection(dbName));
		interval = DatePicker.weekInterval("33/12", 1);
		assertEquals(2, interval.length);
	}

	public void testNotePicking() throws Exception {
		setupNotePicking();

		PreparedStatement ps = np.getCon().prepareStatement("SELECT _id FROM notes where created between ? and ?");
		ps.setLong(1, interval[0]);
		ps.setLong(2, interval[1]);
		assertEquals(11, np.registerNotes(ps.executeQuery()));
		
		np.setTitle("Bericht");
		String pattern = np.getPattern();
		assertFalse("Bericht".equals(pattern));
		assertTrue(Util.matches("Bericht", pattern));
		
		ps = np.getCon().prepareStatement("SELECT _id FROM notes where title regexp ?");
		ps.setString(1, pattern);
		assertEquals(17, np.registerNotes(ps.executeQuery()));
		
		ps = np.getCon().prepareStatement("SELECT _id FROM notes where created between ? and ? and title regexp ?");
		ps.setLong(1, interval[0]);
		ps.setLong(2, interval[1]);
		ps.setString(3, pattern);
		assertEquals(5, np.registerNotes(ps.executeQuery()));
		
		np.setTitle("Bemerk");
		ps.setString(3, np.getPattern());
		assertEquals(4, np.registerNotes(ps.executeQuery()));
	}

	public void testNoteWrapping() throws Exception {
		setupNotePicking();

		PreparedStatement ps = np.preparePicking(np.getPattern(), interval);
		assertEquals(11, np.registerNotes(ps.executeQuery()));
		
		String text = np.all();
		assertTrue(np.isWrapped(text));
		String[][] notes = np.getRecords(text);
		for (int i = 0; i < notes.length; i++) {
			for (int j = 0; j < notes[i].length; j++) 
				if (j == 0)
					assertEquals(np.formatDate((long)np.records[i][j]), notes[i][j]);
				else
					assertEquals(np.records[i][j], notes[i][j]);
		}
		
		text = np.next();
		assertFalse(np.isWrapped(text));
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
        	int days = Util.daysToTodayFrom(2012, 40, 2);
    	    String url = String.format(
    	    		"http://openweathermap.org/data/history?id=4885&cnt=%d&type=day", 
    	    		days + 1);
		    
    	    long dt = Util.dateFromTodayInMillis(-days) / 1000;
            System.out.println(dt);
            
//			JSONTokener tokener = new JSONTokener(reader);
            
//			JSONObject json = JsonUtil.readFromUrl(url);
//		    System.out.println(json.toString());
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
