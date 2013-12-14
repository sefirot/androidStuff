package android.graphics.drawable;

import java.io.InputStream;

public class Drawable
{
	InputStream is;
	
	public InputStream getInputStream() {
		return is;
	}
	
	public Drawable setInputStream(Object object) {
		is = (InputStream) object;
		return this;
	}
}
