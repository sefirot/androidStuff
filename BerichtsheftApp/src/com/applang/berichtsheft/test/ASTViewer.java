package com.applang.berichtsheft.test;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.VelocityUtil.*;

import com.applang.UserContext;
import com.applang.Util.Function;
import com.applang.berichtsheft.ui.components.ActionPanel;
import com.applang.berichtsheft.ui.components.TextArea;
import com.applang.berichtsheft.ui.components.TextComponent;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.StringReader;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import android.util.Log;

public class ASTViewer extends ActionPanel
{
    private static final String TAG = ASTViewer.class.getSimpleName();
    
	public static void main(String[] args) {
		TextArea textArea = new TextArea();
		
        String title = "Velocity AST tool";
		final ASTViewer viewer = new ASTViewer(textArea, 
				null,
				title);
		
		ActionPanel.createAndShowGUI(title, new Dimension(700, 200), viewer, textArea.textArea);
	}

	enum ActionType implements IActionType
	{
		DELETE		(0, "minus_16x16.png", "delete"), 
		INSERT		(1, "plus_16x16.png", "insert"), 
		MODIFY		(2, "bausteine_16x16.png", "modify"), 
		SET			(3, "", "set"), 
		FOREACH		(4, "", "foreach"), 
		STRUCT		(7, "structure_16x16.png", "show structure"), 
		VMFILE		(8, "book_open_16x16.png", "choose .vm-file"), 
		ACTIONS		(9, "", "Actions"); 		//	needs to stay last !
		
	    private final String iconName;
	    private final String toolTip;
	    private final int index;   
	    
	    ActionType(int index, String iconName, String toolTip) {
	    	this.index = index;
	        this.iconName = iconName;
	        this.toolTip = toolTip;
	    }

	    public int index() { return index; }
	    public String iconName()   { return iconName; }
	    public String description() { return toolTip; }
	}

	public File vmFile;
    
    class ManipAction extends Action
    {
    	Object[] params = null;
    	
		public ManipAction(ActionType type, Object...params) {
			super(type);
			this.params = params;
        }
        
		public ManipAction(String text) {
			super(text);
        }
        
        @Override
        protected void action_Performed(ActionEvent ae) {
        	ActionType type = (ActionType)getType();
        	switch (type) {
			case VMFILE:
				vmFile = getVm();
				if (fileExists(vmFile))
					setText(contentsFromFile(vmFile));
				break;
			case STRUCT:
				if (fileExists(vmFile))
					structure(0);
				break;
			default:
				map = map();
				if (map != null) {
					params[3] = map;
					Object node = map.get(NODE);
					if (Visitor.nodeGroup(node) == DIRECTIVE) {
						switch (type) {
						case FOREACH:
						case INSERT:
							params[0] = 2;
							showAST(params);
							break;
						case DELETE:
							params[0] = 3;
							showAST(params);
							break;
						case MODIFY:
							params[0] = 4;
							showAST(params);
							break;
						default:
							break;
						}
					}
					else if (type.index == MODIFY) {
						params[0] = 4;
						showAST(params);
					}
				}
				break;
			}
        }
    }
    
	class ASTModel extends DefaultTableModel
	{
		public Vector<Vector<String>> data = new Vector<Vector<String>>();
		public Vector<String> columns = new Vector<String>();
		
