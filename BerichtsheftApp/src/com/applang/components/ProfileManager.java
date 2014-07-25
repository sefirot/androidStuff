package com.applang.components;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.gjt.sp.jedit.View;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.applang.berichtsheft.BerichtsheftActivity;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.plugin.DataDockable.TransportBuilder;
import com.applang.berichtsheft.plugin.BerichtsheftOptionPane;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

@SuppressWarnings("rawtypes")
public class ProfileManager extends ManagerBase<Element>
{
	public static final String transportsSelector = "/TRANSPORTS";
	
	// NOTE used in scripts
	public ProfileManager(final View view) {
		blockDirty(new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				createUI(view, ProfileManager.this);
			}
		});
	}

	protected TextToggle textArea;
	private JRadioButton[] radioButtons;
	
	@SuppressWarnings("unchecked")
	protected void createUI(final View view, final Container container) {
		comboBoxes = new JComboBox[5];
		textArea = new TextToggle(4,20);
		textArea.getTextEdit().installUndoRedo();
		radioButtons = new JRadioButton[] {new JRadioButton(),new JRadioButton(),new JRadioButton()};
		ButtonGroup group = new ButtonGroup();
		for (AbstractButton btn : radioButtons) 
			group.add(btn);
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
		Box box = new Box(BoxLayout.LINE_AXIS);
		radioButtons[0].setText(getProperty("berichtsheft.transport-push.label"));
		radioButtons[0].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) 
					setOper("push", container);
			}
		});
		box.add(radioButtons[0]);
		box.add(Box.createHorizontalGlue());
		radioButtons[1].setText(getProperty("berichtsheft.transport-pull.label"));
		radioButtons[1].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) 
					setOper("pull", container);
			}
		});
		box.add(radioButtons[1]);
		box.add(Box.createHorizontalGlue());
		radioButtons[2].setText(getProperty("berichtsheft.transport-download.label"));
		radioButtons[2].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) 
					setOper("download", container);
			}
		});
		box.add(radioButtons[2]);
		container.add(surroundingBox(box, "Transport", TitledBorder.CENTER));
		comboBoxes[0] = new JComboBox();
		comboBoxes[0].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) {
					String profile = getProfile();
					if (select(profile) != null)
						setProfile(profile);
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
		Dimension size = comboBoxes[0].getPreferredSize();
		comboBoxes[0].setPreferredSize(new Dimension(500, size.height));
		container.add( Box.createVerticalStrut(10) );
		box = new Box(BoxLayout.LINE_AXIS);
		container.add(labelFor(box, "Profile", CENTER_ALIGNMENT));
		box.add(comboBoxes[0]);
		installAddRemove(box, "profile");
		container.add(box);
		comboBoxes[1] = new JComboBox();
		container.add( Box.createVerticalStrut(10) );
		box = new Box(BoxLayout.LINE_AXIS);
		container.add(labelFor(box, "Flavor", CENTER_ALIGNMENT));
		box.add(comboBoxes[1]);
		box.add(makeCustomButton("berichtsheft.edit-function", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object profile = comboBoxes[0].getSelectedItem();
				Object flavor = comboBoxes[1].getSelectedItem();
				if (notNullOrEmpty(flavor))
					new ScriptManager(view, container, 
							ScriptManager.flavorSelector(flavor), 
							profile);
			}
		}, false));
		box.add(makeCustomButton("berichtsheft.insert-field", new ActionListener() {
			int checkedItem = -1;
			
			public void actionPerformed(ActionEvent evt) {
				Object flavor = comboBoxes[1].getSelectedItem();
				if (notNullOrEmpty(flavor)) {
					final Object[] projection = fullProjection(flavor);
					if (isAvailable(0, projection)) {
						AlertDialog dialog = new AlertDialog.Builder(BerichtsheftActivity.getInstance())
								.setTitle(String.format("Columns for '%s'", flavor))
								.setSingleChoiceItems(
										arraycast(projection, new CharSequence[0]), 
										-1, 
										new OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												checkedItem = which;
											}
										}
								)
								.setNeutralButton(android.R.string.ok, new OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										if (checkedItem > -1) {
											String text = enclose("`", projection[checkedItem].toString());
											textArea.setSelectedText(text);
										}
									}
								})
								.create();
						dialog.open(0.5, 0.5);
					}
				}
			}
		}, false));
		container.add(box);
		container.add( Box.createVerticalStrut(10) );
		comboBoxes[2] = new JComboBox();
		comboBoxes[2].setEditable(true);
		comboEdit(2).addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				setDirty(true);
			}
		});
		Memory.update(comboBoxes[2], true, "Filter.expressions");
		container.add(labelFor(comboBoxes[2], "Filter", CENTER_ALIGNMENT));
		container.add(comboBoxes[2]);
		container.add( Box.createVerticalStrut(10) );
		box = new Box(BoxLayout.LINE_AXIS);
		container.add(labelFor(box, "Template", CENTER_ALIGNMENT));
		textArea.setOnTextChanged(new Job<JComponent>() {
			public void perform(JComponent t, Object[] params) throws Exception {
				setDirty(true);
			}
		});
		box.add(textArea.getUIComponent());
		Box box2 = new Box(BoxLayout.PAGE_AXIS);
		Box box3 = new Box(BoxLayout.LINE_AXIS);
		box3.add(makeCustomButton("berichtsheft.insert-enter", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				textArea.setSelectedText(NEWLINE);
			}
		}, false));
		box3.add(makeCustomButton("berichtsheft.insert-function", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String expr = new ScriptManager(view, container).getFunction();
				textArea.setSelectedText(notNullOrEmpty(expr) ? "|" + expr : "");
			}
		}, false));
		box2.add(box3);
		box3 = new Box(BoxLayout.LINE_AXIS);
		installUpdate(box3);
		box2.add(box3);
		box.add(box2);
		container.add(box);
		container.add( Box.createVerticalStrut(10) );
		box = new Box(BoxLayout.LINE_AXIS);
		comboBoxes[3] = new JComboBox(BerichtsheftOptionPane.separators.keySet().toArray());
		box.add(Box.createHorizontalGlue());
		String labelText = BerichtsheftPlugin.getOptionProperty("record-separator.title");
		box.add(new JLabel( strip(Constraint.END, labelText, ":") ));
		box.add(comboBoxes[3]);
		box.add(Box.createHorizontalGlue());
		comboBoxes[4] = new JComboBox(BerichtsheftOptionPane.decorations.keySet().toArray());
		labelText = BerichtsheftPlugin.getOptionProperty("record-decoration.title");
		box.add(new JLabel( strip(Constraint.END, labelText, ":") ));
		box.add(comboBoxes[4]);
		box.add(Box.createHorizontalGlue());
		container.add(box);
		container.add( Box.createVerticalStrut(10) );
		if ("push".equals(getProperty("TRANSPORT_OPER", "pull")))
			radioButtons[0].setSelected(true);
		else
			radioButtons[1].setSelected(true);
		for (int i = 1; i < comboBoxes.length; i++) {
			comboChangeListener(i);
		}
		Window window = SwingUtilities.getWindowAncestor(container);
		if (window != null) {
			window.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					save(getItem(), false);
				}
			});
		}
	}
	
	private String oper = null;

	public void setOper(String oper, Container container) {
		this.oper = oper;
		boolean download = "download".equals(oper);
		comboBoxes[3].setEnabled(!download);
		comboBoxes[4].setEnabled(!download);
		JLabel label = findFirstComponent(container, "Template_Label");
		label.setText(download ? "URL" : "Template");
		setProperty("TRANSPORT_OPER", oper);
		updateModels(true, false, true);
	}
	
	@Override
	protected Element select(Object...profile) {
		if (ProfileManager.transportsLoaded()) {
			String xpath = transportsSelector + "/PROFILE[@name='" + profile[0] + "' and @oper='" + oper + "']";
			return selectElement(ProfileManager.transports, xpath);
		}
		return null;
	}
	
	@Override
	protected boolean addItem(boolean refresh, Object profile) {
		if (ProfileManager.transportsLoaded()) {
			String template = textArea.getText();
			setTemplate(refresh, template, profile);
			return true;
		}
		return false;
	}
	
	@Override
	protected boolean removeItem(Object profile) {
		Element element = select(profile);
		boolean retval = element != null;
		if (retval) {
			element.getParentNode().removeChild(element);
			updateModels(true, true, false);
		}
		return retval;
	}
	
	private void setTemplate(boolean refresh, String template, Object profile) {
		Object flavor = comboBoxes[1].getSelectedItem();
		Object filter = comboEdit(2).getText();
		Object recordSeparator = comboBoxes[3].getSelectedItem();
		Object recordDecoration = comboBoxes[4].getSelectedItem();
		ProfileManager.setTemplate(template, profile, oper, flavor, filter, recordSeparator, recordDecoration);
		updateModels(refresh, true, true, profile);
	}
	
	@Override
	protected void updateItem(boolean update, Object...params) {
		String profile = param(null, 0, params);
		boolean refresh = profile == null;
		if (refresh)
			profile = getProfile();
		Element element = select(profile);
		if (element != null) {
			String template = textArea.getText();
			boolean download = "download".equals(oper);
			ValList list = download ? 
					null :
					new TransportBuilder().evaluateTemplate(template, null);
			if (update && (download || isAvailable(0, list)))
				setTemplate(refresh, template, profile);
			else
				setProfile(profile);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateModels(boolean refresh, boolean save, boolean keepSelection, Object...params) {
		String profile = param(getProfile(), 0, params);
		if (ProfileManager.transportsLoaded()) {
			if (refresh) {
				blockDirty(new Job<Void>() {
					public void perform(Void t, Object[] params) throws Exception {
						DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxes[0].getModel();
						model.removeAllElements();
						NodeList nodes = 
								evaluateXPath(ProfileManager.transports,
										transportsSelector + "/PROFILE[@oper='" + oper + "']");
						for (int i = 0; i < nodes.getLength(); i++) {
							Element element = (Element) nodes.item(i);
							final String name = element.getAttribute("name");
							if (!name.startsWith("_"))
								model.addElement(name);
						}
						comboBoxes[0].setModel(model);
						DataView.fillFlavorCombo(comboBoxes[1]);
					}
				});
			}
			if (save)
				ProfileManager.saveTransports();
		}
		if (!refresh)
			return;
		if (!keepSelection)
			profile = getProfile();
		setProfile(profile);
	}

	// NOTE used in scripts
	public void setProfile(final String profile) {
		blockDirty(new Job<Void>() {
			public void perform(Void t, Object[] params) throws Exception {
				comboBoxes[0].getModel().setSelectedItem(profile);
				String template = "", flavor = "", filter = "", recordSeparator = "newline", recordDecoration = "none";
				Element element = select(profile);
				if (element != null) {
					flavor = element.getAttribute("flavor");
					Element el = selectElement(element, "./FILTER");
					if (el != null)
						filter = el.getTextContent();
					boolean download = "download".equals(oper);
					String name = download ? "URL" : "TEMPLATE";
					el = selectElement(element, "./" + name);
					if (el != null)
						template = el.getTextContent();
					recordSeparator = element.getAttribute("recordSeparator");
					recordDecoration = element.getAttribute("recordDecoration");
				}
				comboBoxes[1].getModel().setSelectedItem(flavor);
				comboEdit(2).setText(filter);
				comboBoxes[3].getModel().setSelectedItem(recordSeparator);
				comboBoxes[4].getModel().setSelectedItem(recordDecoration);
				textArea.setText(template);
			}
		});
		setDirty(false);
	}

	// NOTE used in scripts
	public String getProfile() {
		Object item = comboBoxes[0].getSelectedItem();
		return stringValueOf(item);
	}

	public static Document transports = null;
	
	public static void saveTransports(Object...params) {
		if (transports != null) {
			String settingsDir = param(BerichtsheftApp.applicationDataPath(), 0, params);
			File file = new File(settingsDir, "transports.xml");
			xmlNodeToFile(transports, true, file);
		}
	}

	public static boolean transportsLoaded(Object...params) {
		if (transports == null) {
			String settingsDir = param(BerichtsheftApp.applicationDataPath(), 0, params);
			File file = new File(settingsDir, "transports.xml");
			if (fileExists(file))
				transports = xmlDocument(file);
		}
		return transports != null;
	}

	public static boolean setTemplate(String template, Object...params) {
		String profile = getProperty("TRANSPORT_PROFILE");
		profile = param(profile, 0, params);
		String oper = getProperty("TRANSPORT_OPER");
		oper = param(oper, 1, params);
		if (notNullOrEmpty(profile) && ProfileManager.transportsLoaded()) {
			boolean download = "download".equals(oper);
			String xpath = transportsSelector + "/PROFILE[@name='" + profile + "' and @oper='" + oper + "']";
			Element element = selectElement(ProfileManager.transports, xpath);
			if (element == null) {
				element = ProfileManager.transports.createElement("PROFILE");
				element.setAttribute("name", profile);
				element.setAttribute("oper", oper);
				if (!download) {
					element.setAttribute("recordSeparator", "whitespace");
					element.setAttribute("recordDecoration", "none");
				}
				ProfileManager.transports.getDocumentElement().appendChild(element);
			}
			String value = param(null, 2, params);
			if (value != null)
				element.setAttribute("flavor", value);
			value = param(null, 3, params);
			if (notNullOrEmpty(value))
				setCDATASection(element, "FILTER", value);
			value = param(null, 4, params);
			if (notNullOrEmpty(value))
				element.setAttribute("recordSeparator", value);
			value = param(null, 5, params);
			if (notNullOrEmpty(value))
				element.setAttribute("recordDecoration", value);
			setCDATASection(element, download ? "URL" : "TEMPLATE", template);
			return true;
		}
		return false;
	}

	private static void setCDATASection(Element element, String name, String data) {
		NodeList nodes;
		while ((nodes = evaluateXPath(element, "./*[name()='" + name + "']")).getLength() > 0)
			element.removeChild(nodes.item(0));
		element = (Element) element.appendChild(ProfileManager.transports.createElement(name));
		CDATASection cdata = ProfileManager.transports.createCDATASection(data);
		element.appendChild(cdata);
	}

	public static ValMap getProfileAsMap(Object...params) {
		ValMap map = vmap();
		String profile = getProperty("TRANSPORT_PROFILE");
		profile = param_String(profile, 0, params);
		String oper = getProperty("TRANSPORT_OPER");
		oper = param_String(oper, 1, params);
		if (notNullOrEmpty(profile) && transportsLoaded()) {
			String xpath = "/TRANSPORTS/PROFILE[@name='" + profile + "' and @oper='" + oper + "']";
			Element element = selectElement(transports, xpath);
			if (element != null) {
				map.put("name", profile);
				map.put("oper", oper);
				if (element.hasAttribute("flavor"))
					map.put("flavor", element.getAttribute("flavor"));
				Element el = selectElement(element, "./FILTER");
				if (el != null)
					map.put("filter", el.getTextContent());
				if (element.hasAttribute("recordSeparator"))
					map.put("recordSeparator", element.getAttribute("recordSeparator"));
				if (element.hasAttribute("recordDecoration"))
					map.put("recordDecoration", element.getAttribute("recordDecoration"));
				String name = "download".equals(oper) ? "URL" : "TEMPLATE";
				el = selectElement(element, "./" + name);
				if (el != null)
					map.put(name.toLowerCase(), el.getTextContent());
			}
		}
		return map;
	}

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	underTest = param("true", 0, args).equals("true");
		int behavior = Behavior.MODAL;
		if (underTest)
			behavior |= Behavior.EXIT_ON_CLOSE | Behavior.ALWAYS_ON_TOP;
		ProfileManager pm = new ProfileManager(null);
		showOptionDialog(null, 
				pm, 
				"Transport profiles", 
				JOptionPane.DEFAULT_OPTION + behavior, 
				JOptionPane.PLAIN_MESSAGE, 
				null, 
				null, null);
	}
}
