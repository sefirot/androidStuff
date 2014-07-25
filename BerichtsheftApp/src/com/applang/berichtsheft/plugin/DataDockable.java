package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.SwingUtil.*;
import static com.applang.PluginUtils.*;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.components.DataView;
import com.applang.components.DatePicker;
import com.applang.components.ProfileManager;
import com.applang.components.DataAdapter;
import com.applang.components.ScriptManager;
import com.applang.components.WeatherManager;
import com.applang.provider.WeatherInfo;
import com.applang.provider.WeatherInfo.Weathers;

import console.Console;

/**
 * 
 * A dockable JPanel as a jEdit plugin.
 *
 */
public class DataDockable extends JPanel implements EBComponent, BerichtsheftActions, DefaultFocusComponent
{
    private static final String TAG = DataDockable.class.getSimpleName();

	private static final long serialVersionUID = 6415522692894321789L;

	private boolean floating;
	private View view;
	
	public class DataToolPanel extends JPanel
	{
		private JLabel label = new JLabel();
	
		public DataToolPanel() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	
			add(makeCustomButton("datadock.choose-uri", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					chooseUri();
				}
			}, false));
			add(makeCustomButton("datadock.update-uri", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					updateUri();
				}
			}, false));
			add(makeCustomButton("datadock.transport-to-buffer", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					transportToBuffer();
				}
			}, false));
			add(makeCustomButton("datadock.transport-from-buffer", new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					final Console console = BerichtsheftShell.getConsole(true);
					if (console != null) {
						BerichtsheftShell.consoleWait(console, true);
						new AlertDialog(view, 
								"Spinner test", 
								"", 
								"the spinner icon in the 'Berichtsheft' console window should be animated", 
								JOptionPane.DEFAULT_OPTION, 
								Behavior.NONE, 
								null, 
								new Job<Void>() {
									public void perform(Void t, Object[] parms) throws Exception {
										BerichtsheftShell.consoleWait(console, false);
									}
								})
							.open();
					}
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
	
	public DataView dataView = BerichtsheftPlugin.getDataView();

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
			dataView.load();
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
    	if (dataView.configureData(view, true)) {
    		setProperty("TRANSPORT_URI", dataView.getUriString());
    		updateUri();
		}
	};
	
	public void updateUri() {
		dataView.nosync();
		NoteDockable dockable = (NoteDockable) BerichtsheftPlugin.getDockable(view, "notedock", false);
		if (dockable != null)
			dockable.propertiesChanged();
		toolPanel.propertiesChanged();
	}

	public void spellCheckSelection() {
		doTransport(view, "spellcheck");
	}

	public void transportFromBuffer() {
		setProperty("TRANSPORT_OPER", "pull");
		invokeAction(view, "commando.Transport");
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
		if (notAvailable(0, columns))
			return;
		final String text = builder.wrapRecords(table);
		showItems(view, "datadock.transport-to-buffer.label", 
				getProperty("datadock.transport-to-buffer.message.1"), 
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
			ArrayList<String> list = alist();
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
			ArrayList<String> list = alist();
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
			int behavior, 
			Job<Void> followUp, Object...params) 
	{
		if (message instanceof JTable) {
			JTable table = (JTable) message;
	        int sel;
			switch (sel = param_Integer(-3, 0, params)) {
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
			message = scrollableViewport(table, new Dimension(800,200));
		}
		return new AlertDialog(view, 
				getProperty(titleProperty), 
				caption, 
				message, 
				optionType,
				behavior, 
				null, 
				followUp).open().getResult();
	}
	
	// NOTE used in scripts
	public static boolean doTransport(final View view, String oper, Object...params) {
		boolean showData = param_Boolean(true, 0, params);
		DataDockable dockable = (DataDockable) BerichtsheftPlugin.getDockable(view, "datadock", false);
		if (dockable == null) {
			BerichtsheftPlugin.consoleMessage("datadock.dockable-required.message");
			return false;
		}
		final String uriString = getProperty("TRANSPORT_URI");
		if (nullOrEmpty(uriString)) {
			BerichtsheftPlugin.consoleMessage("datadock.transport-uri.message");
			return false;
		}
		DataAdapter dataAdapter = new DataAdapter(uriString);
		boolean retval = true;
		try {
			if (dockable != null)
				dockable.dataView.wireObserver(dockable.dataView.getContext().getContentResolver(), true);
			final TransportBuilder builder = new TransportBuilder();
			if ("push".equals(oper)) {
				ValMap profile = ProfileManager.getProfileAsMap();
				String template = stringValueOf(profile.get("template"));
				ValList list = builder.evaluateTemplate(template, profile);
				retval = isAvailable(0, list);
				if (retval) {
					BidiMultiMap projection = builder.elaborateProjection(list.toArray(), 
							dataAdapter.info.getList("name"), 
							dataAdapter.getTableName());
					if (projection == null)
						return false;
					DataView.DataModel model = dataAdapter.query(uriString, projection, profile.get("filter"));
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
							dataAdapter.info.getList("name"), 
							dataAdapter.getTableName());
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
						int[] results = dataAdapter.pickRecords(view, table, uriString, profile);
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
							getProperty("datadock.transport-to-buffer.message.1"), 
							text, 
							JOptionPane.OK_CANCEL_OPTION, 
				    		Behavior.NONE, 
				    		pushThis);
				}
				else
					pushThis.perform(null, null);
		   	}
		} catch (Exception e) {
			Log.e(TAG, "doTransport", e);
		}
		finally {
			if (dockable != null)
				dockable.dataView.reload();
		}
		return retval;
	}

	// NOTE used in scripts
	public static boolean makeWetter(final View view, String oper, Object...params) {
		boolean showData = param_Boolean(true, 0, params);
		String dbPath = dbPath(param_String("", 1, params));
		Boolean async = param_Boolean(true, 2, params);
		boolean retval = true;
		if ("period".equals(oper)) {
			final String uriString = Weathers.CONTENT_URI.toString();
			final DataAdapter dataAdapter = new DataAdapter(WeatherInfo.AUTHORITY, new File(dbPath), uriString);
			final int[] results = ints(0,0,0);
			final ValMap profile = ProfileManager.getProfileAsMap("_weather", "download");
			ValMap map = ScriptManager.getProjectionDefault(profile.get("flavor"), dataAdapter.getTableName());
			final BidiMultiMap projection = (BidiMultiMap) map.get("projection");
			final Object pk = dataAdapter.info.get("PRIMARY_KEY");
			projection.removeKey(pk);
			WeatherManager wm = new WeatherManager() {
				@Override
				public Object updateOrInsert(String location, long time, ValMap values) {
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
					ContentValues contentValues = contentValues(dataAdapter.info, projection.getKeys(), items);
					Object result = dataAdapter.updateOrInsert(uriString, 
							profile, 
							projection, 
							pk, 
							contentValues,
							dataAdapter.skipThis(view, items));
					if (!dataAdapter.checkResult(result, ++results[2])) 
						result = null;
					else if (result instanceof Uri)
						results[0]++;
					else if (result != null)
						results[1] += (Integer) result;
					return result;
				}
			};
			wm.parseAndEvaluate(wm.location, DatePicker.Period.loadParts(0), showData, 
					new Function<Void>() {
						public Void apply(Object... params) {
							int[] results = param(null, 0, params);
							if (results != null) {
								if (results[0] > 0 || results[1] > 0)
									BerichtsheftPlugin.consoleMessage("dataview.updateOrInsert.message", 
											results[0], results[1]);
							}
							return null;
						}
					},
					results, async);
		}
		return retval;
	}

	// NOTE used in scripts
	public static boolean makeDokument(final View view, String oper, Object...params) {
		boolean keep = param_Boolean(false, 0, params);
		boolean retval = false;
		if ("odt".equals(oper)) {
			String dbPath = param_String(null, 1, params);
			String dbPath2 = param_String(null, 2, params);
			DatePicker.Period.load(1);
			String dateString = DatePicker.Period.weekDate();
			int[] weekDate = DatePicker.parseWeekDate(dateString);
			String docPath = BerichtsheftApp.odtDokumentPath("Tagesberichte", weekDate);
			if (BerichtsheftApp.export(
					BerichtsheftApp.odtVorlagePath("Tagesberichte"), 
					docPath, 
					strings(dbPath,dbPath2), 
					weekDate[1], weekDate[0], "\\d", keep))
			{
				retval = true;
			}
			else {
				BerichtsheftPlugin.consoleMessage("berichtsheft.export-document.message.2");
			}
			BerichtsheftPlugin.consoleMessage("berichtsheft.export-document.message.1", docPath);
		}
		return retval;
	}

	public static void main(String...args) {
		BerichtsheftApp.loadSettings();
    	final DataDockable dd = new DataDockable(null, DockableWindowManager.FLOATING);
		showFrame(null, "Data", 
			new UIFunction() {
				public Component[] apply(final Component comp, Object[] parms) {
					JSplitPane sp = splitPane(JSplitPane.VERTICAL_SPLIT);
					sp.setTopComponent(dd);
					sp.setBottomComponent(new JPanel());
					return components(sp);
				}
			}, 
			null, null, 
			Behavior.EXIT_ON_CLOSE);
	}
}
