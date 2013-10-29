package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

import android.content.ContentValues;
import android.net.Uri;

import com.applang.Util.ValList;
import com.applang.Util.ValMap;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.components.DataView;
import com.applang.components.DataView.Provider;
import com.applang.components.DatePicker;
import com.applang.components.ProfileManager;
import com.applang.components.ScriptManager;
import com.applang.components.WeatherManager;
import com.applang.provider.WeatherInfo.Weathers;

/**
 * 
 * A dockable JPanel as a jEdit plugin.
 *
 */
public class DataDockable extends JPanel implements EBComponent, BerichtsheftActions, DefaultFocusComponent
{
	private static final long serialVersionUID = 6415522692894321789L;

	private boolean floating;
	private View view;
	
	public class DataToolPanel extends JPanel
	{
		private JLabel label = new JLabel();
	
		public DataToolPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	
			add(BerichtsheftPlugin.makeCustomButton("datadock.choose-uri", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					chooseUri();
				}
			}, false));
			add(BerichtsheftPlugin.makeCustomButton("datadock.update-uri", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					updateUri();
				}
			}, false));
			add(BerichtsheftPlugin.makeCustomButton("datadock.transport-to-buffer", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					transportToBuffer();
				}
			}, false));
			add(BerichtsheftPlugin.makeCustomButton("datadock.transport-from-buffer", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					transportFromBuffer();
				}
			}, true));
			
			add(Box.createGlue());
			
			Box labelBox = new Box(BoxLayout.Y_AXIS);
			labelBox.add(Box.createGlue());
			
			propertiesChanged();
			
			labelBox.add(label);
			labelBox.add(Box.createGlue());
			
			add(labelBox);
		}
	
		void propertiesChanged() {
			String dbName = dataView.getUriString();
			label.setText(dbName);
			boolean show = "true".equals(BerichtsheftPlugin.getOptionProperty("show-uri"));
			label.setVisible(show);
		}
	}

	private DataToolPanel toolPanel;

	/**
	 * 
	 * @param view the current jedit window
	 * @param position a variable passed in from the script in actions.xml,
	 * 	which can be DockableWindowManager.FLOATING, TOP, BOTTOM, LEFT, RIGHT, etc.
	 * 	see @ref DockableWindowManager for possible values.
	 */
	public DataDockable(View view, String position) {
		super(new BorderLayout());
		this.view = view;
		this.floating = position.equals(DockableWindowManager.FLOATING);

		if (floating)
			this.setPreferredSize(new Dimension(500, 250));

		add(BorderLayout.CENTER, dataView);

		this.toolPanel = new DataToolPanel();
		add(BorderLayout.NORTH, this.toolPanel);

		propertiesChanged();
	}
	
	public DataView dataView = BerichtsheftPlugin.dataView;

	public void focusOnDefaultComponent() {
		dataView.requestFocus();
	}

	// EBComponent implementation
	
	public void handleMessage(EBMessage message) {
		if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}
    
	void propertiesChanged() {
		Font newFont = BerichtsheftOptionPane.makeFont();
		JTable table = dataView.getTable();
		Font oldFont = table.getFont();
		if (!newFont.equals(oldFont)) {
			table.setFont(newFont);
		}
		
		if (dataView.getUri() == null)
			dataView.loadUri();
		else
			dataView.reload();
		
		toolPanel.propertiesChanged();
	}

	// These JComponent methods provide the appropriate points
	// to subscribe and unsubscribe this object to the EditBus.

    @Override
	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);
	}
     
    @Override
	public void removeNotify() {
		super.removeNotify();
		EditBus.removeFromBus(this);
	}
    
	// Actions implementation
	
	public void chooseUri() {
    	if (dataView.configureData(view)) {
    		BerichtsheftPlugin.setProperty("TRANSPORT_URI", dataView.getUriString());
    		updateUri();
		}
    	else
    		BerichtsheftPlugin.consoleMessage("dataview.choose-uri.message");
	};
	
	public void updateUri() {
		dataView.reset(dataView.getUriString());
		NoteDockable dockable = (NoteDockable) BerichtsheftPlugin.getDockable(view, "notedock", false);
		if (dockable != null)
			dockable.propertiesChanged();
		toolPanel.propertiesChanged();
	}

	public void spellCheckSelection() {
		doTransport(view, "spellcheck");
	}

	public void transportFromBuffer() {
		BerichtsheftPlugin.setProperty("TRANSPORT_OPER", "pull");
		BerichtsheftPlugin.invokeAction(view, "commando.Transport");
	}

	public void transportToBuffer() {
		JTable table = dataView.getTable();
		if (hasNoSelection(table)) {
			BerichtsheftPlugin.consoleMessage("datadock.transport-to-buffer.message");
			return; 
		}
		ValList columns = DataView.getSelectedColumnNames(table);
		TransportBuilder builder = new TransportBuilder();
		String template = builder.makeTemplate(columns.toArray());
		columns = builder.evaluateTemplate(template, null);
		if (!isAvailable(0, columns))
			return;
		final String text = builder.wrapRecords(table);
		showItems(view, "datadock.transport-to-buffer.label", 
				BerichtsheftPlugin.getProperty("datadock.transport-to-buffer.message.1"), 
				text, 
				JOptionPane.OK_CANCEL_OPTION, 
	    		Behavior.NONE, 
	    		new Job<Void>() {
					public void perform(Void t, Object[] params) throws Exception {
						BerichtsheftPlugin.getJEditor().setSelectedText(text);
					}
				});
	}
	
	public static class TransportBuilder
	{
		static String CLIPPER = "`";
		public static Pattern TEMPLATE_PATTERN = clippingPattern(CLIPPER, CLIPPER);
		
		public ValList evaluateTemplate(String template, ValMap profile) {
			MatchResult[] mr = notNullOrEmpty(template) ? 
					findAllIn(template, TEMPLATE_PATTERN) : null;
			if (nullOrEmpty(mr)) {
				BerichtsheftPlugin.consoleMessage("datadock.template-evaluation.message.1");
				return null;
			}
			getTemplateOptions(profile, 0);
			ValList projection = vlist();
			ArrayList<String> list = new ArrayList<String>();
			int pos = 0;
			for (int i = 0; i < mr.length; i++) {
				MatchResult m = mr[i];
				if (pos != m.start()) {
					BerichtsheftPlugin.consoleMessage("datadock.template-evaluation.message.2");
					return null;
				}
				String s = m.group(1);
				if (i == 0) 
					list.add(s);
				s = m.group(3);
				if (i < mr.length - 1) 
					s += mr[i+1].group(1);
				list.add(s);
				projection.add(m.group(2));
				pos = m.end();
			}
			if (pos != template.length()) {
				BerichtsheftPlugin.consoleMessage("datadock.template-evaluation.message.2");
				return null;
			}
			fieldSeparators = list.toArray(strings());
			fieldSeparatorPatterns = new Pattern[fieldSeparators.length];
			for (int i = 0; i < fieldSeparators.length; i++) {
				String pat = fieldSeparators[i];
				if (pat.contains(NEWLINE)) {
					String[] parts = pat.split(NEWLINE_REGEX, -2);
					pat = "";
					for (int j = 0; j < parts.length; j++) {
						if (j > 0)
							pat += WHITESPACE_OR_NOTHING_REGEX + "?" + NEWLINE_REGEX;
						String p = parts[j];
						if (p.length() > 0)
							pat += Pattern.quote(p);
					}
				}
				else
					pat = Pattern.quote(pat);
				if (i == 0) {
					if (isWhiteSpace(fieldSeparators[i]))
						pat = WHITESPACE_OR_NOTHING_REGEX;
					pat = "^" + pat;
				}
				else if (i == fieldSeparators.length - 1)
					pat += "$";
				fieldSeparatorPatterns[i] = Pattern.compile(pat);
			}
			return projection;
		}
		
		public BidiMultiMap elaborateProjection(Object[] fields, ValList names, String tableName) {
			BidiMultiMap projection = new BidiMultiMap();
			for (int i = 0; i < fields.length; i++) {
				String[] parts = stringValueOf(fields[i]).split("\\|", 2);
				String field, func = null;
				switch (parts.length) {
				case 2:
					func = parts[1];
				case 1:
					field = parts[0];
					break;
				default:
					continue;
				}
				if (names != null && !names.contains(field)) {
					BerichtsheftPlugin.consoleMessage("datadock.transport-check.message", field, tableName);
					return null;
				}
				projection.add(field, func);
			}
			return projection;
		}
		
		public String makeTemplate(Object[] projection) {
			int length = projection.length;
			if (length < 1)
				return null;
			getTemplateOptions(null, length);
			String template = fieldSeparators[0];
			for (int i = 0; i < length; i++) {
				template += enclose(CLIPPER, projection[i].toString());
				template += fieldSeparators[i + 1];
			}
			return template;
		}
		
		public String wrapRecords(JTable table) {
			boolean isCellSelection = table.getCellSelectionEnabled();
			int[] rows = table.getSelectedRows();
			int[] cols = table.getSelectedColumns();
			int length = isCellSelection ? cols.length : table.getColumnCount();
			DataView.DataModel model = (DataView.DataModel) table.getModel();
			ValList records = vlist();
			for (int i = 0; i < rows.length; i++) {
				String rec = recordDecoration[0] + fieldSeparators[0];
				int row = rows[i];
				row = table.convertRowIndexToModel(row);
				Object[] values = model.getValues(true, row);
				for (int j = 0; j < length; j++) {
					int col = isCellSelection ? cols[j] : j;
					rec += stringValueOf(values[col]);
					rec += fieldSeparators[j + 1];
				}
				rec += recordDecoration[1];
				records.add(rec);
			}
			return join(recordSeparator[0], records.toArray());
		}
		
		public String[] fieldSeparators, recordDecoration;
		public Pattern[] fieldSeparatorPatterns;
		public String[] fieldSeparator, recordSeparator;
		
		private void getTemplateOptions(ValMap profile, int length) {
			Object key = BerichtsheftPlugin.getOptionProperty("field-separator");
			fieldSeparator = (String[]) BerichtsheftOptionPane.separators.get(key);
			ArrayList<String> list = new ArrayList<String>();
			list.add("");
			for (int i = 0; i < length - 1; i++) {
				list.add(fieldSeparator[0]);
			}
			list.add("");
			fieldSeparators = list.toArray(strings());
			if (profile != null && profile.containsKey("recordSeparator"))
				key = profile.get("recordSeparator");
			else
				key = BerichtsheftPlugin.getOptionProperty("record-separator");
			recordSeparator = (String[]) BerichtsheftOptionPane.separators.get(key);
			if (profile != null && profile.containsKey("recordDecoration"))
				key = profile.get("recordDecoration");
			else 
				key = BerichtsheftPlugin.getOptionProperty("record-decoration");
			recordDecoration = (String[]) BerichtsheftOptionPane.decorations.get(key);
		}

		public DataView.DataModel scan(Readable input, BidiMultiMap projection) {
			DataView.DataModel model = 
					new DataView.DataModel().setProjection(projection);
			Scanner scanner = new Scanner(input);
			String delimiter = null;
			try {
				scanner.skip(Pattern.quote(recordDecoration[0]));
				delimiter = Pattern.quote(recordDecoration[1]) + recordSeparator[1] + Pattern.quote(recordDecoration[0]);
				scanner.useDelimiter(delimiter);
				int no = 0;
				do {
					String record = scanner.next();
					if (!scanner.hasNext()) {
						MatchResult mr = findFirstIn(record, Pattern.compile(Pattern.quote(recordDecoration[1])));
						if (mr == null) {
							BerichtsheftPlugin.consoleMessage("datadock.scan.message.1");
							return null;
						}
						record = record.substring(0, mr.start());
					}
					ValList values = splitRecord(record, projection.getKeys().toArray(), ++no);
					if (values == null)
						return null;
					model.addValues(true, values.toArray());
				}
				while (scanner.hasNext());
			} 
			catch (NoSuchElementException e) {
				String msg = delimiter == null ? 
						"datadock.scan.message.2" : "datadock.scan.message.3";
				BerichtsheftPlugin.consoleMessage(msg);
				return null;
			}
			finally {
				scanner.close();
			}
			return model;
		}

		private ValList splitRecord(String record, Object[] fieldNames, int no) {
			MatchResult mr = findFirstIn(record, fieldSeparatorPatterns[0]);
			if (mr == null) {
				BerichtsheftPlugin.consoleMessage("datadock.split.message.1", no);
				return null;
			}
			ValList values = vlist();
			for (int i = 0; i < fieldNames.length; i++) {
				record = record.substring(mr.end());
				Pattern pattern = fieldSeparatorPatterns[i + 1];
				mr = findFirstIn(record, pattern);
				if (mr == null) {
					BerichtsheftPlugin.consoleMessage("datadock.split.message.2", no, fieldNames[i]);
					return null;
				}
				String field = record.substring(0, mr.start());
				values.add(field);
			}
			return values;
		}
		
	}

	public static int showItems(View view, 
			String titleProperty, 
			String caption, 
			Object message, 
			int optionType, 
			int modality, 
			Job<Void> followUp, Object...params) 
	{
		if (message instanceof JTable) {
			JTable table = (JTable) message;
			table.setPreferredScrollableViewportSize(new Dimension(800,200));
//	        table.setFillsViewportHeight(true);
	        int sel;
			switch (sel = paramInteger(-3, 0, params)) {
			case -3:
				break;
			case -2:
				table.setEnabled(false);
				break;
			case -1:
				table.selectAll();
				break;
			default:
				table.getSelectionModel().setSelectionInterval(sel, sel);
				break;
			}
			message = new JScrollPane(table);
		}
		return new JEditOptionDialog(view, 
				BerichtsheftPlugin.getProperty(titleProperty), 
				caption, 
				message, 
				optionType,
				modality, 
				null, followUp).getResult();
	}
	
	// NOTE used in scripts
	public static boolean doTransport(final View view, String oper, Object...params) {
		boolean showData = paramBoolean(true, 0, params);
		DataDockable dockable = (DataDockable) BerichtsheftPlugin.getDockable(view, "datadock", false);
		if (dockable == null) {
			BerichtsheftPlugin.consoleMessage("datadock.dockable-required.message");
			return false;
		}
		final String uriString = BerichtsheftPlugin.getProperty("TRANSPORT_URI");
		if (nullOrEmpty(uriString)) {
			BerichtsheftPlugin.consoleMessage("datadock.transport-uri.message");
			return false;
		}
		final Provider provider = new Provider(uriString);
		boolean retval = true;
		try {
			if (dockable != null)
				dockable.dataView.wireObserver(true);
			final TransportBuilder builder = new TransportBuilder();
			if ("push".equals(oper)) {
				ValMap profile = ProfileManager.getProfileAsMap();
				String template = stringValueOf(profile.get("template"));
				ValList list = builder.evaluateTemplate(template, profile);
				retval = isAvailable(0, list);
				if (retval) {
					BidiMultiMap projection = builder.elaborateProjection(list.toArray(), 
							provider.info.getList("name"), 
							provider.tableName);
					if (projection == null)
						return false;
					DataView.DataModel model = provider.query(uriString, projection, profile.get("filter"));
					if (model == null)
						return false;
					final JTable table = model.makeTable();
					Job<Void> pushThis = new Job<Void>() {
						public void perform(Void t, Object[] params) throws Exception {
							String text = builder.wrapRecords(table);
							BerichtsheftPlugin.getJEditor().setSelectedText(text);
						}
					};
					if (showData) {
						showItems(view, "datadock.transport-to-buffer.label", 
								String.format("%d record(s)", model.getRowCount()),
								table, 
								JOptionPane.OK_CANCEL_OPTION, 
					    		Behavior.NONE, 
					    		pushThis, -1);
					} 
					else
						pushThis.perform(null, null);
				}
				dockable = null;
			}
			else if ("pull".equals(oper)) {
				String text = BerichtsheftPlugin.getJEditor().getSelectedText();
				if (nullOrEmpty(text)) {
					BerichtsheftPlugin.consoleMessage("berichtsheft.no-text-selection.message");
					return false; 
				}
				ValMap profile = ProfileManager.getProfileAsMap();
				String template = stringValueOf(profile.get("template"));
				ValList list = builder.evaluateTemplate(template, profile);
				retval = isAvailable(0, list);
				if (retval) {
					BidiMultiMap projection = builder.elaborateProjection(list.toArray(), 
							provider.info.getList("name"), 
							provider.tableName);
					if (projection == null)
						return false;
					DataView.DataModel model = builder.scan(new StringReader(text), projection);
					if (null == model) {
						BerichtsheftPlugin.consoleMessage("datadock.transport-fail.message", oper, "no data");
						return false;
					}
					JTable table = model.makeTable();
					if (showData) {
						retval = JOptionPane.OK_OPTION == showItems(view,
								"datadock.transport-from-buffer.label",
								String.format("%d record(s)", model.getRowCount()),
								table, 
								JOptionPane.OK_CANCEL_OPTION,
								Behavior.MODAL,
								null, -1);
					}
					if (retval) {
						int[] results = provider.pickRecords(view, table, uriString, profile);
						retval = results != null;
						if (retval)
							BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message", 
									results[0], results[1]);
					} 
				}
			}
			else if ("download".equals(oper)) {
				ValMap profile = ProfileManager.getProfileAsMap();
				String url = stringValueOf(profile.get("url"));
				final String text = readFromUrl(url, "UTF-8");
				Job<Void> pushThis = new Job<Void>() {
					public void perform(Void t, Object[] params) throws Exception {
						BerichtsheftPlugin.getJEditor().setSelectedText(text);
					}
				};
		   	    if (showData) {
					showItems(view, "datadock.download-to-buffer.label", 
							BerichtsheftPlugin.getProperty("datadock.transport-to-buffer.message.1"), 
							text, 
							JOptionPane.OK_CANCEL_OPTION, 
				    		Behavior.NONE, 
				    		pushThis);
				}
				else
					pushThis.perform(null, null);
		   	}
			else if ("weather".equals(oper)) {
				final int[] results = new int[]{0,0,0};
				WeatherManager wm = new WeatherManager() {
					@Override
					public Object updateOrInsert(String location, long time, ValMap values) {
						Object[] names = provider.info.getList("name").toArray();
						ValMap profile = ProfileManager.getProfileAsMap("_weather", "download");
						ValList conversions = vlist();
						ValMap map = ScriptManager.getDefaultConversions(profile.get("brand"), provider.tableName);
						for (int i = 0; i < names.length; i++) {
							Object conv = map.get(names[i]);
							conversions.add(conv);
						}
						BidiMultiMap projection = new BidiMultiMap(new ValList(asList(names)), conversions);
						Object pk = provider.info.get("PRIMARY_KEY");
						projection.removeKey(pk);
						ValList list = vlist();
						for (Object key : projection.getKeys()) {
							if (Weathers.LOCATION.equals(key))
								list.add(location);
							else if (Weathers.CREATED_DATE.equals(key))
								list.add(time);
							else if (Weathers.MODIFIED_DATE.equals(key))
								list.add(now());
							else if (values.containsKey(key))
								list.add(values.get(key));
						}
						Object[] items = list.toArray();
						ContentValues contentValues = contentValues(provider.info, projection.getKeys(), items);
						Object result = provider.updateOrInsert(uriString, 
								profile, 
								projection, 
								pk, 
								contentValues,
								provider.skipThis(view, items));
						if (!provider.checkResult(result, ++results[2])) 
							result = null;
						else if (result instanceof Uri)
							results[0]++;
						else
							results[1] += (int) result;
						return result;
					}
				};
				wm.parseSite(wm.location, DatePicker.Period.loadParts(0));
				wm.evaluate(showData);
				if (results[0] > 0 || results[1] > 0)
					BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message", 
							results[0], results[1]);
			}
			else if ("odt".equals(oper)) {
				String dbPath = dockable.dataView.getDatabasePath();
				String dateString = DatePicker.Period.weekDate();
				int[] weekDate = DatePicker.parseWeekDate(dateString);
				String docName = "Tagesberichte_" + String.format("%d_%d", weekDate[1], weekDate[0]) + ".odt";
				if (BerichtsheftApp.export(
					BerichtsheftApp.berichtsheftPath("Vorlagen/Tagesberichte.odt"), 
					BerichtsheftApp.berichtsheftPath("Dokumente/" + docName), 
					dbPath, 
					weekDate[1], weekDate[0]))
				{
					BerichtsheftPlugin.consoleMessage("berichtsheft.export-document.message", docName);
				}
				else
					BerichtsheftPlugin.consoleMessage("berichtsheft.export-document.message", "");
			}
		} catch (Exception e) {
			Log.log(Log.ERROR, DataDockable.class, e);
		}
		finally {
			if (dockable != null)
				dockable.dataView.reload();
		}
		return retval;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean generateAwkScript(String awkFileName, Object...params) {
		String uriString = BerichtsheftPlugin.getProperty("TRANSPORT_URI");
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
			BerichtsheftPlugin.setProperty("TRANSPORT_TEMPLATE", template);
			map.put(uriString, template);
			putMapSetting("TRANSPORT_MAPPINGS", map);
			ValMap info = table_info(
					BerichtsheftApp.getActivity(), 
					uriString, 
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
}
