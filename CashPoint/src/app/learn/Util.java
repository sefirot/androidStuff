package app.learn;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {

	public static <T> boolean isAvailable(int i, T[] array) {
		return array != null && 
				i > -1 && 
				i < array.length && 
				array[i] != null;
	}

	public static <T> boolean isNullOrEmpty(T[] array) {
		return array == null || array.length < 1;
	}

	public static boolean notNullOrEmpty(String value) {
		return value != null && value.length() > 0;
	}

	public static double delta = 0.00001;

	public static String formatAmount(double value) {
		return String.format("%.2f", value);
	}

	public static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static String timestamp(Date date) {
		return timestampFormat.format(date);
	}

}
