package com.applang.berichtsheft.ui.components;

import java.util.*;
import java.text.SimpleDateFormat;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import static com.applang.Util.*;

public class DatePicker
{
	int monthOfYear, year;
	int rows = 7, cols = 8;
	String date;
	JButton month = new JButton("");
	JButton[] weeks, days;
	Calendar cal = Calendar.getInstance(Locale.US);
	SimpleDateFormat sdf = new SimpleDateFormat();
	
	Long[] timeLine = null;

	public DatePicker(Component relative, Object... params) {
		date = paramString("", 0, params);
		timeLine = param(null, 1, params);
		String title = paramString("Date Picker", 2, params);
		
		sdf.setCalendar(cal);
		String[] header = { "", "S", "M", "T", "W", "T", "F", "S" };
		
		final JDialog dialog = new JDialog();
		dialog.setModal(true);
		JPanel p1 = new JPanel(new GridLayout(rows, cols));
		p1.setPreferredSize(new Dimension(600, 200));
		final JButton[] buttons = new JButton[rows*cols];

		ArrayList<JButton> w = new ArrayList<JButton>();
		ArrayList<JButton> d = new ArrayList<JButton>();
		for (int x = 0; x < buttons.length; x++) {
			final int selection = x;
			buttons[x] = new JButton();
			buttons[x].setFocusPainted(false);
			buttons[x].setBackground(Color.white);
			if (x < cols) {
				buttons[x].setText(header[x]);
				buttons[x].setForeground(Color.red);
			}
			else {
				buttons[x].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						date = buttons[selection].getActionCommand();
						dialog.dispose();
					}
				});
				
				if (x % cols == 0)
					w.add(buttons[x]);
				else
					d.add(buttons[x]);
			}
			p1.add(buttons[x]);
		}
		weeks = w.toArray(new JButton[]{});
		days = d.toArray(new JButton[]{});
		
		JPanel p2 = new JPanel(new GridLayout(1, 3));
		JButton[] move = new JButton[moves.length];
		move[0] = new JButton(moves[0]);
		move[0].addActionListener(moveMonth);
		move[1] = new JButton(moves[1]);
		move[1].addActionListener(moveMonth);
		move[2] = new JButton(moves[2]);
		move[2].addActionListener(moveMonth);
		move[3] = new JButton(moves[3]);
		move[3].addActionListener(moveMonth);
		
		p2.add(move[0]);
		p2.add(move[1]);
		month.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				date = month.getActionCommand();
				dialog.dispose();
			}
		});
		p2.add(month);
		p2.add(move[2]);
		p2.add(move[3]);
		
		dialog.add(p2, BorderLayout.NORTH);
		dialog.add(p1, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(relative);
		if (relative == null)
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setDateString(date);
		dialog.setTitle(title);
		dialog.setVisible(true);
	}
	
	String[] moves = new String[] {"<<", "<", ">", ">>", };

	ActionListener moveMonth = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			cal.set(year, monthOfYear, 1);
			switch (Arrays.asList(moves).indexOf(e.getActionCommand())) {
			case 0:
				cal.add(Calendar.YEAR, -1);
				break;
			case 1:
				cal.add(Calendar.MONTH, -1);
				break;
			case 2:
				cal.add(Calendar.MONTH, 1);
				break;
			case 3:
				cal.add(Calendar.YEAR, 1);
				break;
			}
			monthOfYear = cal.get(Calendar.MONTH);
			year = cal.get(Calendar.YEAR);
			displayDate();
		}
	};
	
	void displayDate() {
		cal.set(year, monthOfYear, 1);
		sdf.applyPattern(monthFormat);
		weekOfMonthOfFirstDayOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
		month.setText(sdf.format(cal.getTime()));
		month.setToolTipText(month.getText());
		clearDisplay();
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		int weekInYear = 0;
		sdf.applyPattern(weekFormat);
		for (int x = dayOfWeek - 1, day = 1; day <= daysInMonth; x++, day++) {
			setDay(x, day);
			weekInYear = setWeek(weekInYear);
			cal.add(Calendar.DATE, 1);
		}
	}

	private void clearDisplay() {
		for (int x = 0; x < days.length; x++) days[x].setText("");
		for (int x = 0; x < weeks.length; x++) weeks[x].setText("");
	}
	
	int weekOfMonthOfFirstDayOfMonth = 1;

	private int setWeek(int weekInYear) {
		int week = weekInYear;
		weekInYear = cal.get(Calendar.WEEK_OF_YEAR);
		if (week != weekInYear) {
			int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
			long time;
			if (week > weekInYear && weekOfMonth > 1)
				time = timeInMillis(year + 1, Calendar.JANUARY, 1);
			else
				time = cal.getTimeInMillis();
			String format = sdf.format(time);
			weeks[weekOfMonth - weekOfMonthOfFirstDayOfMonth].setText(format);
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
		
		monthOfYear = cal.get(Calendar.MONTH);
		year = cal.get(Calendar.YEAR);
		displayDate();
	}

	private void setCalendar(String text) {
		try {
			switch (kindOfDate(text)) {
			case 1: 	sdf.applyPattern(calendarFormat);	break;
			case 2: 	sdf.applyPattern(weekFormat);	break;
			case 3: 	sdf.applyPattern(monthFormat);	break;
			default:
				int day = Integer.parseInt(date);
				cal.set(year, monthOfYear, day);
				sdf.applyPattern(calendarFormat);
				return;
			}
			
			Date dt = sdf.parse(text);
			cal.setTime(dt);
		} catch (Exception e) {}
	}

	public String getDateString() {
		if (date.equals("") || kindOfDate(date) > 0)
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
		
		if (!notNullOrEmpty(format))
			if (kind > 0 || date.length() < 1)
				return date;
			else 
				format = calendarFormat;
		
		setCalendar(date);
		sdf.applyPattern(format);
		return sdf.format(cal.getTime());
	}

	public static void main(String[] args) {
		long time = now();
		System.out.println(pickADate(time, "", "", 
				new Long[]{time - getMillis(1) - 1, time}));
	}

	public static String pickADate(long time, String format, String title, Long[] timeLine) {
		try {
			String dateString = formatDate(time, calendarFormat, Locale.getDefault());
			return new DatePicker(null, dateString, timeLine, title)
					.getDateString(format);
		} catch (Exception e) {
			return "";
		}
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

	public static int[] parseWeekDate(String dateString) {
		int slash = dateString.indexOf("/");
		int[] parts = new int[2];
		parts[0] = Integer.parseInt(dateString.substring(0, slash));
		parts[1] = Integer.parseInt(dateString.substring(1 + slash));
		if (parts[1] < 100) {
			int year = Calendar.getInstance().get(Calendar.YEAR) % 100;
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
		int blank = dateString.indexOf(" ");
		int[] parts = new int[2];
		parts[0] = Integer.parseInt(dateString.substring(0, blank));
		parts[1] = Integer.parseInt(dateString.substring(1 + blank));
		if (parts[1] < 100) {
			int year = Calendar.getInstance().get(Calendar.YEAR) % 100;
			parts[1] = parts[1] > year ? parts[1] + 1900 : parts[1] + 2000;
		}
		return parts;
	}
	
	public static String monthDate(long[] interval) {
		return formatDate(interval[0], monthFormat);
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
}