		Job<Object[]> checkout = new Job<Object[]>() {
			public void perform(Object[] objects, Object[] params) throws Exception {
				int indents = (int) objects[0];
				Node node = (Node) objects[1];
				int blocks = Visitor.blockDepth(node);
    			Token t = (Token) objects[2];
				Vector<String> row = new Vector<String>();
				if (detail) {
					row.addElement(indents + "_" + blocks);
					row.addElement("-- lost+found --");
					row.addElement(Visitor.formatLC(Visitor.getBeginToken(t)));
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
		boolean essentials = true;
		
		public ASTModel(String text, Object...params) {
			detail = paramBoolean(detail, 0, params);
			essentials = paramBoolean(essentials, 1, params);
			if (detail) {
				columns.addElement("level");
				columns.addElement("class");
				columns.addElement("position");
				columns.addElement("tokens");
			}
			else {
				columns.addElement("info");
				columns.addElement("category");
			}
	        try {
				document = RuntimeSingleton.parse( new StringReader(text), "");
				
				Visitor.walk(document, new Function<Object>() {
					public Object apply(Object...params) {
						Node node = param(null, 0, params);
						Visitor.visitLostAndFound(checkout, 
								null, 
								Visitor.beginLC(node));
						int indents = paramInteger(0, 1, params);
						if (!Visitor.isProcessNode(node)) {
							if (!essentials || Visitor.isEssential.apply(node)) {
								Vector<String> row = new Vector<String>();
								int blocks = Visitor.blockDepth(node);
								if (detail) {
									row.addElement(indents + "_" + blocks);
									row.addElement(node.getClass().getSimpleName());
									row.addElement(Visitor.formatLC(Visitor.beginLC(node)));
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
			setWidthAsPercentages(table, getColumnIdentifiers().size() > 2 ? 
					new double[]{0.10, 0.30, 0.10, 0.50} : 
					new double[]{0.80, 0.20});
		}
	}
	
	Dimension size = new Dimension(700, 200);
	String title = null;
	String text = null;
	ValMap map = null;
	int manip = -1;

	private int showAST(final Object[] params) {
		manip = (int)params[0];
		boolean detail = (boolean)params[1];
		boolean essentials = (boolean)params[2];
		map = params[3] instanceof ValMap ? (ValMap) params[3] : null;
		if (manip > 0) {
			final JTextArea textArea = new JTextArea();
    		textArea.setEditable(true);
    		textArea.setPreferredSize(size);
    		String title;
    		int optionType = JOptionPane.DEFAULT_OPTION;
    		switch (manip) {
			case 1:
				title = String.format("Evaluation : '%s'", this.title);
	    		UserContext.setupVelocity(null, true);
	    		textArea.setText(evaluate(new UserContext(), text, TAG));
				break;
			default:
				title = String.format("%s : '%s'", 
						manip == 2 ? "Insert" :
							(manip == 3 ? "Delete" : "Modify"), 
						this.title);
				optionType = JOptionPane.OK_CANCEL_OPTION;
				textArea.setText(text);
				textArea.addAncestorListener(new AncestorListener() {
					public void ancestorRemoved(AncestorEvent event) {
					}
					public void ancestorMoved(AncestorEvent event) {
					}
					public void ancestorAdded(AncestorEvent event) {
						if (map != null) {
							Object node = map.get(NODE);
							int[] span = Visitor.span(node);
							span = getTextOffsets(text, span);
							textArea.setSelectionStart(span[0]);
							if (manip == 2)
								textArea.setSelectionEnd(span[0]);
							else
								textArea.setSelectionEnd(span[1] + 1);
							println(textArea.getSelectedText());
						}
						textArea.requestFocusInWindow();						
					}
				});
				break;
			}
			JPanel container = new JPanel();
			container.add(new JScrollPane(textArea));
			int option = JOptionPane.showOptionDialog(ASTViewer.this, 
					container, 
					title, 
					optionType,
					JOptionPane.PLAIN_MESSAGE, 
					null, 
					null, null);
			if (map != null) {
				if (option == JOptionPane.OK_OPTION)
					text = textArea.getText();
				params[3] = null;
			}
			params[0] = 0;
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
					params[0] = 1;
					showAST(params);
				}
			});
			final ASTModel model = new ASTModel(text, detail, essentials);
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
				menuRecord("delete", DELETE, ActionType.DELETE, new ManipAction(ActionType.DELETE, params)),
				menuRecord("modify", MODIFY, ActionType.MODIFY, new ManipAction(ActionType.MODIFY, params)) 
			);
			table.addMouseListener(new PopupAdapter(popupMenu));
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
			String title = String.format("AST (%d rows) : '%s' ", model.getDataVector().size(), this.title);
			int option = showResizableDialog(table, new Function<Integer>() {
				public Integer apply(Object...parms) {
					JPanel container = new JPanel();
					container.add((Component)parms[0]);
					return showOptionDialog(ASTViewer.this, 
							container, 
							(String)parms[1], 
							4 + JOptionPane.DEFAULT_OPTION, 
							JOptionPane.PLAIN_MESSAGE, 
							null, 
							(Object[])parms[2], parms[3], 
							null, 
							new Function<Boolean>() {
								public Boolean apply(Object... params) {
									switch (dialogResult) {
									case 0:
										params[1] = !(boolean) params[1];
										break;
									case 1:
										params[2] = !(boolean) params[2];
										break;
									default:
										break;
									}
									return false;
								}
							}
					);
				}
			}, title, options, null);
			if (manip < 1)
				params[3] = Visitor.tail(model.document);
			return option;
		}
	}
	
	ValMap map() {
		ValMap map = null;
    	if (popupEvent != null) {
			JTable table = (JTable) popupEvent.getComponent();
			if (table != null) {
				Point pt = new Point(popupEvent.getX(), popupEvent.getY());
				int row = table.rowAtPoint(pt);
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
		}
		return new Object[] {type.description(), action, 
			name, "", null, null, null, null, 
	        new PopupMenuAdapter() {
	        	@Override
	    		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	    			JPopupMenu popup = (JPopupMenu)e.getSource();
	    			printContainer("popupMenuWillBecomeVisible", popup, false);
	    			ValMap map = map();
					if (map != null) {
						boolean enabled = isPossible(actionIdent, map);
						String s = name;
						if ("+-".indexOf(s) > -1)
							s = "\\" + s;
						JMenuItem menuItem = findComponent(popup, s);
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
		};
	}
	
	File getVm() {
		File vm = getFileFromStore("Velocity template", 
				"/home/lotharla/work/velocity/.vm", 
				new FileNameExtensionFilter("vm files", "vm"));
		return vm;
	}
	
	public void structure(int mode) {
		title = vmFile.getName();
		Object[] params = new Object[]{0,true,true,null};
		do {
			text = getText();
			int option = showAST(params);
			setText(text);
			
			switch (option) {
			case 0:
				params[1] = !(boolean) params[1];
				break;
			case 1:
				params[2] = !(boolean) params[2];
				break;
			case 2:
				break;
			case 3:
				break;
			default:
				return;
			}
		} while (true);
	}

	public ASTViewer(TextComponent textArea, Object...params) {
		super(textArea, params);
		
		addButton(ActionType.VMFILE.index(), new ManipAction(ActionType.VMFILE));
		addButton(ActionType.STRUCT.index(), new ManipAction(ActionType.STRUCT));
		
		// TODO Auto-generated constructor stub
	}

}
