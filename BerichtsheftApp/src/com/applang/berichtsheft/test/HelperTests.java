package com.applang.berichtsheft.test;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.MatchResult;
 
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.apache.commons.lang.StringEscapeUtils;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.bsh.NameSpace;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import com.applang.BaseDirective;
import com.applang.Dialogs;
import com.applang.UserContext.EvaluationTask;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.components.DataView;
import com.applang.berichtsheft.plugin.BerichtsheftDockable;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import junit.framework.TestCase;

public class HelperTests extends TestCase {

    private static final String TAG = HelperTests.class.getSimpleName();
    
	protected static void setUpBeforeClass() throws Exception {
	}

	protected static void tearDownAfterClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
		Settings.load("");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private void symbolicLinks(File dir, String targetDir, String...names) throws Exception {
		for (String name : names) {
			Path link = Paths.get(new File(dir, name).getPath());
			link.toFile().delete();
			Path target = Paths.get(new File(targetDir, name).getCanonicalPath());
			Files.createSymbolicLink(link, target);
		}
	}
	
	FileTime getFileTime(String filePath, int kind) throws Exception {
		Path path = Paths.get(filePath);
	    BasicFileAttributeView view = Files.getFileAttributeView(path, BasicFileAttributeView.class);
	    BasicFileAttributes attributes = view.readAttributes();
	    switch (kind) {
		case 0:
			return attributes.creationTime();
		case 1:
			return attributes.lastModifiedTime();
		case 2:
			return attributes.lastAccessTime();
		default:
			return null;
		}
	}
	
	long getFileSize(String filePath) throws Exception {
		Path path = Paths.get(filePath);
	    BasicFileAttributeView view = Files.getFileAttributeView(path, BasicFileAttributeView.class);
	    BasicFileAttributes attributes = view.readAttributes();
	    return attributes.size();
	}
	
