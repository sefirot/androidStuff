package com.applang.berichtsheft.components;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.gjt.sp.jedit.View;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.R;
import com.applang.berichtsheft.plugin.DataDockable.TransportBuilder;
import com.applang.berichtsheft.plugin.BerichtsheftOptionPane;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;

@SuppressWarnings("rawtypes")
public class ProfileManager extends ManagerBase
{
	public static final String transportsSelector = "/TRANSPORTS";
	
	// NOTE used in scripts
	public ProfileManager(final View view) {
		createUI(view, this);
	}

	protected TextEditor textArea;
	private JRadioButton[] radioButtons;
	
	@SuppressWarnings("unchecked")
	protected void createUI(final View view, final Container container) {
		comboBoxes = new JComboBox[4];
		textArea = new TextEditor(4,20);
		radioButtons = new JRadioButton[] {new JRadioButton(),new JRadioButton(),new JRadioButton()};
		ButtonGroup group = new ButtonGroup();
		for (AbstractButton btn : radioButtons) 
			group.add(btn);
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
		Box box = new Box(BoxLayout.LINE_AXIS);
		container.add(labelFor(box, "Transport", CENTER_ALIGNMENT));
		box.add(Box.createHorizontalGlue());
		radioButtons[0].setText(BerichtsheftPlugin.getProperty("berichtsheft.transport-push.label"));
		radioButtons[0].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) 
					setOper("push");
			}
		});
		box.add(radioButtons[0]);
		box.add(Box.createHorizontalGlue());
		radioButtons[1].setText(BerichtsheftPlugin.getProperty("berichtsheft.transport-pull.label"));
		radioButtons[1].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) 
					setOper("pull");
			}
		});
		box.add(radioButtons[1]);
		radioButtons[2].setText(BerichtsheftPlugin.getProperty("berichtsheft.transport-download.label"));
		radioButtons[2].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if (ev.getStateChange() == ItemEvent.SELECTED) 
					setOper("download");
			}
		});
		box.add(radioButtons[2]);
		box.add(Box.createHorizontalGlue());
		container.add(box);
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
					saveChanges(item);
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
		container.add(labelFor(box, "Schema", CENTER_ALIGNMENT));
		box.add(comboBoxes[1]);
		box.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.edit-function", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object profile = comboBoxes[0].getSelectedItem();
				Object schema = comboBoxes[1].getSelectedItem();
				if (notNullOrEmpty(schema))
					new ScriptManager(view, container, 
							ScriptManager.schemaSelector(schema), 
							profile);
			}
		}, false));
		box.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.insert-field", new ActionListener() {
			int checkedItem = -1;
			
			public void actionPerformed(ActionEvent evt) {
				Object schema = comboBoxes[1].getSelectedItem();
				if (notNullOrEmpty(schema)) {
					final Object[] projection = fullProjection(schema);
					if (isAvailable(0, projection)) {
						AlertDialog dialog = new AlertDialog.Builder(BerichtsheftApp.getActivity())
								.setTitle(String.format("Columns for '%s'", schema))
								.setSingleChoiceItems(
										arraycast(projection, new CharSequence[0]), 
										-1, 
										new OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												checkedItem = which;
											}
										}
								)
								.setNeutralButton(R.string.button_ok, new OnClickListener() {
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
		box = new Box(BoxLayout.LINE_AXIS);
		container.add(labelFor(box, "Template", CENTER_ALIGNMENT));
		textArea.setOnTextChanged(new Job<TextComponent>() {
			public void perform(TextComponent t, Object[] params) throws Exception {
				setDirty(true);
			}
		});
		box.add(textArea.getUIComponent());
		Box box2 = new Box(BoxLayout.PAGE_AXIS);
		Box box3 = new Box(BoxLayout.LINE_AXIS);
		box3.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.insert-enter", new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				textArea.setSelectedText(NEWLINE);
			}
		}, false));
		box3.add(BerichtsheftPlugin.makeCustomButton("berichtsheft.insert-function", new ActionListener() {
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
		comboBoxes[2] = new JComboBox(BerichtsheftOptionPane.separators.keySet().toArray());
		box.add(Box.createHorizontalGlue());
		String labelText = BerichtsheftPlugin.getOptionProperty("record-separator.title");
		box.add(new JLabel( strip(false, labelText, ":") ));
		box.add(comboBoxes[2]);
		box.add(Box.createHorizontalGlue());
		comboBoxes[3] = new JComboBox(BerichtsheftOptionPane.decorations.keySet().toArray());
		labelText = BerichtsheftPlugin.getOptionProperty("record-decoration.title");
		box.add(new JLabel( strip(false, labelText, ":") ));
		box.add(comboBoxes[3]);
		box.add(Box.createHorizontalGlue());
		container.add(box);
		container.add( Box.createVerticalStrut(10) );
		if ("push".equals(BerichtsheftPlugin.getProperty("TRANSPORT_OPER", "pull")))
			radioButtons[0].setSelected(true);
		else
			radioButtons[1].setSelected(true);
		Window window = SwingUtilities.getWindowAncestor(container);
		if (window != null) {
			window.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					saveChanges(comboEdit(0).getText());
				}
			});
		}
	}
	
	private String oper = null;

	public void setOper(String oper) {
		this.oper = oper;
		BerichtsheftPlugin.setProperty("TRANSPORT_OPER", oper);
		updateModels(true, false, true);
	}
	
	@Override
	protected Element select(String...profile) {
		if (ProfileManager.transportsLoaded()) {
			String xpath = transportsSelector + "/PROFILE[@name='" + profile[0] + "' and @oper='" + oper + "']";
			return selectElement(ProfileManager.transports, xpath);
		}
		return null;
	}
	
	@Override
	protected boolean addItem(boolean refresh, String profile) {
		if (ProfileManager.transportsLoaded()) {
			String template = textArea.getText();
			setTemplate(refresh, template, profile);
			return true;
		}
		return false;
	}
	
	@Override
	protected boolean removeItem(String profile) {
		boolean retval = false;
		if (deleteThis(profile)) {
			Element element = select(profile);
			retval = element != null;
			if (retval) {
				element.getParentNode().removeChild(element);
				updateModels(true, true, false);
			}
		}
		return retval;
	}
	
	private void setTemplate(boolean refresh, String template, String profile) {
		Object schema = comboBoxes[1].getSelectedItem();
		Object recordSeparator = comboBoxes[2].getSelectedItem();
		Object recordDecoration = comboBoxes[3].getSelectedItem();
		ProfileManager.setTemplate(template, profile, oper, schema, recordSeparator, recordDecoration);
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
			ValList list = new TransportBuilder().evaluateTemplate(template, null);
			if (update && isAvailable(0, list))
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
				DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxes[0].getModel();
				model.removeAllElements();
				NodeList nodes = 
						evaluateXPath(ProfileManager.transports,
								transportsSelector + "/PROFILE[@oper='" + oper + "']");
				for (int i = 0; i < nodes.getLength(); i++) {
					Element element = (Element) nodes.item(i);
					model.addElement(element.getAttribute("name"));
				}
				comboBoxes[0].setModel(model);
				model = (DefaultComboBoxModel) comboBoxes[1].getModel();
				model.removeAllElements();
				model.addElement("");
				ValList schemas = contentAuthorities(providerPackages);
				for (Object schema : schemas) {
					model.addElement(schema);
				}
				comboBoxes[1].setModel(model);
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
	public void setProfile(String profile) {
		comboBoxes[0].getModel().setSelectedItem(profile);
		String template = "", schema = "", recordSeparator = "newline", recordDecoration = "none";
		Element element = select(profile);
		if (element != null) {
			Element el = selectElement(element, "./TEMPLATE");
			if (el != null)
				template = el.getTextContent();
			schema = element.getAttribute("schema");
			recordSeparator = element.getAttribute("recordSeparator");
			recordDecoration = element.getAttribute("recordDecoration");
		}
		comboBoxes[1].getModel().setSelectedItem(schema);
		comboBoxes[2].getModel().setSelectedItem(recordSeparator);
		comboBoxes[3].getModel().setSelectedItem(recordDecoration);
		updateText(textArea, template);
	}

	// NOTE used in scripts
	public String getProfile() {
		Object item = comboBoxes[0].getSelectedItem();
		return stringValueOf(item);
	}

	public static Document transports = null;
	
	public static void saveTransports(Object...params) {
		if (transports != null) {
			String settingsDir = param(System.getProperty("settings.dir", ""), 0, params);
			File file = new File(settingsDir, "transports.xml");
			xmlNodeToFile(transports, true, file);
		}
	}

	public static boolean transportsLoaded(Object...params) {
		if (transports == null) {
			String settingsDir = param(System.getProperty("settings.dir", ""), 0, params);
			File file = new File(settingsDir, "transports.xml");
			if (fileExists(file))
				transports = xmlDocument(file);
		}
		return transports != null;
	}

	public static boolean setTemplate(String template, Object...params) {
		String profile = BerichtsheftPlugin.getProperty("TRANSPORT_PROFILE");
		profile = param(profile, 0, params);
		String oper = BerichtsheftPlugin.getProperty("TRANSPORT_OPER");
		oper = param(oper, 1, params);
		if (notNullOrEmpty(profile) && ProfileManager.transportsLoaded()) {
			String xpath = transportsSelector + "/PROFILE[@name='" + profile + "' and @oper='" + oper + "']";
			Element element = selectElement(ProfileManager.transports, xpath);
			if (element == null) {
				element = ProfileManager.transports.createElement("PROFILE");
				element.setAttribute("name", profile);
				element.setAttribute("oper", oper);
				element.setAttribute("recordSeparator", "whitespace");
				element.setAttribute("recordDecoration", "none");
				ProfileManager.transports.getDocumentElement().appendChild(element);
			}
			String attr = param(null, 2, params);
			if (attr != null)
				element.setAttribute("schema", attr);
			attr = param(null, 3, params);
			if (attr != null)
				element.setAttribute("recordSeparator", attr);
			attr = param(null, 4, params);
			if (attr != null)
				element.setAttribute("recordDecoration", attr);
			NodeList nodes;
			while ((nodes = evaluateXPath(element, "./*[name()='TEMPLATE']")).getLength() > 0)
				element.removeChild(nodes.item(0));
			element = (Element) element.appendChild(ProfileManager.transports.createElement("TEMPLATE"));
			CDATASection cdata = ProfileManager.transports.createCDATASection(template);
			element.appendChild(cdata);
			return true;
		}
		return false;
	}
}
