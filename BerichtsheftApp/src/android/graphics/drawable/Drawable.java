package android.graphics.drawable;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import android.util.Log;

public class Drawable
{
	private static final String TAG = Drawable.class.getSimpleName();
	
	Image image;
	
	public Image getImage() {
		return image;
	}
	
	public Drawable setImage(URL url) {
		try {
			image = ImageIO.read(url);
		} catch (IOException e) {
			Log.e(TAG, "setImage", e);
		}
		return this;
	}
	
	public Drawable setImage(String path) {
		try {
			image = ImageIO.read(new File(path));
		} catch (IOException e) {
			Log.e(TAG, "setImage", e);
		}
		return this;
	}
	
	public Drawable setImage(InputStream is) {
		try {
			image = ImageIO.read(is);
		} catch (IOException e) {
			Log.e(TAG, "setImage", e);
		}
		return this;
	}
}
