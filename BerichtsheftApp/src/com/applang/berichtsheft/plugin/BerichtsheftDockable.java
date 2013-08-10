package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.bsh.NameSpace;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.applang.Util.ValList;
import com.applang.berichtsheft.BerichtsheftApp;
import com.applang.berichtsheft.components.DataView;

/**
 * 
 * Berichtsheft - a dockable JPanel as a jEdit plugin.
 *
 */
public class BerichtsheftDockable extends JPanel
    implements EBComponent, BerichtsheftActions, DefaultFocusComponent {

	private static final long serialVersionUID = 6415522692894321789L;

	private View view;

	private boolean floating;

	DataView dataView;

	private BerichtsheftToolPanel toolPanel;

	/**
	 * 
	 * @param view the current jedit window
	 * @param position a variable passed in from the script in actions.xml,
	 * 	which can be DockableWindowManager.FLOATING, TOP, BOTTOM, LEFT, RIGHT, etc.
	 * 	see @ref DockableWindowManager for possible values.
	 */
	public BerichtsheftDockable(View view, String position) {
		super(new BorderLayout());
		this.view = view;
		this.floating = position.equals(DockableWindowManager.FLOATING);

		if (floating)
			this.setPreferredSize(new Dimension(500, 250));

		dataView = new DataView(BerichtsheftApp.getActivity());
		String uriString = getSetting("dburi", "");
		dataView.setUri(uriString);
		dataView.getTable().setFont(BerichtsheftOptionPane.makeFont());
		add(BorderLayout.CENTER, dataView);

		this.toolPanel = new BerichtsheftToolPanel(this);
		add(BorderLayout.NORTH, this.toolPanel);

		propertiesChanged();
	}
    
	public static Document transports = null;

	public static boolean transportsLoaded(Object...params) {
		if (transports == null) {
			String settingsDir = param(System.getProperty("settings.dir", ""), 0, params);
			File file = new File(settingsDir, "transports.xml");
			if (fileExists(file))
				transports = xmlDocument(file);
		}
		return transports != null;
	}

	public static void saveTransports(Object...params) {
		if (transports != null) {
			String settingsDir = param(System.getProperty("settings.dir", ""), 0, params);
			File file = new File(settingsDir, "transports.xml");
			xmlNodeToFile(transports, true, file);
		}
	}
	
	public void focusOnDefaultComponent() {
		dataView.requestFocus();
	}

	public String getUriString() {
		return dataView.getUri().toString();
	}

	// EBComponent implementation
	
	public void handleMessage(EBMessage message) {
		if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}
    
	private void propertiesChanged() {
		toolPanel.propertiesChanged();
		
		Font newFont = BerichtsheftOptionPane.makeFont();
		JTable table = dataView.getTable();
		Font oldFont = table.getFont();
		if (!newFont.equals(oldFont)) {
			table.setFont(newFont);
		}
		
		dataView.reload();
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
		saveTransports();
	}
    
	// Actions implementation
    
	private Function<File> chooser = new Function<File>() {
		public File apply(Object... params) {
			boolean toOpen = param(true, 0, params);
			File file = param(null, 3, params);
			String fileName = file == null ? null : file.getPath();
			String[] paths = GUIUtilities.showVFSFileDialog(view, fileName,
					toOpen ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG, 
							false);
			if (isAvailable(0, paths)) 
				return new File(paths[0]);
			else
				return null;
		}
	};
	
	public void chooseUri() {
    	if (dataView.askUri(chooser, dataView.getInfo())) {
    		putSetting("dburi", getUriString());
    		updateUri();
		}
    	else
    		this.view.getStatus().setMessageAndClear("Not a valid SQLite database");
	};
	
	public void updateUri() {
		propertiesChanged();
	}

	public void transportFromBuffer() {
		String text = BerichtsheftPlugin.getTextEditor().getSelectedText();
		if (!notNullOrEmpty(text)) {
    		message("No selected text in current buffer");
			return; 
		}
		String path = BerichtsheftPlugin.getTempFile("transport.txt").getPath();
		contentsToFile(new File(path), text);
		String awkFileName = join(".", path.substring(0, path.lastIndexOf('.')), "awk");
		if (generateAwkScript(awkFileName)) {
			BerichtsheftPlugin.setProperty("AWK_PROGFILE", awkFileName);
			String sqliteFileName = join(".", path.substring(0, path.lastIndexOf('.')), "sql");
			BerichtsheftPlugin.setProperty("AWK_INFILE", path);
			BerichtsheftPlugin.setProperty("AWK_OUTFILE", sqliteFileName);
			BerichtsheftPlugin.setProperty("SQLITE_FILE", sqliteFileName);
			BerichtsheftPlugin.setProperty("SQLITE_DBFILE", dataView.getDatabasePath());
			BerichtsheftPlugin.invokeAction(view, "commando.transport");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean generateAwkScript(String awkFileName, Object...params) {
		String uriString = BerichtsheftPlugin.getProperty("TRANSPORT_URI");
		if (!notNullOrEmpty(uriString))
			return false;
		String tableName = dbTableName(uriString);
		Map map = getMapSetting("TRANSPORT_MAPPINGS", new ValMap());
		String template = param((String) map.get(uriString), 0, params);
		if (template == null)
			return false;
		
		ValList projection = evaluateTemplate(template);
		boolean retval = projection != null && projection.size() > 0;
		if (retval) {
			BerichtsheftPlugin.setProperty("TRANSPORT_TEMPLATE", template);
			map.put(uriString, template);
			putMapSetting("TRANSPORT_MAPPINGS", map);
			ValMap schema = table_info(
					BerichtsheftApp.getActivity(), 
					uriString, 
					tableName);
			Object pk = schema.get("PRIMARY_KEY");
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
			sb.append(String.format("\tFS = \"%s\"\n", escapeRegex(fieldSeparator)));
			sb.append(String.format("\tRS = \"%s\"\n", 
					escapeRegex(recordDecoration[1]) + 
					escapeRegex(recordSeparator) + 
					escapeRegex(recordDecoration[0])));
			sb.append("\t");
			int length = fieldSeparators.length;
			for (int i = 0; i < length; i++) {
				String sep = escapeRegex(fieldSeparators[i]);
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
			sb.append("\t\tgsub(/^" + escapeRegex(recordDecoration[0]) + "/, \"\", record)\n");
			sb.append("\tgsub(/" + escapeRegex(recordDecoration[1]) + "$/, \"\", record)\n");
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
			ValList fieldNames = schema.getList("name");
			for (int i = 0; i < array.length; i++) {
				int index = fieldNames.indexOf(array[i]);
				String type = schema.getListValue("type", index).toString();
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

	public static String escapeRegex(String s) {
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
	
	public static Pattern TRANSPORT_TEMPLATE_PATTERN = Pattern.compile("(^[^']+|)'([^']+)'([^']+|$)");

	public static ValList evaluateTemplate(String template) {
		MatchResult[] mr = findAllIn(template, TRANSPORT_TEMPLATE_PATTERN);
		if (mr.length < 1) {
			Log.log(Log.WARNING, BerichtsheftDockable.class, "format definition : no field identified");
			return null;
		}
		getFormatOptions(0);
		ValList projection = new ValList();
		ArrayList<String> list = new ArrayList<String>();
		int pos = 0;
		for (int i = 0; i < mr.length; i++) {
    		MatchResult m = mr[i];
    		if (pos != m.start()) {
    			Log.log(Log.WARNING, BerichtsheftDockable.class, "format definition inconsistent");
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
			Log.log(Log.WARNING, BerichtsheftDockable.class, "format definition inconsistent");
			return null;
		}
		fieldSeparators = list.toArray(strings());
		fieldSeparatorPatterns = new Pattern[fieldSeparators.length];
		for (int i = 0; i < fieldSeparators.length; i++) {
			String pat = Pattern.quote(fieldSeparators[i]);
			if (i == 0)
				pat = "^" + pat;
			else if (i == fieldSeparators.length - 1)
				pat += "$";
			fieldSeparatorPatterns[i] = Pattern.compile(pat);
		}
		return projection;
	}

	public static String makeTemplate(Object[] projection) {
		int length = projection.length;
		if (length < 1)
			return null;
		getFormatOptions(length);
		String template = fieldSeparators[0];
		for (int i = 0; i < length; i++) {
			template += enclose("'", projection[i].toString());
			template += fieldSeparators[i + 1];
		}
		return template;
	}
	
	public static String[] fieldSeparators, recordDecoration;
	public static Pattern[] fieldSeparatorPatterns;
	private static String fieldSeparator, recordSeparator;
	
	private static void getFormatOptions(int length) {
		recordDecoration = (String[]) BerichtsheftOptionPane.decorations
				.get(BerichtsheftPlugin.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-decoration"));
		recordSeparator = (String) BerichtsheftOptionPane.separators
				.get(BerichtsheftPlugin.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-separator"));
		fieldSeparator = (String) BerichtsheftOptionPane.separators
				.get(BerichtsheftPlugin.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-separator"));
		ArrayList<String> list = new ArrayList<String>();
		list.add("");
		for (int i = 0; i < length - 1; i++) {
			list.add(fieldSeparator);
		}
		list.add("");
		fieldSeparators = list.toArray(strings());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String generateText(JTable table) {
		ValList records = new ValList();
		int[] rows = table.getSelectedRows();
		int[] cols = table.getSelectedColumns();
		int length = cols.length;
		
		ValList projection = DataView.getProjection(table);
		String uriString = dataView.addQueryStringToUri(projection);
		BerichtsheftPlugin.setProperty("TRANSPORT_URI", uriString);
		String template = getTemplate();
		if (!notNullOrEmpty(template)) {
			template = makeTemplate(projection.toArray());
			if (setTemplate(template))
				updateTransportCommand();
		}
		
		Map map = getMapSetting("TRANSPORT_MAPPINGS", new ValMap());
		map.put(uriString, template);
		putMapSetting("TRANSPORT_MAPPINGS", map);
		BerichtsheftPlugin.setProperty("TRANSPORT_TEMPLATE", template);
		
		for (int i = 0; i < rows.length; i++) {
			String text = recordDecoration[0] + fieldSeparators[0];
			for (int j = 0; j < length; j++) {
				Object value = table.getModel().getValueAt(rows[i], cols[j]);
				text += value.toString();
				text += fieldSeparators[j + 1];
			}
			text += recordDecoration[1];
			records.add(text);
		}
		return join(recordSeparator, records.toArray());
	}

	public void transportToBuffer() {
		JTable table = dataView.getTable();
		ListSelectionModel sm = table.getSelectionModel();
		if (sm.isSelectionEmpty()) {
			this.view.getStatus().setMessageAndClear("No selected cells in table detected");
			return; 
		}
		String text = generateText(table);
		showItems(view, "berichtsheft.transport-to-buffer.label", text,
	    		new Function<Void>() {
					public Void apply(Object... params) {
						BerichtsheftPlugin.getTextEditor().setSelectedText(param("", 0, params));
						return null;
					}
				}, true);
	}

	private static void showItems(View view, String titleProperty, String text, final Function<Void> followUp, boolean editable) {
		String title = jEdit.getProperty(titleProperty);
		JTextArea textArea = new JTextArea();
		textArea.setName("text");
		textArea.setLineWrap(true);
		textArea.setEditable(editable);
		textArea.setText(text);
		showOptionDialog(view, 
			objects(new JScrollPane(textArea)), 
			title, 
			JOptionPane.OK_CANCEL_OPTION + Modality.ALWAYS_ON_TOP,
			JOptionPane.PLAIN_MESSAGE,
    		null,
    		null, null,
    		null,
    		new Function<Boolean>() {
				public Boolean apply(Object... params) {
					if (JOptionPane.OK_OPTION == dialogResult) {
						Object[] array = param((Object[])null, 2, params);
						JTextArea textArea = findComponent((Container)array[0], "text");
						followUp.apply(textArea.getText());
					}
					return false;
				}
			}
		);
	}
	
	public static String getTemplate() {
		String template = null;
		String uriString = BerichtsheftPlugin.getProperty("TRANSPORT_URI");
		if (notNullOrEmpty(uriString) && transportsLoaded()) {
			String xpath = "//TRANSPORT[@uri='" + uriString + "']/TEMPLATE";
			Element element = selectElement(transports, xpath);
			if (element != null) {
				template = element.getTextContent();
				evaluateTemplate(template);
			}
		}
		return template;
	}
	
	public static boolean setTemplate(String template) {
		String uriString = BerichtsheftPlugin.getProperty("TRANSPORT_URI");
		if (notNullOrEmpty(uriString) && transportsLoaded()) {
			String xpath = "//TRANSPORT[@uri='" + uriString + "']";
			Element element = selectElement(transports, xpath);
			if (element == null) {
				element = transports.createElement("TRANSPORT");
				element.setAttribute("uri", uriString);
				transports.getDocumentElement().appendChild(element);
			}
			NodeList nodes;
			while ((nodes = evaluateXPath(element, "./*[name()='TEMPLATE']")).getLength() > 0)
				element.removeChild(nodes.item(0));
			element = (Element) element.appendChild(transports.createElement("TEMPLATE"));
			CDATASection cdata = transports.createCDATASection(template);
			element.appendChild(cdata);
			return true;
		}
		return false;
	}

	public static void updateTransportCommand() {
		if (transportsLoaded()) {
			File file = new File(BerichtsheftPlugin.userCommandDirectory, "transport.xml");
			if (fileExists(file)) {
				Document doc = xmlDocument(file);
				Element uriChoice = selectElement(doc, "//CHOICE[@VARNAME='uri']");
				if (uriChoice == null) {
					Element after = selectElement(doc, "//CAPTION[@LABEL='Template']");
					uriChoice = doc.createElement("CHOICE");
					uriChoice.setAttribute("LABEL", "content provider");
					uriChoice.setAttribute("VARNAME", "uri");
					after.getParentNode().insertBefore(uriChoice, after);
				}
				NodeList nodes;
				while ((nodes = uriChoice.getChildNodes()).getLength() > 0)
					uriChoice.removeChild(nodes.item(0));
				NodeList transportNodes = evaluateXPath(transports, "//TRANSPORT");
				for (int i = 0; i < transportNodes.getLength(); i++) {
					Element option = (Element) transportNodes.item(i);
					String desc = option.getAttribute("desc");
					String uriString = option.getAttribute("uri");
					option = doc.createElement("OPTION");
					option.setAttribute("LABEL", uriString);
					option.setAttribute("VALUE", uriString);
					uriChoice.appendChild(option);
				}
				uriChoice.setAttribute("EVAL", "jEdit.getProperty(\"TRANSPORT_URI\", \"\")");
				xmlNodeToFile(doc, true, file);
			}
		}
	}
	
	public static boolean doTransport(View view, String oper, String template, Reader input) {
		if (template == null)
			return false;
		
		boolean retval = true;
		if ("pull".equals(oper)) {
			final String uriString = BerichtsheftPlugin.getProperty("TRANSPORT_URI");
			if (!notNullOrEmpty(uriString))
				return false;
			
			ValList list = evaluateTemplate(template);
			retval = list != null && list.size() > 0;
			if (retval) {
				Object[] projection = list.toArray();
				
				setTemplate(template);
				
				String tableName = dbTableName(uriString);
				ValMap schema = table_info(
						BerichtsheftApp.getActivity(), 
						uriString, 
						tableName);
				final Object pk = schema.get("PRIMARY_KEY");
				
				final ValList records = scanInput(input, projection, oper);
				if (null == records)
					return false;
				
				showItems(view, "berichtsheft.transport-from-buffer.label", 
						com.applang.Util2.toString(records.toArray()),
			    		new Function<Void>() {
							public Void apply(Object... params) {
								for (Object items : records) {
									updateOrInsert(uriString, 
											pk, 
											(Object[]) items);
								}
								return null;
							}
						}, false);
			}
		}
		else if ("push".equals(oper)) {
			
		}
		return retval;
	}

	public static ContentValues makeContentValues(String uriString, Object[] items) {
		ContentValues values = new ContentValues();
		Uri uri = Uri.parse(uriString);
		ValList list = split(uri.getQuery(), "&");
		for (int i = 0; i < list.size(); i++) {
			String[] parts = list.get(i).toString().split("=");
			if ("TEXT".equals(parts[1]))
				values.put(parts[0], (String)items[i]);
			else if ("INTEGER".equals(parts[1]))
				values.put(parts[0], toInt(Integer.MIN_VALUE, items[i].toString()));
			else if ("REAL".equals(parts[1]))
				values.put(parts[0], toDouble(Double.NaN, items[i].toString()));
			else if ("BLOB".equals(parts[1]))
				values.put(parts[0], (byte[]) items[i]);
			else
				values.putNull(parts[0]);
		}
		return values;
	}

	private static boolean updateOrInsert(String uriString, Object primaryKeyColumnName, Object[] items) {
		try {
			Context context = BerichtsheftApp.getActivity();
			ContentResolver contentResolver = context.getContentResolver();
			ContentValues contentValues = makeContentValues(uriString, items);
			Uri uri = Uri.parse(uriString);
			boolean primaryKey = primaryKeyColumnName != null;
			boolean primaryKeyExtraColumn = primaryKey && 
					!contentValues.keySet().contains(primaryKeyColumnName.toString());
			if (primaryKeyExtraColumn && transportsLoaded()) {
				NameSpace tmp = new NameSpace(BeanShell.getNameSpace(), "transport");
				tmp.setVariable("contentValues", contentValues);
				Element el = selectElement(transports, "//FUNCTION[@name='updateOrInsert-clause']");
				if (el != null) {
					String script = el.getTextContent();
					String clause = (String) BeanShell.eval(null, tmp, script);
					String pk = primaryKeyColumnName.toString();
					Cursor cursor = contentResolver.query(uri, 
							strings(pk), 
							clause, 
							null, null);
					if (cursor.moveToFirst())
						contentValues.put(pk, cursor.getLong(0));
					cursor.close();
				}
				primaryKeyExtraColumn = false;
			}
			boolean retval = false;
			if (primaryKeyExtraColumn) {
				contentValues.putNull(primaryKeyColumnName.toString());
				retval = contentResolver.insert(uri, contentValues) != null;
			}
			else if (primaryKey) {
				String pk = primaryKeyColumnName.toString();
				Object pkval = contentValues.get(pk);
				contentValues.remove(pk);
				retval = 0 < contentResolver.update(uri, contentValues, "where " + pk + "=?", strings(pkval.toString()));
			}
			else
				retval = contentResolver.insert(uri, contentValues) != null;
			return retval;
		} catch (Exception e) {
			Log.log(Log.ERROR, BerichtsheftDockable.class, 
					String.format("insertOrUpdate on '%s' failed with values '%s'", 
							uriString, Arrays.toString(items)));
			return false;
		}
	}

	private static ValList scanInput(Readable input, Object[] fieldNames, String oper) {
		ValList records = new ValList();
		Scanner scanner = new Scanner(input);
		String delimiter = null;
		try {
			scanner.skip(Pattern.quote(recordDecoration[0]));
			delimiter = Pattern.quote(recordDecoration[1] + recordSeparator + recordDecoration[0]);
			scanner.useDelimiter(delimiter);
			do {
				String record = scanner.next();
				if (!scanner.hasNext()) {
					MatchResult mr = findFirstIn(record, Pattern.compile(Pattern.quote(recordDecoration[1])));
					if (mr == null) {
						Log.log(Log.WARNING, BerichtsheftDockable.class, "input not ending with a closing record decorator");
						return null;
					}
					record = record.substring(0, mr.start());
				}
				ValList fields = splitRecord(record, fieldNames, oper);
				if (fields == null)
					return null;
				records.add(fields.toArray());
			}
			while (scanner.hasNext());
		} catch (NoSuchElementException e) {
			String message = delimiter == null ? 
					"input not starting with an opening record decorator" : 
					"problem recognizing a record";
			Log.log(Log.WARNING, BerichtsheftDockable.class, message);
			return null;
		}
		finally {
			scanner.close();
		}
		return records;
	}

	private static ValList splitRecord(String record, Object[] fieldNames, String oper) {
		MatchResult mr = findFirstIn(record, fieldSeparatorPatterns[0]);
		if (mr == null) {
			Log.log(Log.ERROR, BerichtsheftDockable.class, "record not starting with the opening field separator");
			return null;
		}
		ValList values = new ValList();
		for (int i = 0; i < fieldNames.length; i++) {
			record = record.substring(mr.end());
			mr = findFirstIn(record, fieldSeparatorPatterns[i + 1]);
			if (mr == null) {
				Log.log(Log.ERROR, BerichtsheftDockable.class, String.format("field %d not recognized", i));
				return null;
			}
			String field = record.substring(0, mr.start());
			String name = fieldNames[i].toString();
			if (name.contains("|")) {
				String[] parts = name.split("\\|");
				Object value = doConversion(parts[1], field, oper);
				values.add(value);
			}
			else
				values.add(field);
		}
		return values;
	}

	public static Object doConversion(String function, String value, String oper) {
		try {
			if (!transportsLoaded())
				return value;
			NameSpace tmp = new NameSpace(BeanShell.getNameSpace(), "transport");
			tmp.setVariable("input", value);
			tmp.setVariable("oper", oper);
			Element el = selectElement(transports, "//FUNCTION[@name='" + function + "']");
			if (el == null)
				return value;
			String script = el.getTextContent();
			return BeanShell.eval(null, tmp, script);
		} catch (Exception e) {
			Log.log(Log.ERROR, BerichtsheftDockable.class, 
					String.format("conversion '%s' failed on value '%s'", function, value));
			return null;
		}
	}
}
