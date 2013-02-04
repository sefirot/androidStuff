package com.applang.berichtsheft.ui.components;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.applang.Util;

public class DatePicker
{
	public static final String monthFormat = "MMMM yyyy";
	public static final String weekFormat = "w/yy";
	public static final String dateFormat = "EEEEE, dd.MMM.yyyy";
	
	SimpleDateFormat sdf = new SimpleDateFormat();
	int month, year;
	int rows = 7, cols = 8;
	JLabel lbl = new JLabel("", JLabel.CENTER);
	String date;
	JButton[] weeks, days;
	Calendar cal = Calendar.getInstance();
	
	Long[] timeLine = null;

	public DatePicker(Component relative, Object... params) {
		date = Util.paramString("", 0, params);
		timeLine = Util.param(null, 1, params);
		String title = Util.paramString("Date Picker", 2, params);
		
		final JDialog dialog = new JDialog();
		dialog.setModal(true);
		String[] header = { "", "S", "M", "T", "W", "T", "F", "S" };
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
		JButton btn = new JButton(moves[0]);
		btn.addActionListener(moveMonth);
		p2.add(btn);
		btn = new JButton(moves[1]);
		btn.addActionListener(moveMonth);
		p2.add(btn);
		p2.add(lbl);
		btn = new JButton(moves[2]);
		btn.addActionListener(moveMonth);
		p2.add(btn);
		btn = new JButton(moves[3]);
		btn.addActionListener(moveMonth);
		p2.add(btn);
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
			cal.set(year, month, 1);
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
			month = cal.get(Calendar.MONTH);
			year = cal.get(Calendar.YEAR);
			displayDate();
		}
	};
	
	void displayDate() {
		sdf.applyPattern(monthFormat);
		cal.set(year, month, 1);
		lbl.setText(sdf.format(cal.getTime()));
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

	private int setWeek(int weekInYear) {
		int week = weekInYear;
		weekInYear = cal.get(Calendar.WEEK_OF_YEAR);
		if (week != weekInYear) {
			int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
			long time;
			if (week > weekInYear && weekOfMonth > 1)
				time = Util.timeInMillis(year + 1, Calendar.JANUARY, 1);
			else
				time = cal.getTimeInMillis();
			String format = sdf.format(time);
			weeks[weekOfMonth - 1].setText(format);
		}
		return weekInYear;
	}

	private void setDay(int x, int day) {
		days[x].setText("" + day);
		boolean flag = false;
		int crit = -1;
		if (timeLine != null) {
			long time = Util.timeInMillis(year, -month, day);
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
		if (text.length() > 0)
			try {
				if (isWeekDate(text)) 
					sdf.applyPattern(weekFormat);
				else if (isCalendarDate(text))
					sdf.applyPattern(dateFormat);
				
				Date dt = sdf.parse(text);
				cal.setTime(dt);
			} catch (ParseException e) {}
		
		month = cal.get(Calendar.MONTH);
		year = cal.get(Calendar.YEAR);
		displayDate();
	}

	public String getDateString() {
		if (date.equals("") || isCalendarDate(date) || isWeekDate(date))
			return date;
		else
			return getDateString(dateFormat);
	}

	private String getDateString(String format) {
		int day = Integer.parseInt(date);
		cal.set(year, month, day);
		sdf.applyPattern(format);
		return sdf.format(cal.getTime());
	}

	public static void main(String[] args) {
		long time = Util.now();
		System.out.println(pickADate(time, dateFormat, "", new Long[]{time - Util.getMillis(1) - 1, time}));
	}

	public static String pickADate(long time, String format, String title, Long[] timeLine) {
		try {
			String dateString = Util.formatDate(time, dateFormat);
			return new DatePicker(null, dateString, timeLine, title)
					.getDateString(format);
		} catch (Exception e) {
			return "";
		}
	}
	
	public static boolean isCalendarDate(String dateString) {
		return dateString.indexOf(',') > -1;
	}

	public static boolean isWeekDate(String dateString) {
		int slash = dateString.indexOf("/");
		return slash > -1 && slash == dateString.lastIndexOf("/");
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
	
	public static String weekDate(long[] week) {
		int[] val0 = parseWeekDate(Util.formatDate(week[0], weekFormat));
		int[] val1 = parseWeekDate(Util.formatDate(week[1], weekFormat));
		if (val1[0] - val0[0] == 1 && val0[1] != val1[1])
			return val0[0] + "/" + val1[1] % 100;
		else
			return val0[0] + "/" + val0[1] % 100;
	}
	
	public static long[] dayInterval(String dateString, int days) {
		Date date = Util.parseDate(dateString, dateFormat);
		return Util.dayInterval(date.getTime(), days);
	}
	
	public static long[] weekInterval(String dateString, int weeks) {
		Date date = Util.parseDate(dateString, weekFormat);
		return weekInterval(date, weeks);
	}
	
	public static long[] weekInterval(Date date, int weeks) {
		if (date == null)
			return null;
		else
			return Util.weekInterval(date, weeks);
	}
	
	public static long[] nextWeekInterval(String dateString) {
		long[] week = weekInterval(dateString, 2);
		return Util.weekInterval(new Date(week[1]), -1);
	}
	
	public static long[] previousWeekInterval(String dateString) {
		return weekInterval(dateString, -1);
	}
}