	private void bshTest(String contents) throws Exception {
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
						"doSomethingUseful();", contents));
		File jarsDir = tempDir(true, BerichtsheftPlugin.NAME, "settings", "jars");
		symbolicLinks(jarsDir, ".jedit/jars", 
				"BerichtsheftPlugin.jar",
				"Console.jar",
				"ProjectViewer.jar",
				"InfoViewer.jar",
				"ErrorList.jar",
				"CommonControls.jar",
				"kappalayout.jar");
		File commandoDir = tempDir(false, BerichtsheftPlugin.NAME, "settings", "console", "commando");
		symbolicLinks(commandoDir, ".jedit/console/commando", 
				"android.xml",
				"transport.xml");
		BerichtsheftApp.main(
				"-nosplash",
				"-noserver",
				String.format("-settings=%s", jarsDir.getParent()), 
				String.format("-run=%s", tempFile.getPath()));
	}

	private void performBshTest(final String title, final String contents) {
		show_Frame(null, "",
			new AbstractAction(title) {
				public void actionPerformed(ActionEvent ev) {
					try {
						bshTest(contents);
					} catch (Exception e) {
						Log.e(TAG, title, e);
					}
				}
			}
		);
	}

	public void testCommando() {
		String contents = 
				"import com.applang.berichtsheft.plugin.*;\n" +
				"BerichtsheftToolBar.scanCommandoActions();\n" +
				"BerichtsheftToolBar.commands.getAction(\"commando.transport\").invoke(view);\n";
		performBshTest("testCommando", contents);
	}

	public void testAndroidFileChooser() throws Exception {
		String contents = "import com.applang.berichtsheft.plugin.*;\n" +
				"androidFileName = \"\";\n" +
				"do {\n" +
				"	androidFileName = BerichtsheftPlugin.chooseFileFromSdcard(view,false,androidFileName);\n" +
				"} while (androidFileName != null);";
		performBshTest("testAndroidFileChooser", contents);
//		underTest = true;
//		println(BerichtsheftPlugin.chooseFileFromSdcard(null, false, ""));
	}

	public void testCommands() throws Exception {
		String path = ".jedit/console/commando/transport.xml";
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
					null, null, null))
				performBshTest("COMMAND", script);
		}
	}

	private Object shellRunTest(String script, String target) throws Exception {
		println(script.replaceAll(NEWLINE_REGEX, TAB));
		String response = runShellScript(target, script);
		String sh = "/tmp/" + target + ".sh";
		String res = "/tmp/" + target;
		print(sh, TAB);
		print(getFileSize(sh), TAB);
		print(res, TAB);
		print(getFileSize(res));
		println();
		for (int i = 0; i < 3; i++) {
			print(getFileTime(sh, i), TAB);
			print(getFileTime(res, i));
			println();
		}
		return split(response, NEWLINE_REGEX);
	}
	
	String adb = "/home/lotharla/android-sdk-linux/platform-tools/adb";
	String[] scripts = {
			adb + " shell ls -l \"/sdcard/%s\" | \n" +
			"awk '{\n" +
				"gsub(/[ \\t\\r\\n\\f\\v\\b]+$/, \"\", $NF)\n" +
				"m = match($0, /d/)\n" +
				"if (m == 1) print $NF\"/\" ; else print $NF\n" +
			"}' 2>&1",
	};

	private void existTest(boolean expected, String item, String subdir) throws Exception {
		ValList list = (ValList) shellRunTest(String.format(scripts[0], subdir), "ls");
		assertEquals(expected, list.contains(item));
	}
	
	public void testShellRun() throws Exception {
		underTest = true;
		shellRunTest(BerichtsheftPlugin.buildAdbCommand("-r", "/sdcard/xxx", ""), "rm");
		existTest(false, "xxx/", "");
		
		shellRunTest(BerichtsheftPlugin.buildAdbCommand("mkdir", "/sdcard/xxx/", ""), "mkdir");
		existTest(true, "xxx/", "");
		
		runShellScript("dev", adb + " devices");
		shellRunTest(BerichtsheftPlugin.buildAdbCommand("push", "/sdcard/xxx/", "/tmp/dev"), "push");
		existTest(true, "dev", "xxx/");
		
		shellRunTest(BerichtsheftPlugin.buildAdbCommand("rm", "/sdcard/xxx/dev", ""), "rm");
		existTest(false, "dev", "xxx/");
		
		shellRunTest(BerichtsheftPlugin.buildAdbCommand("rmdir", "/sdcard/xxx/", ""), "rmdir");
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

	String settingsDir = "/home/lotharla/work/Niklas/androidStuff/BerichtsheftApp/.jedit/plugins/berichtsheft";
	
	public void testTransports() {
		assertTrue(BerichtsheftDockable.transportsLoaded(settingsDir));
		NodeList nodes = evaluateXPath(BerichtsheftDockable.transports, "//TRANSPORT");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element el = (Element) nodes.item(i);
			NamedNodeMap attributes = el.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Node node = attributes.item(j);
				println("%s : %s", node.getNodeName(), node.getNodeValue());
			}
		}
		underTest = true;
		String uriString = "file:///home/lotharla/Downloads/note_pad2.db?title=TEXT&note=TEXT&created=INTEGER#notes";
		putSetting("TRANSPORT_URI", uriString);
		String template;
		println(template = BerichtsheftDockable.getTemplate());
		assertTrue(BerichtsheftDockable.setTemplate(template.replace("'created", "'created|unixepoch")));
		println(template = BerichtsheftDockable.getTemplate());
