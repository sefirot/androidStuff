package com.applang.berichtsheft.plugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import org.apache.commons.lang.StringEscapeUtils;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

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
		String tableName = dbTableName(dataView.getUri());
		if (!notNullOrEmpty(tableName)) {
    		this.view.getStatus().setMessageAndClear("No database table specified");
			return; 
		}
		JTable table = dataView.getTable();
		ValList projection = new ValList();
		int[] cols = table.getSelectedColumns();
		for (int i = 0; i < cols.length; i++) {
			TableColumn col = table.getTableHeader().getColumnModel().getColumn(cols[i]);
			projection.add(col.getHeaderValue());
		}
		if (projection.size() < 1) {
    		this.view.getStatus().setMessageAndClear("No selected columns detected");
			return; 
		}
		String text = BerichtsheftPlugin.getTextEditor().getSelectedText();
		if (!notNullOrEmpty(text)) {
    		this.view.getStatus().setMessageAndClear("No selected text in current buffer");
			return; 
		}
		String path = BerichtsheftPlugin.getTempFile("transport.txt").getPath();
		contentsToFile(new File(path), text);
		Buffer buffer = generateAwk(tableName, projection);
		if (buffer != null) {
			String awkFileName = join(".", path.substring(0, path.lastIndexOf('.')), "awk");
			buffer.save(view, awkFileName);
			jEdit._closeBuffer(view, buffer);
			jEdit.setProperty("AWK_INFILE", path);
			jEdit.setProperty("AWK_PROGFILE", awkFileName);
			String sqliteFileName = join(".", path.substring(0, path.lastIndexOf('.')), "sql");
			jEdit.setProperty("AWK_OUTFILE", sqliteFileName);
			jEdit.setProperty("SQLITE_FILE", sqliteFileName);
			jEdit.setProperty("SQLITE_DBFILE", dataView.getDatabasePath());
			BerichtsheftPlugin.invokeAction(view, "commando.transport");
		}
	}

	private Buffer generateAwk(String tableName, ValList projection) {
		ValMap schema = dataView.schema(tableName);
		ValList fieldNames = schema.getList("name");
		List<Integer> list = filterIndex(schema.getList("pk"), false, new Predicate<Object>() {
			public boolean apply(Object t) {
				return Integer.valueOf(t.toString()) < 0;
			}
		});
		Object pk = list.size() == 1 ? fieldNames.get(list.get(0)) : "";
		boolean additionalPk = projection.indexOf(pk) < 1;
		if (additionalPk)
			projection.add(pk);
		Buffer buffer = jEdit.newFile(view);
		JEditTextArea textArea = BerichtsheftPlugin.getTextEditor().getTextArea();
		if (textArea != null) {
			getOptions();
			textArea.setSelectedText("BEGIN {\n");
			String fs = fieldDecoration[3] + 
					fieldSeparator[1] + 
					fieldDecoration[2];
			textArea.setSelectedText(String.format("\tFS = \"%s\"\n", StringEscapeUtils.escapeJava(fs)));
			String rs = fieldDecoration[3] + 
					recordDecoration[3] + recordSeparator[1] + recordDecoration[2] + 
					fieldDecoration[2];
			textArea.setSelectedText(String.format("\tRS = \"%s\"\n", StringEscapeUtils.escapeJava(rs)));
			textArea.setSelectedText("\tprint \"BEGIN TRANSACTION;\"\n");
			textArea.setSelectedText("}\n");
			textArea.setSelectedText("{\n");
			textArea.setSelectedText(String.format("\tprint \"INSERT INTO %s ", tableName));
			Object[] array = projection.toArray();
			textArea.setSelectedText(String.format("(%s) ", join(",", array)));
			for (int i = 0; i < array.length; i++) {
				int index = fieldNames.indexOf(array[i]);
				String type = schema.getListValue("type", index).toString();
				if (additionalPk && pk.equals(array[i]))
					array[i] = "\" \"null\" \"";
				else {
					array[i] = "\" $" + (i + 1) + " \"";
					if ("TEXT".compareToIgnoreCase(type) == 0)
						array[i] = enclose("'", array[i].toString());
				}
			}
			textArea.setSelectedText(String.format("VALUES (%s)", join(",", array)));
			textArea.setSelectedText(";\"\n");
			textArea.setSelectedText("}\n");
			textArea.setSelectedText("END {\n");
			textArea.setSelectedText("\tprint \"COMMIT;\"\n");
			textArea.setSelectedText("}\n");
			return buffer;
		}
		return null;
	}
	
	String[] fieldDecoration, recordDecoration;
	String[] fieldSeparator, recordSeparator;
	
	private void getOptions() {
		fieldDecoration = (String[]) BerichtsheftOptionPane.decorations
				.get(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-decoration"));
		fieldSeparator = (String[]) BerichtsheftOptionPane.separators
				.get(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "field-separator"));
		recordDecoration = (String[]) BerichtsheftOptionPane.decorations
				.get(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-decoration"));
		recordSeparator = (String[]) BerichtsheftOptionPane.separators
				.get(jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "record-separator"));
	}

	private String generateText(JTable table) {
		getOptions();
		ValList records = new ValList();
		int[] rows = table.getSelectedRows();
		int[] cols = table.getSelectedColumns();
		for (int i = 0; i < rows.length; i++) {
			String text = recordDecoration[0];
			ValList fields = new ValList();
			for (int j = 0; j < cols.length; j++) {
				Object value = table.getModel().getValueAt(rows[i], cols[j]);
				fields.add(enclose(fieldDecoration[0], value.toString(), fieldDecoration[1]));
			}
			text += join(fieldSeparator[0], fields.toArray());
			text += recordDecoration[1];
			records.add(text);
		}
		return join(recordSeparator[0], records.toArray());
	}

	public void transportToBuffer() {
		JTable table = dataView.getTable();
		ListSelectionModel sm = table.getSelectionModel();
		if (sm.isSelectionEmpty()) {
			this.view.getStatus().setMessageAndClear("No selected cells in table detected");
			return; 
		}
		final String text = generateText(table);
		String title = jEdit.getProperty("berichtsheft.transport-to-buffer.label");
		JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setText(text);
		showOptionDialog(view, 
			objects(new JScrollPane(textArea)), 
			title, 
			JOptionPane.DEFAULT_OPTION + Modality.ALWAYS_ON_TOP,
			JOptionPane.PLAIN_MESSAGE,
    		null,
    		null, null, 
    		null,
    		new Function<Boolean>() {
				public Boolean apply(Object... params) {
					BerichtsheftPlugin.getTextEditor().setSelectedText(text);
					return false;
				}
			}
		);
	}
}
