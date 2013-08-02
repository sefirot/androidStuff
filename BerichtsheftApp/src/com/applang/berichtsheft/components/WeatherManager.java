package com.applang.berichtsheft.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JPopupMenu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.BerichtsheftShell;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class WeatherManager extends ActionPanel
{
	private static final String TAG = WeatherManager.class.getSimpleName();

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		TextArea textArea = new TextArea();
		
        String title = "WeatherInfo database";
		final WeatherManager weatherManager = new WeatherManager(textArea, 
				null,
				title);
		
		ActionPanel.createAndShowGUI(title, new Dimension(1000, 200), weatherManager, textArea.textArea);
	}

	public WeatherManager(TextComponent textArea, Object... params) {
		super(textArea, params);
		
		addButton(ActionType.DATABASE.index(), new InfoAction(ActionType.DATABASE));
		addButton(4, new InfoAction("Options"));
		
	    JPopupMenu popupMenu = newPopupMenu(
	    	new Object[] {ActionType.IMPORT.description(), new InfoAction(ActionType.IMPORT)}, 
	    	new Object[] {ActionType.TEXT.description(), new InfoAction(ActionType.TEXT)} 
	    );
	    
	    attachDropdownMenu(buttons[4], popupMenu);
	}
	
	@Override
	public void finish(Object... params) {
		if (getCon() != null)
			try {
				updateOnRequest(true);
				getCon().close();
			} catch (Exception e) {
				handleException(e);
			}
		putSetting("database", dbName);
		super.finish(params);
	}
    
    public class InfoAction extends Action
    {
		public InfoAction(ActionType type) {
			super(type);
        }
        
		public InfoAction(String text) {
			super(text);
        }
        
        @Override
        protected void action_Performed(ActionEvent ae) {
        	ActionType t = (ActionType)getType();
        	if (t != null) {
				switch (t) {
				case DATABASE:
					dbName = chooseDatabase(dbName);
					if (notNullOrEmpty(dbName))
						openConnection(dbName);
					break;
				case IMPORT:
					retrieveWeatherData();
					break;
				default:
					return;
				}
			}
        }
    }
	
	private static final String URL_MONTHREP = "http://www.mundomanz.com/meteo_p/monthrep?" +
			"countr=GERMANY&" +
			"ind=%s&" +
			"year=%s&" +
			"month=%s&" +
			"l=1&" +
			"action=display";
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
	private static final String URL_SUMMARY = "http://www.mundomanz.com/meteo_p/byind?" +
			"countr=GERMANY&" +
			"ind=%s&" +
			"year=%s&" +
			"month=%s&" +
			"day=%s&" +
			"n_days=%d&" +
			"trans=PA&" +
			"time=all&" +
			"l=1&action=display";
	
	String urlString(boolean broken, String location, int...dateParts) {
		String url;
		String monthString = "0" + dateParts[1];
		monthString = monthString.substring(monthString.length() - 2, monthString.length());
		switch (dateParts[3]) {
		case Period.MONTH:
			url = String.format(URL_MONTHREP, 
					location, 
					"" + dateParts[0], 
					monthString);
			break;

		default:
			url = String.format(URL_SUMMARY, 
					location, 
					"" + dateParts[0], 
					monthString,
					"" + dateParts[2],
					dateParts[3]);
			break;
		}
		if (broken)
			url = url.replaceAll("(\\?|\\&)", "$1\n");
		return url;
	}
	
	public static Uri siteUri(String translation, String location, String time, int...dateParts) {
		Uri.Builder builder = new Uri.Builder()
			.scheme("http")
			.authority("www.mundomanz.com")
			.appendPath("meteo_p")
			.appendQueryParameter("countr", "GERMANY")
			.appendQueryParameter("ind", location)
			.appendQueryParameter("year", "" + dateParts[0])
			.appendQueryParameter("month", String.format("%02d", dateParts[1]));
		switch (dateParts[3]) {
		case Period.MONTH:
			builder = builder.appendPath("monthrep");
			break;
		default:
			builder = builder.appendPath("byind")
				.appendQueryParameter("day", "" + dateParts[2])
				.appendQueryParameter("n_days", "" + dateParts[3])
				.appendQueryParameter("time", time)
				.appendQueryParameter("trans", translation);
			break;
		}
		return builder
			.appendQueryParameter("l", "1")
			.appendQueryParameter("action", "display")
			.build();
	}
	
	public void parseSite(String location, int[] dateParts) {
		final Uri uri = siteUri("PA", location, "all", dateParts);
		BerichtsheftShell.print("connecting '%s'\n... ", uri);
		long millis = waiting(null, new ComponentFunction<Void>() {
			public Void apply(Component comp, Object[] parms) {
				try {
					doc = Jsoup.connect(uri.toString())
							.timeout(100000)
							.get();
					setText(doc.toString());
				} catch (Exception e) {
					handleException(e);
					doc = null;
				}
				
				return null;
			}
		});
		BerichtsheftShell.print("%d sec(s)", millis / 1000, NEWLINE);
	}
	
	private Document doc = null;

	public ValMap summary(Object...args) {
		final ValMap summary = new ValMap();
		AlertDialog dialog = null;
		try {
			if (param(true, 0, args)) {
				TextView tv = new TextView(null, true);
				tv.getTextArea().setFont(monoSpaced());
				tv.setId(1);
				dialog = new AlertDialog.Builder(BerichtsheftApp.getActivity(),	false)
						.setTitle(param("", 1, args))
						.setView(tv)
						.setNeutralButton(R.string.button_close,
								new OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
									}
								}).create();
				dialog.setModalExclusionType(AlertDialog.ModalExclusionType.APPLICATION_EXCLUDE);
				dialog.setSize(new Dimension(800, 400));
				dialog.open();
			}
			final Object out = dialog != null ? dialog.feed(1) : "";
			waiting(null, new ComponentFunction<Void>() {
				public Void apply(Component comp, Object[] parms) {
					Elements elements = doc.select("pre:contains(D | h |)");
					if (elements.size() > 0) {
						String header = elements.get(0).text();
						println(out, header);
						MatchResult[] vbars = findAllIn(header, Pattern.compile("\\|"));
						int[] columnPos = new int[vbars.length + 2];
						columnPos[0] = -1;
						for (int i = 0; i <= vbars.length; i++) {
							columnPos[i+1] = i < vbars.length ? vbars[i].start() : header.length();
						}
						elements = doc.select(".dat");
						if (elements.size() > 0) {
							for (Element element : elements) {
								String row = element.text();
								println(out, row);
								for (int i = 0; i < columnPos.length - 1; i++) {
									String columnName = header.substring(columnPos[i] + 1, columnPos[i+1]).trim();
									ValList column = summary.getList(columnName);
									int startPos = Math.min(columnPos[i] + 1, row.length());
									int endPos = i == columnPos.length - 2 ? 
											row.length() : 
											Math.min(columnPos[i+1] + 1, row.length());
									String value = row.substring(Math.max(0, startPos), endPos);
									column.add(value.trim());
								}
							}
						}
					}
					return null;
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "summary", e);
		}
		return summary;
	}
	
	public static void printSummary(ValMap map) {
		println(map.toString().replaceAll("\\], ", "\\],\n"));
	}
