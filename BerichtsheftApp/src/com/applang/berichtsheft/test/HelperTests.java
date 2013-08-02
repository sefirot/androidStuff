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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
 
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

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
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private JLabel label;
	private JButton cancel;
	private JButton start;
	private JTextField textField;
	
	public void testBeanShell() throws Exception {
		String path = ".jedit/console/commando/android.xml";
		File file = new File(path);
		assertTrue(fileExists(file));
		String xml = contentsFromFile(file);
		Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
		for (Element elem : doc.getElementsByTag("COMMAND")) {
			File tempFile = BerichtsheftPlugin.getTempFile("script.bsh");
			contentsToFile(tempFile, 
					String.format("void doSomethingUseful() {\n" +
							"    void run() {\n" +
							"        view = jEdit.getLastView();\n" +
							"		 %s\n" +
							"    }\n" +
							"    if(jEdit.getLastView() == null)\n" +
							"        VFSManager.runInAWTThread(this);\n" +
							"    else\n" +
							"        run();\n" +
							"}\n" +
							"doSomethingUseful();", elem.text()));
			BerichtsheftApp.main("-noserver",
					"-nosplash",
					"-nosettings",
					String.format("-run=%s", tempFile.getPath()));
		}
	}

	public void testProcess() throws Exception {
    	ProcessBuilder builder = new ProcessBuilder(
        		"awk", 
        		"'NR > 1 {print $1}'", "scripts/descriptions.awk");
        builder.directory(new File(System.getProperty("user.dir")));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        
        OutputStream os = process.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        osw.close();
        
        InputStream is = process.getInputStream();
        BufferedReader bisr = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        bisr.close();
	}
	
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
		show_Frame("",
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
		ValList values = new ValList();
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
		final javax.swing.Action action = param(null, 1, params);
		showFrame(null, param("", 0, params),
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
		HelperTests.show_Frame("", null);
	}
}
