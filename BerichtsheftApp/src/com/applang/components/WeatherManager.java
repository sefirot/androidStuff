package com.applang.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.View;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.applang.PluginUtils;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.berichtsheft.plugin.BerichtsheftShell;
import com.applang.provider.WeatherInfo.Weathers;

import console.Console;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;
import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

public class WeatherManager extends ActionPanel
{
	private static final String TAG = WeatherManager.class.getSimpleName();

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final DataView dataView = new DataView();
		        String title = "WeatherInfo database";
				WeatherManager weatherManager = new WeatherManager(dataView, 
						null,
						title);
				ActionPanel.createAndShowGUI(title, 
						new Dimension(1000, 200), 
						Behavior.EXIT_ON_CLOSE,
						weatherManager, 
						new Function<Component>() {
							public Component apply(Object...params) {
								Component c = findFirstComponent(dataView, "south");
								if (c != null)
									dataView.remove(c);
								return dataView.getUIComponent();
							}
						});
			}
		});
	}

	private JLabel uriLabel;
	private DataView dataView;
	
	public WeatherManager() {
		super(null);
	}

	public WeatherManager(IComponent dataView, Object... params) {
		super(dataView, params);
		this.dataView = (DataView) iComponent;
		
		addButton(this, ActionType.CALENDAR.index(), new InfoAction(ActionType.CALENDAR));
		addButton(this, 4, new InfoAction("Weather"));
		addButton(this, ActionType.DATABASE.index(), new InfoAction(ActionType.DATABASE));
		JPopupMenu popupMenu = newPopupMenu(
			objects(ActionType.IMPORT.description(), new InfoAction(ActionType.IMPORT)),
			objects(ActionType.TEXT.description(), new InfoAction(ActionType.TEXT))
		);
		attachDropdownMenu(buttons[4], popupMenu);
		add(Box.createHorizontalStrut(10));
		add(uriLabel = new JLabel());
	}
	
	@Override
	public void start(Object... params) {
		super.start(params);
		dbFilePath = getSetting("database", "");
	}
	
	@Override
	public void finish(Object... params) {
		if (getCon() != null)
			try {
				updateOnRequest();
				getCon().close();
			} catch (Exception e) {
				handleException(e);
			}
		putSetting("database", dbFilePath);
		super.finish(params);
	}
    
    public class InfoAction extends CustomAction
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
					if (dataView.configureData(null, false)) {
						dbFilePath = dataView.getDataConfiguration().getPath();
						if (notNullOrEmpty(dbFilePath))
							openConnection(dbFilePath);
						uriLabel.setText(dbFilePath);
					}
					break;
				case CALENDAR:
					DatePicker.Period.pick(0);
					break;
				case IMPORT:
					parseAndEvaluate(location, DatePicker.Period.loadParts(0), true, null);
					break;
				default:
					return;
				}
			}
        }
    }
    
    public String location = "10519";			//	BONN-ROLEBER
	
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
		case DatePicker.Period.MONTH:
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
		case DatePicker.Period.MONTH:
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
	
	private Document parseSite(String location, int[] dateParts) {
		Document doc = null;
		switch (dateParts[3]) {
		case DatePicker.Period.MONTH:
			break;
		default:
			dateParts = DatePicker.extendPeriod(1, dateParts);
		}
		Uri uri = siteUri("PA", location, "all", dateParts);
		BerichtsheftPlugin.print("connecting '%s'\n... ", uri);
		long millis = System.currentTimeMillis();
		try {
			doc = Jsoup.connect(uri.toString())
					.timeout(100000)
					.get();
		} catch (Exception e) {
			handleException(e);
		}
		BerichtsheftPlugin.print("%d sec(s)", (System.currentTimeMillis() - millis) / 1000, NEWLINE);
		return doc;
	}
    
	public void parseAndEvaluate(final String location, final int[] period, 
			final boolean popup, 
			final Function<Void> followUp, final Object... params) 
	{
		Boolean async = param_Boolean(true, 1, params);
		if (!async) {
			Document doc = parseSite(location, period);
			evaluate(doc, popup);
			if (followUp != null)
				followUp.apply(params);
		}
		else {
			final Console console = BerichtsheftShell.getConsole(true);
			if (console != null)
				BerichtsheftShell.consoleWait(console, true);
			Task<Document> task = new Task<Document>(null, 
					new Job<Document>() {
				public void perform(Document doc, Object[] params) throws Exception {
					evaluate(doc, popup);
					if (followUp != null)
						followUp.apply(params);
				}
			}, params) 
			{
				@Override
				protected Document doInBackground() {
					try {
						setProgress(1);
						return parseSite(location, period);
					} finally {
						setProgress(100);
					}
				}
			};
			task.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if ("progress" == evt.getPropertyName()) {
						if (console != null) {
							BerichtsheftShell.setBerichtsheftShell(console);
							int progress = (Integer) evt.getNewValue();
							BerichtsheftShell.consoleWait(console, progress < 100);
						}
					} 
				}
			});
			task.execute();
		}
	}
	
	private Document doc = null;

	private ValMap summary(boolean popup, String title) {
		final ValMap summary = vmap();
		View view = BerichtsheftApp.getJEditView();
		try {
			final Object out = popup ? new StringWriter() : "";	
			waiting(view, new ComponentFunction<Void>() {
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
			if (popup) {
				JTextArea textArea = new JTextArea();
				textArea.setFont(monoSpaced());
				textArea.setText(((StringWriter)out).toString());
				Component component = new JScrollPane(textArea);
				component.setPreferredSize(new Dimension(800,400));
				int result = new AlertDialog(view, 
						getProperty("datadock.weather.title"), 
						title, 
						component, 
						JOptionPane.OK_CANCEL_OPTION,
						Behavior.MODAL, 
						loadIcon("datadock.weather.icon"), 
						null).open().getResult();
				if (result != JOptionPane.OK_OPTION)
					return null;
			}
		} catch (Exception e) {
			Log.e(TAG, "summary", e);
		}
//		println(com.applang.Util2.toString(summary));
		return summary;
	}

	public AlertDialog feedableDialog(String title) {
		AlertDialog dialog;
		TextView tv = new TextView(null, null);
		tv.getTaggedComponent().setFont(monoSpaced());
		dialog = new AlertDialog.Builder(BerichtsheftActivity.getInstance(), false)
				.setTitle(title)
				.setView(tv)
				.setNeutralButton(android.R.string.close,
					new OnClickListener() {
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.cancel();
						}
					})
				.create();
		dialog.setModalExclusionType(AlertDialog.ModalExclusionType.APPLICATION_EXCLUDE);
		dialog.open(new Dimension(800, 400));
		return dialog;	
		//	feedableDialog(title).feed(1);
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
	int weight(Object cc, Object we) {
		int skc = -1;
		MatchResult mr = findFirstIn(stringValueOf(cc), Pattern.compile("(\\d+)\\/8"));
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
			if (-1 < (Integer)val) {
				avg += (Integer)val;
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
	
	public static Pattern PRECIPITATION_PATTERN = clippingPattern("\\(", "\\)");
	
	private void evaluateSummary(ValMap summary) {
		if (summary == null)
			return;
		ValMap vormittag = vmap(), nachmittag = vmap();
		BidiMultiMap precip = bmap(3);
		ArrayList<Double> temps = alist();
		ValList list = summary.getList("D");
		if (notAvailable(0, list))
			return;
		String old = list.get(0).toString(), day = old;
		for (int i = 0; i < list.size(); i++) {
			String hour = summary.getListValue("h", i).toString();
			Long instant = pointInTime(day, stripUnits(hour));
			if ("06Z".compareTo(hour) <= 0 && "19Z".compareTo(hour) >= 0) {
				int weight = weight(
						summary.getListValue("CC", i), 
						summary.getListValue("WE", i));
				if ("12Z".compareTo(hour) < 0) 
					nachmittag.getList(day).add(weight);
				else
					vormittag.getList(day).add(weight);
			}
			Object oT = summary.getListValue("T", i);
			oT = stripUnits(oT.toString());
			temps.add(toDouble(Double.NaN, oT.toString()));
			Object oPR = summary.getListValue("PR", i);
			if (notNullOrEmpty(oPR)) {
				MatchResult[] mr = findAllIn(oPR.toString(), PRECIPITATION_PATTERN);
				if (mr.length > 0) {
					Integer hours = toInt(0, stripUnits(mr[0].group(2)));
					precip.add(instant, 
							hoursFromDate(instant, -hours), 
							toDouble(0.1, mr[0].group(1)));
				}
			}
			if (i < list.size() - 1)
				day = summary.getListValue("D", i + 1).toString();
			else
				day = "";
			if (!old.equals(day)) {
				int oldDay = toInt(-1, old);
				Long time = DatePicker.Period.getMillis(oldDay);
				if (time != null) {
					String description = "";
					String vm = averaged(vormittag.getList(old));
					if (notNullOrEmpty(vm))
						description += String.format("v.m. %s ", vm);
					String nm = averaged(nachmittag.getList(old));
					if (notNullOrEmpty(nm))
						description += String.format("n.m. %s ", nm);
					ValMap values = vmap();
					values.put(Weathers.DESCRIPTION, description);
					values.put(Weathers.PRECIPITATION, interpolate(old, precip));
					values.put(Weathers.MAXTEMP, Collections.max(temps));
					values.put(Weathers.MINTEMP, Collections.min(temps));
					if (null == updateOrInsert(location, time, values))
						break;
				}
				old = day;
				temps = alist();
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
	
	private double interpolate(String day, BidiMultiMap precip) {
		double value = 0.0;
		long[] interval = intervalInTime(day);
		timeLine = precip.getKeys().toArray(new Long[0]);
//		println("timeLine", formatDates(timeLine));
//		println("interval", formatDates(interval));
		int[] index = ints(
			index(interval[0]), index(interval[1])
		);
		long[] old = null;
		for (int i = index[1]; i < index[0]; i++) {
			ValList rec = precip.get(i);
			long[] ival = interval(rec);
			double precipitation = (Double)rec.get(2);
			double contrib = contribution(interval, ival, precipitation);
			double isect = old == null ? 0.0 :  
				intersection(ival, old);
			double portion = (1.0 - isect) * contrib;
//			println(formatDates(ival), precipitation, contrib, portion);
			value += portion;
			old = ival;
		}
//		println("precipitation on %s : %f", formatDate(epoch), value);
		return value;
	}
	
	private double contribution(long[] interval, long[] ival, double value) {
		long upper = Math.min(interval[1], ival[1]);
		long lower = Math.max(interval[0], ival[0]);
		double portion = 1.0 * (upper - lower) / (ival[1] - ival[0]);
		return portion * value;
	}
	
	private long[] interval(ValList rec) {
		return new long[]{
			(Long)rec.get(1), (Long)rec.get(0)
		};
	}
	
	private double intersection(long[] ival1, long[] ival2) {
		if (ival2[0] >= ival1[1] || ival2[1] <= ival1[0])
			return 0.0;
		else if (ival2[1] >= ival1[1] && ival2[0] <= ival1[0])
			return 1.0;
		else if (ival1[1] >= ival2[1] && ival1[0] <= ival2[0])
			return 1.0;
		else if (ival2[1] >= ival1[1])
			return 1.0 * (ival1[1] - ival2[0]) / (ival2[1] - ival2[0]);
		else
			return 1.0 * (ival2[1] - ival1[0]) / (ival1[1] - ival1[0]);
	}
	
	private int index(long epoch) {
		return pointer(criterion(epoch));
	}
	
	Long[] timeLine;
	
	private int criterion(long epoch) {
		return Arrays.binarySearch(timeLine, epoch, new Comparator<Long>() {
			@Override
			public int compare(Long l1, Long l2) {
				if (l1 < l2)
					return 1;
				else if (l1 > l2)
					return -1;
				else
					return 0;
			}
		});
	}
	
	private int pointer(int crit) {
		if (crit < 0)
			crit = -crit - 1;
		return crit;
	}

	private long[] intervalInTime(String day) {
		Long epoch = DatePicker.Period.getMillis(toInt(-1, day));
		if (epoch == null)
			return null;
		else
			return dayInterval(epoch, 1);
	}

	private Long pointInTime(String day, String hour) {
		return DatePicker.Period.getMillisByHour(toInt(-1, day), toInt(0, hour));
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
    	ValMap values = vmap();
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
    		long time = DatePicker.Period.getMillis(day);
			if (null == updateOrInsert(location, time, values))
				break;
		}
	}

	private void evaluate(Document doc, boolean popup) {
		this.doc = doc;
		count = ints(0,0);
		switch (DatePicker.Period.length) {
		case DatePicker.Period.MONTH:
			measurements("Daily extreme temperatures");
			measurements("24 hours rainfall");
			break;
		default:
			evaluateSummary(summary(popup, DatePicker.Period.description()));
			break;
		}
		if (count[0] > 0 || count[1] > 0)
			BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message", 
				count[1], count[0]);
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

	public Object updateOrInsert(String location, long time, ValMap values) {
		if (getCon() == null) {
			message("database connection not open");
			BerichtsheftPlugin.print(location, 
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
