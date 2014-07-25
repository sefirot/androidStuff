package com.applang.components;

import com.applang.UserContext;
import com.applang.berichtsheft.plugin.BerichtsheftPlugin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.StringReader;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import android.util.Log;

import static com.applang.Util.*;
import static com.applang.SwingUtil.*;
import static com.applang.VelocityUtil.*;
import static com.applang.PluginUtils.*;

public class ASTViewer extends ActionPanel
{
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final TextToggle textToggle = new TextToggle();
				textToggle.createBufferedTextArea("velocity", "/modes/velocity_pure.xml");
				String title = "Velocity AST tool";
				ASTViewer viewer = new ASTViewer(textToggle, 
						null,
						title);
				ActionPanel.createAndShowGUI(title, 
						new Dimension(600, 300), 
						Behavior.EXIT_ON_CLOSE, 
						viewer, 
						new Function<Component>() {
							public Component apply(Object...params) {
								return textToggle.getUIComponent();
							}
						});
			}
		});
	}

	enum ActionType implements CustomActionType
	{
		DELETE		(0, "manager.action-DELETE"), 
		INSERT		(1, "manager.action-INSERT"), 
		MODIFY		(2, "manager.action-MODIFY"), 
		SET			(3, "set"), 
		FOREACH		(4, "foreach"), 
		TOGGLE		(6, "manager.action-TOGGLE2"), 
		STRUCT		(7, "manager.action-STRUCT"), 
		VMFILE		(8, "manager.action-VMFILE"), 
		ACTIONS		(9, "Actions"); 		//	needs to stay last !
		
		private final int index;   
	    private final String resourceName;
	    
	    ActionType(int index, String resourceName) {
	    	this.index = index;
	        this.resourceName = resourceName;
	    }

		@Override
	    public String resourceName()   { return resourceName; }
		@Override
	    public int index() { return index; }
		@Override
		public String iconName() {
			return getProperty(resourceName + ".icon");
		}
		@Override
		public String description() {
			return getProperty(resourceName.concat(".label"));
		}
		@Override
		public String name(int state) {
			return getProperty(resourceName.concat(".label") + "." + state);
		}
	}

	public File vmFile;
    
    class ManipAction extends CustomAction
    {
		public ManipAction(ActionType type) {
			super(type);
			if (type.equals(ActionType.TOGGLE)) {
				putValue(NAME, type.name(1));
			}
        }
        
		public ManipAction(String text) {
			super(text);
        }
        
        @Override
        protected void action_Performed(ActionEvent ae) {
        	ActionType type = (ActionType)getType();
        	if (type == null)
        		type = ActionType.INSERT;
        	switch (type) {
			case VMFILE:
				vmFile = getVm();
				if (fileExists(vmFile))
					setText(contentsFromFile(vmFile));
				else
					setText("");
				break;
			case TOGGLE:
				toggle(this, getTextToggle().getTextEdit().toggler);
				break;
			case STRUCT:
				if (fileExists(vmFile))
					structure(0);
				break;
			default:
				map = map(popupEvent);
				if (map != null) {
					Object node = map.get(NODE);
					if (type.index == MODIFY) {
						setSelection(node, false);
					}
					else if (type.index == DELETE) {
						setSelection(node, false);
						getTextToggle().setSelectedText(null);
					}
					else if (type.index == INSERT) {
						setSelection(node, true);
					}
					if (Visitor.nodeGroup(node) == DIRECTIVE) {
						switch (type) {
						case SET:
							break;
						case FOREACH:
							break;
						default:
							break;
						}
					}
				}
				break;
			}
        }
    }
    
	boolean isSciptView() {
		return getTextToggle().getTextArea2() == null;
	}
    
    private void setSelection(Object node, boolean noSpan) {
    	if (isSciptView()) {
			int[] span = Visitor.span(node);
			span = getTextOffsets(getTextToggle().getText(), span);
			getTextToggle().setSelection(span[0], noSpan ? -1 : span[1]);
		}
    }
	
	public void structure(int mode) {
		title = vmFile.getName();
		script = getText();
		showAST();
	}
    
	class ASTModel extends DefaultTableModel
	{
		public Vector<Vector<String>> data = new Vector<Vector<String>>();
		public Vector<String> columns = new Vector<String>();
		
		Job<Object[]> checkout = new Job<Object[]>() {
			public void perform(Object[] objects, Object[] params) throws Exception {
				int indents = (Integer) objects[0];
				Node node = (Node) objects[1];
				int blocks = Visitor.blockDepth(node);
    			Token t = (Token) objects[2];
				Vector<String> row = new Vector<String>();
				if (detail) {
					row.addElement(indents + "_" + blocks);
					row.addElement("-- lost+found --");
					row.addElement(Visitor.formatLC(Visitor.getBeginToken(t)));
					row.addElement(Visitor.formatLC(Visitor.getEndToken(t)));
					row.addElement(t.image);
				}
				else {
					row.addElement(indentedLine(t.image, 
							indentor, 
							indents,
							blocks));
					row.addElement("");
				}
				data.addElement(row);
				maps.addElement(nodeMap(node, indents, t));
			}
		};
		
		boolean detail = true;
		public boolean isDetail() {
			return detail;
		}

		boolean essentials = true;
		public boolean isEssentials() {
			return essentials;
		}
		
		public ASTModel(String text, Object...params) {
			detail = param_Boolean(detail, 0, params);
			essentials = param_Boolean(essentials, 1, params);
			if (detail) {
				columns.addElement("level");
				columns.addElement("class");
				columns.addElement("begin");
				columns.addElement("end");
				columns.addElement("tokens");
			}
			else {
				columns.addElement("info");
				columns.addElement("category");
			}
	        try {
				document = parse( new StringReader(text), "");
				
				Visitor.walk(document, new Function<Object>() {
					public Object apply(Object...params) {
						Node node = param(null, 0, params);
						Visitor.visitLostAndFound(checkout, 
								null, 
								Visitor.beginLC(node));
						int indents = param_Integer(0, 1, params);
						if (!Visitor.isProcessNode(node)) {
							if (!essentials || Visitor.isEssential.apply(node)) {
								Vector<String> row = new Vector<String>();
								int blocks = Visitor.blockDepth(node);
								if (detail) {
									row.addElement(indents + "_" + blocks);
									row.addElement(node.getClass().getSimpleName());
									row.addElement(Visitor.formatLC(Visitor.beginLC(node)));
									row.addElement(Visitor.formatLC(Visitor.endLC(node)));
									row.addElement(Visitor.tokens(node));
								}
								else {
									String line = indentedLine(
											Visitor.nodeInfo(node), 
											indentor, 
											indents, 
											blocks);
									row.addElement(line);
									row.addElement(Visitor.nodeCategory(node));
								}
								data.addElement(row);
								maps.addElement(nodeMap(node, indents, null));
							}
						}
						return null;
					}
				});
				Visitor.visitLostAndFound(checkout, 
						null, 
						Integer.MAX_VALUE, 0);
			} catch (Exception e) {
				Log.e(TAG, "ASTModel", e);
			}
	        setDataVector(data, columns);	
		}
		
		public SimpleNode document = null;
		public Vector<ValMap> maps = new Vector<ValMap>();
		
		@SuppressWarnings("rawtypes")
		public Vector getColumnIdentifiers() {
			return this.columnIdentifiers;
		}
		
		public void setColumnWidths(JTable table) {
			int cols = getColumnIdentifiers().size();
			setColumnWidthsAsPercentages(table, cols > 2 ? 
					new double[]{0.10, 0.20, 0.10, 0.10, 0.50} : 
					new double[]{0.80, 0.20});
		}
	}
	
	private JTable configureTable(final ASTModel model) {
		final JTable table = new JTable(model);
		model.setColumnWidths(table);
		table.setPreferredScrollableViewportSize(size);
		table.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				size = table.getSize();
			}
		});
		JPopupMenu popupMenu = newPopupMenu(
			menuRecord("+", INSERT, ActionType.INSERT, newMenu(ActionType.INSERT.description())),
			menuRecord("delete", DELETE, ActionType.DELETE, new ManipAction(ActionType.DELETE)),
			menuRecord("modify", MODIFY, ActionType.MODIFY, new ManipAction(ActionType.MODIFY)) 
		);
		table.addMouseListener(new PopupAdapter(popupMenu));
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					map = map(e);
					if (map != null) {
						Object node = map.get(NODE);
						setSelection(node, false);
					}
				}
			}
		});
		model.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				int column = e.getColumn();
				switch (e.getType()) {
				case TableModelEvent.UPDATE:
			        if ("tokens".equals(model.columns.get(column))) {
			        	int row = e.getFirstRow();
			        	ValMap map = model.maps.get(row);
			        	Node node = (Node) map.get(NODE);
			        	Object value = model.getValueAt(row, column);
						Visitor.update(node, value);
			        }
					break;
				}
			}
	    });
		return table;
	}
	
	private int showText(Object...params) {
		JTextArea textArea = new JTextArea();
		textArea.setEditable(true);
		textArea.setPreferredSize(size);
		String title = String.format("Evaluation : '%s'", this.title);
		UserContext.setupVelocity(null, true);
		textArea.setText(UserContext.toPlainText(script));
		JOptionPane optionPane = new JOptionPane(new JScrollPane(textArea), 
				JOptionPane.PLAIN_MESSAGE, 
				JOptionPane.DEFAULT_OPTION, 
				null, 
				null, null);
		JDialog dialog = optionPane.createDialog(ASTViewer.this, title);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.setVisible(true);
		Object value = optionPane.getValue();
		return value instanceof Integer ? (Integer) value : JOptionPane.CLOSED_OPTION;
	}
	
	private Dimension size = new Dimension(700, 200);
	private String title = null;
	private String script = null;
	private ValMap map = null;
	private int manip = -1;

	private int showAST(final Object...params) {
		manip = param_Integer(0, 0, params);
		final boolean detail = param_Boolean(true, 1, params);
		final boolean essentials = param_Boolean(true, 2, params);
		map = param(null, 3, params);
		if (manip > 0) {
			int option = showText(params);
			return option < 0 ? option : 2;
		}
		else {
			JButton eval = new JButton("Evaluation");
			Object[] options = new Object[] {
				detail ? "Info" : "Detail",
				essentials ? "all" : "essentials",
				"Update", 
				eval, 
			};
			eval.setMnemonic(KeyEvent.VK_V);
			eval.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showAST(1, detail, essentials, map);
				}
			});
			ASTModel model = new ASTModel(script, detail, essentials);
			final JTable table = configureTable(model);
			String title = String.format("AST (%d rows) : '%s' ", model.getDataVector().size(), this.title);
			int option = showOptionDialog(ASTViewer.this, 
					scrollableViewport(table), 
					title, 
					Behavior.MODAL + JOptionPane.DEFAULT_OPTION, 
					JOptionPane.PLAIN_MESSAGE, 
					null, 
					options, null, 
					new Function<Boolean>() {
						public Boolean apply(Object... parms) {
							PropertyChangeEvent e = (PropertyChangeEvent) parms[0];
							JOptionPane optionPane = (JOptionPane) e.getSource();
							Object[] options = (Object[]) parms[1];
							ASTModel model = (ASTModel) table.getModel();
							boolean flag;
							switch (dialogResult) {
							case 0:
								flag = !model.isDetail();
								table.setModel(new ASTModel(script, 
										flag, 
										model.isEssentials()));
								options[0] = flag ? "Info" : "Detail";
								break;
							case 1:
								flag = !model.isEssentials();
								table.setModel(new ASTModel(script, 
										model.isDetail(), 
										flag));
								options[1] = flag ? "all" : "essentials";
								break;
							case 2:
								script = Visitor.tail(model.document);
								table.setModel(new ASTModel(script, 
										model.isDetail(), 
										model.isEssentials()));
								break;
							default:
								return false;
							}
							optionPane.setOptions(options);
							optionPane.updateUI();
							return true;
						}
					}
			);
			return option;
		}
	}
	
	ValMap map(MouseEvent me) {
		ValMap map = null;
    	if (me != null) {
			JTable table = (JTable) me.getComponent();
			if (table != null) {
				Point pt = new Point(me.getX(), me.getY());
				int row = table.rowAtPoint(pt);
				table.setRowSelectionInterval(row, row);
				ASTModel model = (ASTModel) table.getModel();
				map = model.maps.get(row);
			}
		}
		return map;
	}
	
	Object[] menuRecord(final String name, final int actionIdent, ActionType type, Object action) {
		if (action instanceof JMenu) {
			JMenu menu = (JMenu) action;
			for (String key : UserContext.directives().keySet()) {
				menu.add(new JMenuItem()).setAction(new ManipAction(key));
			}
			menu.setIcon(iconFrom(type.iconName()));
		}
		return objects(type.description(), action, 
			name, "", null, null, null, null, 
	        new PopupMenuAdapter() {
	        	@Override
	    		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	    			JPopupMenu popup = (JPopupMenu)e.getSource();
	    			printContainer("popupMenuWillBecomeVisible", popup, _null());
	    			ValMap map = map(popupEvent);
					if (map != null) {
						boolean enabled = isPossible(actionIdent, map);
						String s = name;
						if ("+-".indexOf(s) > -1)
							s = "\\" + s;
						JMenuItem menuItem = findFirstComponent(popup, s);
						menuItem.setEnabled(enabled);
						if (menuItem instanceof JMenu) {
							JMenu menu = (JMenu) menuItem;
							final Map<String, String> anweisungen = UserContext.directives(map);
							for (int i = 0; i < menu.getItemCount(); i++) {
								JMenuItem item = menu.getItem(i);
								String text = item.getText();
								if (!anweisungen.containsKey(text))
									item.setEnabled(false);
							}
						}
					}
	    		}
	        }
		);
	}
	
	public static File getVm() {
		final File store = new File("/home/lotharla/work/velocity/.vm");
		File vm = getFileFromStore(1, 
			"Velocity template", 
			new FileNameExtensionFilter("vm files", "vm"),
			null, 
			new Function<String[]>() {
				public String[] apply(Object... params) {
					return contentsFromFile(store).split(NEWLINE_REGEX);
				}
			}, 
			new Job<String[]>() {
				public void perform(String[] fileNames, Object[] params) throws Exception {
					contentsToFile(store, join(NEWLINE, fileNames));
				}
			});
		return vm;
	}

	public ASTViewer(ITextComponent textArea, Object...params) {
		super(textArea, params);
		
		addButton(this, ActionType.VMFILE.index(), new ManipAction(ActionType.VMFILE));
		addButton(this, ActionType.STRUCT.index(), new ManipAction(ActionType.STRUCT));
		addToggle(this, ActionType.TOGGLE.index(), new ManipAction(ActionType.TOGGLE));
		
		// TODO Auto-generated constructor stub
	}

}
