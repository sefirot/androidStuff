package com.applang.berichtsheft.test;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.apache.commons.lang.StringEscapeUtils;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.bsh.Interpreter;
import org.gjt.sp.jedit.bsh.NameSpace;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applang.BaseDirective;
import com.applang.Dialogs;
import com.applang.PromptDirective;
import com.applang.UserContext.EvaluationTask;
import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.DataDockable;
import com.applang.berichtsheft.plugin.DataDockable.TransportBuilder;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;
import com.applang.components.AndroidBridge;
import com.applang.components.DataForm;
import com.applang.components.DataConfiguration;
import com.applang.components.DataView;
import com.applang.components.DataView.DataModel;
import com.applang.components.DataView.ProjectionModel;
import com.applang.components.DatePicker;
import com.applang.components.ManagerBase;
import com.applang.components.ProfileManager;
import com.applang.components.DataAdapter;
import com.applang.components.ScriptManager;
import com.applang.components.TextEdit;
import com.applang.components.TextToggle;
import com.applang.components.WeatherManager;
import com.applang.provider.NotePadProvider;
import com.applang.provider.WeatherInfoProvider;
import com.applang.provider.WeatherInfo.Weathers;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;
import junit.framework.*;

public class HelperTests extends TestCase
{
	private static final String TAG = HelperTests.class.getSimpleName();

    public HelperTests(String name) {
    	super(name);
    }
    
