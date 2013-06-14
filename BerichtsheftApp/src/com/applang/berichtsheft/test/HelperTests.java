package com.applang.berichtsheft.test;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.VelocityUtil.*;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Random;
 
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.json.JSONArray;

import android.app.Activity;
import android.content.Intent;

import com.applang.BaseDirective;
import com.applang.Dialogs;
import com.applang.UserContext.EvaluationTask;

import junit.framework.TestCase;

public class HelperTests extends TestCase {

    private static final String TAG = HelperTests.class.getSimpleName();
    
	public HelperTests(String name) {
		super(name);
	}

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
	 
	class NumberWorker extends Task<String> {
 
		public NumberWorker(Job<String> followUp, Object... params) {
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
	
	private void start() {
		start.setEnabled(false);
		textField.setText("");
		worker = new NumberWorker(new Job<String>() {
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
		worker = new EvaluationTask(new Activity(), null, null, null, new Job<Object>() {
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
	    		new ComponentFunction<Component[]>() {
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
				 
						ImageIcon image = iconFrom("spinner.gif");
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
	    		new ComponentFunction<Component[]>() {
					@Override
					public Component[] apply(Component comp, Object[] parms) {
						return null;
					}
	    		},
	    		new ComponentFunction<Component[]>() {
					@Override
					public Component[] apply(Component comp, Object[] parms) {
						return null;
					}
	    		},
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
	
	public void testOptionDialogModeless() {
		showFrame(null, "", 
			new ComponentFunction<Component[]>() {
				public Component[] apply(Component comp, Object[] parms) {
					final JFrame frame = (JFrame) comp;
					Button btn = new Button("modeless");
					btn.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							showOptionDialog(frame, "modeless", 
									"test",
									JOptionPane.DEFAULT_OPTION + 4, 
									JOptionPane.PLAIN_MESSAGE,
									null, 
									new Object[]{"opt1","opt2"}, null, 
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
					return new Component[] {btn};
				}
			}, 
			null, 
			null, 
			true);
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
	
	public void testDialogs() {
		BaseDirective.setOptions(-1);
		Intent intent = new Intent(Dialogs.PROMPT_ACTION)
				.putExtra(BaseDirective.TYPE, Dialogs.DIALOG_LIST)
				.putExtra(BaseDirective.VALUES, 
						BaseDirective.options.keySet().toArray(new String[0]));
		new Activity().startActivity(intent);
		String result = intent.getExtras().getString(BaseDirective.RESULT);
		if ("null".equals(result))
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
			values.addAll(Arrays.asList(getStateStrings()));
			break;
		default:
			break;
		}
		intent = new Intent(Dialogs.PROMPT_ACTION)
				.putExtra(BaseDirective.TYPE, type)
				.putExtra(BaseDirective.TITLE, result)
				.putExtra(BaseDirective.PROMPT, prompt)
				.putExtra(BaseDirective.VALUES, values.toArray(new String[0]));
		new Activity().startActivity(intent);
		result = intent.getExtras().getString(BaseDirective.RESULT);
		println(result);
		testDialogs();
    }

    public static String[] getStateStrings() {
    	try {
			String res = resourceFrom("states.json", "UTF-8");
			return ((ValList) walkJSON(null, new JSONArray(res), null)).toArray(new String[0]);
		} catch (Exception e) {
			fail(e.getMessage());
			return null;
		}
	}

}
