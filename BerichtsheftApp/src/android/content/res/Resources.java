package android.content.res;

import java.io.InputStream;
import java.lang.reflect.Field;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class Resources
{
	private static final String TAG = Resources.class.getSimpleName();
	
	private Context context;
	
	public Resources(Context context) {
		this.context = context;
	}
	public String getString(int id) {
		String pkg = context.getPackageName();
		try {
			Class<?> c = Class.forName(pkg + ".R");
			for (Class<?> inner : c.getDeclaredClasses()) {
				if ("string".equals(inner.getSimpleName())) {
					for (Field field : inner.getDeclaredFields()) {
						if ("int".equals(field.getType().getSimpleName()) && field.getInt(null) == id) {
							String name = field.getName();
							InputStream is = c.getResourceAsStream("/res/values/strings.xml");
							Document doc = Jsoup.parse(is, "UTF-8", "", Parser.xmlParser());
							for (Element elem : doc.getElementsByAttribute("name")) {
								if (name.equals(elem.attr("name")))
									return elem.text();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "getString", e);
		}
		return "";
	}
	public Drawable getDrawable (int id) {
		return null;
	}
    public int getDimensionPixelOffset(int id) {
    	return 0;
    }
}
