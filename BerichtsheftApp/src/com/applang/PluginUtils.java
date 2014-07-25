package com.applang;

import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.IOUtilities;

import com.inet.jortho.FileUserDictionary;
import com.inet.jortho.SpellChecker;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;
import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class PluginUtils {

    private static final String TAG = PluginUtils.class.getSimpleName();

	public static boolean insideJEdit() {
		try {
			jEdit.getProperty("tip.show");
			return true;
		} catch (NullPointerException ex) {
			return false;
		}
	}

	public static String getSettingsDirectory() {
		if (!insideJEdit()) {
			return System.getProperty("jedit.settings.dir");
		}
		return jEdit.getSettingsDirectory();
	}

	public static Properties loadProperties(String fileName) {
		diag_println(DIAG_OFF, ".props : ", fileName);
		Properties props = new Properties();
		if (notNullOrEmpty(fileName)) {
			InputStream in = null;
			try {
				JarFile jarFile = null;
				if (isJarUri(fileName)) {
					URL url = new URL(fileName);
					url = new URL(url.getFile());
					String[] parts = url.getFile().split("!/");
					jarFile = new JarFile(new File(parts[0]));
					JarEntry jarEntry = jarFile.getJarEntry(parts[1]);
					in = jarFile.getInputStream(jarEntry);
				}
				else {
					File file = new File(fileName);
					if (file.isFile()) {
						in = new FileInputStream(file);
					} else {
						in = PluginUtils.class.getResourceAsStream(fileName);
					}
				}
				props.load(in);
				if (jarFile != null)
					jarFile.close();
			} catch (Exception e) {
				Log.e(TAG, "loadProperties", e);
			} finally {
				IOUtilities.closeQuietly((Closeable) in);
			}
		}
		return props;
	}
	
	// NOTE used in scripts
	public static String getProperty(String name) {
		return getProperty(name, null);
	}
	
	public static String getProperty(String name, String defaultValue) {
		String prop;
		if (insideJEdit()) 
			prop = jEdit.getProperty(name, defaultValue);
		else
			prop = properties.getProperty(name, defaultValue);
		if (nullOrEmpty(prop) && (name.endsWith("_COMMAND") || name.endsWith("_SDK"))) 
			prop = getSetting(name, "");
		return prop;
	}
	
	public static Properties properties = null;
	static {
		if (!insideJEdit()) {
			String part = System.getProperty("plugin.props");
			String fileName = pathCombine(relativePath(), part);
			if (!fileExists(fileName)) {
				fileName = resourceUrlOf(part, Constraint.END).toString();
			}
			properties = loadProperties(fileName);
		}
		messageRedirection();
	}
	
	// NOTE used in scripts
	public static void setProperty(String name, String value) {
		if (insideJEdit())
			jEdit.setProperty(name, value);
		else
			properties.setProperty(name, value);
	}
    
	public static void saveSettings() {
		if (insideJEdit())
			jEdit.saveSettings();
	}
    
	public static void setStatusMessage(String msg) {
		View view = jEdit.getActiveView();
		if (view != null)
			view.getStatus().setMessageAndClear(msg);
		else
			Toast.makeText(Activity.getInstance(), msg, Toast.LENGTH_LONG).show(true);
	}
    
	public static void messageRedirection() {
		messRedirection = new Function<String>() {
			public String apply(Object... params) {
				String message = param("", 0, params);
				setStatusMessage(message);
				return message;
			}
		};
	}

	// NOTE used in scripts
	public static void invokeAction(View view, String actionName) {
		EditAction action = jEdit.getAction(actionName);
		if (action != null)
			action.invoke(view);
	}

	// NOTE used in scripts
	public static ImageIcon loadIcon(String path) {
		if (path != null && path.endsWith(".icon"))
			path = getProperty(path);
		if (nullOrEmpty(path))
			return null;
		try {
			if (path.startsWith("/"))
				return iconFrom(path);
			else if (!insideJEdit()) {
				path = pathCombine("/org/gjt/sp/jedit/icons/themes/tango", path);
				return iconFrom(path);
			}
			else
				return (ImageIcon) GUIUtilities.loadIcon(path);
		} catch (Exception e) {
			path = "/org/gjt/sp/jedit/icons/themes/tango/22x22/emblems/emblem-unreadable.png";
			return iconFrom(path);
		}
	}

	public static AbstractButton makeCustomButton(String name, ActionListener listener, boolean flip) {
		String toolTip = getProperty(name.concat(".label"));
		ImageIcon icon = loadIcon(name + ".icon");
		if (flip && icon != null) {
			Image img = icon.getImage();
			BufferedImage bimg = horizontalflip(img);
			icon = new ImageIcon(bimg);
		}
		AbstractButton b = new RolloverButton(icon);
		if (listener != null) {
			b.addActionListener(listener);
			b.setEnabled(true);
		} else {
			b.setEnabled(false);
		}
		b.setToolTipText(toolTip);
		return b;
	}

	public static AbstractButton makeCustomButton(CustomAction customAction, boolean flip) {
		AbstractButton btn;
		CustomActionType type = customAction.getType();
		if (type != null) {
			btn = makeCustomButton(type.resourceName(), null, flip);
			btn.setEnabled(true);
		}
		else {
			btn = new RolloverButton();
			String name = customAction.getValue(Action.NAME).toString();
			btn.setName(name);
			btn.setText(name);
		}
		btn.setAction(customAction);
		return btn;
	}

	public static void jEditDebugLogging() {
		org.gjt.sp.util.Log.init(true,org.gjt.sp.util.Log.DEBUG);
		org.gjt.sp.jedit.Debug.TOKEN_MARKER_DEBUG = true;
		org.gjt.sp.jedit.Debug.CHUNK_CACHE_DEBUG = true;
	}

	//	NOTE	leaves standard-out going to log
	public static <T> T suppressJEditErrorLog(Function<T> func, Object...params) {
		try {
			org.gjt.sp.util.Log.init(true,org.gjt.sp.util.Log.ERROR + 1);
			return func.apply(params);
		} 
		finally {
			org.gjt.sp.util.Log.init(false,org.gjt.sp.util.Log.WARNING);
		}
	}

	public static void setupSpellChecker(String path) {
		if (!fileExists(new File(path, "dictionaries.cnf")))
			return;
		SpellChecker.setUserDictionaryProvider( new FileUserDictionary(path) );
		try {
			SpellChecker.registerDictionaries( new URL("file", null, path + "/"), null );
		} catch (MalformedURLException e) {
			Log.e(TAG, "setupSpellChecker", e);
		}
	}

	public static void spellChecking(JTextComponent jtc, boolean register) {
		if (register)
			SpellChecker.register(jtc);
		else
			SpellChecker.unregister(jtc);
	}

	public static final String MAGIC = "magic";

	public static String magicFile(String name, String magic, Properties props) {
		String path = tempPath() + PATH_SEP + name;
		try {
			Writer writer = write(new StringWriter(), ":");
			writer = write_assoc(writer, MAGIC, magic);
			writer = write(writer, ":");
			writer = write_assoc(writer, "wrap", "none");
			writer = write(writer, ":");
			String comment = writer.toString();
			FileWriter fileWriter = new FileWriter(path);
			props.store(fileWriter, comment);
			fileWriter.close();
		}
		catch (Exception e) {
			Log.e(TAG, "magicFile", e);
		}
		return path;
	}

	public static String getNextMagicTemp() {
		int magicTitledCount = 0;
		for (String name : new File(tempPath()).list()) 
			if (name.startsWith(MAGIC)) 
				try {
					magicTitledCount = Math.max(
							magicTitledCount,
							Integer.parseInt(name
									.substring(MAGIC.length())));
				}
				catch(NumberFormatException nf) {}
		return MAGIC + "_" + (magicTitledCount + 1);
	}

	public static Buffer createMagicBuffer() {
		View view = jEdit.getActiveView();
		String parent = null;
		if (view != null) {
			Buffer buffer = view.getBuffer();
			parent = buffer.getDirectory();
		}
		if (parent == null) {
			parent = System.getProperty("user.home");
		}
		VFS vfs = VFSManager.getVFSForPath(parent);
		if ((vfs.getCapabilities() & VFS.WRITE_CAP) == 0) {
			// cannot write on that VFS, creating untitled buffer in home directory
			parent = System.getProperty("user.home");
		}
		Buffer buffer = jEdit.openTemporary(view, tempPath(), getNextMagicTemp(),true, null);
		jEdit.commitTemporary(buffer);
		return buffer;
	}

	public static void parseBufferLocalProperties(Buffer buffer) {
		try {
			Class<?> cl = Class.forName("org.gjt.sp.jedit.buffer.JEditBuffer");
			Method method = cl.getDeclaredMethod("parseBufferLocalProperties");
			method.setAccessible(true);
			method.invoke(buffer);
		} 
		catch (Exception e) {
			Log.e(TAG, "parseBufferLocalProperties", e);
		}
	}

	@SuppressWarnings("unchecked")
	public static EditPane[] getEditPanesFor(final Buffer buffer) {
		try {
			JEditVisitorAdapter visitor = new JEditVisitorAdapter() {
				public Set<EditPane> editPanes = new HashSet<EditPane>();
				@Override
				public void visit(EditPane editPane) {
					if (buffer.equals(editPane.getBuffer()))
						editPanes.add(editPane);
				}
			};
			jEdit.visit(visitor);
			return ((Set<EditPane>) visitor.getClass().getField("editPanes").get(visitor))
					.toArray(new EditPane[0]);
		} 
		catch (Exception e) {
			return null;
		}
	}

	public static void setEditPane(EditPane editPane) {
		View view = editPane.getView();
		try {
			Class<?> cl = Class.forName("org.gjt.sp.jedit.View");
			Method method = cl.getDeclaredMethod("setEditPane", EditPane.class);
			method.setAccessible(true);
			method.invoke(view, editPane);
		} 
		catch (Exception e) {
			Log.e(TAG, "setEditPane", e);
		}
	}

	public static String absolutePath(String relPath) {
		String absPath = insideJEdit() ? 
				jEdit.getSettingsDirectory() : 
				Resources.getCodeSourceLocation(PluginUtils.class).getPath();
		int index = absPath.indexOf(".jedit");
		String part = index > -1 ? absPath.substring(0, index) : relativePath();
		absPath = pathCombine(part, relPath);
		return absPath;
	}

}
