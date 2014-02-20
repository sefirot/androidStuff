package com.applang.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import android.util.Log;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class DatePicker
{
	private static final String TAG = DatePicker.class.getSimpleName();
	public static int modality = Behavior.MODAL;

	public static void main(String[] args) {
		modality |= Behavior.EXIT_ON_CLOSE;
		long time = now();
		println(pickADate(time, "", "", timeLine(time, 1)));
	}
	
	class PickButton extends JButton
	{
		public PickButton() {
	        this(null, null, 0,0,0,0);
		}

		public PickButton(String text) {
	        this(text, null, 6,0,6,0);
		}
	
		public PickButton(String text, Icon icon, int...tlbr) {
			super(text, icon);
			adjustButtonSize(this, tlbr);
		}
	}

	int monthOfYear, year;
	int rows = 7, cols = 8;
	String date;
	PickButton month = new PickButton("");
	PickButton[] weeks, days;
	SimpleDateFormat sdf = new SimpleDateFormat();
	Long[] timeLine = null;
	Font font = monoSpaced(Font.BOLD);

	public DatePicker(Component relative, Object... params) {
		date = param_String("", 0, params);
		timeLine = param(null, 1, params);
		String title = param_String("Date Picker", 2, params);
		final boolean cancelable = param_Boolean(false, 3, params);
		sdf.setCalendar(getCalendar());
		showDialog(null, relative, 
	    		title, 
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						JDialog dialog = (JDialog) comp;
						JPanel[] panels = panels(dialog);
						dialog.add(panels[1], BorderLayout.NORTH);
						if (cancelable)
							dialog.add(panels[2], BorderLayout.SOUTH);
						dialog.add(panels[0], BorderLayout.CENTER);
						return null;
					}
	    		},
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						setDateString(date);
						month.requestFocusInWindow();
						return null;
					}
	    		},
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						if (cancelable && !dateChanged)
							setDate("");
						return null;
					}
	    		},
	    		modality);
	}
	
	public JPanel[] panels(final JDialog dialog) {
		JPanel p1 = new JPanel(new GridLayout(rows, cols));
		p1.setPreferredSize(new Dimension(400, 200));
		final PickButton[] buttons = new PickButton[rows*cols];
		String[] header = { "", "S", "M", "T", "W", "T", "F", "S" };
		ArrayList<PickButton> w = alist();
		ArrayList<PickButton> d = alist();
		for (int x = 0; x < buttons.length; x++) {
			final int selection = x;
			buttons[x] = new PickButton();
			buttons[x].setFocusPainted(false);
			buttons[x].setBorderPainted(false);
			buttons[x].setBackground(Color.white);
			if (x < cols) {
				buttons[x].setText(header[x]);
				buttons[x].setForeground(Color.red);
			}
			else {
				buttons[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						setDate(buttons[selection].getActionCommand());
						dialog.setVisible(false);
					}
				});
				
				if (x % cols == 0)
					w.add(buttons[x]);
				else
					d.add(buttons[x]);
			}
			p1.add(buttons[x]);
		}
		weeks = w.toArray(new PickButton[]{});
		days = d.toArray(new PickButton[]{});
		JPanel p2 = new JPanel(new GridLayout(1, 5));
		move[0] = new PickButton(moves[0]);
		move[0].addActionListener(moveMonth);
		move[1] = new PickButton(moves[1]);
		move[1].addActionListener(moveMonth);
		move[2] = new PickButton(moves[2]);
		move[2].addActionListener(moveMonth);
		move[3] = new PickButton(moves[3]);
		move[3].addActionListener(moveMonth);
		p2.add(move[0]);
		p2.add(move[1]);
		month.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setDate(month.getActionCommand());
				dialog.setVisible(false);
			}
		});
		p2.add(month);
		p2.add(move[2]);
		p2.add(move[3]);
		JPanel p3 = new JPanel(new GridLayout(1, 2));
		p3.add(new JLabel("number of days (into the past)"));
		final JFormattedTextField tf = new JFormattedTextField("" + ndays);
		tf.setFont(font);
		tf.setForeground(Color.BLUE);
		tf.setHorizontalAlignment(JFormattedTextField.CENTER);
		tf.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				ndays = toInt(ndays, tf.getText());
			}
		});
		p3.add(tf);
		return new JPanel[] {p1,p2,p3};
	}
	
	private boolean dateChanged = false;

	public void setDate(String date) {
		this.date = date;
		dateChanged = true;
	}
	
	String[] moves = strings("<<", "<", ">", ">>");
	PickButton[] move = new PickButton[moves.length];

	ActionListener moveMonth = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			getCalendar().set(year, monthOfYear, 1);
			switch (asList(moves).indexOf(e.getActionCommand())) {
			case 0:
				getCalendar().add(Calendar.YEAR, -1);
				break;
			case 1:
				getCalendar().add(Calendar.MONTH, -1);
				break;
			case 2:
				getCalendar().add(Calendar.MONTH, 1);
				break;
			case 3:
				getCalendar().add(Calendar.YEAR, 1);
				break;
			}
			monthOfYear = getCalendar().get(Calendar.MONTH);
			year = getCalendar().get(Calendar.YEAR);
			displayDate();
		}
	};
	
	void displayDate() {
		getCalendar().set(year, monthOfYear, 1);
		weekOfMonthOfFirstDayOfMonth = getCalendar().get(Calendar.WEEK_OF_MONTH);
		month.setForeground(Color.BLUE);
		Date time = getCalendar().getTime();
		sdf.applyPattern("MMM yyyy");
		month.setText(sdf.format(time));
		sdf.applyPattern(monthFormat);
		month.setToolTipText(sdf.format(time));
		clearDisplay();
		int dayOfWeek = getCalendar().get(Calendar.DAY_OF_WEEK);
		int daysInMonth = getCalendar().getActualMaximum(Calendar.DAY_OF_MONTH);
		int weekInYear = 0;
		sdf.applyPattern(weekFormat);
		for (int x = dayOfWeek - 1, day = 1; day <= daysInMonth; x++, day++) {
			setDay(x, day);
			weekInYear = setWeek(weekInYear);
			getCalendar().add(Calendar.DATE, 1);
		}
	}

	private void clearDisplay() {
		for (int x = 0; x < days.length; x++) days[x].setText("");
		for (int x = 0; x < weeks.length; x++) weeks[x].setText("");
	}
	
	int weekOfMonthOfFirstDayOfMonth = 1;

	private int setWeek(int weekInYear) {
		int week = weekInYear;
		weekInYear = getCalendar().get(Calendar.WEEK_OF_YEAR);
		if (week != weekInYear) {
			int weekOfMonth = getCalendar().get(Calendar.WEEK_OF_MONTH);
			long time;
			if (week > weekInYear && weekOfMonth > 1)
				time = timeInMillis(year + 1, Calendar.JANUARY, 1);
			else
				time = getCalendar().getTimeInMillis();
			String format = sdf.format(time);
			PickButton btn = weeks[weekOfMonth - weekOfMonthOfFirstDayOfMonth];
			btn.setForeground(Color.BLUE);
			btn.setText(format);
		}
		return weekInYear;
	}

	private void setDay(int x, int day) {
		days[x].setText("" + day);
		boolean flag = false;
		int crit = -1;
		if (timeLine != null) {
			long time = timeInMillis(year, -monthOfYear, day);
			crit = Arrays.binarySearch(timeLine, time);
			if (crit < 0) {
				crit = -crit - 2; 
				flag = crit > -1 && crit < timeLine.length - 1;
			}
			else
				flag = true;
		}
		days[x].setForeground(flag ? 
				Color.GREEN : 
				Color.BLACK);
	}

	public void setDateString(String text) {
		setCalendar(text);
		
		monthOfYear = getCalendar().get(Calendar.MONTH);
		year = getCalendar().get(Calendar.YEAR);
		displayDate();
	}

	private void setCalendar(String text) {
		try {
			switch (kindOfDate(text)) {
			case 1: 	sdf.applyPattern(calendarFormat);	break;
			case 2: 	sdf.applyPattern(weekFormat);	break;
			case 3: 	sdf.applyPattern(monthFormat);	break;
			default:
				int day = Integer.parseInt(text);
				getCalendar().set(year, monthOfYear, day);
				sdf.applyPattern(calendarFormat);
				return;
			}
			
			Date dt = sdf.parse(text);
			getCalendar().setTime(dt);
		} catch (Exception e) {}
	}

	public String getDateString() {
		if (date.length() < 1 || kindOfDate(date) > 0)
			return date;
		else
			return getDateString(calendarFormat);
	}

	private String getDateString(String format) {
		int kind = kindOfDate(date);
		if (weekFormat.equals(format) && kind == 2)
			return date;
		if (monthFormat.equals(format) && kind == 3)
			return date;
		if (calendarFormat.equals(format) && kind == 1)
			return date;
		
		if (nullOrEmpty(format))
			if (kind > 0 || date.length() < 1)
				return date;
			else 
				format = calendarFormat;
		
		setCalendar(date);
		sdf.applyPattern(format);
		return sdf.format(getCalendar().getTime());
	}

	public static String pickADate(long time, String format, String title, Long...timeLine) {
		try {
			String dateString = formatDate(time, calendarFormat, Locale.getDefault());
			return new DatePicker(null, dateString, timeLine, title)
					.getDateString(format);
		} catch (Exception e) {
			Log.e(TAG, "pickADate", e);
		}
		return "";
	}
	
	public static Long[] timeLine(long time, int days) {
		ValList list = vlist();
		for (int i = days; i > 0; i--) {
			list.add(time - getMillis(i) + 1);
		}
		list.add(time);
		return list.toArray(new Long[0]);
	}

	public static int[] pickAPeriod(int[] period, String title) {
		try {
			long time = dateInMillis(period[0], period[1] - 1, period[2]);
			ndays = Math.max(1, period[3]);
			String dateString = formatDate(time, calendarFormat, Locale.getDefault());
			dateString = new DatePicker(null, dateString, timeLine(time, ndays), title, true)
					.getDateString("");
			if (dateString.length() > 0)
				return parsePeriod(dateString);
		} catch (Exception e) {
			Log.e(TAG, "pickAPeriod", e);
		}
		return null;
	}
	
	public static final String monthFormat = "MMMM yyyy";
	public static final String weekFormat = "w/yy";
	public static final String calendarFormat = "EEEEE, dd.MMM.yyyy";
	
	public static int kindOfDate(String dateString) {
		if (isWeekDate(dateString))
			return 2;
		else if (isCalendarDate(dateString))
			return 1;
		else if (isMonthDate(dateString))
			return 3;
		else
			return 0;
	}

	public static boolean isCalendarDate(String dateString) {
		int slash = dateString.indexOf(",");
		return slash > -1 && slash < dateString.length() - 2 && slash == dateString.lastIndexOf(",");
	}

	public static boolean isWeekDate(String dateString) {
		int slash = dateString.indexOf("/");
		return slash > -1 && slash < dateString.length() - 1 && slash == dateString.lastIndexOf("/");
	}

	public static boolean isMonthDate(String dateString) {
		int slash = dateString.indexOf(" ");
		return slash > -1 && slash < dateString.length() - 1 && slash == dateString.lastIndexOf(" ");
	}

	public static int[] parseCalendarDate(String dateString) {
		return getCalendarDate(toTime(dateString, calendarFormat, Locale.getDefault()));
	}

	public static int[] parseWeekDate(String dateString) {
		int slash = dateString.indexOf("/");
		if (slash < 0)
			return null;
		int[] parts = new int[2];
		parts[0] = Integer.parseInt(dateString.substring(0, slash));
		parts[1] = Integer.parseInt(dateString.substring(1 + slash));
		if (parts[1] < 100) {
			int year = getCalendarDate(now())[2] % 100;
			parts[1] = parts[1] > year ? parts[1] + 1900 : parts[1] + 2000;
		}
		return parts;
	}
	
	public static String weekDate(long[] interval) {
		int[] val0 = parseWeekDate(formatDate(interval[0], weekFormat));
		int[] val1 = parseWeekDate(formatDate(interval[1], weekFormat));
		if (val1[0] - val0[0] == 1 && val0[1] != val1[1])
			return val0[0] + "/" + val1[1] % 100;
		else
			return val0[0] + "/" + val0[1] % 100;
	}

	public static int[] parseMonthDate(String dateString) {
		int[] parts = new int[2];
		int year = getCalendar().get(Calendar.YEAR) % 100;
		try {
			Date date = new SimpleDateFormat(monthFormat, Locale.getDefault()).parse(dateString);
			getCalendar().setTime(date);
			parts[0] = getCalendar().get(Calendar.MONTH);
			parts[1] = getCalendar().get(Calendar.YEAR);
		} catch (ParseException e) {
			return null;
		}
		if (parts[1] < 100) {
			parts[1] = parts[1] > year ? parts[1] + 1900 : parts[1] + 2000;
		}
		return parts;
	}
	
	public static String monthDate(long[] interval) {
		return formatDate(interval[0], monthFormat);
	}

	private static int ndays = 1;
	/**
	 * @param dateString	like from {@link #DatePicker}
	 * @return	an int array [year, month, day, number of days] or null if dateString is not recognized
	 */
	public static int[] parsePeriod(String dateString) {
		int[] parts = null;
		int kind = DatePicker.kindOfDate(dateString);
		int days;
		switch(kind) {
		case 1:
			parts = DatePicker.parseCalendarDate(dateString);
			days = ndays;
			break;
		case 2:
			long[] interval = weekInterval(dateString, 1);
			parts = getCalendarDate(interval[1] - getMillis(1));
			days = 7;
			break;
		case 3:
			parts = DatePicker.parseMonthDate(dateString);
			days = -1;
			break;
		default:
			return parts;
		}
		arrayreverse(parts);
		List<Integer> list = fromIntArray(parts);
		switch(kind) {
		case 3:
			list.add(1);
			break;
		}
		list.set(1, list.get(1) + 1);
		list.add(days);
		return toIntArray(list);
	}
	
	public static int[] extendPeriod(int days, int...period) {
		dateInMillis(period[0], period[1] - 1, period[2], days);
		period[0] = getCalendar().get(Calendar.YEAR);
		period[1] = getCalendar().get(Calendar.MONTH) + 1;
		period[2] = getCalendar().get(Calendar.DAY_OF_MONTH);
		period[3] += days;
		return period;
	}
	
	public static long[] dayInterval(String dateString, int days) {
		Long date = toTime(dateString, calendarFormat);
		return com.applang.Util.dayInterval(date, days);
	}
	
	public static long[] weekInterval(String dateString, int weeks) {
		Date date = toDate(dateString, weekFormat);
		return weekInterval(date, weeks);
	}
	
	private static long[] weekInterval(Date date, int weeks) {
		if (date == null)
			return null;
		else
			return com.applang.Util.weekInterval(date, weeks);
	}
	
	public static long[] monthInterval(String dateString, int months) {
		Date date = toDate(dateString, monthFormat);
		long[] interval = monthInterval(date, months);
		return new long[] {interval[0], interval[1], interval[1]};
	}
	
	private static long[] monthInterval(Date date, int months) {
		if (date == null)
			return null;
		else
			return com.applang.Util.monthInterval(date, months);
	}
	
	public static long[] toInterval(String dateString, int size) {
		switch (kindOfDate(dateString)) {
		case 3:
			return monthInterval(dateString, size);
		case 2:
			return weekInterval(dateString, size);
		case 1:
			return dayInterval(dateString, size);
		default:
			return com.applang.Util.dayInterval(toTime(dateString), size);
		}
	}
	
	public static long[] nextWeekInterval(String dateString) {
		long[] week = weekInterval(dateString, 2);
		return com.applang.Util.weekInterval(new Date(week[1]), -1);
	}
	
	public static long[] previousWeekInterval(String dateString) {
		return weekInterval(dateString, -1);
	}
	
	public static class Period
	{
		public static String[] periods = strings("weather.period","berichtsheft.period");
		public static int year, month, day, length;
		public static final int MONTH = -1;
		
		public static int[] getParts() {
			return ints(year, month, day, length);
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
				datesInMillis.removeAll();
				for (int i = 0; i < length; i++) {
					long time = dateInMillis(year, month - 1, Period.day, -i);
	    			int d = getCalendar().get(Calendar.DAY_OF_MONTH);
					datesInMillis.add(d, time);
				}
			}
		}
		
		private static BidiMultiMap datesInMillis = new BidiMultiMap();
		
		public static void save(int no) {
			putSetting(periods[no], Arrays.toString(getParts()));
			Settings.save();
		}
    	
	    public static void load(int no) {
			String string = getSetting(periods[no], "");
			MatchResult[] matches = findAllIn(string, Pattern.compile("\\d+"));
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
    	
	    public static int[] loadParts(int no) {
	    	load(no);
	    	return getParts();
		}
	 	
	    public static void saveParts(int no, int[] parts) {
			Period.setParts(parts);
			Period.save(no);
		}
		
		public static String weekDate() {
			if (notAvailable(0, datesInMillis.getKeys()))
				return "";
			Long time = (Long) datesInMillis.getValues().get(-1);
			long[] interval = weekInterval(new Date(time), 1);
			return DatePicker.weekDate(interval);
		}
	    
		// NOTE used in scripts
	    public static Long getMillis(int day) {
	    	if (length < 1)
	    		return dateInMillis(year, month - 1, day);
	    	else
	    		return datesInMillis.getValue(day);
	    }
		
		public static long getMillisByHour(int day, int hour) {
			long epoch = dateInMillis(year, month - 1, day);
			return hoursFromDate(epoch, hour);
	    }
		
		public static long[] getInterval() {
			if (isAvailable(0, datesInMillis.getKeys())) 
				return new long[] {
					(Long)datesInMillis.getValues().get(-1),
					(Long)datesInMillis.getValues().get(0)
				};
			return null;
		}
	 	
		// NOTE used in scripts
	    public static String description() {
	    	switch (length) {
			case MONTH:
				return formatDate(getMillis(day), monthFormat);
			default:
				if (isAvailable(0, datesInMillis.getKeys())) {
					Long time = (Long) datesInMillis.getValues().get(-1);
					return String.format("%d day(s) starting %s", length,
							formatDate(time, calendarFormat));
				}
				else
					return formatDate(getMillis(day), calendarFormat);
			}
		}
	 	
		// NOTE used in scripts
	    public static String getDescription(int no) {
			Period.load(no);
			return description();
		}
	 	
		// NOTE used in scripts
	    public static boolean pick(int no) {
			Period.load(no);
	
			int[] period = pickAPeriod(Period.getParts(), "Pick day, week or month");
			if (period == null)
				return false;
			
			Period.saveParts(no, period);
			return true;
		}
	}

}