//		BerichtsheftDockable.saveTransports(settingsDir);
	}

	public static String encodeXml(String string, boolean decode) {
		return decode ? StringEscapeUtils.unescapeXml(string) : StringEscapeUtils.escapeXml(string);
	}

	public void testConversion() {
		System.setProperty("settings.dir", settingsDir);
		Object time = now();
		println(time = BerichtsheftDockable.doConversion("unixepoch", "" + time, "push"));
		println(time = BerichtsheftDockable.doConversion("unixepoch", "" + time, "pull"));
		BerichtsheftDockable.saveTransports();
		
		String uriString = "file:///home/lotharla/Downloads/note_pad2.db?title=TEXT&note=TEXT&created=INTEGER#notes";
		println(uriString = encodeUri(uriString, false));
		println(uriString = encodeUri(uriString, true));
		println(uriString = encodeXml(uriString, false));
		println(uriString = encodeXml(uriString, true));
	}

	public void testTemplate() throws Exception {
    	String template = " 'xxx'\t'yyy'\f'zzz' ";
    	MatchResult[] mr = findAllIn(template, BerichtsheftDockable.TRANSPORT_TEMPLATE_PATTERN);
    	for (int i = 0; i < mr.length; i++) {
    		MatchResult m = mr[i];
    		println("%d(%d,%d,%d)%s", i, m.groupCount(), m.start(), m.end(), m.group());
    		for (int j = 1; j <= m.groupCount(); j++) {
    			println(m.group(j));
    		}
		}
    	println("(%d)%s", template.length(), template);
    	underTest = true;
    	ValList projection = BerichtsheftDockable.evaluateTemplate(template);
		assertEquals(3, projection.size());
		assertEquals("xxx", projection.get(0));
		assertEquals("yyy", projection.get(1));
		assertEquals("zzz", projection.get(2));
		assertEquals(4, BerichtsheftDockable.fieldSeparators.length);
		for (int i = 0; i < BerichtsheftDockable.fieldSeparators.length; i++) {
			String s = BerichtsheftDockable.fieldSeparators[i];
			println(Arrays.toString(strings(s, BerichtsheftDockable.escapeRegex(s))));
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
		worker = new EvaluationTask(BerichtsheftApp.getActivity(), null, null, null, new Job<Object>() {
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
	
	public void testSwingworker() throws Exception {
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
	    		true);
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
									Modality.ALWAYS_ON_TOP + JOptionPane.DEFAULT_OPTION, 
									JOptionPane.PLAIN_MESSAGE,
									null, 
									objects("opt1","opt2"), null, 
									null, 
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
			true);
	}
	
	public void testInputDialog() throws Exception {
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
	
	public void testOptionDialog() throws Exception {
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
					null, null, value);
			println(String.valueOf(result));
		}
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
										Modality.NONE);
							}
						}));
					}
				}, null, null, true);
	}
	
	public void testDialogFeed() throws Exception {
		final int id = 1;
		final AlertDialog dialog = new AlertDialog.Builder(BerichtsheftApp.getActivity(), false)
				.setView(new TextView(null, true).setId(id))
				.setNeutralButton(R.string.button_close, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.create();
		dialog.open();
		TextView tv = (TextView) dialog.findViewById(id);
		tv.getComponent().setPreferredSize(new Dimension(100,100));
		tv.append("Random\n");
		show_Frame(null, "",
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
	
	public void testPrompts() {
		BaseDirective.setOptions(-1);
		String result = BerichtsheftApp.prompt(
				Dialogs.DIALOG_LIST, 
				"", "Prompts", 
				toStrings(BaseDirective.options.keySet()));
		if (result == null)
			return;
		
		int type = (int) BaseDirective.options.get(result);
		String prompt = "";
		ValList values = list();
		switch (type) {
		case Dialogs.DIALOG_TEXT_ENTRY:
			prompt = "name";
			values.add("xxx");
			break;
		case Dialogs.DIALOG_TEXT_INFO:
			prompt = "text";
			values.add(contentsFromFile(new File(relativePath(), 
					"bin/com/applang/berichtsheft/test/Kein Fehler im System.txt")));
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
			prompt = "United";
			values.addAll(list(getStateStrings()));
			break;
		default:
			break;
		}
		result = BerichtsheftApp.prompt(type, result, prompt, toStrings(values));
		println(String.valueOf(result));
		testPrompts();
    }

    public static String[] getStateStrings() {
    	try {
			String res = resourceFrom("/res/raw/states.json", "UTF-8");
			return ((ValList) walkJSON(null, new JSONArray(res), null)).toArray(new String[0]);
		} catch (Exception e) {
			fail(e.getMessage());
			return null;
		}
	}
    
    Uri uri = getConstantByName("com.applang.provider.WeatherInfo", "Weathers", "CONTENT_URI");

    public void testDataView() throws Exception {
    	String propFileName = "/tmp/.properties";
    	Settings.load(propFileName);
		final DataView dv = new DataView(BerichtsheftApp.getActivity());
		dv.askUri(null, null);
		uri = dv.getUri();
    	Deadline.wait = 2000;
    	showFrame(null, uri.toString(), new UIFunction() {
    		public Component[] apply(final Component comp, Object[] parms) {
    			dv.load(uri);
    			return components(dv);
    		}
    	}, null, null, true);
    	Settings.save(propFileName);
    }

    public void testFileView() throws Exception {
    	
    }
	
	public static void show_Frame(Object...params) {
		final javax.swing.Action action = param(null, 2, params);
		showFrame(param(new Rectangle(100,100,100,100), 0, params), param("", 1, params),
				new UIFunction() {
					public Component[] apply(final Component comp, Object[] parms) {
						return components(action != null ? 
								new JButton(action) : 
								new JButton(new AbstractAction("Dispose") {
									@Override
									public void actionPerformed(ActionEvent ev) {
										pullThePlug((JFrame)comp);
									}
								}));
					}
				}, null, null, true);
	}
	
	public static void main(String...args) {
		HelperTests.show_Frame();
	}
}
