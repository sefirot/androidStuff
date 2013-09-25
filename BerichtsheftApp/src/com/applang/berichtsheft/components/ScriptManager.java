package com.applang.berichtsheft.components;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.bsh.NameSpace;
import org.gjt.sp.jedit.gui.RolloverButton;
import org.gjt.sp.util.Log;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.applang.SwingUtil.Behavior;
import com.applang.Util.Job;
import com.applang.Util.ValMap;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

@SuppressWarnings("rawtypes")
public class ScriptManager extends ManagerBase<Element>
{
	public static String schemaSelector(Object name) {
		return String.format("/SCHEMA[@name='%s']", name);
	}
	
	public static boolean setDefaultConversions(Object schema, String tableName, ValMap conv) {
		if (ProfileManager.transportsLoaded()) {
			String xpath = ProfileManager.transportsSelector + schemaSelector(schema);
			Element element = selectElement(ProfileManager.transports, xpath);
			if (element != null) {
				ValMap map = getDefaultConversions(schema, null);
				map.put(tableName, conv);
				JSONStringer jsonWriter = new JSONStringer();
				try {
					toJSON(jsonWriter, "", map, null);
				} catch (Exception e) {
					Log.log(Log.ERROR, ScriptManager.class, e);
					return false;
				}
				String text = jsonWriter.toString();
				CDATASection cdata = ProfileManager.transports.createCDATASection(text);
				NodeList nodes = evaluateXPath(element, "./CONVERSIONS/text()");
				if (nodes != null && nodes.getLength() > 0) {
					Node node = nodes.item(0);
					node.getParentNode().replaceChild(cdata, node);
				}
				else {
					Element el = ProfileManager.transports.createElement("CONVERSIONS");
					el.appendChild(cdata);
					element.appendChild(el);
				}
				ProfileManager.saveTransports();
				return true;
			}
		}
		return false;
	}
	
	public static ValMap getDefaultConversions(Object schema, String tableName) {
		if (ProfileManager.transportsLoaded()) {
			String xpath = ProfileManager.transportsSelector + schemaSelector(schema);
			Element element = selectElement(ProfileManager.transports, xpath);
			if (element != null) {
				NodeList nodes = evaluateXPath(element, "./CONVERSIONS/text()");
				if (nodes != null && nodes.getLength() > 0) {
					Node node = nodes.item(0);
					try {
						String text = node.getTextContent();
						ValMap map = (ValMap) walkJSON(null, new JSONObject(text), null);
						if (map != null)
							if (notNullOrEmpty(tableName)) {
								if (map.containsKey(tableName))
									return (ValMap) map.get(tableName);
							}
							else
								return map;
					} catch (Exception e) {
						Log.log(ERROR, ScriptManager.class, e);
					}
				}
			}
		}
		return vmap();
	}

	public static Object doConversion(Object value, String function, String oper, Object...params) {
		try {
			if (!ProfileManager.transportsLoaded() || nullOrEmpty(function))
				return value;
			NameSpace tmp = new NameSpace(BeanShell.getNameSpace(), "transport");
			tmp.setVariable("value", value);
			tmp.setVariable("oper", oper);
			Element el = selectElement(ProfileManager.transports, "//FUNCTION[@name='" + function + "']");
			if (el == null) {
				BerichtsheftPlugin.consoleMessage("berichtsheft.doConversion.message.1", function);
				return value;
			}
			String script = param(el.getTextContent(), 0, params);
			return BeanShell.eval(null, tmp, script);
		} 
		catch (Exception e) {
			BerichtsheftPlugin.consoleMessage("berichtsheft.doConversion.message.2", function, value);
			return null;
		}
	}
	
	private static final String DEFAULT_BUTTON_KEY = stringValueOf(defaultOptions(JOptionPane.OK_CANCEL_OPTION).get(0));
	
	private TextEditor textArea;
	private JLabel mess;

