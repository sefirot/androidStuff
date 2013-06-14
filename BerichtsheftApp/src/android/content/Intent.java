package android.content;

import android.os.Bundle;

public class Intent
{
	public Intent() {
	}

	public Intent(String action) {
		this.action = action;
	}
	
	private String action;
	
	public String getAction() {
		return action;
	}
	
	public Intent putExtra(String name, int value) {
		extras.put(name, value);
		return this;
	}
	public Intent putExtra(String name, String value) {
		extras.put(name, value);
		return this;
	}
	public Intent putExtra(String name, String[] value) {
		extras.put(name, value);
		return this;
	}

	private Bundle extras = new Bundle();

	public Intent putExtras(Bundle value) {
		this.extras = value;
		return this;
	}

	public Bundle getExtras() {
		return this.extras;
	}
}
