package app.learn;

public class Util {

	public static <T> boolean isAvailable(int i, T[] array) {
		return i > -1 && i < array.length && array[i] != null;
	}

	public static boolean notNullOrEmpty(String value) {
		return value != null && value.length() > 0;
	}

	public static double delta = 0.00001;

}
