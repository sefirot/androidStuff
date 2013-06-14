package android.os;

import com.applang.Util.ValMap;

public class Bundle extends ValMap
{
	public void putString(String key, String value) {
		super.put(key, value);
	}

	public void putStringArray(String key, String[] value) {
		super.put(key, value);
	}

	public void putInt(String key, int value) {
		super.put(key, value);
	}

	public String getString(String key) {
		return (String) super.get(key);
	}

	public String[] getStringArray(String key) {
		return (String[]) super.get(key);
	}

	public int getInt(String key) {
		return (Integer) super.get(key);
	}

	public boolean containsKey(String key) {
		return super.containsKey(key);
	}

}