	public ScriptManager(final View view, Component relative, Object...params) {
		String selector = com.applang.Util.paramString("", 0, params);
		this.selector += selector;
		this.profile = param(null, 1, params);
		
		//	resizing problems (like in BoxLayout) of org.gjt.sp.jedit.textarea.StandaloneTextArea (behind TextEditor) 
		//	make this dialog construction preferable
		
		String title = com.applang.Util.paramString("Script editor", 2, params);
		final String function = param(null, 3, params);
		int behavior = paramInteger(Behavior.MODAL, 4, params);
		showDialog(view, relative, 
	    		title, 
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						final JDialog wnd = (JDialog) comp;
						blockChange(new Job<Void>() {
							public void perform(Void t, Object[] params) throws Exception {
								createUI(view, wnd.getContentPane());
							}
						});
						return null;
					}
	    		},
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						JDialog wnd = (JDialog) comp;
						JButton btn = findComponent(wnd.getContentPane(), DEFAULT_BUTTON_KEY);
						btn.getRootPane().setDefaultButton(btn);
						if (function != null)
							setFunction(function);
						return null;
					}
	    		},
	    		new UIFunction() {
					public Component[] apply(Component comp, Object[] parms) {
						comboBoxes[0].setSelectedIndex(-1);
						return null;
					}
	    		},
	    		behavior);
	}
	
	private String selector = ProfileManager.transportsSelector;
	private String profile = null;

	@Override
	protected Element select(Object...function) {
		if (ProfileManager.transportsLoaded()) {
			String xpath = selector;
			if (isAvailable(0, function)) {
				xpath += "/FUNCTION[@name='" + function[0] + "'";
				if (notNullOrEmpty(profile))
					xpath += " and @profile='" + profile + "'";
				xpath += "]";
			}
			return selectElement(ProfileManager.transports, xpath);
		}
		return null;
	}
	
	@Override
	protected boolean addItem(boolean refresh, Object function) {
		if (ProfileManager.transportsLoaded()) {
			String body = textArea.getText();
			if (notNullOrEmpty(body)) {
				Element element = ProfileManager.transports.createElement("FUNCTION");
				element.setAttribute("name", stringValueOf(function));
				if (notNullOrEmpty(profile))
					element.setAttribute("profile", profile);
				Element el = ProfileManager.transports.createElement("BODY");
				CDATASection cdata = ProfileManager.transports.createCDATASection(body);
				el.appendChild(cdata);
				element.appendChild(el);
				select().appendChild(element);
				updateModel(true, true, true, function);
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected boolean removeItem(Object function) {
		Element element = select(function);
		boolean retval = element != null;
		if (retval) {
			element.getParentNode().removeChild(element);
			updateModel(true, true, false);
		}
		return retval;
	}
	
	@Override
	protected void updateItem(boolean accept, Object...params) {
		String function = param(null, 0, params);
		boolean refresh = function == null;
		if (refresh)
			function = getFunction();
		Element element = select(function);
		if (element != null) {
			String body = textArea.getText();
			if (accept) {
				NodeList nodes = evaluateXPath(element, "./BODY/text()");
				if (nodes != null && nodes.getLength() > 0) {
					Node node = nodes.item(0);
					CDATASection cdata = ProfileManager.transports.createCDATASection(body);
					node.getParentNode().replaceChild(cdata, node);
					updateModel(refresh, true, true);
				}
			}
			else if (refresh)
				setFunction(function);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateModel(boolean refresh, boolean save, boolean keepSelection, Object...params) {
		String function = param(getFunction(), 0, params);
		if (ProfileManager.transportsLoaded()) {
			if (refresh) {
				blockChange(new Job<Void>() {
					public void perform(Void t, Object[] params) throws Exception {
						DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxes[0].getModel();
						model.removeAllElements();
						NodeList nodes = evaluateXPath(
								ProfileManager.transports, selector + "/FUNCTION");
						for (int i = 0; i < nodes.getLength(); i++) {
							Element element = (Element) nodes.item(i);
							model.addElement(element.getAttribute("name"));
						}
						comboBoxes[0].setModel(model);
					}
				});
			}
			if (save)
				ProfileManager.saveTransports();
		}
		if (!refresh)
			return;
		if (!keepSelection)
			function = getFunction();
		setFunction(function);
	}

	// NOTE used in scripts
	public String getFunction() {
		Object item = comboBoxes[0].getSelectedItem();
		return stringValueOf(item);
	}

	// NOTE used in scripts
	public void setFunction(final String function) {
		blockChange(new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				comboBoxes[0].getModel().setSelectedItem(function);
				String body = "";
				Element element = select(function);
				if (element != null) {
					NodeList nodes = evaluateXPath(element, "./BODY/text()");
					if (nodes != null && nodes.getLength() > 0) {
						Node node = nodes.item(0);
						body = node.getTextContent();
					}
				}
				textArea.setText(body);
			}
		});
		setDirty(false);
	}
	
	private void createUI(final View view, final Container container) {
		comboBoxes = new JComboBox[] {new JComboBox()};
		textArea = new TextEditor().createBufferTextArea("beanshell", "/modes/java.xml");
		
		JToolBar bar = new JToolBar();
		final RolloverButton btn = new RolloverButton();
		btn.setName(DEFAULT_BUTTON_KEY);
		btn.setText(DEFAULT_BUTTON_KEY);
		bar.add(btn);
		comboBoxes[0].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) {
					String function = getFunction();
					if (select(function) != null)
						setFunction(function);
					else
						setDirty(true);
				}
				else if (ev.getStateChange() == ItemEvent.DESELECTED) {
					String item = stringValueOf(ev.getItem());
					save(item, false);
				}
			}
		});
		comboBoxes[0].setEditable(true);
		comboEdit(0).setHorizontalAlignment(JTextField.CENTER);
		bar.add(comboBoxes[0]);
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save(getItem(), false);
				Window window = SwingUtilities.getWindowAncestor(container);
				if (window != null)
					window.dispose();
			}
		});
		comboEdit(0).addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					btn.doClick();
				}
			}
		});
		installAddRemove(bar, "function");
		installUpdate(bar);
		container.add(bar, BorderLayout.NORTH);
		final RolloverButton test = new RolloverButton();
		test.setText("Test");
		test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testThis(view, container, comboEdit(0).getText());
			}
		});
		test.setEnabled(findAllIn(selector, Pattern.compile("/")).length < 2);
		bar.add(test);
		textArea.setOnTextChanged(new Job<TextComponent>() {
			public void perform(TextComponent t, Object[] params) throws Exception {
				setDirty(true);
			}
		});
		container.add(textArea.getUIComponent(), BorderLayout.CENTER);
		mess = new JLabel();
		mess.setHorizontalAlignment(JTextField.CENTER);
		container.add(mess, BorderLayout.SOUTH);
		updateModel(true, false, false);
	}
	
	String[] keys = { "oper", "type", "value" };
	Object[] values = { "push", "TEXT", null };

	private void testThis(View view, Component relative, final String name) {
		mess.setText("");
		final DefaultTableModel model = new DefaultTableModel(3, 2) {
			@Override
			public Object getValueAt(int row, int col) {
				if (col == 0)
					return keys[row];
				else
					return values[row];
			}
			@Override
			public void setValueAt(Object aValue, int row, int col) {
				if (col == 1)
					values[row] = aValue;
			}
			@Override
	        public Class<?> getColumnClass(int col) {
				if (col == 1) {
					if ("push".equals(values[0])) {
						if ("INTEGER".equals(values[1]))
							return Long.class;
						else if ("REAL".equals(values[1]))
							return Double.class;
						else if ("BLOB".equals(values[1]))
							return Byte[].class;
					}
					return String.class;
				}
	            return super.getColumnClass(col);
	        }
			@Override
			public boolean isCellEditable(int row, int col) {
				if (col == 0)
					return false;
				else
					return true;
			}
		};
		final JTable table = new JTable(model) {
			{
				setRowSelectionAllowed(false);
				setColumnSelectionAllowed(false);
			}
			
			String[] type_options = { "TEXT", "INTEGER", "REAL", "BLOB", "NULL" };
			String[] oper_options = { "push", "pull" };
			
			@SuppressWarnings("unchecked")
			TableCellEditor[] editors = new TableCellEditor[] {
				new DefaultCellEditor(new JComboBox(oper_options)) {
					{
						addCellEditorListener(new CellEditorListener() {
							public void editingStopped(ChangeEvent e) {
								if ("pull".equals(values[0])) {
									model.setValueAt("TEXT", 1, 1);
								}
							}
							public void editingCanceled(ChangeEvent e) {
							}
						});
					}
				},
				new DefaultCellEditor(new JComboBox(type_options)),
				null,
			};
			@Override
			public TableCellEditor getCellEditor(int row, int col)
			{
			    TableCellEditor editor = editors[row];
			    if (editor != null)
			        return editor;
			    else
			    	return super.getCellEditor(row,col);
			}
		};
		Object message = new Object[] {table};
		showOptionDialog(relative, message, 
				String.format("Testing '%s'", name), 
	    		JOptionPane.DEFAULT_OPTION + Behavior.MODAL,
	    		JOptionPane.PLAIN_MESSAGE,
	    		null,
	    		objects("Perform"),
	    		null, null, 
	    		new Function<Boolean>() {
					public Boolean apply(Object... params) {
						TableCellEditor editor = table.getCellEditor();
						if (editor != null)
							editor.stopCellEditing();
						Object value = values[2];
						if ("NULL".equals(values[1]))
							value = null;
						Object result = ScriptManager.doConversion(
								value, 
								name, 
								values[0].toString(),
								textArea.getText());
						mess.setText(String.valueOf(result));
						return false;
					}
				});
	}

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	underTest = param("true", 0, args).equals("true");
		int behavior = Behavior.MODAL;
		if (underTest)
			behavior |= Behavior.EXIT_ON_CLOSE;
		new ScriptManager(null, null, null, null, null, null, behavior);
	}
}
