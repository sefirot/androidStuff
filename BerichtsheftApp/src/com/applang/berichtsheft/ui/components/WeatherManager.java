package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.applang.Util;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;

public class WeatherManager extends ToolPanel
{
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		BerichtsheftTextArea textArea = new BerichtsheftTextArea();
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JLabel label = new JLabel("");
        label.setName("mess");
        bar.add(label);
		
        String title = "WeatherInfo database";
		final WeatherManager weatherManager = new WeatherManager(textArea, 
				null,
				title, label);
		
        JFrame frame = new JFrame(title) {
			protected void processWindowEvent(WindowEvent we) {
				if (we.getID() == WindowEvent.WINDOW_CLOSING) 
					try {
						weatherManager.updateOnRequest(true);
						weatherManager.getCon().close();
					} catch (Exception e) {
						Util.handleException(e);
					}
				
				super.processWindowEvent(we);
			}
        };
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container contentPane = frame.getContentPane();
		contentPane.setPreferredSize(new Dimension(1000, 200));

		JScrollPane scroll = new JScrollPane(textArea.textArea);
		contentPane.add(scroll, BorderLayout.CENTER);

		contentPane.add(bar, BorderLayout.PAGE_END);
		weatherManager.addToContainer(contentPane, BorderLayout.PAGE_START);
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public WeatherManager(TextComponent textArea, Object... params) {
		super(textArea, params);
		
		addButton(3, new InfoAction(ActionType.IMPORT));
	}
    
    public class InfoAction extends ToolAction
    {
		public InfoAction(ActionType type) {
			super(type);
        }
        
        @Override
        protected void action_Performed(ActionEvent ae) {
        	switch (type) {
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
		Util.waiting(this.getParent(), new Util.ComponentFunction<Void>() {
			public Void apply(Component comp, Object[] parms) {
				try {
					doc = Jsoup.connect(urlString(false))
							.timeout(100000)
							.get();
				} catch (Exception e) {
					Util.handleException(e);
					doc = null;
				}
				
				return null;
			}
		});
	}
	
	Document doc = null;

	void measurements(final String marker) {
		Util.waiting(this.getParent(), new Util.ComponentFunction<Void>() {
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
            	values.put("precipitation", parseFloat(column.get(1).text()));
        	else {
	        	values.put("maxtemp", parseFloat(column.get(1).text()));
	        	values.put("mintemp", parseFloat(column.get(2).text()));
        	}
        	updateOrInsert(location, time, values);
		}
	}

	private float parseFloat(String text) {
		try {
			return Float.parseFloat(text);
		} catch (NumberFormatException e) {
			return Float.NaN;
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
			
			if (Util.question(String.format("Retrieve weather data from \n'%s' ?", urlString(true)))) {
				measurements("Daily extreme temperatures");
				measurements("24 hours rainfall");
			}
			
			time = Util.parseDate(dateString, format).getTime();
		} while (Util.question("more"));
	}

	public boolean openConnection(Object... params) {
		ResultSet rs = null;
		try {
			if (con != null && !con.isClosed())
				con.close();
			
			String driver = Util.paramString("org.sqlite.JDBC", 2, params);
			Class.forName(driver);
			
			String db = Util.paramString(dbName, 0, params);
			String scheme = Util.paramString("sqlite", 1, params);
			boolean memoryDb = "sqlite".equals(scheme) && db == null;
			
			String url = "jdbc:" + scheme + ":" + (memoryDb ? "" : db);
			con = DriverManager.getConnection(url);
			stmt = con.createStatement();
			
			String database = Util.paramString("sqlite_master", 3, params);
			rs = stmt.executeQuery("select name from " + database + " where type = 'table'");
			
		    while (rs.next()) 
		        if (rs.getString(1).equals("weathers")) 
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
			Util.handleException(e);
			con = null;
			return false;
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
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
			Util.handleException(e);
			return -1;
		}
	}

}
