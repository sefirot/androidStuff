package com.applang.berichtsheft.ui;

// {{{ imports
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.gui.DefaultFocusComponent;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.StandardUtilities;

import com.applang.Util;
// }}}

// {{{ Berichtsheft class
/**
 * 
 * Berichtsheft - a dockable JPanel as a jEdit plugin.
 *
 */
public class Berichtsheft extends JPanel
    implements EBComponent, BerichtsheftActions, DefaultFocusComponent {

    // {{{ Instance Variables
	private static final long serialVersionUID = 6415522692894321789L;

	private String dbName;

	private String defaultFilename;

	private View view;

	private boolean floating;

	BerichtsheftTextArea textArea;

	private BerichtsheftToolPanel toolPanel;
    // }}}

    // {{{ Constructor
	/**
	 * 
	 * @param view the current jedit window
	 * @param position a variable passed in from the script in actions.xml,
	 * 	which can be DockableWindowManager.FLOATING, TOP, BOTTOM, LEFT, RIGHT, etc.
	 * 	see @ref DockableWindowManager for possible values.
	 */
	public Berichtsheft(View view, String position) {
		super(new BorderLayout());
		this.view = view;
		this.floating = position.equals(DockableWindowManager.FLOATING);

		textArea = new BerichtsheftTextArea();
		textArea.textArea.setFont(BerichtsheftOptionPane.makeFont());

		this.dbName = jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "filepath");

		this.toolPanel = new BerichtsheftToolPanel(this);
		add(BorderLayout.NORTH, this.toolPanel);

		if (floating)
			this.setPreferredSize(new Dimension(500, 250));

		JScrollPane pane = new JScrollPane(textArea.textArea);
		add(BorderLayout.CENTER, pane);

		readFile();
	}
    // }}}

    // {{{ Member Functions
    
    // {{{ focusOnDefaultComponent
	public void focusOnDefaultComponent() {
		textArea.textArea.requestFocus();
	}
    // }}}

    // {{{ getFileName
	public String getFilename() {
		return dbName;
	}
    // }}}

	// EBComponent implementation
	
    // {{{ handleMessage
	public void handleMessage(EBMessage message) {
		if (message instanceof PropertiesChanged) {
			propertiesChanged();
		}
	}
    // }}}
    
    // {{{ propertiesChanged
	private void propertiesChanged() {
		toolPanel.propertiesChanged();
		
		Font newFont = BerichtsheftOptionPane.makeFont();
		if (!newFont.equals(textArea.textArea.getFont())) {
			textArea.textArea.setFont(newFont);
		}
	}
    // }}}

	// These JComponent methods provide the appropriate points
	// to subscribe and unsubscribe this object to the EditBus.

    // {{{ addNotify
	public void addNotify() {
		super.addNotify();
		EditBus.addToBus(this);
	}
     // }}}
     
    // {{{ removeNotify
	public void removeNotify() {
		saveFile();
		super.removeNotify();
		EditBus.removeFromBus(this);
	}
    // }}}
    
	// NotepadActions implementation

    // {{{
	public void saveFile() {
		if (!Util.notNullOrEmpty(dbName))
			return;
		
	}
    // }}}
    
    // {{{ chooseFile
	public void chooseFile() {
		String title = jEdit.getProperty(BerichtsheftPlugin.OPTION_PREFIX + "choose-file.title");
		File file = Util.chooseFile(true, view, title, new File(dbName), null);
		if (file != null) {
			if (!file.getPath().equals(dbName)) {
				dbName = file.getPath();
				jEdit.setProperty(
						BerichtsheftPlugin.OPTION_PREFIX + "filepath",
						this.dbName);
			}
			toolPanel.propertiesChanged();
		}
	}
    // }}}

    // {{{ copyToBuffer
	public void copyToBuffer() {
		jEdit.newFile(view);
		view.getEditPane().getTextArea().setText(textArea.getText());
	}
    // }}}
    // {{{ readFile()
	/**
	 * Helper method
	 */
	private void readFile() {
		if (!Util.notNullOrEmpty(dbName))
			return;

/*		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new FileReader(dbName));
			StringBuffer sb = new StringBuffer(2048);
			String str;
			while ((str = bf.readLine()) != null) {
				sb.append(str).append('\n');
			}
			bf.close();
			textArea.setText(sb.toString());
		} catch (FileNotFoundException fnf) {
			Log.log(Log.ERROR, Berichtsheft.class, "notepad file " + dbName
					+ " does not exist");
		} catch (IOException ioe) {
			Log.log(Log.ERROR, Berichtsheft.class,
					"could not read notepad file " + dbName);
		}
*/	}
    // }}}
    // }}}
}
// }}}
