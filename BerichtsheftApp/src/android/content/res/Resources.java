package android.content.res;

import java.io.InputStream;
import java.lang.reflect.Field;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import static com.applang.SwingUtil.*;

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
			switch (id) {
			case android.R.string.close:
				return (String) defaultOptions(JOptionPane.DEFAULT_OPTION).get(0);
			case android.R.string.cancel:
				return (String) UIManager.get("OptionPane.cancelButtonText");
			case android.R.string.ok:
				return (String) UIManager.get("OptionPane.okButtonText");
			case android.R.string.yes:
				return (String) UIManager.get("OptionPane.yesButtonText");
			case android.R.string.no:
				return (String) UIManager.get("OptionPane.noButtonText");
			default:
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
				break;
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
