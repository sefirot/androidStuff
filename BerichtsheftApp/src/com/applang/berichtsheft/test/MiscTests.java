package com.applang.berichtsheft.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

//import antlr.RecognitionException;
//import antlr.TokenStreamException;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.ZipUtil.*;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.components.DatePicker;
import com.applang.berichtsheft.components.FormEditor;
import com.applang.berichtsheft.components.NotePicker;
import com.applang.berichtsheft.components.NotePicker.NoteFinder;
import com.applang.berichtsheft.components.TextEditor;

//import com.sdicons.json.model.JSONValue;
//import com.sdicons.json.parser.JSONParser;

public class MiscTests extends XMLTestCase
{
	@Override
	public void setUp() throws Exception {
		super.setUp();
		underTest = true;
		BerichtsheftApp.loadSettings();
		textEditor = new TextEditor();
		np = new NotePicker(textEditor);
		if (tempfile.exists())
			tempfile.delete();
	}

	File tempfile = new File("/tmp/temp.xml");
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (np != null && np.getCon() != null)
			np.getCon().close();
		Settings.save();
		underTest = false;
	}

	TextEditor textEditor;
	NotePicker np;
	
	public void testDateTime() throws Exception {
		int year = 2012;
		int weekInYear = 53;
		int dayInWeek = 2;
		long millis = timeInMillis(year, weekInYear, dayInWeek);
		assertEquals(timeInMillis(2012, -Calendar.DECEMBER, 31), millis);
		millis = timeInMillis(2013, -Calendar.JANUARY, 1);
		String kalenderWoche = String.format("%d/%d", weekInYear % 52, (year + 1) % 100);
		assertEquals(kalenderWoche, formatDate(millis, DatePicker.weekFormat));
		
		long millisPerDay = getMillis(1);
		long sevenDaysAWeek = timeInMillis(2013, 1, 7);
		assertEquals(timeInMillis(2013, 1, 6) + millisPerDay, sevenDaysAWeek);
		long eightDaysAWeek = timeInMillis(2013, 1, 8);
		assertEquals(eightDaysAWeek - millisPerDay, sevenDaysAWeek);
		
		long[] week = DatePicker.weekInterval("53/12", 1);
		assertEquals("2/13", formatDate(week[1], DatePicker.weekFormat));
		String dateString = "1/13";
		assertEquals(dateString, DatePicker.weekDate(week));
		week = DatePicker.nextWeekInterval(dateString);
		dateString = formatDate(week[0], DatePicker.weekFormat);
		assertEquals("2/13", dateString);
		week = DatePicker.previousWeekInterval(dateString);
		assertEquals("1/13", DatePicker.weekDate(week));
		
		millis = timeInMillis(1954, -Calendar.JANUARY, 27);
		dateString = formatDate(millis, DatePicker.weekFormat);
		assertFalse(DatePicker.isCalendarDate(dateString));
		assertTrue(DatePicker.isWeekDate(dateString));
		int[] weekDate = DatePicker.parseWeekDate(dateString);
		assertEquals(5, weekDate[0]);
		assertEquals(1954, weekDate[1]);
		dateString = formatDate(now(), DatePicker.weekFormat);
		weekDate = DatePicker.parseWeekDate(dateString);
		assertTrue(weekDate[1] > 2000);
		
		assertEquals(null, toDate("", DatePicker.calendarFormat));
		
		println("absolute zero : %10.2f °C", absoluteZero);
		for (int i = Calendar.JANUARY; i <= Calendar.DECEMBER; i++) {
			print("%02d\t", i);
		}
	}
	
	public void testDatePicker() {
	    int[] dateParts = null;
		do {
			dateParts = DatePicker.pickAPeriod(dateParts, "pick day, week or month");
			if (dateParts == null)
				return;
			
			println(dateParts);
		} while (true);
	}

	long[] interval;

	void setupNotes(String db, Object... params) throws ParseException {
		assertTrue("openConnection failed", np.openConnection(db, params));
		interval = DatePicker.weekInterval("33/12", 1);
		assertEquals(2, interval.length);
	}
	
	public void testAllNotes() throws Exception {
//		assertTrue(np.openConnection("databases/berichtsheft.db"));
		assertTrue(np.openConnection("/home/lotharla/Downloads/_note_pad1.db"));
//		assertTrue(np.openConnection("/home/lotharla/work/Niklas/note_pad_april 5-03-2013.db"));
		long[] time = new long[]{
				dateInMillis(2013, 0, 1),
				dateInMillis(2014, 0, 1),
		};
		PreparedStatement ps = np.preparePicking(true, NotePicker.allCategories, time);
		ResultSet rs = ps.executeQuery();
		np.registerNotes(rs);
		String text = np.all();
//		System.out.println(text);
		contentsToFile(new File("/tmp/notes.txt"), text);
	}
	
	int generateData(boolean empty, 
			Pattern expat, int grp, 
			int[][] dates, String[] categories, 
			Integer... params) throws Exception 
	{
		InputStream is = MiscTests.class.getResourceAsStream("Kein Fehler im System.txt");
		MatchResult[] excerpts = excerptsFrom(is, expat);
		
		Integer start = param(0, 0, params);
		Integer length = param(dates != null ? dates.length : 0, 1, params);
		
		if (empty) {
			int cnt = np.delete(NotePicker.allCategories, NotePicker.allDates, false);
			assertEquals(cnt, np.delete(NotePicker.allCategories, NotePicker.allDates, true));
		}
		
		int cnt = 0;
		long time = dateFromTodayInMillis(0);
		for (int i = start; i < excerpts.length; i++) {
			MatchResult m = excerpts[i];
			int j = i - start;
			if (isAvailable(j, categories))
				np.setCategory(categories[j]);
			time = dates != null && j < dates.length ?
					timeInMillis(dates[j][0], dates[j][1], dates[j][2]) : 
					dateFromTodayInMillis(1, new Date(time), true);
			String dateString = np.formatDate(1, time);
			String note = m.group(grp);
			if (empty)
				assertThat(
						np.insert(1+i-start, note, np.getCategory(), time), 
						is(greaterThan(-1)));
			else
				assertThat(
						np.updateOrInsert(np.getPattern(), dateString, note, true), 
						is(greaterThan(-1L)));
			cnt++;
			if (j >= length - 1)
				break;
		}
		return cnt;
	}
	
	public void testData() throws Exception {
		new File(test_db).delete();
		assertTrue(np.openConnection(test_db));
		
		int cnt = generateData(true, expat, 1, dates, categories, 2);
		assertEquals(dates.length, cnt);
	}

	String test_db = "/tmp/test.db";
	Pattern expat = Pattern.compile("(?s)\\n([^\\{\\}]+?)(?=\\n)");
	
	public void testKeinFehler() throws Exception {
		assertTrue(np.openConnection(test_db));
		
		int[][] dates = new int[][] {{2012, -Calendar.DECEMBER, 24}};
		
		int length = 19;
		assertEquals(length, 
				generateData(true, expat, 1, 
				dates, 
				strings("1."), 
				2, length));
		length = 7;
		assertEquals(length, 
				generateData(false, expat, 1, 
				dates, 
				strings("2."), 
				21, length));
		length = 10;
		assertEquals(length, 
				generateData(false, expat, 1, 
				dates, 
				strings("3."), 
				28, length));
		
		assertEquals(36, np.finder.keyLine(NotePicker.allCategories).length);
	}
	
	public void _testMySql() throws Exception {
		setupNotes("//localhost/note_pad?user=lotharla&password=gnalppA", 
				"mysql", 
				"com.mysql.jdbc.Driver", 
				"note_pad");
		
		int cnt = generateData(true, NotePicker.notePattern1, 2, dates, categories);
		
		PreparedStatement ps = np.getCon().prepareStatement("select _id from notes");
		assertEquals(cnt, np.registerNotes(ps.executeQuery()));
		
		try {
			assertThat(
					np.insert(4, "", np.getCategory(), timeInMillis(dates[2][0], dates[2][1], dates[2][2])), 
					is(greaterThan(-1)));
			fail("expected to fail on UNIQUE constraint in the notes table");
		} catch (Exception e) {}
	}

	int[][] dates = new int[][] {
			{2012, 52, Calendar.SUNDAY}, 
			{2012, 52, Calendar.SUNDAY}, 
			{2013, 1, Calendar.SUNDAY}, 
	};
	String[] categories = strings(
			"Bemerkung", 
			"Bericht", 
			"Bericht" 
	);

	public void testNoteFinding() throws Exception {
		testData();
		
		NoteFinder finder = np.finder;
		String[] keys = finder.keyLine(NotePicker.allCategories);
		assertEquals(asList(keys).toString(), dates.length, keys.length);
		
		np.setPattern(categories[1]);
		long epoch = timeInMillis(dates[1][0], dates[1][1], dates[1][2]);
		
		assertTrue(finder.previousBunchAvailable(epoch));
		assertFalse(finder.bunchAvailable(epoch - 1));
		assertTrue(finder.bunchAvailable(epoch));
		assertFalse(finder.bunchAvailable(epoch + 1));
		assertTrue(finder.nextBunchAvailable(epoch));
		
		long after = timeInMillis(dates[2][0], dates[2][1], dates[2][2]);
		long before = timeInMillis(dates[0][0], dates[0][1], dates[0][2]);
		np.setPattern("Berich");
		assertEquals(finder.keyValue(epoch, categories[1]), finder.find(NotePicker.Direction.HERE, epoch));
		np.setPattern("Bericht_");
		assertEquals(finder.keyValue(after, categories[2]), finder.find(NotePicker.Direction.HERE, epoch));
		np.setPattern("xxx");
		assertEquals(finder.keyValue(after, categories[2]), finder.find(NotePicker.Direction.HERE, after));
		np.setPattern("Bericht");
		assertEquals(finder.keyValue(epoch, categories[1]), finder.find(NotePicker.Direction.HERE, epoch));
		assertEquals(finder.keyValue(after, categories[2]), finder.find(NotePicker.Direction.NEXT, epoch));
		assertEquals(finder.keyValue(before, categories[0]), finder.find(NotePicker.Direction.PREV, epoch));
		
		interval = DatePicker.weekInterval("52/12", 1);
		epoch = finder.epochFromKey(finder.find(NotePicker.Direction.NEXT, interval));
		assertEquals("1/13", np.formatDate(2, epoch));
		
		np.setPattern("Bemerkung");
		assertFalse(finder.previousBunchAvailable(interval[0]));
		assertTrue(finder.bunchAvailable(interval[0]));
		assertTrue(finder.nextBunchAvailable(interval[0]));
		
		np.setPattern("Bericht");
		assertTrue(finder.previousBunchAvailable(interval[0]));
		assertTrue(finder.bunchAvailable(interval[0]));
		assertTrue(finder.nextBunchAvailable(interval[0]));
		
		np.setPattern("Bemerkung");
		assertTrue(finder.previousBunchAvailable(interval[1]));
		assertFalse(finder.bunchAvailable(interval[1]));
		assertFalse(finder.nextBunchAvailable(interval[1]));
		
		np.setPattern("Bericht");
		assertTrue(finder.previousBunchAvailable(interval[1]));
		assertTrue(finder.bunchAvailable(interval[1]));
		assertFalse(finder.nextBunchAvailable(interval[1]));
		
		generateData(false, expat, 1, new int[][] {
				{2012, 51, Calendar.THURSDAY}, 
				{2012, 52, Calendar.THURSDAY}, 
				{2013, 2, Calendar.THURSDAY}, 
		}, strings(
				"x", "y", "z" 
		), 2);
		
		for (Object p : np.finder.specialPatterns.getValues()) {
			np.setPattern(p.toString());
			keys = finder.keyLine(np.getPattern());
			
			interval = DatePicker.weekInterval("52/12", 1);
			assertTrue(finder.bunchAvailable(interval[0]));
			if (NotePicker.allCategories.equals(p)) {
				assertTrue(finder.previousBunchAvailable(interval));
			}
			else {
				assertFalse(finder.previousBunchAvailable(interval));
			}
			assertTrue(finder.bunchAvailable(interval));
			assertTrue(finder.nextBunchAvailable(interval));
			
			interval = DatePicker.weekInterval("1/13", 1);
			epoch = finder.epochFromKey(finder.find(NotePicker.Direction.PREV, interval));
			assertEquals("52/12", np.formatDate(2, epoch));
			
			np.setPattern(p.toString());
			assertTrue(finder.previousBunchAvailable(interval));
			assertTrue(finder.bunchAvailable(interval));
			if (NotePicker.allCategories.equals(p)) {
				assertTrue(finder.nextBunchAvailable(interval));
			}
			else {
				assertFalse(finder.nextBunchAvailable(interval));
			}
		}
	}
	
	public void testNoteFinding2() throws Exception {
		setupKeinFehler();
		
		String[] keys = np.finder.keyLine(NotePicker.allCategories);
		assertEquals(asList(keys).toString(), 36, keys.length);
		np.setPattern(NotePicker.allCategories);
		
		np.pickNote("1/13", np.getPattern());
		String text = np.getText();
		assertTrue(np.isWrapped(text));
	};

	public void testNotePicking() throws Exception {
		setupKeinFehler();
		
		PreparedStatement ps = np.getCon().prepareStatement("SELECT _id FROM notes where title regexp ?");
		ps.setString(1, NotePicker.allCategories);
		assertEquals(36, np.registerNotes(ps.executeQuery()));

		interval = new long[] {
				timeInMillis(2013, 0, 1),
				timeInMillis(2013, 0, 2),
		};
		
		ps = np.preparePicking(true, NotePicker.allCategories, interval);
		ValMap map = getResultMap(ps);
		assertEquals(2, map.size());
		long[] ids = new long[2];
		int i = 0;
		for (Map.Entry<String,Object> entry : map.entrySet()) {
			String categ = entry.getValue().toString();
			long time = Long.parseLong(entry.getKey());
			long id = getId(categ, time);
			long modified = getModified(id);
			Thread.sleep(1);
			assertEquals(id, np.updateOrInsert(categ, np.formatDate(1, time), categ, true));
			assertThat(getModified(id), is(greaterThan(modified)));
			ids[i] = id;
			i++;
		}
		
		assertEquals(36, np.delete(NotePicker.allCategories, NotePicker.allDates, false));
		np.remove(false, NotePicker.allCategories, np.formatDate(1, interval[0]));
		assertEquals(34, np.delete(NotePicker.allCategories, NotePicker.allDates, false));
	}

	private void setupKeinFehler() throws Exception {
		assertTrue(np.openConnection(test_db));
		ValMap map = getResultMap(
				np.getCon().prepareStatement("select title,count(_id) from notes group by title"));
		if (!map.containsKey("1.") || !map.get("1.").toString().equals("19") || 
				!map.containsKey("2.") || !map.get("2.").toString().equals("7") || 
				!map.containsKey("3.") || !map.get("3.").toString().equals("10"))
			testKeinFehler();
	}

	private long getId(String categ, long time) throws Exception, SQLException {
		PreparedStatement ps = np.preparePicking(false, categ, time);
		assertEquals(1, np.registerNotes(ps.executeQuery()));
		return np.ids[0];
	}

	private long getModified(long id) throws SQLException {
		PreparedStatement ps = np.getCon().prepareStatement("SELECT modified FROM notes where _id = ?");
		ps.setLong(1, id);
		ResultSet rs = ps.executeQuery();
		assertTrue(rs.next());
		return rs.getLong(1);
	}

	public void testNoteWrapping() throws Exception {
		String text = np.wrapNote("foo", "bar");
		assertEquals("{{{ foo\nbar\n}}}", text);
		MatchResult m = findFirstIn(text, NotePicker.notePattern1);
		assertEquals(2, m.groupCount());
		assertEquals("foo", m.group(1));
		assertEquals("bar", m.group(2));
		text = np.wrapNote(now(), "", "");
		m = findFirstIn(text, NotePicker.notePattern2);
		assertEquals(3, m.groupCount());
		assertEquals("", m.group(2));
		assertEquals("", m.group(3));

		setupKeinFehler();
		
		interval = new long[] {
				timeInMillis(2012, -11, 30),
				timeInMillis(2012, -11, 31),
		};
		
		np.setPattern(NotePicker.allCategories);
		
		PreparedStatement ps = np.preparePicking(true, np.getPattern(), interval);
		assertEquals(3, np.registerNotes(ps.executeQuery()));
		
		text = np.all();
		assertTrue(np.isWrapped(text));
		String[][] notes = np.getRecords(text);
		for (int i = 0; i < notes.length; i++) {
			for (int j = 0; j < notes[i].length; j++) 
				if (j == 0)
					assertEquals(np.formatDate(1, (Long)np.records[i][j]), notes[i][j]);
				else
					assertEquals(np.records[i][j], notes[i][j]);
		}
		
		np.finder.keyLine(NotePicker.allCategories);
		np.pickNote(DatePicker.weekDate(weekInterval(new Date(interval[0]), 1)), np.getPattern());
		assertTrue(np.isWrapped(np.getText()));
	}

	public void testSqliteRegex() throws Exception {
		String dbfile = "/tmp/temp.db";
		new File(dbfile).delete();
		
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:");
		Statement stat = conn.createStatement();
		assertEquals(0, stat.executeUpdate("DROP TABLE if exists try;"));
		assertEquals(0, stat.executeUpdate("CREATE TABLE try (a TEXT);"));
		assertEquals(1, stat.executeUpdate("insert into try (a) values ('foo');"));
		assertEquals(1, stat.executeUpdate("insert into try (a) values ('bar');"));
		assertEquals(1, stat.executeUpdate("insert into try (a) values ('bat');"));
		assertEquals(1, stat.executeUpdate("insert into try (a) values ('woo');"));
		assertEquals(1, stat.executeUpdate("insert into try (a) values ('oop');"));
		assertEquals(1, stat.executeUpdate("insert into try (a) values ('craw');"));
		assertEquals(0, stat.executeUpdate("backup to " + dbfile));
		stat.close();
		conn.close();
		
		String inputfile = "/tmp/control.xml";
		contentsToFile(new File(inputfile), 
				"<control>" +
					"<QUERY " + 
						"statement=\"SELECT a FROM try WHERE a REGEXP ?\" typeinfo=\"string\" />" + 
					"<textelement query=\"1\" param1=\"^b.*\" day=\"0\" />" + 
					"<textelement query=\"1\" param1=\".*a.*\" day=\"0\" />" + 
					"<textelement query=\"1\" param1=\"w?oop?\" day=\"0\" />" + 
				"</control>");
		
		String styleSheet = getSetting("content.xsl", "scripts/content.xsl");
		xmlTransform(inputfile, styleSheet, tempfile.getPath(), 
				"debug", "yes", 
				"dbfile", dbfile
		);
		
		Document doc = xmlDocument(tempfile);
		NodeList tables = doc.getElementsByTagName("table");
		assertEquals(3, tables.getLength());
		for (int i = 0; i < tables.getLength(); i++) {
			NodeList rows = tables.item(i).getChildNodes();
			assertEquals(new int[]{2,3,2}[i], rows.getLength());
			for (int j = 0; j < rows.getLength(); j++) {
				NodeList cols = rows.item(j).getChildNodes();
				assertEquals(1, cols.getLength());
				Node cell = cols.item(0);
				switch (i) {
				case 0:
					switch (j) {
					case 0:		assertEquals("bar", cell.getTextContent());			break;
					case 1:		assertEquals("bat", cell.getTextContent());			break;
					}
					break;
				case 1:
					switch (j) {
					case 0:		assertEquals("bar", cell.getTextContent());			break;
					case 1:		assertEquals("bat", cell.getTextContent());			break;
					case 2:		assertEquals("craw", cell.getTextContent());		break;
					}
					break;
				case 2:
					switch (j) {
					case 0:		assertEquals("woo", cell.getTextContent());			break;
					case 1:		assertEquals("oop", cell.getTextContent());			break;
					}
					break;
				}
			}
		}
	}

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
	
	public void testXMLPiping() throws Exception {
		// Instantiate  a TransformerFactory.
	  	TransformerFactory tFactory = TransformerFactory.newInstance();
	    // Determine whether the TransformerFactory supports The use uf SAXSource 
	    // and SAXResult
	    if (tFactory.getFeature(SAXSource.FEATURE) && tFactory.getFeature(SAXResult.FEATURE))
	    { 
			// Cast the TransformerFactory to SAXTransformerFactory.
			SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);	  
			// Create a TransformerHandler for each stylesheet.
			TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource(getSetting("control.xsl", "scripts/control.xsl")));
			TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource(getSetting("content.xsl", "scripts/content.xsl")));
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
	    
		check_documents(0, contentfile.getPath(), tempfile.getPath());
	}
	
	File contentfile = new File(getSetting("content.xml", "scripts/content.xml"));
	String paramsFilename = getSetting("params.xml", "scripts/params.xml");

	Pattern textElementPattern = Pattern.compile("(.*form\\[1\\]/(text|textarea)\\[(\\d+)\\])");
	
	String check_documents(int way, String inputFilename, String outputFilename) throws Exception {
		StringBuilder sb = new StringBuilder();
		
		assertTrue(String.format("'%s' doesn't exist", inputFilename), new File(inputFilename).exists());
		assertTrue(String.format("'%s' doesn't exist", outputFilename), new File(outputFilename).exists());
		Diff diff = compareXML(new FileReader(inputFilename), new FileReader(outputFilename));
		if (!diff.similar()) {
			Document input = xmlDocument(new File(inputFilename));
			Document test = xmlDocument(new File(outputFilename));
			
			String t = "", desc = "", id = "";
	        DetailedDiff detailedDiff = new DetailedDiff(diff);
			List<?> allDifferences = detailedDiff.getAllDifferences();
			for (Object detail : allDifferences) {
				Difference df = (Difference) detail;
				String testXpathLocation = df.getTestNodeDetail().getXpathLocation();
				String controlXpathLocation = df.getControlNodeDetail().getXpathLocation();
				MatchResult mr = findFirstIn(controlXpathLocation, textElementPattern);
				String xpath = mr == null ? "/" : mr.group(1);
				id = xpathEngine.evaluate(xpath + "/@id", input);
				if (!t.equals(id)) {
					switch (way) {
					case 3:
						String inputValue = xpathEngine.evaluate(controlXpathLocation, input);
						String testValue = xpathEngine.evaluate(testXpathLocation, test);
						sb.append(String.format("%s : %s (%s)\n", testXpathLocation, testValue, inputValue));
						break;
					case 1:
						assertEquals(id, id, xpathEngine.evaluate(xpath + "/@current-value", test));
						break;
					case 2:
						NodeList nodes = xpathEngine.getMatchingNodes(
								String.format("/control/textelement[@formid='%s']", id), 
								control);
						if (nodes.getLength() > 0) {
							NamedNodeMap attributes = nodes.item(0).getAttributes();
							Node q = attributes.getNamedItem("query");
							if (q != null) {
								String val = q.getNodeValue();
								NodeList queries = xpathEngine.getMatchingNodes(
										String.format("/control/QUERY[%d]", Integer.parseInt(val)), 
										control);
								if (queries.getLength() > 0) {
									Element query = (Element)queries.item(0);
									String sql = query.getAttribute("statement");
									PreparedStatement ps = con.prepareStatement(sql);
									String[] typeinfo = query.getAttribute("typeinfo").split(",");
									for (int i = 0, j = 1; i < typeinfo.length; i++) {
										q = null;
										while (q == null && j < 10) {
											q = attributes.getNamedItem("param" + j);
											j++;
										}
										val = q.getNodeValue();
										if (typeinfo[i].contains("integer"))
											ps.setLong(i + 1, Long.parseLong(val));
										else
											ps.setString(i + 1, val);
									}
									ResultSet rs = ps.executeQuery();
									val = "";
									if (rs.next())
										val = rs.getString(1);
									if (val.equals("NaN"))
										val = "";
									assertEquals(id, val, xpathEngine.evaluate(xpath + "/@current-value", test));
								}
							}
						}
						
						if (desc.length() > 0) {
							sb.append(String.format("%s %s\n", t, desc));
							desc = "";
						}
						break;
					}
					t = id;
				}
				
				desc += "|" + df.getDescription();
			}
			if (way == 2 && desc.length() > 0)
				sb.append(String.format("%s %s\n", id, desc));
		}
		
		return sb.toString();
	}
	
	SimpleXpathEngine xpathEngine = new SimpleXpathEngine();
	Document control = null;
	Connection con = null;

	public void testContent() throws Exception {
		File tempDir = tempDir(true, BerichtsheftApp.NAME, "odt");
		try {
			File source = new File("Vorlagen/Tagesberichte.odt");
			assertTrue(source.exists());
			File archive = new File(tempDir, "Vorlage.zip");
			copyFile(source, archive);
			assertTrue(archive.exists());
	    	int unzipped = unzipArchive(archive, 
	    			new UnzipJob(tempDir.getPath()), 
	    			false);
	    	assertTrue(archive.delete());
	
	    	String content = pathCombine(tempDir.getPath(), "content.xml");
			String _content = pathCombine(tempDir.getPath(), "_content.xml");
			assertTrue(new File(content).renameTo(new File(_content)));
			
			String styleSheet1 = getSetting("control.xsl", "scripts/control.xsl");
			String styleSheet2 = getSetting("content.xsl", "scripts/content.xsl");
			Class.forName("org.sqlite.JDBC");
			for (int way = 1; way < 3; way++) {
				switch (way) {
				case 1:
					xmlTransform(paramsFilename, styleSheet2, content, 
							"inputfile", _content, 
							"control", -1);
					break;

				case 2:
					assertTrue(new File(content).delete());
					xmlTransform(paramsFilename, styleSheet1, "/tmp/control.xml"); 
					
					control = xmlDocument(new File("/tmp/control.xml"));
					String url = xpathEngine.evaluate("/control/DBINFO/dburl", control);
					con = DriverManager.getConnection(url);
					
					xmlTransform("/tmp/control.xml", styleSheet2, content, 
							"inputfile", _content); 
					break;

				case 3:
					assertTrue(new File(content).delete());
					assertTrue(BerichtsheftApp.pipe(_content, content, new FileReader(paramsFilename)));
					break;
				}
				check_documents(way, _content, content);
			}
			if (con != null)
				con.close();
			
			assertTrue(new File(_content).delete());
			
			File destination = new File("Dokumente/Tagesberichte.odt");
			if (destination.exists())
				destination.delete();
			int zipped = zipArchive(destination, 
					tempDir.getPath(), 
					tempDir.getPath());
			assertTrue(destination.exists());
			assertEquals(unzipped, zipped);
			
			int updated = updateArchive(destination, 
	    			tempDir.getPath(), 
	    			1, 
	    			content, content);
			assertEquals(unzipped, updated);
		}
		finally {
			assertTrue(deleteDirectory(tempDir));
		}
	}

	public void testExport() throws Exception {
		Document doc = xmlDocument(new File(paramsFilename));
		String dbName = xpathEngine.evaluate("/params/dbfile", doc);
		int year = toInt(2013, xpathEngine.evaluate("/params/year", doc));
		int weekInYear = toInt(5, xpathEngine.evaluate("/params/weekInYear", doc));
		int dayInWeek = toInt(1, xpathEngine.evaluate("/params/dayInWeek", doc));
		String dateString = DatePicker.pickADate(
				timeInMillis(year, weekInYear, dayInWeek), 
				DatePicker.weekFormat,
				"Pick week for 'Tagesberichte'");
		if (dateString.length() < 1)
			return;
		
		int[] parts = DatePicker.parseWeekDate(dateString);
		contentsToFile(new File(paramsFilename), 
			BerichtsheftApp.parameters(
				dbName, 
				parts[1], 
				parts[0]));
		
		assertTrue(BerichtsheftApp.export(
				"Vorlagen/Tagesberichte.odt", 
				"Dokumente/Tagesberichte_", 
				dbName, 
				parts[1], 
				parts[0]));
	}

	public void testXPath() throws Exception {
		Document doc = xmlDocument(new File(getSetting("content.xml", "scripts/content.xml")));
//		Document doc = xmlDocument(new File("Vorlagen/Tagesberichte_2012/styles.xml"));
		
/*		String path = 
				"/document-content" +
				"/body[1]" +
				"/text[1]" +
				"/p[1]" +
				"/control" +
				"[@control='%s']";
		path = String.format(path, "control32");
		String path = 
				"/document-styles" +
				"/automatic-styles" +
				"/page-layout" +
				"/page-layout-properties";
*/		String path = 
				"/document-content" +
				"/body" +
				"/text" +
				"/p" +
				"/frame[1]" +
				"/image";

		NodeList nodes = evaluateXPath(doc, path);
//		NodeList nodes = xpathEngine.getMatchingNodes(path, doc);
		
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Element el = (Element) nodes.item(i);
			NamedNodeMap attributes = el.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Node node = attributes.item(j);
				println("%s : %s", node.getNodeName(), node.getNodeValue());
			}
		}
	}
	
	public void testMimicry() throws Exception {
		new File("/tmp/debug.out").delete();
		File dir = tempDir(true, BerichtsheftApp.NAME);

		String content = getSetting("content.xml", "scripts/content.xml");
		String mask = getSetting("mask.xsl", "scripts/mask.xsl");
		String output = "/tmp/content.xml";
		
		clearMappings();
		xmlTransform(content, mask, output, 
				"mode", 1);
		
		FormEditor.pages = dir.listFiles();
		int length = FormEditor.pages.length;
		assertEquals(2, length);
		for (int i = 0; i < length; i++) {
			assertTrue(mappings.containsKey("frame" + (i+1) + "_image"));
			assertTrue(mappings.containsKey("frame" + (i+1) + "_width"));
			assertTrue(mappings.containsKey("frame" + (i+1) + "_height"));
		}
		
		String[] keys = strings("control32_x");
		String[] values = strings("xxx");
		
		for (int i = 0; i < keys.length; i++) 
			mappings.put(keys[i], values[i]);
//		System.out.println(mappings);
		
		for (int i = 0; i < keys.length; i++) {
			Document doc = FormEditor.map2page(mappings, i, false);
			xmlNodeToFile(doc, true, FormEditor.pages[i]);
		}
		
		xmlTransform(content, mask, output, 
				"mode", 2);
		
		String report = check_documents(3, content, output);
//		System.out.println(report);
		
		for (int i = 0; i < keys.length; i++) 
			assertTrue(report, report.contains(values[i]));
			
		assertTrue(deleteDirectory(dir));
	}
	
	public void testFormEdit() throws Exception {
		String inputPath = "Vorlagen/Tagesberichte.odt";
		String outputPath = "Dokumente/Tagesberichte.odt";
		assertTrue(FormEditor.perform(inputPath, outputPath, true, 
			new Job<FormEditor>() {
				public void perform(FormEditor formEditor, Object[] params) throws Exception {
					formEditor.updateSplitComponents(0, "action1=&control1=on&x=.5&y=.2&width=&height=");
				}
			}, 
			new Job<Void>() {
				public void perform(Void t, Object[] params) throws Exception {
					String report = check_documents(3, params[0].toString(), params[1].toString());
					assertTrue(report, report.contains("@x"));
					assertTrue(report, report.contains("0.5"));
				}
			}
		));
	}
	
	@SuppressWarnings("rawtypes")
	public void testClasses() throws Exception {
		for (String pkg : providerPackages) {
			Class[] cls = getLocalClasses(pkg);
			for (Class cl : filter(asList(cls), false, new Predicate<Class>() {
				@Override
				public boolean apply(Class c) {
					String name = c.getName();
					return !name.contains("$") && !name.endsWith("Provider");
				}
			}))
				println(cl);
		}
		println(contentAuthorities(providerPackages));
	}
}
