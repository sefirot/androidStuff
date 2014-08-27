package com.applang;

import java.awt.Component;
import java.awt.Container;
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
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
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
import org.gjt.sp.jedit.textarea.TextArea;
import org.gjt.sp.jedit.visitors.JEditVisitorAdapter;
import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;

import com.inet.jortho.FileUserDictionary;
import com.inet.jortho.SpellChecker;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class PluginUtils {

	public static boolean insideJEdit() {
		try {
			jEdit.getProperty("tip.show");
			return true;
		} catch (NullPointerException ex) {
			return false;
		}
	}

	public static String getSettingsDirectory() {
		if (insideJEdit()) 
			return jEdit.getSettingsDirectory();
		else
			return System.getProperty("jedit.settings.dir");
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
				Log.log(Log.ERROR, PluginUtils.class + ".loadProperties", e);
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
			longToast(msg);
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
			Log.log(Log.ERROR, PluginUtils.class + ".setupSpellChecker", e);
		}
	}

	public static void spellChecking(JTextComponent jtc, boolean register) {
		if (register)
			SpellChecker.register(jtc);
		else
			SpellChecker.unregister(jtc);
	}

	public static final String FEATURE = "feature";

	public static String featureFile(String name, String feature, Properties props) {
		String path = tempPath() + PATH_SEP + name;
		try {
			Writer writer = write(new StringWriter(), ":");
			writer = write_assoc(writer, FEATURE, feature);
			writer = write(writer, ":");
			writer = write_assoc(writer, "wrap", "none");
			writer = write(writer, ":");
			String comment = writer.toString();
			FileWriter fileWriter = new FileWriter(path);
			props.store(fileWriter, comment);
			fileWriter.close();
		}
		catch (Exception e) {
			Log.log(Log.ERROR, PluginUtils.class + ".featureFile", e);
		}
		return path;
	}

	public static String getNextFeatureTemp() {
		int featureTitledCount = 0;
		for (String name : new File(tempPath()).list()) 
			if (name.startsWith(FEATURE)) 
				try {
					featureTitledCount = Math.max(
							featureTitledCount,
							Integer.parseInt(name
									.substring(FEATURE.length())));
				}
				catch(NumberFormatException nf) {}
		return FEATURE + "_" + (featureTitledCount + 1);
	}

	public static Buffer createFeatureBuffer() {
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
		Buffer buffer = jEdit.openTemporary(view, tempPath(), getNextFeatureTemp(),true, null);
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
			Log.log(Log.ERROR, PluginUtils.class + ".parseBufferLocalProperties", e);
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
			Log.log(Log.ERROR, PluginUtils.class + ".setEditPane", e);
		}
	}

	public static class DoubleFeature
	{
		private JComponent widget = null;
		
		public void setWidget(JComponent widget) {
			this.widget = widget;
		}
	
		public JComponent getWidget() {
			return widget;
		}
	
		public DoubleFeature(TextArea textArea) {
			setTextArea(textArea);
		}
	
	    @Override
		public String toString() {
			Writer writer = write(new StringWriter(), identity(this));
			String t = "";
			for (int i = 0; i < textAreas.length; i++) {
				t += textAreas[i] == null ? "-" : "+";
			}
			writer = write_assoc(writer, "textAreas", t, 1);
			writer = write_assoc(writer, "widget", 
					functionValueOrElse("null", 
						new Function<String>() {
							public String apply(Object...params) {
								return widget.getClass().getSimpleName();
							}
						}
					), 
					1);
			Container container = getFeature().getParent();
			if (container instanceof EditPane) {
				EditPane editPane = (EditPane)container;
				Buffer buffer = editPane.getBuffer();
				if (buffer != null)
					writer = write_assoc(writer, "buffer", buffer, 1);
			}
			return writer.toString();
		}
		
		protected TextArea[] textAreas = {null,null};
	
		public void setTextArea(TextArea textArea) {
			textAreas[0] = textArea;
		}
		
		public TextArea getTextArea() {
			return textAreas[0];
		}
		
		public TextArea getTextArea2() {
			return textAreas[1];
		}
		
		public Component getFeature() {
			return textAreas[0] != null ? textAreas[0] : widget;
		}
		
		public void addFeatureTo(Container container) {
			addCenterComponent(getFeature(), container);
			container.validate();
			container.repaint();
		}
		
		protected boolean isolate(Object...params) {
			Component target = getFeature();
			Container container = target.getParent();
			if (container == null) {
				Log.log(Log.ERROR, getClass().getName() + ".isolate", String.format("%s cannot be isolated", identity(target)));
				return false;
			}
			container.remove(target);
			if (params.length > 0)
				params[0] = container;
			return true;
		}
		
		protected void integrate(Container container, boolean featured) {
			if (container != null) {
				if (featured) {
					if (textAreas[0] != null) {
						textAreas[1] = textAreas[0];
						setTextArea(null);
					}
				}
				else {
					if (textAreas[1] != null) {
						setTextArea(textAreas[1]);
						textAreas[1] = null;
					}
				}
				addFeatureTo(container);
				container = null;
			}
		}
	
		public void toggle(boolean featured, Job<Container> inIsolation, Object...args) {
			Object[] params = {null};
			if (isolate(params)) {
				Container container = (Container) params[0];
				if (inIsolation != null)
					try {
						inIsolation.perform(container, args);
					} catch (Exception e) {
						Log.log(Log.ERROR, getClass().getName() + ".toggle", e);
					}
				integrate(container, featured);
			}
	    }
	
		public static final String FOCUS = "focus";
		public static final String REQUEST = "request";
		
		public boolean focused = false;
		
		public static Component[] focusRequestComponents(Container container) {
			final Component[] focused = findComponents(container, new Predicate<Component>() {
				public boolean apply(Component c) {
					String name = stringValueOf(c.getName());
					return check(name, Constraint.AMONG, DoubleFeature.FOCUS) || 
							check(name, Constraint.AMONG, DoubleFeature.REQUEST);
				}
			});
			no_println("focusRequest", identity(asList(focused).toArray()));
			return focused;
		}
	
		public void requestFocus() {
			TextArea textArea = getTextArea();
			if (textArea != null)
				textArea.requestFocus();
			else if (widget != null) {
				Component component = findFirstComponent(widget, FOCUS, Constraint.AMONG);
				if (component != null) {
					component.requestFocusInWindow();
					diag_println(DIAG_OFF, "focus", 
							identity(component), 
							identity(SwingUtilities.getAncestorOfClass(EditPane.class, component)));
				}
			}
		}
	}

}
