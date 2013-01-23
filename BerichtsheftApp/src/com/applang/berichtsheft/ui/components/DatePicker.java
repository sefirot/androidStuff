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
		JButton btn = new JButton("<<");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				year--;
				displayDate();
			}
		});
		p2.add(btn);
		btn = new JButton("<");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				month--;
				displayDate();
			}
		});
		p2.add(btn);
		p2.add(lbl);
		btn = new JButton(">");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				month++;
				displayDate();
			}
		});
		p2.add(btn);
		btn = new JButton(">>");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				year++;
				displayDate();
			}
		});
		p2.add(btn);
		dialog.add(p2, BorderLayout.NORTH);
		dialog.add(p1, BorderLayout.CENTER);
		dialog.pack();
		dialog.setLocationRelativeTo(relative);
		setDateString(Util.paramString("", 0, params));
		dialog.setTitle("Date Picker");
		dialog.setVisible(true);
	}

	void displayDate() {
		sdf.applyPattern(monthFormat);
		cal.set(year, month, 1);
		lbl.setText(sdf.format(cal.getTime()));
		for (int x = 0; x < days.length; x++) days[x].setText("");
		for (int x = 0; x < weeks.length; x++) weeks[x].setText("");
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		int weekInYear = 0;
		sdf.applyPattern(weekFormat);
		for (int x = dayOfWeek - 1, day = 1; day <= daysInMonth; x++, day++) {
			days[x].setText("" + day);
			boolean flag = timeLine != null && 
					Arrays.binarySearch(timeLine, Util.dateInMillis(year, -month, day)) > -1; 
			days[x].setForeground(flag ? Color.GREEN : Color.BLACK);
			int week = weekInYear;
			weekInYear = cal.get(Calendar.WEEK_OF_YEAR);
			if (week != weekInYear) {
				int y = cal.get(Calendar.WEEK_OF_MONTH);
				long time;
				if (week > weekInYear && y > 1)
					time = Util.dateInMillis(year + 1, Calendar.JANUARY, 1);
				else
					time = cal.getTimeInMillis();
				String format = sdf.format(time);
				weeks[y - 1].setText(format);
			}
			cal.add(Calendar.DATE, 1);
		}
	}

	public void setDateString(String text) {
		if (text.length() > 0)
			try {
				if (text.indexOf("/") > -1) 
					sdf.applyPattern(weekFormat);
				else if (text.indexOf(",") > -1)
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
		
		cal.set(year, month, Integer.parseInt(date));
		sdf.applyPattern(dateFormat);
		return sdf.format(cal.getTime());
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
		int[] val0 = parseWeekDate(Util.formatDate(week[0], DatePicker.weekFormat));
		int[] val1 = parseWeekDate(Util.formatDate(week[1], DatePicker.weekFormat));
		if (val1[0] - val0[0] == 1 && val0[1] != val1[1])
			return val0[0] + "/" + val1[1] % 100;
		else
			return val0[0] + "/" + val0[1] % 100;
	}
	
	public static long[] weekInterval(String dateString, int weeks) {
		Date date = Util.parseDate(dateString, DatePicker.weekFormat);
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

