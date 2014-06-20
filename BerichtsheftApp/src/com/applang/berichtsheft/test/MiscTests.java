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

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import junit.framework.Test;
import junit.framework.TestSuite;

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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.applang.Util2;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.berichtsheft.plugin.DataDockable;
import com.applang.components.DataView;
import com.applang.components.ActionPanel.ActionType;
import com.applang.components.DataView.DataModel;
import com.applang.components.DatePicker.Period;
import com.applang.components.DatePicker;
import com.applang.components.FormEditor;
import com.applang.components.NotePicker;
import com.applang.components.TextToggle;
import com.applang.components.WeatherManager;
import com.applang.components.NotePicker.NoteFinder;
import com.applang.provider.NotePad;
import com.applang.provider.PlantInfo;
import com.applang.provider.WeatherInfo;

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.ZipUtil.*;

public class MiscTests extends XMLTestCase
{
	public MiscTests(String testName) {
		super(testName);
		this.testName = testName;
	}

	private String testName = null;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		underTest = true;
		BerichtsheftApp.loadSettings();
		if (!"testOdtDokument".equals(testName) && !"testPiping".equals(testName)) {
			textToggle = new TextToggle();
			np = new NotePicker(null, textToggle);
		}
		if (tempfile.exists())
			tempfile.delete();
		contentfile = new File(BerichtsheftApp.applicationDataPath("Skripte/content.xml"));
		paramsFilename = BerichtsheftApp.applicationDataPath("Skripte/params.xml");

	}

	File tempfile = new File(tempPath(), "temp.xml");
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (np != null && np.getCon() != null)
			np.getCon().close();
		Settings.save();
		underTest = false;
	}
	
	public static void main(String...args) {
		junit.textui.TestRunner.run(partialSuite());
	}
	
	public static Test partialSuite() { 
	    TestSuite suite = new TestSuite(); 
	    suite.addTest(new MiscTests("testClasses")); 
	    suite.addTest(new MiscTests("testKeinFehler")); 
	    suite.addTest(new MiscTests("testContentUris")); 
	    suite.addTest(new MiscTests("testNotesBrowsing")); 
	    return suite;
	}

	TextToggle textToggle;
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
	    int[] period = Period.loadParts(1);
	    DatePicker.modality |= Behavior.TIMEOUT;
		do {
			period = DatePicker.pickAPeriod(period, "pick day, week or month");
			if (period == null)
				return;
			
			println(period);
		} while (true);
	}

	long[] interval;
	
	public void _testAllNotes() throws Exception {
//		assertTrue(np.openConnection("databases/berichtsheft.db"));
		assertTrue(np.openConnection("/home/lotharla/Downloads/_note_pad1.db"));
//		assertTrue(np.openConnection("/home/lotharla/work/Niklas/note_pad_april 5-03-2013.db"));
		long[] time = new long[]{
				dateInMillis(2013, 0, 1),
				dateInMillis(2014, 0, 1),
		};
		PreparedStatement ps = np.preparePicking(true, NotePicker.allTitles, time);
		ResultSet rs = ps.executeQuery();
		np.registerNotes(rs);
		String text = np.all();
//		System.out.println(text);
		contentsToFile(new File(tempPath(), "notes.txt"), text);
	}
	
	boolean randomizeTimeOfDay = false;
	
	SQLiteDatabase db = null;

	private void openConnection(String dbPath) {
		assertTrue(makeSureExists(NotePad.AUTHORITY, new File(dbPath)));
		db = SQLiteDatabase.openOrCreateDatabase(dbPath, null);
	}
	
	int generateData(boolean empty, 
			Pattern expat, int grp, 
			int[][] dates, String[] categories, 
			Integer... params) throws Exception 
	{
		InputStream is = new BerichtsheftActivity().getResources().getAssets().open("Kein Fehler im System.txt");
		MatchResult[] excerpts = excerptsFrom(is, expat);
		
		Integer start = param(0, 0, params);
		Integer length = param(dates != null ? dates.length : 0, 1, params);
		
		Cursor c;
		if (empty) {
			c = db.rawQuery("select count(*) from notes", null);
			if (c != null && c.moveToFirst()) {
				int cnt = c.getInt(0);
				if (cnt > 0)
					assertEquals(cnt, db.delete("notes", null, null));
			}
		}
		
		int cnt = 0;
		long time = dateFromTodayInMillis(0);
		String category = "";
		for (int i = start; i < excerpts.length; i++) {
			MatchResult m = excerpts[i];
			int j = i - start;
			if (isAvailable(j, categories))
				category = categories[j];
			time = dates != null && j < dates.length ?
					timeInMillis(dates[j][0], dates[j][1], dates[j][2]) : 
					dateFromTodayInMillis(1, new Date(time), randomizeTimeOfDay);
			long[] interval = dayInterval(time, 1);
			ContentValues values = new ContentValues();
			values.put("note", m.group(grp));
			values.put("title", category);
			values.put("created", time);
			values.put("modified", now());
			c = db.rawQuery("select _id from notes where created between ? and ? and title regexp ?", 
					strings("" + interval[0], "" + (interval[1] - 1), category));
			boolean update = c != null && c.moveToFirst();
			if (update)
				assertThat(
						db.update("notes", values, "_id=?", strings("" + c.getLong(0))), 
						is(greaterThan(-1)));
			else
				assertThat(
						db.insert("notes", null, values), 
						is(greaterThan(-1L)));
			cnt++;
			if (j >= length - 1)
				break;
		}
		return cnt;
	}

	private ValMap results(final int kind) {
		String sql;
		switch (kind) {
		case 1:
			sql = "select count(*) from notes";
			break;
		case 0:
			sql = "select title,count(_id) from notes group by title";
			break;
		default:
			return null;
		}
	    return getResults(db.rawQuery(sql, null), 
		    	new Function<String>() {
					public String apply(Object... params) {
						Cursor cursor = param(null, 0, params);
						switch (kind) {
						case 1:
							return "count";
						case 0:
							return cursor.getString(0);
						default:
							return null;
						}
					}
		        }, 
		    	new Function<Object>() {
					public Object apply(Object... params) {
						Cursor cursor = param(null, 0, params);
						switch (kind) {
						case 1:
							return cursor.getInt(0);
						case 0:
							return cursor.getInt(1);
						default:
							return null;
						}
					}
		        }
		    );
	}

	String test_db = tempPath() + "/test.db";
	Pattern expat = Pattern.compile("(?s)\\n([^\\{\\}]+?)(?=\\n)");
	int[] date = ints(2012, -Calendar.DECEMBER, 24);
	
	public void testKeinFehler() throws Exception {
		try {
			openConnection(test_db);
			
			int[][] dates = new int[][] {date};
			
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
			
			assertEquals(36, results(1).get("count"));
		} finally {
			if (db != null)
				db.close();
		}
	}
	
	private void setupKeinFehler(boolean newDb) {
		if (newDb)
			new File(test_db).delete();
		try {
			openConnection(test_db);
			ValMap map = results(0);
			if (!new Integer(19).equals(map.get("1.")) || 
					!new Integer(7).equals(map.get("2.")) || 
					!new Integer(10).equals(map.get("3.")))
				testKeinFehler();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (db != null)
				db.close();
		}
	}
	
	public void testFlavoredUris() {
		String dbFileName = test_db;
		new File(dbFileName).delete();
		String[] tableNames = strings("notes", "weathers", "plants");
		String[] flavors = strings(NotePad.AUTHORITY, WeatherInfo.AUTHORITY, PlantInfo.AUTHORITY);
		ValMap info;
		for (int i = 0; i < flavors.length; i++) {
			String tableName = tableNames[i];
			Uri furi = fileUri(dbFileName, tableName);
			assertEquals(tableName, dbTableName(furi));
			assertTrue(makeSureExists(flavors[i], new File(dbFileName)));
			println(runShellScript("dump", BerichtsheftPlugin.sqliteScript(dbFileName, ".tables")));
		}
		Context context = new BerichtsheftActivity();
		for (int i = flavors.length - 1; i > -1; i--) {
			String flavor = flavors[i];
			String tableName = tableNames[i];
			Uri curi = contentUri(flavor, tableName);
			assertEquals(tableName, dbTableName(curi));
			info = table_info2(context, curi, tableName, flavor);
			assertTrue(info.containsKey("PRIMARY_KEY"));
			assertTrue(info.containsKey("VERSION"));
			assertEquals(databaseName(flavor), getDatabaseFile(context, curi).getName());
			context.registerFlavor(flavor, dbFileName);
			assertEquals(dbFileName, getDatabaseFile(context, curi).getPath());
			info = table_info2(context, curi, tableName, flavor);
			assertTrue(info.containsKey("PRIMARY_KEY"));
			assertTrue(info.containsKey("VERSION"));
		}
		for (int i = 0; i < flavors.length; i++)
			assertTrue(context.unregisterFlavor(flavors[i]));
	}
	
	public void testNotesBrowsing() {
		setupKeinFehler(false);
		DataView dv = new DataView();
		Uri uri = fileUri(test_db, "notes");
		dv.setUri(uri);
		np.setDataView(dv);
		assertFalse(np.usingJdbc());
		assertEquals(0, np.pkColumn);
		assertEquals(35, np.lastRow());
		DataModel model = (DataModel) dv.getTable().getModel();
		ValList columns = model.columns;
		BidiMultiMap projection = new BidiMultiMap(columns);
		projection.putValue("created", "unixepoch");
		model.setProjection(projection);
		int[] index = {
			columns.indexOf("created"), 
			columns.indexOf("title"), 
			columns.indexOf("note"), 
			columns.indexOf("_id")
		};
		assertEquals(0, np.pkRow);
		for (int i = 0; i < model.getRowCount(); i++) {
			Object[] values = model.getValues(true, i);
			println(values);
			Object expected = values[index[2]];
			Object[] result = (Object[]) np.select(values[index[0]], values[index[1]]);
			assertEquals(expected, result[2]);
			assertEquals(values[index[3]], result[3]);
			assertEquals(expected, np.getText());
			if (np.isActionEnabled(ActionType.NEXT.index()))
				np.browse(ActionType.NEXT);
		}
		assertEquals(np.lastRow(), np.pkRow);
	}

	public void testNoteFinding() {
		setupKeinFehler(false);
		DataView dv = new DataView();
		Uri uri = fileUri(test_db, "notes");
		dv.setUri(uri);
		np.setDataView(dv);
		String[] keys = np.finder.keyLine("%");
		assertEquals(asList(keys).toString(), 36, keys.length);
		keys = np.finder.keyLine();
		println((Object)keys);
		assertEquals(asList(keys).toString(), 36, keys.length);
		DataModel model = (DataModel) dv.getTable().getModel();
		int[] index = {
				model.columns.indexOf("created"), 
				model.columns.indexOf("title")
		};
		for (int i = 0; i < model.getRowCount(); i++) {
			Object[] values = model.getValues(false, i);
			Long time = (Long)values[index[0]];
//			time += getRandom().nextInt((int)getMillis(1));
			int found = np.finder.pointer(time, (String)values[index[1]]);
			assertEquals(i, found);
		}		
	};

	public void testNotesManagment() {
		setupKeinFehler(false);
		DataView dv = new DataView();
		Uri uri = fileUri(test_db, "notes");
		dv.setUri(uri);
		np.setDataView(dv);
		assertFalse(np.isDirty());
		int cnt = np.comboBoxes[0].getItemCount();
		assertEquals(3, cnt);
		for (int i = 0; i < 3; i++) 
			assertEquals((i + 1) + ".", np.comboBoxes[0].getItemAt(i));
		long time = timeInMillis(date[0], date[1], date[2]);
		for (String stanza : strings("1.","2.","3.")) {
			String dateString = formatDate(time, DatePicker.calendarFormat);
			Object[] result = (Object[]) np.select(dateString, stanza);
			assertEquals(time, result[0]);
			assertEquals(stanza, result[1]);
			assertThat(stringValueOf(result[2]).length(), is(greaterThan(0)));
		}
		np.setText("ein Fehler im System");
		Object[] record = objects(
			formatDate(time, DatePicker.calendarFormat), 
			"4."			
		);
		np.save(record, true);
		assertThat(np.comboBoxes[0].getItemAt(3).toString(), is(equalTo("4.")));
		assertEquals(36, np.lastRow());
		assertFalse(np.isDirty());
		textToggle.getTextEdit().insert("k", 0);
		assertTrue(np.isDirty());
		np.setDate(record[0].toString());
		np.setTitle(record[1].toString());
		np.updateChange(true);
		assertFalse(np.isDirty());
		Object[] rec = (Object[]) np.select(record);
		assertThat(rec, notNullValue());
		assertEquals(37L, rec[3]);
		np.setRow(rec[3]);
//		assertEquals(36, np.pkRow);
		np.delete(record);
//		assertThat(np.comboBoxes[0].getItemAt(3).toString(), not(equalTo("4.")));
	};

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
	
	public void testData() throws Exception {
		new File(test_db).delete();
		openConnection(test_db);
		int cnt = generateData(true, expat, 1, dates, categories, 2);
		assertEquals(dates.length, cnt);
		db.close();
	}

	void setupNotes(String db, Object... params) throws ParseException {
		assertTrue("openConnection failed", np.openConnection(db, params));
		interval = DatePicker.weekInterval("33/12", 1);
		assertEquals(2, interval.length);
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
					np.insert(4, "", np.getTitle(), timeInMillis(dates[2][0], dates[2][1], dates[2][2])), 
					is(greaterThan(-1)));
			fail("expected to fail on UNIQUE constraint in the notes table");
		} catch (Exception e) {}
	}	
    
    int _generateData(boolean empty,
                    Pattern expat, int grp,
                    int[][] dates, String[] categories,
                    Integer... params) throws Exception
    {
		InputStream is = new BerichtsheftActivity().getResources().getAssets().open("Kein Fehler im System.txt");
        MatchResult[] excerpts = excerptsFrom(is, expat);
        
        Integer start = param(0, 0, params);
        Integer length = param(dates != null ? dates.length : 0, 1, params);
        
        if (empty) {
            int cnt = np.delete(NotePicker.allTitles, NotePicker.allDates, false);
            assertEquals(cnt, np.delete(NotePicker.allTitles, NotePicker.allDates, true));
        }
        
        int cnt = 0;
        long time = dateFromTodayInMillis(0);
        for (int i = start; i < excerpts.length; i++) {
            MatchResult m = excerpts[i];
            int j = i - start;
            if (isAvailable(j, categories))
            	np.setTitle(categories[j]);
            time = dates != null && j < dates.length ?
                    timeInMillis(dates[j][0], dates[j][1], dates[j][2]) :
                    dateFromTodayInMillis(1, new Date(time), randomizeTimeOfDay);
            String dateString = NotePicker.formatDate(1, time);
            String note = m.group(grp);
            if (empty)
                assertThat(
                    np.insert(1+i-start, note, np.getTitle(), time),
                    is(greaterThan(-1)));
            else
                assertThat(
                    np.updateOrInsert(dateString, np.getPattern(), note, true),
                    is(greaterThan(-1L)));
            cnt++;
            if (j >= length - 1)
                break;
        }
        return cnt;
    }

	
	public void _testNotesListing() throws Exception {
		setupKeinFehler(false);
		assertTrue(np.openConnection(test_db));
		PreparedStatement ps = np.getCon().prepareStatement(
				"select _id,title,note,created,modified from notes order by created,title");
		ResultSet rs = ps.executeQuery();
		int rows = np.registerNotes(rs);
		for (int i = 0; i < rows; i++) {
			Object[] values = np.records[i];
			println(values);
		}
		rs.close();
	}

	public void _testNoteFinding1() throws Exception {
		testData();
		assertTrue(np.openConnection(test_db));
		NoteFinder finder = np.finder;
		String[] keys = finder.keyLine(NotePicker.allTitles);
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
		assertEquals(finder.keyValue(epoch, categories[1]), finder.find(ActionType.PICK, epoch));
		np.setPattern("Bericht_");
		assertEquals(finder.keyValue(after, categories[2]), finder.find(ActionType.PICK, epoch));
		np.setPattern("xxx");
		assertEquals(finder.keyValue(after, categories[2]), finder.find(ActionType.PICK, after));
		np.setPattern("Bericht");
		assertEquals(finder.keyValue(epoch, categories[1]), finder.find(ActionType.PICK, epoch));
		assertEquals(finder.keyValue(after, categories[2]), finder.find(ActionType.NEXT, epoch));
		assertEquals(finder.keyValue(before, categories[0]), finder.find(ActionType.PREVIOUS, epoch));
		
		interval = DatePicker.weekInterval("52/12", 1);
		epoch = finder.epochFromKey(finder.find(ActionType.NEXT, interval));
		assertEquals("1/13", NotePicker.formatDate(2, epoch));
		
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
		_generateData(false, expat, 1, 
			new int[][] {
				{2012, 51, Calendar.THURSDAY}, 
				{2012, 52, Calendar.THURSDAY}, 
				{2013, 2, Calendar.THURSDAY}, 
			}, 
			strings("x", "y", "z"), 2);
		for (Object p : np.finder.specialPatterns.getValues()) {
			np.setPattern(p.toString());
			keys = finder.keyLine(np.getPattern());
			
			interval = DatePicker.weekInterval("52/12", 1);
			assertTrue(finder.bunchAvailable(interval[0]));
			if (NotePicker.allTitles.equals(p)) {
				assertTrue(finder.previousBunchAvailable(interval));
			}
			else {
				assertFalse(finder.previousBunchAvailable(interval));
			}
			assertTrue(finder.bunchAvailable(interval));
			assertTrue(finder.nextBunchAvailable(interval));
			
			interval = DatePicker.weekInterval("1/13", 1);
			epoch = finder.epochFromKey(finder.find(ActionType.PREVIOUS, interval));
			assertEquals("52/12", NotePicker.formatDate(2, epoch));
			
			np.setPattern(p.toString());
			assertTrue(finder.previousBunchAvailable(interval));
			assertTrue(finder.bunchAvailable(interval));
			if (NotePicker.allTitles.equals(p)) {
				assertTrue(finder.nextBunchAvailable(interval));
			}
			else {
				assertFalse(finder.nextBunchAvailable(interval));
			}
		}
	}
	
	public void _testNoteFinding2() {
		setupKeinFehler(false);
		assertTrue(np.openConnection(test_db));
		String[] keys = np.finder.keyLine(NotePicker.allTitles);
		println((Object)keys);
		assertEquals(asList(keys).toString(), 36, keys.length);
		np.setPattern(NotePicker.allTitles);
		
		np.pickNote("1/13", np.getPattern());
		String text = np.getText();
		assertTrue(np.isWrapped(text));
	};

	public void _testNotePicking() throws Exception {
		randomizeTimeOfDay = true;
		setupKeinFehler(true);
		assertTrue(np.openConnection(test_db));
		PreparedStatement ps = np.getCon().prepareStatement("SELECT _id FROM notes where title regexp ?");
		ps.setString(1, NotePicker.allTitles);
		assertEquals(36, np.registerNotes(ps.executeQuery()));

		interval = new long[] {
				timeInMillis(2013, 0, 1),
				timeInMillis(2013, 0, 2),
		};
		
		ps = np.preparePicking(true, NotePicker.allTitles, interval);
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
			assertEquals(id, np.updateOrInsert(NotePicker.formatDate(1, time), categ, categ, true));
			assertThat(getModified(id), is(greaterThan(modified)));
			ids[i] = id;
			i++;
		}
		
		assertEquals(36, np.delete(NotePicker.allTitles, NotePicker.allDates, false));
		np.remove(false, NotePicker.allTitles, NotePicker.formatDate(1, interval[0]));
		assertEquals(34, np.delete(NotePicker.allTitles, NotePicker.allDates, false));
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

	public void _testNoteWrapping() throws Exception {
		assertTrue(np.openConnection(test_db));
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

		setupKeinFehler(false);
		
		interval = new long[] {
				timeInMillis(2012, -11, 30),
				timeInMillis(2012, -11, 31),
		};
		
		np.setPattern(NotePicker.allTitles);
		
		PreparedStatement ps = np.preparePicking(true, np.getPattern(), interval);
		assertEquals(3, np.registerNotes(ps.executeQuery()));
		
		text = np.all();
		assertTrue(np.isWrapped(text));
		String[][] notes = np.listRecords(text);
		for (int i = 0; i < notes.length; i++) {
			for (int j = 0; j < notes[i].length; j++) 
				if (j == 0)
					assertEquals(NotePicker.formatDate(1, (Long)np.records[i][j]), notes[i][j]);
				else
					assertEquals(np.records[i][j], notes[i][j]);
		}
		
		np.finder.keyLine(NotePicker.allTitles);
		np.pickNote(DatePicker.weekDate(weekInterval(new Date(interval[0]), 1)), np.getPattern());
		assertTrue(np.isWrapped(np.getText()));
	}

	public void _testSqliteJdbcRegex() throws Exception {
		String dbfile = test_db;
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
		
		String inputfile = tempPath() + "/control.xml";
		contentsToFile(new File(inputfile), 
			String.format(
				"<control>" +
					"<DBINFO>" +
						"<dbdriver>org.sqlite.JDBC</dbdriver>" +
						"<dburl>jdbc:sqlite:%s</dburl>" +
						"<user />" +
						"<password/>" +
					"</DBINFO>" +
					"<QUERY " +
						"dbinfo=\"1\" " + 
						"statement=\"SELECT a FROM try WHERE a REGEXP ?\" " +
						"typeinfo=\"string\" />" + 
					"<textelement query=\"1\" param1=\"^b.*\" day=\"0\" />" + 
					"<textelement query=\"1\" param1=\".*a.*\" day=\"0\" />" + 
					"<textelement query=\"1\" param1=\"w?oop?\" day=\"0\" />" + 
				"</control>", dbfile));
		
		String styleSheet = getSetting("content.xsl", null);	//	BerichtsheftApp.berichtsheftPath("Skripte/content.xsl")
		assertNotNull(styleSheet);
		xmlTransform(inputfile, styleSheet, tempfile.getPath(), 
				"debug", "yes"
		);
		
		Document doc = xmlDocument(tempfile);
		NodeList tables = doc.getElementsByTagName("table");
		assertEquals(3, tables.getLength());
		for (int i = 0; i < tables.getLength(); i++) {
			NodeList rows = tables.item(i).getChildNodes();
			assertEquals(ints(2,3,2)[i], rows.getLength());
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

	public void testSqliteRegex() throws Exception {
		setupKeinFehler(false);
		String controlfile = tempPath() + "/control.xml";
		contentsToFile(new File(controlfile), 
			String.format(
				"<control>" +
					"<DBINFO>" +
						"<dbdriver>android.database.sqlite</dbdriver>" +
						"<dburl>%s</dburl>" +
						"<user />" +
						"<password/>" +
					"</DBINFO>" +
					"<QUERY " +
						"dbinfo=\"1\" " + 
						"statement=\"SELECT note FROM notes WHERE note REGEXP ?\" " +
						"typeinfo=\"string\" />" + 
					"<textelement query=\"1\" param1=\"(?i)^Kein.+\" day=\"0\" />" + 
					"<textelement query=\"1\" param1=\".+fehler.+\" day=\"0\" />" + 
					"<textelement query=\"1\" param1=\".+system$\" day=\"0\" />" + 
				"</control>", fileUri(test_db, null)));
		BerichtsheftApp.performQueries(controlfile);
		println(contentsFromFile(new File(controlfile)));
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
	
	public void testPiping() throws Exception {
		// Instantiate  a TransformerFactory.
	  	TransformerFactory tFactory = TransformerFactory.newInstance();
	    // Determine whether the TransformerFactory supports The use uf SAXSource 
	    // and SAXResult
	    if (tFactory.getFeature(SAXSource.FEATURE) && tFactory.getFeature(SAXResult.FEATURE))
	    { 
			// Cast the TransformerFactory to SAXTransformerFactory.
			SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);	  
			String controlStyleSheet = BerichtsheftApp.applicationDataPath("Skripte/control.xsl");
			String contentStyleSheet = BerichtsheftApp.applicationDataPath("Skripte/content.xsl");
			// Create a TransformerHandler for each stylesheet.
			final TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource(controlStyleSheet));
			TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource(contentStyleSheet));
			tHandler2.getTransformer().setParameter("inputfile", "content.xml");
//			TransformerHandler tHandler3 = saxTFactory.newTransformerHandler(new StreamSource(tempPath() + "/foo3.xsl"));
			TransformerHandler tHandler = new TransformerHandler() {
				@Override
				public void unparsedEntityDecl(String name, String publicId,
						String systemId, String notationName) throws SAXException {
					tHandler1.unparsedEntityDecl(name, publicId, systemId, notationName);
				}
				@Override
				public void notationDecl(String name, String publicId, String systemId)
						throws SAXException {
					tHandler1.notationDecl(name, publicId, systemId);
				}
				@Override
				public void startEntity(String name) throws SAXException {
					tHandler1.startEntity(name);
				}
				@Override
				public void startDTD(String name, String publicId, String systemId)
						throws SAXException {
					tHandler1.startDTD(name, publicId, systemId);
				}
				@Override
				public void startCDATA() throws SAXException {
					tHandler1.startCDATA();
				}
				@Override
				public void endEntity(String name) throws SAXException {
					tHandler1.endEntity(name);
				}
				@Override
				public void endDTD() throws SAXException {
					tHandler1.endDTD();
				}
				@Override
				public void endCDATA() throws SAXException {
					tHandler1.startCDATA();
				}
				@Override
				public void comment(char[] ch, int start, int length) throws SAXException {
					tHandler1.comment(ch, start, length);
				}
				@Override
				public void startPrefixMapping(String prefix, String uri)
						throws SAXException {
					tHandler1.startPrefixMapping(prefix, uri);
				}
				@Override
				public void startElement(String uri, String localName, String qName,
						Attributes atts) throws SAXException {
					tHandler1.startElement(uri, localName, qName, atts);
				}
				@Override
				public void startDocument() throws SAXException {
					tHandler1.startDocument();
				}
				@Override
				public void skippedEntity(String name) throws SAXException {
					tHandler1.skippedEntity(name);
				}
				@Override
				public void setDocumentLocator(Locator locator) {
					tHandler1.setDocumentLocator(locator);
				}
				@Override
				public void processingInstruction(String target, String data)
						throws SAXException {
					tHandler1.processingInstruction(target, data);
				}
				@Override
				public void ignorableWhitespace(char[] ch, int start, int length)
						throws SAXException {
					tHandler1.ignorableWhitespace(ch, start, length);
				}
				@Override
				public void endPrefixMapping(String prefix) throws SAXException {
					tHandler1.endPrefixMapping(prefix);
				}
				@Override
				public void endElement(String uri, String localName, String qName)
						throws SAXException {
					tHandler1.endElement(uri, localName, qName);
				}
				@Override
				public void endDocument() throws SAXException {
					tHandler1.endDocument();
				}
				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {
					tHandler1.characters(ch, start, length);
				}
				@Override
				public void setSystemId(String systemID) {
					tHandler1.setSystemId(systemID);
				}
				@Override
				public void setResult(Result result) throws IllegalArgumentException {
					tHandler1.setResult(result);
				}
				@Override
				public Transformer getTransformer() {
					return tHandler1.getTransformer();
				}
				@Override
				public String getSystemId() {
					return tHandler1.getSystemId();
				}
			};
			// Create an XMLReader.
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(tHandler);
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
	    
		check_transform(0, contentfile.getPath(), tempfile.getPath());
	}
	
	File contentfile;
	String paramsFilename;

	Pattern textElementPattern = Pattern.compile("(.*form\\[1\\]/(text|textarea)\\[(\\d+)\\])");
	
	String check_transform(int way, String inputFilename, String outputFilename) throws Exception {
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
			File source = new File(BerichtsheftApp.odtVorlagePath("Tagesberichte"));
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
			String controlStyleSheet = BerichtsheftApp.applicationDataPath("Skripte/control.xsl");
			String contentStyleSheet = BerichtsheftApp.applicationDataPath("Skripte/content.xsl");
			Class.forName("org.sqlite.JDBC");
			for (int way : ints(1,2,3)) {
				switch (way) {
				case 1:
					xmlTransform(paramsFilename, contentStyleSheet, content, 
							"inputfile", _content, 
							"control", -1);
					break;
				case 2:
					String temp = pathCombine(tempDir.getParent(), "control.xml");
					if (fileExists(content))
						assertTrue(new File(content).delete());
					xmlTransform(paramsFilename, controlStyleSheet, temp); 
					BerichtsheftApp.performQueries(temp);
					control = xmlDocument(new File(temp));
					String url = xpathEngine.evaluate("/control/DBINFO/dburl", control);
					xmlTransform(temp, contentStyleSheet, content, 
							"inputfile", _content); 
					con = DriverManager.getConnection(url);
					break;
				case 3:
					if (fileExists(content))
						assertTrue(new File(content).delete());
					assertTrue(BerichtsheftApp.pipe(_content, content, new FileReader(paramsFilename)));
					break;
				}
				check_transform(way, _content, content);
			}
			if (con != null)
				con.close();
			
			assertTrue(new File(_content).delete());
			
			File destination = new File(BerichtsheftApp.odtDokumentPath("Tagesberichte"));
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
	    DatePicker.modality |= Behavior.TIMEOUT;
		String dateString = DatePicker.pickADate(
				timeInMillis(year, weekInYear, dayInWeek), 
				DatePicker.weekFormat,
				"Pick week for 'Tagesberichte'");
		if (dateString.length() < 1)
			return;
		
		int[] parts = DatePicker.parseWeekDate(dateString);
		contentsToFile(new File(paramsFilename), 
			BerichtsheftApp.parameters(
				strings(dbName,dbName), 
				parts[1], 
				parts[0]));
		
		assertTrue(BerichtsheftApp.export(
				BerichtsheftApp.odtVorlagePath("Tagesberichte"), 
				BerichtsheftApp.odtDokumentPath("Tagesberichte_"), 
				strings(dbName,dbName), 
				parts[1], 
				parts[0]));
	}

	String test1_db = tempPath() + "/test1.db", test2_db = tempPath() + "/test2.db";
	int[] date2 = ints(2012, -Calendar.DECEMBER, 25);
	int[] period = ints(2013, 1, 1, 7);
	
	public void testKeinFehler2() throws Exception {
		try {
			openConnection(test1_db);
			
			int[][] dates = new int[][] {date2};
			
			int length = 19;
			assertEquals(length, 
				generateData(true, expat, 1, 
					dates, 
					strings("Bericht"), 
					2, length));
			length = 7;
			assertEquals(length, 
				generateData(false, expat, 1, 
					dates, 
					strings("Bemerkung2"), 
					21, length));
			int[] date = getCalendarDate(dateInMillis(date2[0], -date2[1], date2[2], length));
			dates = new int[][] {{date[2], -date[1], date[0]}};
			length = 10;
			assertEquals(length, 
				generateData(false, expat, 1, 
					dates, 
					strings("Bemerkung3"), 
					28, length));
			
			assertEquals(36, results(1).get("count"));
		} finally {
			if (db != null)
				db.close();
		}
	}
	
	private void setupKeinFehler2(boolean newDb) {
		if (newDb) {
			new File(test1_db).delete();
			new File(test2_db).delete();
		}
		try {
			openConnection(test1_db);
			ValMap map = results(0);
			if (!new Integer(19).equals(map.get("Bericht")) || 
					!new Integer(7).equals(map.get("Bemerkung2")) || 
					!new Integer(10).equals(map.get("Bemerkung3")))
				testKeinFehler2();
			if (!hasDbWeatherforPeriod(test2_db))
				testWetter();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (db != null)
				db.close();
		}
	}
	
	private boolean hasDbWeatherforPeriod(String dbFileName) throws Exception {
		DatePicker.Period.setParts(period);
		long[] interval = DatePicker.Period.getInterval();
		WeatherManager wm = new WeatherManager();
		assertTrue(wm.openConnection(dbFileName));
		PreparedStatement ps = wm.getCon().prepareStatement(
				"select created from weathers " +
				"where created between ? and ? and location=? " +
				"order by created, location");
		ps.setLong(1, interval[0]);
		ps.setLong(2, interval[1] - 1);
		ps.setString(3, wm.location);
		BidiMultiMap bidi = getResultMultiMap(ps, _null());
		ValList list = bidi.getKeys();
		boolean retval = list.size() > 0 && 
				Long.compare((Long) list.get(0), interval[0]) >= 0 && 
				Long.compare((Long) list.get(-1), interval[1]) <= 0;
		wm.closeConnection();
		return retval;
	}

	public void testWetter() throws Exception {
		DatePicker.Period.saveParts(0, period);
		assertTrue(DataDockable.makeWetter(null, "period", false, test2_db, false));
		assertTrue(hasDbWeatherforPeriod(test2_db));
	}

	public void testOdtDokument() throws Exception {
		setupKeinFehler2(false);
		long time = now();
		DatePicker.Period.saveParts(1, period);
		assertTrue(DataDockable.makeDokument(null, "odt", true, test1_db, test2_db));
		String dateString = DatePicker.Period.weekDate();
		int[] weekDate = DatePicker.parseWeekDate(dateString);
		String dokumentPath = BerichtsheftApp.odtDokumentPath("Tagesberichte", weekDate);
		assertTrue(fileExists(dokumentPath));
		long docTime = Util2.getFileTime(dokumentPath, 1).toMillis();
		assertThat(docTime/1000, is(greaterThanOrEqualTo(time/1000)));
		File tempDir = tempDir(false, BerichtsheftApp.NAME, "odt");
    	String content = pathCombine(tempDir.getPath(), "content.xml");
		String _content = pathCombine(tempDir.getParentFile().getPath(), "_content.xml");
		assertThat(Util2.getFileSize(content), is(greaterThan(Util2.getFileSize(_content))));
		String report = check_transform(2, _content, content);
		println(report);
	}

	public void _testXPath() throws Exception {
		Document doc = xmlDocument(new File(BerichtsheftApp.applicationDataPath("Skripte/content.xml")));
//		Document doc = xmlDocument(new File(BerichtsheftApp.applicationDataPath("Vorlagen/Tagesberichte_2012/styles.xml")));
		
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
		new File(diagFilePath).delete();
		File dir = tempDir(true, BerichtsheftApp.NAME);

		String content = BerichtsheftApp.applicationDataPath("Skripte/content.xml");
		String mask = BerichtsheftApp.applicationDataPath("Skripte/mask.xsl");
		String output = tempPath() + "/content.xml";
		
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
		
		String report = check_transform(3, content, output);
//		System.out.println(report);
		
		for (int i = 0; i < keys.length; i++) 
			assertTrue(report, report.contains(values[i]));
			
		assertTrue(deleteDirectory(dir));
	}
	
	public void testFormEdit() throws Exception {
    	Deadline.WAIT = 2000;
		String inputPath = BerichtsheftApp.odtVorlagePath("Tagesberichte");
		String outputPath = BerichtsheftApp.odtDokumentPath("Tagesberichte");
		assertTrue(FormEditor.perform(inputPath, outputPath, true, 
			new Job<FormEditor>() {
				public void perform(FormEditor formEditor, Object[] params) throws Exception {
					formEditor.updateSplitComponents(0, "action1=&control1=on&x=.5&y=.2&width=&height=");
				}
			}, 
			new Job<Void>() {
				public void perform(Void t, Object[] params) throws Exception {
					String report = check_transform(3, params[0].toString(), params[1].toString());
					assertTrue(report, report.contains("@x"));
					assertTrue(report, report.contains("0.5"));
				}
			}
		));
	}
	
	public static Test suite() {
		if ( TestUtils.hasTestCases() ) {
			return TestUtils.getSuite( MiscTests.class );
		}
		return new TestSuite(MiscTests.class);
	}
}