	protected void setUp() throws Exception {
		super.setUp();
		BerichtsheftApp.loadSettings();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private void unsetTimeout() {
		AlertDialog.behavior = Behavior.setTimeout(AlertDialog.behavior, false);
	}

	private void setTimeout(int millis) {
		AlertDialog.behavior = Behavior.setTimeout(AlertDialog.behavior, true);
		Deadline.WAIT = millis;
	}
	
    public static String[] getStateStrings() {
    	try {
			String res = resourceFrom("/res/raw/states.json", "UTF-8");
			return toStrings(((ValList) walkJSON(null, new JSONArray(res), null)));
		} catch (Exception e) {
			fail(e.getMessage());
			return null;
		}
	}

	private Set<?> lafDefaults() {
		ValList list = vlist();
		for (Object object : UIManager.getLookAndFeelDefaults().keySet()) {
			list.add(stringValueOf(object));
		}
		return sortedSet(list);
	}
	
	private void setupJEdit(String script, Object...params) {
		File tempFile = BerichtsheftPlugin.getTempFile("test.bsh");
		contentsToFile(tempFile, 
				String.format(
						"void doSomethingUseful() {\n" +
						"    void run() {\n" +
						"        view = jEdit.getLastView();\n" +
						"		 %s\n" +
						"    }\n" +
						"    if(jEdit.getLastView() == null)\n" +
						"        VFSManager.runInAWTThread(this);\n" +
						"    else\n" +
						"        run();\n" +
						"}\n" +
						"doSomethingUseful();", script));
		String settingsDir = BerichtsheftPlugin.jedit_test_settings();
		properties = null;
        String[] args = strings(
				"-nosplash",
				"-noserver",
				String.format("-settings=%s", settingsDir), 
				String.format("-run=%s", tempFile.getPath()), 
				join(" ", params));
        BerichtsheftApp.main(args);
//		com.jdotsoft.jarloader.JarClassLoader jcl = new JarClassLoader();
//		jcl.invokeMain("com.applang.berichtsheft.BerichtsheftApp", args);
	}

	private void scriptTest(final String title, final String script, final Object...params) {
		startFrame(
			new AbstractAction(title) {
				public void actionPerformed(ActionEvent ev) {
					setupJEdit(script, params);
				}
			}
		);
	}

	public void testCommando() {
		String script = String.format(
				"com.applang.berichtsheft.plugin.BerichtsheftPlugin.invokeAction(view, \"%s\");\n", 
				"commando.Dokumente");
		scriptTest("testCommando", script);
	}
	
	public void testInquireUri() {
    	String dbPath = tempPath() + "/temp.db";
		Uri uri = fileUri(dbPath + "~", "notes");
		String uriString = stringValueOf(uri);
		String script = String.format(
				"com.applang.berichtsheft.plugin.BerichtsheftPlugin.loadSettings();\n" +
				"uriString = com.applang.components.DataConfiguration.inquireUri(\"%s\", false);\n" +
				"com.applang.berichtsheft.plugin.BerichtsheftShell.print(new Object[]{uriString});", 
				uriString);
		scriptTest("testInquireUri", script);
	}

	public void testWetter() {
		String script = "view.getDockableWindowManager().showDockableWindow(\"datadock\");\n";
		script += String.format(
				"com.applang.berichtsheft.plugin.BerichtsheftPlugin.loadSettings();\n" +
				"com.applang.berichtsheft.plugin.BerichtsheftPlugin.invokeAction(view, \"%s\");\n", 
				"commando.Wetter");
		scriptTest("testWetter", script);
	}

	public void testAndroidFileChooser() throws Exception {
		String script = "import com.applang.components.*;\n" +
				"androidFileName = \"\";\n" +
				"do {\n" +
				"	androidFileName = AndroidBridge.chooseFileFromSdcard(view,false,androidFileName);\n" +
				"} while (androidFileName != null);";
		scriptTest("testAndroidFileChooser", script);
//		underTest = true;
//		println(AndroidBridge.chooseFileFromSdcard(null, false, ""));
	}
	
	public void testSpellchecking() {
		String script = "import com.applang.berichtsheft.plugin.*;\n" +
				"jEdit.openFile(view.getEditPane(), \"" + POEM + "\");\n" + 
				"view.getEditPane().getTextArea().selectAll();\n" + 
				"actionName = \"berichtsheft.spellcheck-selection\";\n" +
				"BerichtsheftPlugin.invokeAction(view, actionName);";
		scriptTest("testSpellchecking", script);
	}
	
    private static final String POEM = "/home/lotharla/work/Workshop/Examples/poem.txt";

	private ValList shellRunTest(String script, String name) throws Exception {
		String response = runShellScript(name, script);
		println("script", flatten(script));
		String sh = tempPath() + PATH_SEP + name + ".sh";
		println(sh, flatten(contentsFromFile(new File(sh))));
		String resp = tempPath() + PATH_SEP + name;
		println(resp, flatten(contentsFromFile(new File(resp))));
//		print(sh, TAB);
//		print(getFileSize(sh), TAB);
//		print(res, TAB);
//		print(getFileSize(res));
//		println();
//		for (int i = 0; i < 3; i++) {
//			print(getFileTime(sh, i), TAB);
//			print(getFileTime(res, i));
//			println();
//		}
		return split(response, NEWLINE_REGEX);
	}
	
	private void existTest(boolean expected, String item, String subdir) throws Exception {
		String adb = AndroidBridge.getAdbCommand();
		String script = 
				adb + " shell ls -l \"/sdcard/%s\" | \n" +
					"awk '{\n" +
						"gsub(/[ \\t\\r\\n\\f\\v\\b]+$/, \"\", $NF)\n" +
						"m = match($0, /d/)\n" +
						"if (m == 1) print $NF\"/\" ; else print $NF\n" +
					"}'";
		script = String.format(script, subdir);
		ValList list = (ValList) split(runShellScript("test", script), NEWLINE_REGEX);
		assertEquals(expected, list.contains(item));
	}
	
	public void testShellRun() throws Exception {
		BerichtsheftPlugin.checkAvailabilityOfTools();
		String adb = AndroidBridge.getAdbCommand();
		assertTrue("Android adb command not available", fileExists(adb));
		if (shellRunTest(AndroidBridge.buildAdbCommand("", "", ""), "test").get(0).toString().startsWith("error"))
			return;
		println("devices :", AndroidBridge.deviceInfo(null));
		shellRunTest(AndroidBridge.buildAdbCommand("-r", "/sdcard/xxx", ""), "rm");
		existTest(false, "xxx/", "");
		shellRunTest(AndroidBridge.buildAdbCommand("mkdir", "/sdcard/xxx/", ""), "mkdir");
		existTest(true, "xxx/", "");
		shellRunTest(AndroidBridge.buildAdbCommand("push", "/sdcard/xxx/", tempPath() + "/dev"), "push");
		existTest(true, "dev", "xxx/");
		assertTrue(new File(tempPath() + "/dev").delete());
		shellRunTest(AndroidBridge.buildAdbCommand("pull", "/sdcard/xxx/dev", tempPath()), "pull");
		assertTrue(new File(tempPath() + "/dev").exists());
		shellRunTest(AndroidBridge.buildAdbCommand("rm", "/sdcard/xxx/dev", ""), "rm");
		existTest(false, "dev", "xxx/");
		shellRunTest(AndroidBridge.buildAdbCommand("rmdir", "/sdcard/xxx/", ""), "rmdir");
		existTest(false, "xxx/", "");
	}

	public void testBeanShell() throws Exception {
		NameSpace tmp = new NameSpace(BeanShell.getNameSpace(), "conversion");
		tmp.setVariable("d", dateInMillis(2013,7,7));
		String script = 
				"import com.applang.*;" +
				"Util.formatDate(d, Util.timestampFormat); ";
		Object value = BeanShell.eval(null, tmp, script);
		println(value);
	}

	public void _testBeanShell2() throws Exception {
		Interpreter i = new Interpreter();
		i.source(".jedit/macros/scripts/executor.bsh");
	}

	public void _testCommands() throws Exception {
		String path = ".jedit/console/commando/Android.xml";
		File file = new File(path);
		assertTrue(fileExists(file));
		String xml = contentsFromFile(file);
		org.jsoup.nodes.Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
		for (org.jsoup.nodes.Element elem : doc.getElementsByTag("COMMAND")) {
			String script = elem.text();
			JTextArea textArea = new JTextArea(script);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setPreferredSize(new Dimension(200,200));
			if (JOptionPane.OK_OPTION == showOptionDialog(null, 
					new JScrollPane(textArea), 
					path, 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.PLAIN_MESSAGE, 
					null, 
					null, null)) {
				NameSpace tmp = new NameSpace(BeanShell.getNameSpace(), "Android");
				tmp.setVariable("oper", "pull");
				tmp.setVariable("androidFile", "/sdcard/result2");
				tmp.setVariable("pcDir", tempPath());
				tmp.setVariable("params", "");
				println(BeanShell.eval(null, tmp, script));
			}
		}
	}

	public void testTransports() throws Exception {
		assertTrue(ProfileManager.transportsLoaded());
		printNodeInfo(ProfileManager.transports.getDocumentElement());
    	String dbPath = createNotePad();
    	String flavor = "com.applang.provider.NotePad";
    	Object[] projection = fullProjection(flavor);
		String profile = dbTable(fileUri(dbPath, null), "notes").toString();
		setProperty("TRANSPORT_PROFILE", profile);
		String oper = "pull";
		setProperty("TRANSPORT_OPER", oper);
		println();
		underTest = true;
		String template = builder.makeTemplate(projection);
    	printMatchResults(template, TransportBuilder.TEMPLATE_PATTERN);
		assertTrue(ProfileManager.setTemplate(template, profile, oper, flavor));
		ValMap map = ProfileManager.getProfileAsMap();
		println("profileMap", map);
		MapEditorComponent mec = new MapEditorComponent(map, 
				object(contentAuthorities(providerPackages).toArray()));
		new AlertDialog(null, 
				getProperty("manager.action-PROFILE.label"), 
				"", 
				mec, 
				JOptionPane.DEFAULT_OPTION,
				Behavior.MODAL, 
				loadIcon("manager.action-PROFILE.icon"), 
				null)
			.open();
		String template2 = template.replace("created", "created|unixepoch");
		assertTrue(ProfileManager.setTemplate(template2));
		map = ProfileManager.getProfileAsMap();
		assertEquals(template2, map.get("template"));
		assertEquals(oper, map.get("oper"));
		println("profileMap", map);
		println();
		printNodeInfo(ProfileManager.transports.getDocumentElement());
//		ProfileManager.saveTransports();
	}

	private void printNodeInfo(Object item) {
		NodeList nodes = evaluateXPath(item, "./*");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element el = (Element) nodes.item(i);
			NamedNodeMap attributes = el.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Node node = attributes.item(j);
				println("%s : %s", node.getNodeName(), node.getNodeValue());
			}
		}
	}

	public static String encodeXml(String string, boolean decode) {
		return decode ? StringEscapeUtils.unescapeXml(string) : StringEscapeUtils.escapeXml(string);
	}

	public void testEncode() {
		String uriString = "file:///tmp/temp.db?title=TEXT&note=TEXT&created=INTEGER#notes";
		println(uriString = codeUri(uriString, false));
		println(uriString = codeUri(uriString, true));
		println(uriString = encodeXml(uriString, false));
		println(uriString = encodeXml(uriString, true));
	}

	public void testConversion() {
		Object time = now();
		println(time = ScriptManager.doConversion(time, "unixepoch", "push"));
		println(time = ScriptManager.doConversion(time, "unixepoch", "pull"));
		println(time = ScriptManager.doConversion("" + time, "unixepoch", "push"));
		println(time = ScriptManager.doConversion("" + time, "unixepoch", "pull"));
		println(time = ScriptManager.doConversion(null, "unixepoch", "push"));
		println(time = ScriptManager.doConversion(null, "unixepoch", "pull"));
	}

	public void testTemplate() {
    	String template = " `xxx``yyy`\f`zzz` ";
    	printMatchResults(template, TransportBuilder.TEMPLATE_PATTERN);
    	underTest = true;
    	ValList projection = builder.evaluateTemplate(template, null);
		assertEquals(3, projection.size());
		assertEquals("xxx", projection.get(0));
		assertEquals("yyy", projection.get(1));
		assertEquals("zzz", projection.get(2));
		assertEquals(4, builder.fieldSeparators.length);
		for (int i = 0; i < builder.fieldSeparators.length; i++) {
			String s = builder.fieldSeparators[i];
			println(Arrays.toString(strings(s, escapeAwkRegex(s))));
		}
		printMatchResults("2.0(6h)", WeatherManager.PRECIPITATION_PATTERN);
		printMatchResults("Tr(12h)", WeatherManager.PRECIPITATION_PATTERN);
	}

	Uri uri = getConstantByName("CONTENT_URI", "com.applang.provider.WeatherInfo", "Weathers");
	
    private String createNotePad() throws Exception {
    	BerichtsheftActivity activity = BerichtsheftActivity.getInstance();
    	activity.deleteDatabase(NotePadProvider.DATABASE_NAME);
		long now = now();
		String pattern = DatePicker.calendarFormat;
		long today = toTime(formatDate(now, pattern), pattern);
		ProviderTests.generateNotePadData(activity, true, 
				new Object[][] {
					{ 1L, "kein", "Kein", null, now }, 
					{ 2L, "fehler", "Fehler", null, now }, 
					{ 3L, "efhler", "eFhler", null, now }, 
					{ 4L, "ehfler", "ehFler", null, now }, 
					{ 5L, "im", "im", null, now }, 
					{ 6L, "system", "System", null, now }, 
					{ 1L, "Velocity1", "$kein $fehler $im $system", today, now }, 
					{ 2L, "Velocity2", "$kein $efhler $im $system", today - getMillis(2), now }, 	
					{ 3L, "Velocity3", "$kein $ehfler $im $system", today - getMillis(1), now }, 	
				});
		
    	uri = getConstantByName("CONTENT_URI", "com.applang.provider.NotePad", "NoteColumns");
    	File dbFile = getDatabaseFile(activity, uri);
    	String dbPath = tempPath() + "/temp.db";
    	copyFile(dbFile, new File(dbPath));
    	return dbPath;
    }
	
    private String createWeatherInfo() throws Exception {
    	BerichtsheftActivity activity = new BerichtsheftActivity();
    	activity.deleteDatabase(WeatherInfoProvider.DATABASE_NAME);
		
		long now = now();
		String pattern = DatePicker.calendarFormat;
		long today = toTime(formatDate(now, pattern), pattern);
		ContentResolver contentResolver = activity.getContentResolver();
		ProviderTests.generateData(contentResolver, Weathers.CONTENT_URI, true, new Object[][] {
			{ 1L, "here", "overcast", 11.1f, 1f, -1f, today, now }, 	
		});
		
    	uri = getConstantByName("CONTENT_URI", "com.applang.provider.WeatherInfo", "Weathers");
    	File dbFile = getDatabaseFile(activity, uri);
    	String dbPath = tempPath() + "/temp.db";
    	copyFile(dbFile, new File(dbPath));
    	return dbPath;
    }

    public Object unixepoch(String oper, Object value) {
    	Object result = null;
    	String pattern = DatePicker.calendarFormat;
    	if ("pull".equals(oper))
    		result = toTime(value.toString(), new Object[]{pattern});
    	else if ("push".equals(oper))
    		result = formatDate(Long.valueOf(stringValueOf(value)), new Object[]{pattern});
    	return result;
    }

    public void _testUnixepoch() {
    	println(unixepoch("push", ""));
    	println(unixepoch("push", null));
    }
	
	@SuppressWarnings("unchecked")
	public void testProjection() throws Exception {
		setTimeout(1000);
    	String dbPath = createNotePad();
    	String tableName = "notes";
    	String flavor = "com.applang.provider.NotePad";
		Uri uri = fileUri(dbPath, tableName);
		DataView dv = new DataView();
		dv.setUri(uri);
		DataAdapter dataAdapter = new DataAdapter(dv);
		BidiMultiMap projection = new BidiMultiMap(dataAdapter.info.getList("name"));
		projection.removeKey("_id");
		Context context = BerichtsheftActivity.getInstance();
		context.setFlavor(flavor);
		ProjectionModel projectionModel = new ProjectionModel(context, uri, flavor, projection);
		projectionModel.injectFlavor();
		projectionModel = DataView.askProjection(context, null, projectionModel, AlertDialog.behavior);
		projection = projectionModel.getExpandedProjection();
		String sortOrder = projectionModel.getSortOrder();
		println(projection);
		boolean unchanged = !projectionModel.changed;
		if (unchanged) assertThat((Boolean) projection.getValue("_id", 3), is(equalTo(false)));
		DataModel dataModel = dataAdapter.query(dv.getUriString(), projection, null,null, sortOrder);
		projection = dataModel.getProjection();
		if (unchanged) {
			assertThat(projection.getKeys().size(), is(equalTo(4)));
			assertThat(dataModel.data.get(0).size(), is(equalTo(5)));
			assertThat(dataModel.columns.size(), is(equalTo(5)));
			assertThat(dataModel.conversions.length, is(equalTo(5)));
			assertThat(dataModel.getColumnCount(), is(equalTo(4)));
			assertThat((Class<String>) dataModel.getColumnClass(0),
					is(equalTo(String.class)));
			assertThat((Class<String>) dataModel.getColumnClass(1),
					is(equalTo(String.class)));
			assertThat((Class<Long>) dataModel.getColumnClass(2),
					is(equalTo(Long.class)));
			assertThat((Class<Long>) dataModel.getColumnClass(3),
					is(equalTo(Long.class)));
			long unixepoch = -1L;
			for (int i = 0; i < dataModel.getRowCount(); i++) {
				assertThat((Long) dataModel.getValueAt(i, 2), 
						greaterThanOrEqualTo(unixepoch));
				unixepoch = (Long) dataModel.getValueAt(i, 2);
			}
		}
		JTable table = dataModel.makeTable();
		new AlertDialog.Builder(context)
				.setTitle("testProjection")
				.setView(new View(scrollableViewport(table, new Dimension(400,100))))
				.setNeutralButton(android.R.string.close, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}})
				.create()
				.open();
		unsetTimeout();
	}
	
	public void testDataConfig() {
		dataConfigTest(new Job<DataConfiguration>() {
			public void perform(DataConfiguration dc, Object[] parms) throws Exception {
				println("data configuration\n" +
						"\turi\t%s\n" +
						"\tpath\t%s\n" +
						"\ttable\t%s\n" +
						"\tflavor\t%s\n" +
						"\tsortOrder\t%s\n" +
						"\tprojection\t%s", 
						dc.getUri(), 
						dc.getPath(), 
						dc.getTableName(), 
						dc.getFlavor(), 
						dc.getSortOrder(), 
						dc.getProjection());
			}
		});
	}
	
	public void dataConfigTest(Job<DataConfiguration> job, Object...params) {
		println(dbPath(null));
		String orig_uriString = getSetting("uri", null);
		String orig_database = getSetting("database", null);
		try {
			putSetting("uri", "");
			putSetting("database", "");
			DataConfiguration dc = new DataConfiguration(new BerichtsheftActivity(), null, null);
			while (dc.display(null))
				try {
					job.perform(dc, params);
				} catch (Exception e) {
					fail(e.getMessage());
				}
		}
		finally {
			putSetting("uri", orig_uriString);
			putSetting("database", orig_database);
		}
	}
	
	public void testDataConfig2() throws Exception {
    	String dbPath = createNotePad();
    	String tableName = "notes";
		DataView dv = new DataView();
		Uri uri = fileUri(dbPath, tableName);
		assertTrue(isFileUri(stringValueOf(uri)));
		dv.setUri(uri);
		Context context = new BerichtsheftActivity();
		DataConfiguration dataConfig = dv.getDataConfiguration();
		if (dataConfig.getProjectionModel() == null) {
			ProjectionModel pmodel = new ProjectionModel(context, uri, null);
			dataConfig.setProjectionModel(pmodel);
		}
		while (dv.configureData(null, true)) {
			println(dv.getUri());
			println(dv.getFlavor());
			BidiMultiMap projection = 
					dataConfig.getProjectionModel().getExpandedProjection();
			DataModel model = new DataModel().setProjection(projection);
			println(model.columns);
			println(model.getProjection());
			dataConfig.save();
		}
	}

    public void testWeatherInfo() throws Exception {
    	String dbPath = createWeatherInfo();
    	final WeatherManager wm = new WeatherManager();
		if (wm.openConnection(dbPath)) {
			wm.parseAndEvaluate("10519", DatePicker.Period.loadParts(0), true, 
				new Function<Void>() {
					public Void apply(Object... params) {
						wm.closeConnection();
						return null;
					}
				});
			startFrame(null);
		}
    }

    public void testUpdateOrInsert() throws Exception {
    	String dbPath = createNotePad();
    	String uriString = fileUri(dbPath, "notes").toString();
		DataAdapter dataAdapter = new DataAdapter(uriString);
		BidiMultiMap projection = builder.elaborateProjection(objects("title", "note", "created"), null, "");
       	ContentValues values = contentValues(dataAdapter.info, projection.getKeys(), "title", "note", now());
		ValMap profile = ProfileManager.getProfileAsMap("tagesberichte", "pull");
       	
		String pk = BaseColumns._ID;
		Object result = dataAdapter.updateOrInsert(uriString, profile, projection, pk, values);
		assertNotNull(result);
		assertTrue(result instanceof Uri);
		long id = ContentUris.parseId((Uri) result);
		assertThat(id, is(greaterThan(-1l)));
		result = dataAdapter.updateOrInsert(uriString, profile, projection, pk, values);
		assertNotNull(result);
		assertTrue(result instanceof Integer);
		assertThat((Integer)result, is(greaterThan(0)));
		
		projection.insert(0, pk, null);
		values.putNull(pk.toString());
		result = dataAdapter.updateOrInsert(uriString, profile, projection, null, values);
		assertNotNull(result);
		assertTrue(result instanceof Uri);
		assertThat(ContentUris.parseId((Uri) result), is(lessThan(0l)));
		values.put("created", now());
		result = dataAdapter.updateOrInsert(uriString, profile, projection, "", values);
		assertNotNull(result);
		assertTrue(result instanceof Uri);
		assertThat(ContentUris.parseId((Uri) result), allOf(greaterThan(-1l), not(equalTo(id))));
	}

	private String generateTagesberichteTemplate(String oper) {
		underTest = true;
		BerichtsheftPlugin.setOptionProperty("record-decoration", "fold");
		BerichtsheftPlugin.setOptionProperty("record-separator", "whitespace");
		if ("push".equals(oper)) {
			return " `created|unixepoch` '`title`'\n`note`\n";
		}
		else if ("pull".equals(oper)) {
			return " `modified|now``created|unixepoch` '`title`'\n`note`\n";
		}
		return null;
    }

	public void testScan() throws Exception {
		setTimeout(1000);
		String template = generateTagesberichteTemplate("pull");
		ValList fields = builder.evaluateTemplate(template, null);
		assertTrue(isAvailable(0, fields));
		BidiMultiMap projection = builder.elaborateProjection(fields.toArray(), null, "");
		assertNotNull(projection);
		println(projection);
		InputStream is = HelperTests.class.getResourceAsStream("tagesberichte.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		DataView.DataModel model = builder.scan(reader, projection);
		is.close();
		assertNotNull(model);
		println(model);
		JTable table = model.makeTable();
    	DataDockable.showItems(null,
				"datadock.transport-from-buffer.label",
				String.format("%d record(s)", model.getRowCount()),
				table, 
				JOptionPane.DEFAULT_OPTION,
				AlertDialog.behavior,
				null, -2);
		unsetTimeout();
	}
	
	TransportBuilder builder = new TransportBuilder();
	
	public void testRoundTrip() throws Exception {
		setTimeout(1000);
    	String dbPath = createNotePad();
    	String uriString = fileUri(dbPath, "notes").toString();
    	DataAdapter dataAdapter = new DataAdapter(uriString);
		String name = "tagesberichte";
		ValMap profile = ProfileManager.getProfileAsMap(name, "push");
		Object flavor = profile.get("flavor");
		String[] full = toStrings(fullProjection(flavor));
		println(com.applang.Util.toString(dataAdapter.query(uriString, full)));
		Object[][] mods = dataAdapter.query(uriString, strings("_id","modified","title"));
//		println(com.applang.toString(mods));
		String template = stringValueOf(profile.get("template"));
    	ValList list = builder.evaluateTemplate(template, profile);
		BidiMultiMap projection = builder.elaborateProjection(list.toArray(), dataAdapter.info.getList("name"), dataAdapter.getTableName());
		DataView.DataModel model = dataAdapter.query(uriString, projection);
    	JTable table = model.makeTable();
		DataDockable.showItems(null, 
				"datadock.transport-to-buffer.label", 
				String.format("%d record(s)", model.getRowCount()),
				table, 
				JOptionPane.DEFAULT_OPTION, 
				AlertDialog.behavior, 
	    		null, -1);
		String text = builder.wrapRecords(table);
		println(text);
		profile = ProfileManager.getProfileAsMap(name, "pull");
		template = stringValueOf(profile.get("template"));
    	list = builder.evaluateTemplate(template, profile);
		projection = builder.elaborateProjection(list.toArray(), dataAdapter.info.getList("name"), dataAdapter.getTableName());
		model = builder.scan(new StringReader(text), projection);
		table = model.makeTable();
    	DataDockable.showItems(null,
				"datadock.transport-from-buffer.label",
				String.format("%d record(s)", model.getRowCount()),
				table, 
				JOptionPane.DEFAULT_OPTION,
				AlertDialog.behavior,
				null, -1);
//    	profile.put("name", "tagesbirichte");
    	int[] results = dataAdapter.pickRecords(null, table, uriString, profile);
//		assertNotNull("process canceled", results);
		println("results", results);
		println(com.applang.Util.toString(dataAdapter.query(uriString, full)));
		for (int i = 0; i < mods.length; i++) {
			Object[] mod = mods[i];
			Object[][] m = dataAdapter.query(uriString, strings("modified"), "_id=?", strings(mod[0].toString()));
			if ((Long)m[0][0] > (Long)mod[1])
				println(String.format("record '%s' updated", mod[2]));
		}
		unsetTimeout();
	}
	
	public void testQuery() throws Exception {
		setTimeout(1000);
    	String dbPath = createNotePad();
    	String uriString = fileUri(dbPath, "notes").toString();
    	String flavor = "com.applang.provider.NotePad";
    	DataAdapter dataAdapter = new DataAdapter(uriString);
    	Object[] fields = fullProjection(flavor);
    	ValList list = builder.evaluateTemplate(builder.makeTemplate(fields), null);
    	assertTrue(Arrays.equals(fields, list.toArray()));
    	ValMap map = ProfileManager.getProfileAsMap("tagesberichte", "push");
    	println(map);
    	assertTrue(map.containsKey("template"));
    	list = builder.evaluateTemplate(map.get("template").toString(), map);
		BidiMultiMap projection = builder.elaborateProjection(list.toArray(), dataAdapter.info.getList("name"), dataAdapter.getTableName());
		assertNotNull(projection);
		DataView.DataModel model = dataAdapter.query(uriString, projection);
		println(model);
    	final JTable table = model.makeTable();
		int result = DataDockable.showItems(null, "berichtsheft.transport-to-buffer.label", 
				String.format("%d record(s)", model.getRowCount()),
				table, 
				JOptionPane.OK_CANCEL_OPTION, 
				AlertDialog.behavior, 
	    		new Job<Void>() {
					public void perform(Void t, Object[] params) throws Exception {
						String text = builder.wrapRecords(table);
						println(text);
					}
				}, -1);
		println(result);
		unsetTimeout();
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean generateAwkScript(String awkFileName, Object...params) {
		String uriString = getProperty("TRANSPORT_URI");
		if (nullOrEmpty(uriString))
			return false;
		String tableName = dbTableName(uriString);
		Map map = getMapSetting("TRANSPORT_MAPPINGS", vmap());
		String template = param((String) map.get(uriString), 0, params);
		if (template == null)
			return false;
		TransportBuilder builder = new TransportBuilder();
		ValList projection = builder.evaluateTemplate(template, null);
		boolean retval = isAvailable(0, projection);
		if (retval) {
			setProperty("TRANSPORT_TEMPLATE", template);
			map.put(uriString, template);
			putMapSetting("TRANSPORT_MAPPINGS", map);
			ValMap info = table_info(
					new BerichtsheftActivity(), 
					Uri.parse(uriString), 
					tableName);
			Object pk = info.get("PRIMARY_KEY");
			boolean additionalPk = projection.indexOf(pk) < 1;
			if (additionalPk)
				projection.add(pk);
			StringBuilder sb = new StringBuilder();
			sb.append("function assert(condition, string)\n" +
					"{\n" +
					"\tif (! condition) {\n" +
					"\t\tprintf(\"%s:%d: assertion failed: %s\", FILENAME, FNR, string) > \"/dev/stderr\"\n" +
					"\t\t_assert_exit = 1\n" +
					"\t\texit 1\n" +
					"\t}\n" +
					"}\n");
			sb.append("BEGIN {\n");
			sb.append(String.format("\tFS = \"%s\"\n", escapeAwkRegex(builder.fieldSeparator[0])));
			sb.append(String.format("\tRS = \"%s\"\n", 
					escapeAwkRegex(builder.recordDecoration[1]) + 
					escapeAwkRegex(builder.recordSeparator[0]) + 
					escapeAwkRegex(builder.recordDecoration[0])));
			sb.append("\t");
			int length = builder.fieldSeparators.length;
			for (int i = 0; i < length; i++) {
				String sep = escapeAwkRegex(builder.fieldSeparators[i]);
				if (i == 0)
					sep = "^" + sep;
				else {
					sb.append(" ; ");
					if (i == length - 1)
						sep += "$";
				}
				sb.append("sep[" + i + "] = \"" + sep + "\"");
			}
			sb.append("\n");
			sb.append("\tprint \"BEGIN TRANSACTION;\"\n");
			sb.append("}\n");
			sb.append("{\n");
			sb.append("\trecord = $0\n");
			sb.append("\tif (NR < 2)\n");
			sb.append("\t\tgsub(/^" + escapeAwkRegex(builder.recordDecoration[0]) + "/, \"\", record)\n");
			sb.append("\tgsub(/" + escapeAwkRegex(builder.recordDecoration[1]) + "$/, \"\", record)\n");
			sb.append("\tfor (i = 0; i < " + length + "; i++)\n");
			sb.append("\t{\n");
			sb.append("\t\tm = match(record,sep[i])\n");
			sb.append("\t\tassert(m > 0, sprintf(\"sep[%d] not matched\", i))\n");
			sb.append("\t\tif (i < 1)\n");
			sb.append("\t\t\tassert(RSTART == 1, sprintf(\"sep[0] matched at position %d\", RSTART))\n");
			sb.append("\t\telse\n");
			sb.append("\t\t\t$i = substr(record,1,RSTART-1)\n");
			sb.append("\t\trecord = substr(record,RSTART+RLENGTH)\n");
			sb.append("\t}\n");
			sb.append("\tlen = length(record)\n");
			sb.append("\tassert(len < 1, sprintf(\"%d chars left unmatched\", len))\n");
			sb.append(String.format("\tprint \"INSERT INTO %s ", tableName));
			Object[] array = projection.toArray();
			sb.append(String.format("(%s) ", join(",", array)));
			ValList fieldNames = info.getList("name");
			for (int i = 0; i < array.length; i++) {
				int index = fieldNames.indexOf(array[i]);
				String type = info.getListValue("type", index).toString();
				if (additionalPk && array[i].equals(pk))
					array[i] = "\" \"null\" \"";
				else {
					array[i] = "\" $" + (i + 1) + " \"";
					if ("TEXT".compareToIgnoreCase(type) == 0)
						array[i] = enclose("'", array[i].toString());
				}
			}
			sb.append(String.format("VALUES (%s)", join(",", array)));
			sb.append(";\"\n");
			sb.append("}\n");
			sb.append("END {\n");
			sb.append("\tif (_assert_exit) exit 1\n");
			sb.append("\tprint \"COMMIT;\"\n");
			sb.append("}\n");
			contentsToFile(new File(awkFileName), sb.toString());
		}
		return retval;
	}

	public static String escapeAwkRegex(String s) {
		String escaped = "";
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case 7: escaped += "\\a"; break;
			case 8: escaped += "\\b"; break;
			case 9: escaped += "\\t"; break;
			case 10: escaped += "\\n"; break;
			case 11: escaped += "\\v"; break;
			case 12: escaped += "\\f"; break;
			case 13: escaped += "\\r"; break;
			case '"': escaped += "\\\""; break;
			case '/': escaped += "\\/"; break;
			case '^': escaped += "\\^"; break;
			case '$': escaped += "\\$"; break;
			case '.': escaped += "\\."; break;
			case '[': escaped += "\\["; break;
			case ']': escaped += "\\]"; break;
			case '|': escaped += "\\|"; break;
			case '(': escaped += "\\("; break;
			case ')': escaped += "\\)"; break;
			case '{': escaped += "\\{"; break;
			case '}': escaped += "\\}"; break;
			case '*': escaped += "\\*"; break;
			case '+': escaped += "\\+"; break;
			case '?': escaped += "\\?"; break;
			case ' ': escaped += " "; break;
			default:
				escaped += "\\" + Integer.toOctalString(ch);
				break;
			}
		}
		return escaped;
	}

	public void _testAwkScript() {
		DataView dataView = new DataView();
		dataView.load(uri);
		String text = "{{{Berufsschule	H	Friday, 17.May.2013}}}\n" +
				"{{{Bericht	XXX	Wednesday, 22.May.2013}}}\n" +
				"{{{Bemerkung	V	Thursday, 18.Jul.2013}}}";
		String path = BerichtsheftPlugin.getTempFile("transport.txt").getPath();
		contentsToFile(new File(path), text);
		String awkFileName = join(".", path.substring(0, path.lastIndexOf('.')), "awk");
		if (generateAwkScript(awkFileName)) {
			setProperty("AWK_PROGFILE", awkFileName);
			String sqliteFileName = join(".", path.substring(0, path.lastIndexOf('.')), "sql");
			setProperty("AWK_INFILE", path);
			setProperty("AWK_OUTFILE", sqliteFileName);
			setProperty("SQLITE_FILE", sqliteFileName);
			setProperty("SQLITE_DBFILE", dataView.getDataConfiguration().getPath());
		}
    }
	
	private JLabel label;
	private JButton cancel;
	private JButton start;
	private JTextField textField;

	class RandomWorker extends Task<String> {
 
		public RandomWorker(Job<String> followUp, Object... params) {
			super(null, followUp, params);
		}

		private Random r = new Random();
 
		@Override
		protected String doInBackground() throws Exception {
			long time = System.currentTimeMillis();
			while (System.currentTimeMillis() - time < 5000 && !Thread.currentThread().isInterrupted()) {
				r.nextInt();
			}
			return String.valueOf(r.nextInt(10) + 1);
		}
	}
	
	SwingWorker<?,?> worker;
	 
	private void cancel() {
		start.setEnabled(true);
		label.setVisible(false);
		cancel.setEnabled(false);
		if (worker != null) {
			worker.cancel(true);
			worker = null;
		}
 
	}
	
	@SuppressWarnings("unused")
	private void start() {
		start.setEnabled(false);
		textField.setText("");
		worker = new RandomWorker(new Job<String>() {
			public void perform(String message, Object[] params) throws Exception {
				textField.setText(message);
				worker = null;
				cancel();
			}
		});
		worker.execute();
		label.setVisible(true);
		cancel.setEnabled(true);
	}
	
	private void start2() {
		start.setEnabled(false);
		textField.setText("");
		String t = 
				"#set($key=\"\")\n" +
				"#prompt(\"label\",$key,\"value\")\n" +
				"$key";
		worker = new EvaluationTask(new BerichtsheftActivity(), null, null, null, new Job<Object>() {
			public void perform(Object s, Object[] params) throws Exception {
				textField.setText(s.toString());
				worker = null;
				cancel();
			}
		}, t);
		worker.execute();
		label.setVisible(true);
		cancel.setEnabled(true);
	}
	
	public void _testSwingworker() throws Exception {
		showFrame(null, 
				"test", 
	    		new UIFunction() {
					@Override
					public Component[] apply(Component comp, Object[] parms) {
						JFrame frame = (JFrame)comp;
						JPanel panel = new JPanel();
						panel.setLayout(new GridLayout(2, 1));
				 
						JPanel buttons = new JPanel(new FlowLayout());
						start = new JButton("Start");
						start.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								start2();
							}
						});
						start.setDefaultCapable(true);
						buttons.add(start);
						cancel = new JButton("Cancel");
						cancel.setEnabled(false);
						cancel.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								cancel();
							}
						});
						buttons.add(cancel);
						panel.add(buttons);
				 
						ImageIcon image = iconFrom("/images/spinner.gif");
						label = new JLabel(image);
						JPanel answer = new JPanel(new GridLayout(1, 2));
						textField = new JTextField("");
						answer.add(textField);
						answer.add(label);
						label.setVisible(false);
						panel.add(answer);
				 
						frame.setTitle("Lucky result generator");
						frame.setPreferredSize(new Dimension(300, 100));
						frame.getContentPane().setLayout(new BorderLayout());
						frame.getContentPane().add(panel);
						
						return null;
					}
	    		},
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						return null;
					}
	    		},
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						return null;
					}
	    		},
	    		Behavior.TIMEOUT);
	}
	
	public void testOptionDialogModeless() {
		showFrame(null, "", 
			new UIFunction() {
				public Component[] apply(Component comp, Object[] parms) {
					final JFrame frame = (JFrame) comp;
					JButton btn = new JButton("modeless");
					btn.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							showOptionDialog(frame, "modeless", 
									"test",
									Behavior.ALWAYS_ON_TOP + JOptionPane.DEFAULT_OPTION, 
									JOptionPane.PLAIN_MESSAGE,
									null, 
									objects("opt1","opt2"), null, 
									new Function<Boolean>() {
										public Boolean apply(Object... params) {
											println("dialogResult " + dialogResult);
											return question("keep this dialog");
										}
									}
							);
						}
					});
					return components(btn);
				}
			}, 
			null, 
			null, 
			Behavior.TIMEOUT);
	}
	
	public void _testInputDialog() throws Exception {
		String title = "Text ";
		String prompt = "label";
		String value = "text";
    	for (int optionType = -1; optionType <= 3; optionType++) {
			Object result = showInputDialog(null, prompt, 
					title + (optionType < 3 ? "entry" : "info"),
					optionType, JOptionPane.PLAIN_MESSAGE,
					null, null, value);
			println(dialogResult, String.valueOf(result));
		}
	}
	
	public void _testOptionDialog() throws Exception {
		String title = "Option ";
		String prompt = "prompt";
		String value = "text";
    	for (int optionType = 2; optionType >= -1; optionType--) {
    		switch (optionType) {
			case 3:
				break;
			}
			Object result = showOptionDialog(null, prompt, 
					title + (optionType < 3 ? "entry" : "info"),
					optionType, JOptionPane.PLAIN_MESSAGE,
					null, 
					null, value);
			println(String.valueOf(result));
		}
	}
	
	public void testResizableOptionDialog() {
		JTextArea textArea = new JTextArea("");
		textArea.setEditable(true);
		textArea.setText(readAsset(BerichtsheftActivity.getInstance(), "Kein Fehler im System.txt"));
		ValList list = defaultOptions(12);
		Object initialOption = list.remove(-1);
		Object[] options = list.toArray();
		int option = showResizableDialog(textArea, null, new Function<Integer>() {
			public Integer apply(Object...parms) {
				JScrollPane scrollPane = new JScrollPane((Component) parms[0]);
				scrollPane.setPreferredSize(new Dimension(500,200));
				return JOptionPane.showOptionDialog(null, scrollPane, "testResizableOptionDialog", 
						JOptionPane.DEFAULT_OPTION, 
						JOptionPane.PLAIN_MESSAGE, 
						null, 
						(Object[])parms[1], parms[2]);
			}
		}, options, initialOption);
		println("dialogResult %d", option);
	}
	
	public void testLongTextEntry() {
		final JTextField entry = new JTextField(20);
		showDialog(null, null, "testLongTextEntry", 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						Box box = new Box(BoxLayout.X_AXIS);
						box.add(entry);
						box.add(makeCustomButton("datadock.choose-uri", new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								entry.setText(BerichtsheftPlugin.inquireDbFileName(null, null));
							}
						}, false));
						setMaximumDimension(box, 100);
						return components(box);
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						entry.addComponentListener(new ComponentAdapter() {
							@Override
							public void componentResized(ComponentEvent e) {
								String text = trimPath(uri.toString(), entry.getWidth(), entry.getFont(), entry);
								setLongText(entry, text);
							}
						});
						return null;
					}
				}, 
				null, 
				Behavior.MODAL);
	}
	
	public void testSingleChoice() {
		println(JOptionPane.showInputDialog(null, "states", "united", 
				JOptionPane.PLAIN_MESSAGE,
	            null,
	            getStateStrings(), null));
	}
	
	@SuppressWarnings("deprecation")
	public void testMultiChoice() {
		@SuppressWarnings({ "rawtypes", "unchecked" })
    	JList list = new JList(getStateStrings());
		JOptionPane.showMessageDialog(null, list, "united", 
				JOptionPane.PLAIN_MESSAGE);
		println(Arrays.toString(list.getSelectedValues()));
	}
	
	public void testTiming() throws Exception {
		showFrame(null, "frame",
				new UIFunction() {
					public Component[] apply(final Component comp, Object[] parms) {
						return components(new JButton(new AbstractAction("dialog") {
							public void actionPerformed(ActionEvent ev) {
								showDialog(comp, comp, "dialog", 
										new UIFunction() {
											public Component[] apply(final Component comp, Object[] parms) {
												return components(new JButton(new AbstractAction("timing") {
													public void actionPerformed(ActionEvent e) {
														waiting(comp, new ComponentFunction<Void>(){
															public Void apply(Component comp, Object[] parms) {
																Timing timing = param(null, 0, parms);
																while (timing.current() < 1000) {
																	println(timing.current(), comp);
																	delay(100);
																}
																return null;
															}
														});
													}
												}));
											}
										}, 
										null, 
										null, 
										Behavior.NONE);
							}
						}));
					}
				}, null, null, Behavior.TIMEOUT);
	}
	
	public void testDialogFeed() throws Exception {
		final int id = 1;
		TextView tv = new TextView(null, null);
		final AlertDialog dialog = new AlertDialog.Builder(new BerichtsheftActivity(), Behavior.NONE)
				.setView(tv)
				.setNeutralButton(android.R.string.close, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create();
		dialog.open();
		tv = (TextView) dialog.findViewById(id);
		tv.getComponent().setPreferredSize(new Dimension(100,100));
		tv.append("Random\n");
		startFrame(
				new AbstractAction("random") {
					@Override
					public void actionPerformed(ActionEvent ev) {
						try {
							Writer out = dialog.feed(id);
							out.write( String.format("%f%n", Math.random()) );
							out.close();
						} catch (Exception e) {
							Log.e(TAG, "", e);
						}
					}
				});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void _testUIDefaults() {
        Set defaults = UIManager.getLookAndFeelDefaults().entrySet();
        TreeSet ts = new TreeSet(new Comparator() {
           public int compare(Object a, Object b) {
              Map.Entry ea = (Map.Entry) a;
              Map.Entry eb = (Map.Entry) b;
              return stringValueOf(ea.getKey()).compareTo(stringValueOf(eb.getKey()));
           }
        });
        ts.addAll(defaults);
        for (Iterator i = ts.iterator(); i.hasNext();) {
           Map.Entry entry = (Map.Entry) i.next();
           print(entry.getKey() + " = " );
           println(entry.getValue());
        }
	}
	
	public void testTextToggle() {
		final TextToggle textToggle = new TextToggle();
		textToggle.createBufferedTextArea("velocity", "/modes/velocity_pure.xml");
		Container contentPane = new JPanel(new BorderLayout());
		contentPane.add(textToggle.getUIComponent());
		showForm(contentPane);
	}
	
	public void testTextEdit() {
		final Activity activity = BerichtsheftActivity.getInstance();
		LinearLayout linearLayout = linearLayout(activity, 2);
		activity.setContentView(linearLayout);
		ViewGroup contentPane = (ViewGroup) getContentView(activity);
		showForm(doLayout(contentPane, true), 
			new Job<View>() {
				public void perform(View vw, Object[] parms) throws Exception {
					TextEdit te = (TextEdit) vw;
					if (te.getTextToggle() != null) {
						te.showScriptArea(true);
						te.setScript(readAsset(activity, "hello.vm"));
						te.setText(te.getScript());
					}
				}
			}, 
			linearLayout.getChildAt(0));
	}
	
	public void testDataForm() throws Exception {
		setTimeout(1000);
		final Context context = BerichtsheftActivity.getInstance();
    	String dbPath = createNotePad();
    	String tableName = "bausteine";
    	String flavor = "com.applang.provider.NotePad";
		Uri uri = fileUri(dbPath, tableName);
		File dbFile = new File(dbPath);
		String uriString = uri.toString();
		ProjectionModel projectionModel = new ProjectionModel(context, uri, flavor);
		projectionModel.injectFlavor();
		while ((projectionModel = 
				DataView.askProjection(context, uriString, 
						projectionModel, AlertDialog.behavior, JOptionPane.OK_CANCEL_OPTION)) != null) {
			ManagerBase<?> manager = new ManagerBase<Object>();
			manager.installUpdate(manager);
			DataForm dataForm = new DataForm(context, 
					manager,
					projectionModel);
			println(dataForm.mProjectionModel);
			DataAdapter dataAdapter = new DataAdapter(
					projectionModel.getFlavor(), dbFile, uriString);
			DataModel model = dataAdapter.query(uriString, projectionModel.getExpandedProjection());
			if (model.getRowCount() < 1) 
				continue;
			JTable table = model.makeTable();
			table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			int retval = new AlertDialog(null, 
					"Pick a Record", 
					"", 
					scrollableViewport(table, new Dimension(400,200)), 
					JOptionPane.OK_CANCEL_OPTION, 
					AlertDialog.behavior, 
					null, null).open().getResult();
			if (retval > 0)
				continue;
			int rowIndex = table.getSelectedRow();
			String pk = stringValueOf(dataAdapter.info.get("PRIMARY_KEY"));
			Long id = (Long) model.getValues(false, rowIndex)[model.columns.indexOf(pk)];
			ValList result = vlist();
			for (int i = 0; i < model.getColumnCount(); i++) {
				result.add(model.getValueAt(rowIndex, i));
			}
			dataForm.setContent(result.toArray());
			showForm(dataForm.getContainer());
			if (manager.isDirty()) {
				ContentValues values = contentValues(dataAdapter.info, 
						dataForm.mProjectionModel.getProjection().getKeys(),
						dataForm.getContent());
				assertThat(dataAdapter.update(appendId(uriString, id), values), is(greaterThan(0)));
				Toast.makeText(context, String.format("record %d updated", id),
						Toast.LENGTH_LONG).show();
			}
		}
		unsetTimeout();
	}

	public LinearLayout linearLayout(Context context, int kind) {
		LinearLayout linearLayout = com.applang.Util1.linearLayout(context, 
        		LinearLayout.HORIZONTAL, 
        		LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        LayoutParams layoutParams = marginLayoutParams(
        		LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 
        		5,	5, 5, 5);
        TextEdit te;
        switch (kind) {
		case 1:
			te = new TextEdit(context, 
					Resources.attributeSet(context, 
							"feature=\"spellchecking\"", 
							"android:inputType=\"textMultiLine\""));
			te.setText("hello world");
			break;
		case 2:
			te = new TextEdit(context, 
					Resources.attributeSet(context, 
							"feature=\"togglable\"", 
							"mode=\"velocity\"", 
							"modeFile=\"/modes/velocity_pure.xml\"", 
							"android:inputType=\"textMultiLine\"", 
							"readOnly=\"true\""));
			break;
		default:
			TextView tv = new TextView(context);
			linearLayout.addView(tv, layoutParams);
			EditText et = new EditText(context);
			linearLayout.addView(et, layoutParams);
			tv.setText("hello");
			et.setText("world");
			return linearLayout;
		}
        linearLayout.addView(te, layoutParams);
		return linearLayout;
	}
	
	public void testLinearLayout() {
		Context context = BerichtsheftActivity.getInstance();
        LinearLayout linearLayout = linearLayout(context, 0);
		showForm(doLayout(linearLayout));
	}
	
	public void testRelativeLayout2() {
		Context context = BerichtsheftActivity.getInstance();
		RelativeLayout viewGroup = new RelativeLayout(context);
		TextView textView = new TextView(context);
		LayoutParams layoutParams = new RelativeLayout.LayoutParams(context, 
				Resources.attributeSet(context, 
						"android:layout_alignLeft=\"view__4\""));
		viewGroup.addView(textView, layoutParams);
		EditText editText = new EditText(context);
		layoutParams = new RelativeLayout.LayoutParams(context, 
				Resources.attributeSet(context, 
						"android:layout_above=\"view_3\"", 
						"android:layout_marginLeft=\"@dimen/margin\"", 
						"android:layout_marginTop=\"@dimen/margin_half\"", 
						"android:layout_marginRight=\"@dimen/margin_half\"",
						"android:layout_marginBottom=\"@dimen/margin_half\""));
		viewGroup.addView(editText, layoutParams);
		TextView tv = (TextView) viewGroup.findViewWithTag("*3");
		tv.setText("hello");
		EditText et = (EditText) viewGroup.findViewWithTag("*4");
		et.setText("world");
		Container container = doLayout(viewGroup);
		viewGroup.printSprings();
		showForm(container);
	}
	
	public void testRelativeLayout() {
		Context context = BerichtsheftActivity.getInstance();
		RelativeLayout viewGroup = buttonGroup(context);
		Container container = doLayout(viewGroup);
//		viewGroup.printSprings();
		showForm(container);
	}
	
	private RelativeLayout buttonGroup(Context context) {
		RelativeLayout viewGroup = new RelativeLayout(context);
		AttributeSet attributeSet = Resources.attributeSet(context, 
			"android:layout_marginLeft=\"@dimen/margin\"", 
			"android:layout_marginTop=\"@dimen/margin_half\"", 
			"android:layout_marginRight=\"@dimen/margin_half\"",
			"android:layout_marginBottom=\"@dimen/margin_half\"");
		addButton(context, "Button 1", viewGroup, attributeSet);
		addButton(context, "2", viewGroup, attributeSet);
		addButton(context, "Button 3", viewGroup, attributeSet);
		addButton(context, "Long-Named Button 4", viewGroup, attributeSet, 
				"android:layout_toLeftOf=\"view_5\"");
		addButton(context, "Button 5", viewGroup, attributeSet);
		return viewGroup;
	}
	
	private Button addButton(Context context, String text, ViewGroup vg, 
			AttributeSet attributeSet, Object...params) 
	{
		Button btn = new Button(context);
		btn.setText(text);
		RelativeLayout.LayoutParams layoutParams = attributeSet != null ?
				new RelativeLayout.LayoutParams(context, attributeSet) :
				new RelativeLayout.LayoutParams();
		layoutParams.addRules(context, params);
		vg.addView(btn, layoutParams);
		return btn;
	}
	
	public void testSpringLayout() {
		Spring sum = Spring.sum(Spring.constant(0, 0, 0), Spring.constant(5, 5, 5));
		println(sum.getValue());
		Context context = BerichtsheftActivity.getInstance();
		RelativeLayout viewGroup = buttonGroup(context);
    	Container container = viewGroup.getContainer();
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View view = viewGroup.getChildAt(i);
			container.add(view.getComponent());
		}
//		viewGroup.finalLayout();
		springLayout(viewGroup);
		showForm(container);
	}

	private void springLayout(RelativeLayout viewGroup) {
		SpringLayout springLayout = viewGroup.getLayout();
        Spring pad = Spring.constant(5);
        Spring ySpring = pad;
        Spring xSpring = pad;
        Spring maxHeightSpring = Spring.constant(0);
		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			int j = i==2?3:(i==3?2:i);
			View view = viewGroup.getChildAt(j);
            SpringLayout.Constraints cons = springLayout.getConstraints(view.getComponent());
            cons.setX(xSpring);
            cons.setY(ySpring);
            if (j == -3) {
            	View anchor = viewGroup.getChildAt(2);
            	SpringLayout.Constraints anchorCons = springLayout.getConstraints(anchor.getComponent());
            	anchorCons.setY(Spring.sum(
						pad,
						cons.getConstraint(SpringLayout.SOUTH)));
            	ySpring = Spring.sum(pad, cons.getConstraint("South"));
            }
            else
            	xSpring = Spring.sum(pad, cons.getConstraint("East"));
            maxHeightSpring = Spring.max(maxHeightSpring, cons.getConstraint("South"));
		}
		SpringLayout.Constraints cons = springLayout.getConstraints(viewGroup.getContainer());
        cons.setConstraint("East", xSpring);
        cons.setConstraint("South", Spring.sum(maxHeightSpring, ySpring));
	}
	
	public void testSpringLayout2() {
		Context context = BerichtsheftActivity.getInstance();
		RelativeLayout relativeLayout = relativeLayout(context, 
        		LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		TextView tv = new TextView(context);
		tv.setText("hello");
		relativeLayout.addView(tv, tv.getLayoutParams());
		EditText et = new EditText(context);
		et.setText("world");
        relativeLayout.addView(et, et.getLayoutParams());
        
		Container container = doLayout(relativeLayout);
		assertTrue(swapComponents(tv.getComponent(), et.getComponent()));
		
        SpringLayout springLayout = relativeLayout.getLayout();
		SpringLayout.Constraints tvCons = 
        		springLayout.getConstraints(tv.getComponent());
        SpringLayout.Constraints etCons =
        		springLayout.getConstraints(et.getComponent());
        tvCons.setX(
        		Spring.sum(Spring.constant(5),
                etCons.getConstraint(SpringLayout.EAST)));

        relativeLayout.printSprings();
        showForm(container);
	}
	
	public void testFormBuilder() {
		Context context = BerichtsheftActivity.getInstance();
		String resName = "standard_form.xml";	//	
		DataForm dataForm = new DataForm(context, new ManagerBase<Object>(), null, resName);
/*
 */		
		dataForm.builder.addStringField("String");
		dataForm.builder.addIntegerField("Integer");
		dataForm.builder.addFloatField("Float");
		dataForm.builder.addBlobField("Blob");
		dataForm.builder.addTextField("Text");
		showForm(dataForm.getContainer());
	}
	
	public void testForm() {
		setTimeout(1000);
		setupSpellChecker(BerichtsheftApp.applicationDataPath());
		final Context context = BerichtsheftActivity.getInstance();
		dataConfigTest(new Job<DataConfiguration>() {
			public void perform(DataConfiguration dc, Object[] parms) throws Exception {
				ProjectionModel projectionModel = dc.getProjectionModel();
				DataForm dataForm = new DataForm(context, 
						new ManagerBase<Object>(), 
						projectionModel);
				println(dataForm.mProjectionModel);
				File dbFile = new File(dc.getPath());
				String uriString = dc.getUri().toString();
				DataAdapter dataAdapter = new DataAdapter(
						projectionModel.getFlavor(), 
						dbFile, 
						uriString);
				ValList keys = dataForm.mProjectionModel.getProjection().getKeys();
				Object[][] result = dataAdapter.query(uriString, toStrings(keys));
				if (isAvailable(0, result)) 
					dataForm.setContent(result[0]);
				showForm(dataForm.getContainer());
			}
		});
		unsetTimeout();
	}
	
	public void testForm1() {
		Context context = BerichtsheftActivity.getInstance();
		ViewGroup viewGroup = single_button(context);
		showForm(ViewGroup.build(viewGroup, true));
	}
	
	private RelativeLayout single_button(Context context) {
		RelativeLayout relativeLayout = new RelativeLayout(context);
		AttributeSet attributeSet;
		ImageButton imbtn = new ImageButton(context, attributeSet = Resources.attributeSet(context, 
				"android:id=\"@+id/button1\"", 
				"android:src=\"@drawable/dropdown\"", 
				"android:layout_width=\"wrap_content\"", 
				"android:layout_height=\"wrap_content\"", 
				"android:layout_alignParentRight=\"true\"", 
				"android:layout_alignParentBottom=\"true\"", 
				"android:layout_marginLeft=\"@dimen/margin_half\"", 
				"android:layout_marginTop=\"@dimen/margin_half\"", 
				"android:layout_marginRight=\"@dimen/margin_half\"",
				"android:layout_marginBottom=\"@dimen/margin_half\""));
		relativeLayout.addView(imbtn, new RelativeLayout.LayoutParams(context, attributeSet));
		return relativeLayout;
	}
	
	public void testForm2() {
		Context context = BerichtsheftActivity.getInstance();
		DataForm dataForm = new DataForm(context, 
				new ManagerBase<Object>(), 
				null, 
				R.layout.construct_form_header);
		ViewGroup viewGroup = construct_form_header(context);
		println(viewGroup);
		showForm(dataForm.getContainer());
	}
	
	private RelativeLayout construct_form_header(Context context) {
		RelativeLayout relativeLayout = new RelativeLayout(context);
		AttributeSet attributeSet;
		ImageButton imbtn = new ImageButton(context, attributeSet = Resources.attributeSet(context, 
				"android:id=\"@+id/button1\"", 
				"android:src=\"@drawable/dropdown\"", 
				"android:layout_width=\"wrap_content\"", 
				"android:layout_height=\"wrap_content\"", 
				"android:layout_alignParentLeft=\"true\"", 
				"android:layout_alignParentTop=\"true\"", 
				"android:layout_marginLeft=\"@dimen/margin_half\"", 
				"android:layout_marginTop=\"@dimen/margin_half\"", 
				"android:layout_marginRight=\"@dimen/margin_half\"",
				"android:layout_marginBottom=\"@dimen/margin_half\""));
		relativeLayout.addView(imbtn, new RelativeLayout.LayoutParams(context, attributeSet));
		Button btn = new Button(context, attributeSet = Resources.attributeSet(context, 
				"android:id=\"@+id/cancel\"", 
				"android:text=\"@android:string/cancel\"", 
				"android:layout_width=\"wrap_content\"", 
				"android:layout_height=\"wrap_content\"", 
				"android:layout_alignParentRight=\"true\"", 
				"android:layout_alignParentTop=\"true\"", 
				"android:layout_marginLeft=\"@dimen/margin_half\"", 
				"android:layout_marginTop=\"@dimen/margin_half\"",
				"android:layout_marginRight=\"@dimen/margin_half\"",
				"android:layout_marginBottom=\"@dimen/margin_half\""));
		relativeLayout.addView(btn, new RelativeLayout.LayoutParams(context, attributeSet));
		btn = new Button(context, attributeSet = Resources.attributeSet(context, 
				"android:id=\"@+id/ok\"", 
				"android:text=\"@android:string/ok\"", 
				"android:layout_width=\"wrap_content\"", 
				"android:layout_height=\"wrap_content\"", 
				"android:layout_toLeftOf=\"@id/cancel\"", 
				"android:layout_alignParentTop=\"true\"", 
				"android:paddingLeft=\"@dimen/padding\"", 
			    "android:paddingRight=\"@dimen/padding\"", 
				"android:layout_marginLeft=\"@dimen/margin_half\"", 
				"android:layout_marginTop=\"@dimen/margin_half\"", 
				"android:layout_marginBottom=\"@dimen/margin_half\""));
		relativeLayout.addView(btn, new RelativeLayout.LayoutParams(context, attributeSet));
		return relativeLayout;
	}

	public Container doLayout(ViewGroup viewGroup, Object...params) {
		println(viewGroup);
		final Container container = ViewGroup.build(viewGroup, true);
		container.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				printContainer("form", container.getParent(), false);
			}
		});
		if (param_Boolean(false, 0, params))
			container.setPreferredSize(new Dimension(100,100));
 		return container;
	}

	public void showForm(final Container container, Object...params) {
		Deadline.WAIT = 3000;
		showFrame(null, "Form", 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						return components(container);
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						Job<View> job = param(null, 0, parms);
						if (job != null)
							try {
								job.perform((View) param(null, 1, parms), parms);
							} catch (Exception e) {
								Log.e(TAG, "showForm", e);
							}
						printContainer("form", (Container)comp, true);
						LayoutManager layout = container.getLayout();
						if (layout instanceof SpringLayout) {
							SpringLayout springLayout = (SpringLayout) layout;
							for (int i = 0; i < container.getComponentCount(); i++) {
								Component component = container.getComponent(i);
								SpringLayout.Constraints cons = springLayout.getConstraints(component);
								no_println(
										cons.getConstraint(SpringLayout.WEST).getValue(),
										cons.getConstraint(SpringLayout.EAST).getValue());
							}
						}
						return null;
					}
				}, 
				null, 
				Behavior.TIMEOUT,
				params);
		Deadline.WAIT = 1000;
	}
	
	@SuppressWarnings("resource")
	public void testResources() throws IOException {
		String fileName = "BerichtsheftPlugin.props";
		assertNull(resourceUrlOf(fileName, Constraint.END));
		String path = pathCombine(relativePath(), fileName);
		assertTrue(fileExists(path));
		path = pathCombine(System.getProperty("jedit.settings.dir"), "jars/BerichtsheftPlugin.jar");
		JarFile jarFile = new JarFile(new File(path));
		JarEntry jarEntry = jarFile.getJarEntry(fileName);
		InputStream inputStream = jarFile.getInputStream(jarEntry);
		assertNotNull(inputStream);
		inputStream.close();
		assertEquals(-65536, Resources.colorValue(BerichtsheftActivity.getInstance(), "@color/opaque_red"));
		assertEquals(-65536, Resources.colorValue(BerichtsheftActivity.getInstance(), "#ff0000"));
		Context context = BerichtsheftActivity.getInstance();
		File[] files = new File(BerichtsheftApp.applicationDataPath("res/layout")).listFiles();
		for (File file : files) {
			String relPath = relativePath(file.getPath(), Resources.getSettingsPath());
			ViewGroup vg = (ViewGroup) LayoutInflater.from(new BerichtsheftActivity())
					.inflate(relPath);
			assertNotNull(vg);
			no_println(vg);
		}
		assertEquals("Berichtsheft", 
				context.getResources().getString(R.string.app_name));
		assertEquals(51, ProviderTests.getStateStrings(context).length);
		assertThat(readAsset(context, "hello.vm").length(), is(greaterThan(0)));
		printMatchResults("@dimen/margin", Resources.XML_RESOURCE_PATTERN, "no");
		printMatchResults("@com.applang.berichtsheft.R:dimen/margin", Resources.XML_RESOURCE_PATTERN, "no");
		println(Resources.getAbsolutePath("/assets/hello.vm", Constraint.END));
		println(Resources.getAbsolutePath("/res/values/strings.xml", Constraint.END));
		println(Resources.getAbsolutePath("/res/layout/construct_form_header.xml", Constraint.END));
		println("user.dir", System.getProperty("user.dir"));
		println("user.home", System.getProperty("user.home"));
	}
	
	public void testAlertDialogs() {
		setTimeout(1000);
		AlertDialog.alerter(new Activity(), "alerter", new Exception());
		new AlertDialog(null, 
				getProperty("berichtsheft.spellcheck-selection.title"), 
				"", 
				"Hello", 
				JOptionPane.DEFAULT_OPTION,
				Behavior.MODAL|Behavior.TIMEOUT, 
				loadIcon("manager.action-SPELLCHECK.icon"), 
				null)
			.open();
		AlertDialog.chooser(new Activity(), "chooser", getStateStrings());
		unsetTimeout();
	}
	
	public void testPrompts() {
		BaseDirective.setOptions(-1);
		PromptDirective.prompt(new BerichtsheftActivity(
			new Job<String>() {
				public void perform(String result, Object[] parms) throws Exception {
					Context context = param(null, 0, parms);
					if (result == null)
						return;
					int type = (Integer) BaseDirective.options.get(result);
					String prompt = "";
					ValList values = vlist();
					switch (type) {
					case Dialogs.DIALOG_TEXT_ENTRY:
						prompt = "Gedicht";
						values.add(readAsset(context, "Kein Fehler im System.txt"));
						break;
					case Dialogs.DIALOG_TEXT_INFO:
						prompt = "poem";
						values.add(readAsset(context, "Kein Fehler im System.txt"));
						break;
					case Dialogs.DIALOG_YES_NO_MESSAGE:
						prompt = "macht nix";
						values.add("ja");
						values.add("nein");
						break;
					case Dialogs.DIALOG_YES_NO_LONG_MESSAGE:
						prompt = "macht nix";
						values.add("nee ... gar nix");
						values.add("doch, es macht");
						values.add("schon ein bisschen");
						values.add("ist egal");
						break;
					case Dialogs.DIALOG_LIST:
					case Dialogs.DIALOG_SINGLE_CHOICE:
					case Dialogs.DIALOG_MULTIPLE_CHOICE:
						prompt = "Choice";
//						String[] array = getStateStrings();
//						values.addAll(asList(array));
						values.addAll(lafDefaults());
						break;
					default:
						println("not implemented");
						return;
					}
					PromptDirective.prompt(new BerichtsheftActivity(
							new Job<String>() {
								public void perform(String result, Object[] parms) throws Exception {
									println(String.valueOf(result));
									testPrompts();
								}
							}), 
							type, result, 
							prompt, toStrings(values)); 
				}
			}
		),
		Dialogs.DIALOG_LIST, 
		"", "Prompts", 
		toStrings(BaseDirective.options.keySet()));
	}
	
	public void testJOrtho() {
		setupSpellChecker(BerichtsheftApp.applicationDataPath());
		final TextToggle textEditor = new TextToggle();
    	Deadline.WAIT = 2000;
    	showFrame(null, 
				"Spellchecker",
				new UIFunction() {
					public Component[] apply(final Component comp, Object[] parms) {
				        try {
							textEditor.setText(
								new Scanner(new File(POEM))
									.useDelimiter("\\Z").next());
						} catch (Exception e) {}
				        Component component = textEditor.getUIComponent();
				        component.setPreferredSize(new Dimension(400, 400));
						return components(component);
					}
				}, 
				new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						textEditor.getTextEdit().installSpellChecker();
						return null;
					}
				}, 
				null, 
				Behavior.TIMEOUT);
	}
	
	public void testMagic() {
		Properties props = new Properties();
		String path = featureFile(getNextFeatureTemp(), "spell", props);
		println(contentsFromFile(new File(path)));
	}
	
	public void _testProgressMonitor() {
		startFrame(
				new AbstractAction("testProgressMonitor") {
					@Override
					public void actionPerformed(ActionEvent ev) {
						JFrame frame = new TestFrame();
						frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
						frame.setLocationRelativeTo(null);
						frame.setVisible(true);
					}
				});
	}
	
	class TestFrame extends JFrame
	{
	   public static final int DEFAULT_WIDTH = 300;
	   public static final int DEFAULT_HEIGHT = 200;

	   public TestFrame() {
	      setTitle("ProgressMonitorInputStreamTest");
	      setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	      textArea = new JTextArea();
	      add(new JScrollPane(textArea));
	      chooser = new JFileChooser();
	      chooser.setCurrentDirectory(new File(tempPath()));
	      JMenuBar menuBar = new JMenuBar();
	      setJMenuBar(menuBar);
	      JMenu fileMenu = new JMenu("File");
	      menuBar.add(fileMenu);
	      openItem = new JMenuItem("Open file");
	      openItem.addActionListener(new ActionListener() {
	    	  public void actionPerformed(ActionEvent event) {
				try {
				   textArea.setText("");
				   openFile();
				}
				catch (Exception exception) {
				   exception.printStackTrace();
				}
	    	  }
	      });
	      fileMenu.add(openItem);
	      openItem2 = new JMenuItem("Open URL");
	      openItem2.addActionListener(new ActionListener() {
	    	  public void actionPerformed(ActionEvent event) {
				try {
				   textArea.setText("");
				   openUrl();
				}
				catch (Exception exception) {
				   exception.printStackTrace();
				}
	    	  }
	      });
	      fileMenu.add(openItem2);
	      exitItem = new JMenuItem("Exit");
	      exitItem.addActionListener(new ActionListener()
	         {
	            public void actionPerformed(ActionEvent event)
	            {
	               System.exit(0);
	            }
	         });
	      fileMenu.add(exitItem);
	      JToolBar south = southStatusBar(this);
	      south.add(progressBar = new JProgressBar());
	   }

	   private JProgressBar progressBar;
	   private JMenuItem openItem;
	   private JMenuItem openItem2;
	   private JMenuItem exitItem;
	   private JTextArea textArea;
	   private JFileChooser chooser;

		public void openFile() throws Exception {
			int r = chooser.showOpenDialog(this);
			if (r != JFileChooser.APPROVE_OPTION) return;
			final File f = chooser.getSelectedFile();
	      
			FileInputStream fileIn = new FileInputStream(f);
			new ScanTask(this, "Reading " + f.getName(), fileIn,
		    		new Job<Void>() {
						public void perform(Void t, Object[] parms) throws Exception {
							String line = param("", 0, parms);
							textArea.append(line);
							textArea.append("\n");
						}
					},
		    		null);
		}

		public void openUrl() throws Exception {
			String url = WeatherManager.siteUri("DE", "10519", "all", 2012, 12, 31, 10).toString();
//			String url = "http://www.us.apache.org/dist/struts/source/struts-2.3.15.2-src.zip";
			InputStream is = new URL(url).openStream();
			progressBar.setIndeterminate(true);
			new ScanTask(this, "Reading " + url, is,
		    		new Job<Void>() {
						public void perform(Void t, Object[] parms) throws Exception {
							Object[] params = reduceDepth(parms[1]);
							params[0] = param_Integer(0, 0, params) + 1;
							Thread.sleep(0);
						}
					},
		    		new Job<Void>() {
						public void perform(Void t, Object[] parms) throws Exception {
							String text = param("", 0, parms);
							textArea.append(text);
							println("%d lines", reduceDepth(parms[1])[0]);
							Document doc = Jsoup.parse(text);
							textArea.append(doc.html());
							progressBar.setIndeterminate(false);
						}
					},
					0);
		}
	}
	
	public static Test suite() {
		if ( TestUtils.hasTestCases() ) {
			return TestUtils.getSuite( HelperTests.class );
		}
		return new TestSuite(HelperTests.class);
	}

}