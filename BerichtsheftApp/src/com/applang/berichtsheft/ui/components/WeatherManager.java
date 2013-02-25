package com.applang.berichtsheft.ui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.swing.JPopupMenu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.applang.SwingUtil;
import com.applang.Util;
import com.applang.Util2;

public class WeatherManager extends ToolPanel implements ActionListener
{
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
		
		ToolPanel.createAndShowGUI(title, new Dimension(1000, 200), weatherManager, textArea.textArea);
	}

	public WeatherManager(TextComponent textArea, Object... params) {
		super(textArea, params);
		
		addButton(3, new InfoAction("Options"));
		
	    JPopupMenu popupMenu = SwingUtil.newPopupMenu(
	    	new Object[] {ToolType.IMPORT.description(), new InfoAction(ToolType.IMPORT)} 
	    );
	    
	    SwingUtil.attachDropdownMenu(buttons[3], popupMenu);
	}
	
	@Override
	protected void finish(Object... params) {
		try {
			updateOnRequest(true);
			getCon().close();
		} catch (Exception e) {
			SwingUtil.handleException(e);
		}
	}
    
    public class InfoAction extends SwingUtil.Action
    {
		public InfoAction(ToolType type) {
			super(type);
        }
        
		public InfoAction(String text) {
			super(text);
        }
        
        @Override
        protected void action_Performed(ActionEvent ae) {
        	ToolType t = (ToolType)getType();
        	if (t != null) {
				switch (t) {
				case IMPORT:
					dbName = chooseDatabase(dbName);
					if (Util.notNullOrEmpty(dbName) && openConnection(dbName))
						retrieveWeatherData();
					break;
				default:
					return;
				}
			}
        }
    }
	
	int year, month;
	String location;
	
	private static final String SYNOP_MONTH = "http://www.mundomanz.com/meteo_p/monthrep?" +
			"countr=GERMANY&" +
			"ind=%s&" +
			"year=%s&" +
			"month=%s&" +
			"l=1&" +
			"action=display";
	
	String urlString(boolean broken) {
		String monthString = "0" + this.month;
		monthString = monthString.substring(monthString.length() - 2, monthString.length());
		String url = String.format(SYNOP_MONTH, 
				this.location, 
				"" + this.year, 
				monthString);
		if (broken)
			url = url.replaceAll("(\\?|\\&)", "$1\n");
		return url;
	}
	
	public void connectWeatherURL(String location, int year, int month) {
		this.year = year;
		this.month = month;
		this.location = location;
		SwingUtil.waiting(this.getParent(), new SwingUtil.ComponentFunction<Void>() {
			public Void apply(Component comp, Object[] parms) {
				try {
					doc = Jsoup.connect(urlString(false))
							.timeout(100000)
							.get();
				} catch (Exception e) {
					SwingUtil.handleException(e);
					doc = null;
				}
				
				return null;
			}
		});
	}
	
	Document doc = null;

	void measurements(final String marker) {
		SwingUtil.waiting(this.getParent(), new SwingUtil.ComponentFunction<Void>() {
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
    	Util.ValMap values = new Util.ValMap();
    	Elements column = table.select("td:eq(0)");
    	boolean precipitation = column.get(1).text().toLowerCase().startsWith("prec");
    	Element row = table.select("tr").first();
		for (int i = 1; i < row.select("td").size(); i++) {
        	column = table.select(String.format("td:eq(%d)", i));
        	int day = Integer.parseInt(column.first().text());
        	long time = Util.timeInMillis(year, -month + 1, day);
        	if (precipitation)
            	values.put("precipitation", Util.toFloat(Float.NaN, column.get(1).text()));
        	else {
	        	values.put("maxtemp", Util.toFloat(Float.NaN, column.get(1).text()));
	        	values.put("mintemp", Util.toFloat(Float.NaN, column.get(2).text()));
        	}
        	updateOrInsert(location, time, values);
		}
	}
	
	public void retrieveWeatherData() {
		long time = Util.now();
		String format = "MM yyyy";
		do {
			String dateString = DatePicker.pickADate(time, format,
					"Pick month for weather info", null);
			if (dateString.length() < 1)
				return;
			
			int sep = dateString.indexOf(' ');
			connectWeatherURL("10519", 
					Integer.parseInt(dateString.substring(sep + 1)), 
					Integer.parseInt(dateString.substring(0, sep)));
			
			if (SwingUtil.question(String.format("Retrieve weather data from \n'%s' ?", urlString(true)))) {
				measurements("Daily extreme temperatures");
				measurements("24 hours rainfall");
			}
			
			time = Util.parseDate(dateString, format).getTime();
		} while (SwingUtil.question("more"));
	}

	@Override
	public boolean openConnection(String dbPath, Object... params) {
		try {
			if (super.openConnection(dbPath, Util2.arrayextend(params, true, "weathers")))
				return true;
		    
		    stmt.execute("CREATE TABLE weathers (" +
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
			SwingUtil.handleException(e);
			con = null;
			return false;
		}
	}

	public int update(long id, Util.ValMap values) throws Exception {
		String sql = "update weathers set";
		for (Map.Entry<String,Object> entry : values.entrySet())
			sql += " " + entry.getKey() + "='" + entry.getValue().toString() + "',";
		PreparedStatement ps = con.prepareStatement(sql + " modified = ? where _id = ?");
		ps.setLong(1, Util.now());
		ps.setLong(2, id);
		return ps.executeUpdate();
	}

	public int insert(long id, Util.ValMap values, String location, long time) throws Exception {
		String keys = "", vals = "";
		for (String key : values.keySet())
			keys += "," + key;
		for (Object value : values.values())
			vals += ",'" + value.toString() + "'";
		String sql = String.format(
				"insert into weathers (_id%s,location,created,modified) VALUES (?%s,?,?,?)", 
				keys, vals);
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setLong(1, id);
		ps.setString(2, location);
		ps.setLong(3, time);
		ps.setLong(4, Util.now());
		return ps.executeUpdate();
	}

	public long newId() throws Exception {
		ResultSet rs = con.createStatement().executeQuery("select max(_id) from weathers");
		long id = rs.next() ? rs.getLong(1) : -1;
		rs.close();
		return 1 + id;
	}

	public long getIdOfDay(String location, long time) throws SQLException {
		long[] interval = Util.dayInterval(time, 1);
		PreparedStatement ps = con.prepareStatement(
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

	public long updateOrInsert(String location, long time, Util.ValMap values) {
		try {
			long id = getIdOfDay(location, time);
			if (id > -1) 
				update(id, values);
			else {
				id = newId();
				insert(id, values, location, time);
			}
			return id;
		} catch (Exception e) {
			SwingUtil.handleException(e);
			return -1;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