/*
	SKY COVER									weight
		0/8				CLR-CLEAR					0
		1/8 TO 4/8		SCT-SCATTERED				3
        5/8 TO 7/8		BKN-BROKEN					6
        8/8				OVC-OVERCAST				9
        				OBS-OBSCURED				10
        				POB-PARTIAL OBSCURATION
	avg
		sonnig : 0-2
		leicht bewölkt : 2-5
		aufgelockert : 5-8
		stark bewölkt : 8-10
*/	
	int weight(String cc, String we) {
		int skc = -1;
		MatchResult mr = findFirstIn(cc, Pattern.compile("(\\d+)\\/8"));
		if (mr != null)
			skc = Integer.parseInt(mr.group(1));
		switch (skc) {
		case 0:
			return 0;
		case 1: case 2: case 3: case 4:
			return 3;
		case 5: case 6: case 7:
			return 6;
		case 8:
			return 9;
		default:
			if ("rain".equals(we))
				return 10;
			else
				return -1;
		}
	}
	
	String averaged(ValList list) {
		int n = 0;
		float avg = 0;
		for (Object val : list)
			if (-1 < (int)val) {
				avg += (int)val;
				n++;
			}
		if (n < 1)
			return "";
		avg /= n;
		if (avg < 2)
			return "sonnig";
		else if (avg < 5)
			return "leicht bewölkt";
		else if (avg < 8)
			return "aufgelockert";
		else
			return "stark bewölkt";
	}
	
	public void evaluateSummary(ValMap summary) {
//		printSummary(summary);
		ValMap vormittag = new ValMap();
		ValMap nachmittag = new ValMap();
		ValList list = summary.getList("D");
		if (list == null || list.size() < 1)
			return;
		
		String old = list.get(0).toString(), day = old;
		for (int i = 0; i < list.size(); i++) {
			String hour = summary.getListValue("h", i).toString();
			if ("06Z".compareTo(hour) <= 0 && "19Z".compareTo(hour) >= 0) {
				int weight = weight(
						summary.getListValue("CC", i).toString(), 
						summary.getListValue("WE", i).toString());
				if ("12Z".compareTo(hour) < 0) 
					nachmittag.getList(day).add(weight);
				else
					vormittag.getList(day).add(weight);
			}
			
			if (i < list.size() - 1)
				day = summary.getListValue("D", i + 1).toString();
			else
				day = "";
			if (!old.equals(day)) {
				String description = "";
				String vm = averaged(vormittag.getList(old));
				if (notNullOrEmpty(vm))
					description += String.format("v.m. %s ", vm);
				String nm = averaged(nachmittag.getList(old));
				if (notNullOrEmpty(nm))
					description += String.format("n.m. %s ", nm);
				ValMap values = new ValMap();
				values.put("description", description);
				updateOrInsert(location, toInt(-1, old), values);
				old = day;
			}
		}
//		for (String d : vormittag.keySet()) {
//			list = vormittag.getList(d);
//			println("%s vormittag", d, list, averaged(list));
//		}
//		for (String d : vormittag.keySet()) {
//			list = nachmittag.getList(d);
//			println("%s nachmittag", d, list, averaged(list));
//		}
	}

	void measurements(final String marker) {
		waiting(null, new ComponentFunction<Void>() {
			public Void apply(Component comp, Object[] parms) {
				storeValues(tableAfter(marker, 1));
				storeValues(tableAfter(marker, 2));
				
				return null;
			}
		});
	}
	
	Element tableAfter(String text, int follower) {
		Element table = null, el = null;
    	Elements tables = doc.select("table:contains(" + text + ")");
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
					follower--;
					if (follower > 0)
						continue;
					table = tables.first();
    				break;
    			}
    		}
    	}
    	return table;
 	}

    void storeValues(Element table) {
    	ValMap values = new ValMap();
    	Elements column = table.select("td:eq(0)");
    	boolean precipitation = column.get(1).text().toLowerCase().startsWith("prec");
    	Element row = table.select("tr").first();
		for (int i = 1; i < row.select("td").size(); i++) {
        	column = table.select(String.format("td:eq(%d)", i));
        	int day = Integer.parseInt(column.first().text());
        	if (precipitation)
            	values.put("precipitation", toFloat(Float.NaN, column.get(1).text()));
        	else {
	        	values.put("maxtemp", toFloat(Float.NaN, column.get(1).text()));
	        	values.put("mintemp", toFloat(Float.NaN, column.get(2).text()));
        	}
        	updateOrInsert(location, day, values);
		}
	}
    
    public static class Period
    {
    	public static final int MONTH = -1;
    	
    	public static int year, month, day, length;
    	
    	public static int[] getParts() {
    		return new int[]{year, month, day, length};
    	}
    	
    	public static void setParts(int...parts) {
    		if (parts != null) {
    			for (int i = 0; i < Math.min(4, parts.length); i++) 
    				switch (i) {
    				case 0:	year = parts[i];	break;
    				case 1:	month = parts[i];	break;
    				case 2:	day = parts[i];	break;
    				case 3:	length = parts[i];	break;
    				}
    			if (length > 1) {
    				for (int i = 1; i < length; i++) {
    					long time = dateInMillis(year, month - 1, Period.day, -i);
    	    			int d = getCalendar().get(Calendar.DAY_OF_MONTH);;
						datesInMillis.put(d, time);
					}
    			}
    		}
    	}
        
        public static Long getMillis(int day) {
        	if (length < 2 || day == Period.day)
        		return dateInMillis(year, month - 1, day);
        	else
        		return datesInMillis.get(day);
        }
    	
    	private static HashMap<Integer,Long> datesInMillis = new HashMap<Integer,Long>();
    	private static String key = "weather.period";
    	
    	public static void save() {
    		putSetting(key, Arrays.toString(getParts()));
    	}
        	
        public static void load() {
    		MatchResult[] matches = findAllIn(getSetting(key, ""), Pattern.compile("\\d+"));
    		if (matches.length > 0) {
    			int[] parts = new int[matches.length];
    			for (int i = 0; i < parts.length; i++) {
    				parts[i] = toInt(-1, matches[i].group());
    			}
    			setParts(parts);
    		}
    		else {
    			Calendar cal = getCalendar();
    			cal.setTimeInMillis(now());
    			setParts(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    		}
    	}
     	
        public static String description() {
        	switch (length) {
			case MONTH:
				return formatDate(getMillis(day), DatePicker.monthFormat);
			default:
				return String.format("%d day(s) ending %s", length, formatDate(getMillis(day), DatePicker.calendarFormat));
			}
    	}
    }
    
    String location = "10519";			//	BONN-ROLEBER
	
	public void retrieveWeatherData() {
		Period.load();

		int[] period = DatePicker.pickAPeriod(Period.getParts(), "Weather info : pick day, week or month");
		if (period == null)
			return;
		
		Period.setParts(period);
		Period.save();
		
		new Task<Void>(null, 
			new Job<Void>() {
				public void perform(Void t, Object[] params) throws Exception {
					evaluate(true);
				}
			}) {
				@Override
				protected Void doInBackground() throws Exception {
					parseSite(location, Period.getParts());
					return null;
				}
			}.execute();
	}

	public void evaluate(boolean popup) {
		count = new int[2];
		switch (Period.length) {
		case Period.MONTH:
			measurements("Daily extreme temperatures");
			measurements("24 hours rainfall");
			break;
		default:
			evaluateSummary(summary(popup, Period.description()));
			break;
		}
		BerichtsheftShell.print("%d record(s) updated, %d inserted", count[0], count[1], NEWLINE);
	}

	@Override
	public boolean openConnection(String dbPath, Object... params) {
		try {
			if (super.openConnection(dbPath, arrayextend(params, true, "weathers")))
				return true;
		    
			getStmt().execute("CREATE TABLE weathers (" +
		    		"_id INTEGER PRIMARY KEY," +
		    		"description TEXT," +
		    		"location TEXT," +
		    		"precipitation FLOAT," +
		    		"maxtemp FLOAT," +
		    		"mintemp FLOAT," +
		    		"created INTEGER," +
		    		"modified INTEGER)");

		    return true;
		} catch (Exception e) {
			handleException(e);
			return false;
		}
	}

	public int update(long id, ValMap values) throws Exception {
		String sql = "update weathers set";
		for (Map.Entry<String,Object> entry : values.entrySet())
			sql += " " + entry.getKey() + "='" + entry.getValue().toString() + "',";
		PreparedStatement ps = getCon().prepareStatement(sql + " modified = ? where _id = ?");
		ps.setLong(1, now());
		ps.setLong(2, id);
		return ps.executeUpdate();
	}

	public int insert(long id, ValMap values, String location, long time) throws Exception {
		String keys = "", vals = "";
		for (String key : values.keySet())
			keys += "," + key;
		for (Object value : values.values())
			vals += ",'" + value.toString() + "'";
		String sql = String.format(
				"insert into weathers (_id%s,location,created,modified) VALUES (?%s,?,?,?)", 
				keys, vals);
		PreparedStatement ps = getCon().prepareStatement(sql);
		ps.setLong(1, id);
		ps.setString(2, location);
		ps.setLong(3, time);
		ps.setLong(4, now());
		return ps.executeUpdate();
	}

	public long newId() throws Exception {
		ResultSet rs = getStmt().executeQuery("select max(_id) from weathers");
		long id = rs.next() ? rs.getLong(1) : -1;
		rs.close();
		return 1 + id;
	}

	public long getIdOfDay(String location, long time) throws SQLException {
		long[] interval = dayInterval(time, 1);
		PreparedStatement ps = getCon().prepareStatement(
				"select _id from weathers " +
				"where created between ? and ? and location=? " +
				"order by created, location");
		ps.setLong(1, interval[0]);
		ps.setLong(2, interval[1] - 1);
		ps.setString(3, location);
		ResultSet rs = ps.executeQuery();
		
		long id = -1;
		if (rs.next()) 
			id = rs.getLong(1);
		
		rs.close();
		return id;
	}
	
	private int[] count = new int[2];

	public long updateOrInsert(String location, int day, ValMap values) {
		long time = Period.getMillis(day);
		if (getCon() == null) {
			message("database connection not open");
			BerichtsheftShell.print(location, 
					formatDate(time, DatePicker.calendarFormat, Locale.getDefault()), 
					values, 
					NEWLINE);
			return -1;
		}
		try {
			long id = getIdOfDay(location, time);
			if (id > -1) {
				update(id, values);
				count[0]++;
			}
			else {
				id = newId();
				insert(id, values, location, time);
				count[1]++;
			}
			return id;
		} catch (Exception e) {
			handleException(e);
			return -1;
		}
	}

}
