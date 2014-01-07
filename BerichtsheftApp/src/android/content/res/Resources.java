package android.content.res;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;

public class Resources
{
	private static final String TAG = Resources.class.getSimpleName();
	
	private Context context;
	
	public Resources(Context context) {
		this.context = context;
	}
	public String getString(int id) {
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
				Object[] params = {null};
				resourceLookup(id, context.getPackageName(), "string", new Job<Class<?>>() {
					public void perform(Class<?> c, Object[] parms) throws Exception {
						String name = param_String(null,0,parms);
						InputStream is = c.getResourceAsStream("/res/values/strings.xml");
						Document doc = Jsoup.parse(is, "UTF-8", "", Parser.xmlParser());
						for (Element elem : doc.getElementsByAttribute("name")) {
							if (name.equals(elem.attr("name"))) {
								Object[] params = param(null,1,parms);
								if (params != null)
									params[0] = elem.text();
								break;
							}
						}
					}
				}, params);
				if (null != params[0])
					return (String) params[0];
			}
		} catch (Exception e) {
			Log.e(TAG, "getString", e);
		}
		return "";
	}
	public int getDimensionPixelOffset(int id) {
		try {
			switch (id) {
			default:
				Object[] params = {0};
				resourceLookup(id, context.getPackageName(), "dimen", new Job<Class<?>>() {
					public void perform(Class<?> c, Object[] parms) throws Exception {
						String name = param_String(null,0,parms);
						InputStream is = c.getResourceAsStream("/res/values/dimens.xml");
						Document doc = Jsoup.parse(is, "UTF-8", "", Parser.xmlParser());
						for (Element elem : doc.getElementsByAttribute("name")) {
							if (name.equals(elem.attr("name"))) {
								Object[] params = param(null,1,parms);
								if (params != null)
									params[0] = toInt(0, stripUnits(elem.text()));
								break;
							}
						}
					}
				}, params);
				if (null != params[0])
					return (Integer) params[0];
			}
		} catch (Exception e) {
			Log.e(TAG, "getDimensionPixelOffset", e);
		}
		return 0;
	}
	public Drawable getDrawable (int id) {
		try {
			switch (id) {
			default:
				Object[] params = {null};
				resourceLookup(id, context.getPackageName(), "drawable", new Job<Class<?>>() {
					public void perform(Class<?> c, Object[] parms) throws Exception {
						final String name = "/res/drawable/" + param_String(null,0,parms);
						Set<URL> res = getResourceURLs(
							new ResourceURLFilter() {
								public boolean accept(URL resourceUrl) {
									String url = resourceUrl.getFile();
								    return url.contains(name);
								}
							});
						if (res.size() == 1) {
							String path = res.iterator().next().getFile();
							path = path.substring(path.indexOf(name));
							InputStream is = c.getResourceAsStream(path);
							Object[] params = param(null, 1, parms);
							if (params != null)
								params[0] = is;
						}
					}
				}, params);
				if (null != params[0])
					return new Drawable().setInputStream(params[0]);
			}
		} catch (Exception e) {
			Log.e(TAG, "getDrawable", e);
		}
		return null;
	}
	public InputStream openRawResource(int id) {
		try {
			switch (id) {
			default:
				Object[] params = {null};
				resourceLookup(id, context.getPackageName(), "raw", new Job<Class<?>>() {
					public void perform(Class<?> c, Object[] parms) throws Exception {
						final String name = "/res/raw/" + param_String(null,0,parms);
						Set<URL> res = getResourceURLs(
							new ResourceURLFilter() {
								public boolean accept(URL resourceUrl) {
									String url = resourceUrl.getFile();
								    return url.contains(name);
								}
							});
						if (res.size() == 1) {
							String path = res.iterator().next().getFile();
							path = path.substring(path.indexOf(name));
							InputStream is = c.getResourceAsStream(path);
							Object[] params = param(null, 1, parms);
							if (params != null)
								params[0] = is;
						}
					}
				}, params);
				if (null != params[0])
					return (InputStream) params[0];
			}
		} catch (Exception e) {
			Log.e(TAG, "openRawResource", e);
		}
		return null;
	}
	
	private void resourceLookup(int id, String pkg, String nameOfInnerClass, Job<Class<?>> lookup, Object...params) throws Exception {
		Class<?> c = Class.forName(pkg + (notNullOrEmpty(pkg) ? ".R" : "R"));
		for (Class<?> inner : c.getDeclaredClasses()) {
			if (nameOfInnerClass.equals(inner.getSimpleName())) {
				for (Field field : inner.getDeclaredFields()) {
					if ("int".equals(field.getType().getSimpleName()) && field.getInt(null) == id) {
						lookup.perform(c, objects(field.getName(), params));
						return;
					}
				}
			}
		}
	}
	
	public interface ResourceURLFilter {
		public boolean accept(URL resourceUrl);
	}

	private static void collectURL(ResourceURLFilter f, Set<URL> s, URL u) {
		if (f == null || f.accept(u)) {
			s.add(u);
		}
	}

	private static void iterateFileSystem(File r, ResourceURLFilter f, Set<URL> s) throws MalformedURLException, IOException {
		File[] files = r.listFiles();
		for (File file: files)
			if (file.isDirectory())
				iterateFileSystem(file, f, s);
			else if (file.isFile())
				collectURL(f, s, file.toURI().toURL());
	}

	private static void iterateJarFile(File file, ResourceURLFilter f, Set<URL> s) throws MalformedURLException, IOException {
		JarFile jarfile = new JarFile(file);
		for(Enumeration<JarEntry> je = jarfile.entries(); je.hasMoreElements();) {
			JarEntry j = je.nextElement();
			if (!j.isDirectory()) {
				collectURL(f, s, new URL("jar", "", file.toURI() + "!/" + j.getName()));
			}
		}
		jarfile.close();
	}

	private static void iterateEntry(File p, ResourceURLFilter f, Set<URL> s) throws MalformedURLException, IOException {
		if (p.isDirectory()) {
			iterateFileSystem(p, f, s);
		} else if (p.isFile() && p.getName().toLowerCase().endsWith(".jar")) {
			iterateJarFile(p, f, s);
		}
	}

	public static Set<URL> getResourceURLs() throws IOException, URISyntaxException {
		return getResourceURLs((ResourceURLFilter)null);
	}
	
	public static Set<URL> getResourceURLs(ResourceURLFilter filter) throws IOException, URISyntaxException {
		Set<URL> collectedURLs = new HashSet<URL>();
		URLClassLoader ucl = (URLClassLoader)ClassLoader.getSystemClassLoader();
		for (URL url: ucl.getURLs()) {
			iterateEntry(new File(url.toURI()), filter, collectedURLs);
		}
		return collectedURLs;
	}
	
	public static Set<URL> getResourceURLs(Class<?> rootClass) throws Exception {
		String packageName = rootClass.getName();
		packageName = packageName.substring(0, packageName.lastIndexOf("."));
		return getResourceURLs(packageName, (ResourceURLFilter)null);
	}
	
	public static Set<URL> getResourceURLs(String packageName, ResourceURLFilter filter) throws Exception {
		Set<URL> collectedURLs = new HashSet<URL>();
		URI location = getCodeSourceLocation(Resources.class);
//		location = location.resolve(packageName.replace('.', '/'));
		iterateEntry(new File(location), filter, collectedURLs);
		return collectedURLs;
	}
	
	public static URI getCodeSourceLocation(Class<?> rootClass) {
		try {
			CodeSource src = rootClass.getProtectionDomain().getCodeSource();
			return src.getLocation().toURI();
		} catch (URISyntaxException e) {
			Log.e(TAG, "getCodeSourceLocation", e);
			throw new RuntimeException("code source not available");
		}
	}
	
	public static String className(String resourceClassFileName, String sourceLocation) {
		return strip(true, resourceClassFileName.substring(sourceLocation.length()), "!/").replace('/', '.');
	}
	
	public static void list(Writer writer, String packageName, ResourceURLFilter filter) {
		try {
			for (URL u: getResourceURLs(packageName, filter)) {
				writer.append(u + NEWLINE);
			}
		} catch (Exception e) {
			Log.e(TAG, "list", e);
		}
	}
	
	public AssetManager getAssets() {
		return new AssetManager();
	}
}
