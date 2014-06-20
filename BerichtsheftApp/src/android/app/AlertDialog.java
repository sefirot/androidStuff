package android.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.Writer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class AlertDialog extends Dialog implements DialogInterface
{
	public static int behavior = Behavior.MODAL;
	
	public AlertDialog(Frame owner, 
			String title, String caption, 
			Object message, 
			int optionType, int behavior, 
			Icon icon, 
			Job<Void> followUp, Object...params)
	{
		super(owner, behavior);
		setTitle(title);
		this.optionType = optionType;
		this.options = defaultOptions(optionType);
		this.followUp = followUp;
		this.params = params;
		if (icon == null)
			icon = defaultIcon(optionType);
		init(caption, icon, message);
	}
	
	int optionType = JOptionPane.DEFAULT_OPTION;
	ValList options = vlist();
	Job<Void> followUp;
	Object[] params;

	private void init(String caption, Icon icon, Object message) {
		JPanel contentPane = new JPanel(new BorderLayout(12,12));
		contentPane.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(contentPane);
		Box iconBox = new Box(BoxLayout.Y_AXIS);
		iconBox.add(new JLabel(icon));
		iconBox.add(Box.createGlue());
		contentPane.add(BorderLayout.WEST,iconBox);
		JPanel centerPanel = new JPanel(new BorderLayout(6,6));
		centerPanel.add(BorderLayout.NORTH,
			new JLabel(htmlize(caption)));
		if (message instanceof Component) 
			centerPanel.add(BorderLayout.CENTER, (Component)message);
		else {
			JTextArea textArea = new JTextArea(10, 80);
			textArea.setText(stringValueOf(message));
			textArea.setLineWrap(true);
			textArea.setCaretPosition(0);
			centerPanel.add(BorderLayout.CENTER, new JScrollPane(textArea));
		}
		contentPane.add(BorderLayout.CENTER,centerPanel);
		Box box = new Box(BoxLayout.X_AXIS);
		JButton btn = null;
		for (int i = 0; i < options.size() - 1; i++) {
			btn = addButton(i, box);
		}
		box.add(Box.createGlue());
		contentPane.add(BorderLayout.SOUTH,box);
		switch (optionType) {
		case 5:
			result = 4;
			break;
		default:
			result = JOptionPane.CANCEL_OPTION;
			break;
		}
		if (btn != null)
			getRootPane().setDefaultButton(btn);
		pack();
		setLocationRelativeTo(getParent());
	}
	
	private JButton addButton(int index, Box box) {
		box.add(Box.createGlue());
		JButton btn = new JButton(stringValueOf(options.get(index)));
		btn.addActionListener(new ActionHandler());
		box.add(btn);
		return btn;
	}

	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			String cmd = evt.getActionCommand();
			if (stringValueOf(options.get(0)).equals(cmd)) {
				ok();
				return;
			}
			result = options.indexOf(cmd);
			if (optionType == 5 && getResult() == 1) {
				ok();
				result = 1;
				return;
			}
			cancel();
		}
	}
	
	//	called if enter key pressed
	public void ok() {
		if (viewGroup == null) {
			result = JOptionPane.OK_OPTION;
			if (followUp != null)
				try {
					followUp.perform(null, params);
				} catch (Exception e) {
					Log.e(TAG, "followUp", e);
				}
		}
		else {
			JButton btn = null;
			Container contentPane = getContentPane();
			if (contentPane instanceof JOptionPane) {
				JOptionPane optionPane = (JOptionPane) contentPane;
				Object[] options = optionPane.getOptions();
				Component focused = KeyboardFocusManager
						.getCurrentKeyboardFocusManager().getFocusOwner();
				int index = arrayindexof(focused, options);
				if (index > -1)
					btn = (JButton) options[index];
				else {
					Object option = optionPane.getInitialValue();
					if (option instanceof JButton)
						btn = (JButton) option;
				}
			}
			if (btn != null)
				btn.doClick();
		}
		cancel();		
	}
	
	//	called if escape key pressed
	public void cancel() {
		if (viewGroup != null)
			dismiss();		
		else
			dispose();
	}

	public AlertDialog(Context context, int behavior) {
		super(null, behavior);
		viewGroup = new ViewGroup(context);
		setEnterEnabled(false);
	}
	
	ViewGroup viewGroup = null;

    public View findViewById(int id) {
    	return viewGroup.findViewById(id);
	}
    
    @SuppressWarnings("unchecked")
	public <T extends JComponent> T findComponentById(int id, Object...names) {
    	View vw = findViewById(id);
    	if (vw == null)
    		return null;
    	Container comp = (Container) vw.getComponent();
    	ValList list = vlist(names);
    	while (comp != null && list.size() > 0) {
    		comp = findFirstComponent(comp, stringValueOf(list.get(0)));
    		list.remove(0);
    	}
    	return (T)comp;
    }
	
	@SuppressWarnings("resource")
	public Writer feed(int id) {
		final TextView tv = (TextView) findViewById(id);
		try {
			if (tv != null) {
				PipedWriter writer = new PipedWriter();
				final PipedReader reader = new PipedReader(writer);
				Runnable r = new Runnable() {
					public void run() {
				        try {
				            BufferedReader br = new BufferedReader(reader);
				            String line = null;
				            while((line = br.readLine()) != null)
				            {
				                synchronized (tv) {
				                	tv.append(line + NEWLINE);
								}
				            }
				            br.close();
				        }
				        catch (Exception e) {
				            Log.e(TAG, "feed", e);
				        }
					}
				};
				new Thread(r).start();
				return writer;
			}
		} catch (Exception e) {
			Log.e(TAG, "feed", e);
		}
		return null;
	}

	public static class Builder
	{
		AlertDialog dialog;
		Resources res;
		JOptionPane optionPane;
		
		public Builder(Context context, Object...params) {
			dialog = new AlertDialog(context, param_Integer(behavior, 0, params));
			res = context.getResources();
			optionPane = new JOptionPane();
			int optionType = param_Integer(JOptionPane.DEFAULT_OPTION - 1, 1, params);
			if (optionType >= JOptionPane.DEFAULT_OPTION) {
				ValList list = defaultOptions(optionType);
				for (int i = 0; i < list.size() - 1; i++) {
					String option = stringValueOf(list.get(i));
					addOption(-1-i, option, new OnClickListener() {
						public void onClick(DialogInterface dlg, int which) {
							dialog.result = which;
							dlg.dismiss();
						}
					});
				}
				int which = list.indexOf(list.get(-1));
				while (which > -1 && nullOrEmpty(list.get(which)))
					which--;
				setInitialOption(which);
				dialog.result = which;
			}
		}
		
		public AlertDialog dialog() {
			return dialog;
		}
        
		String title = "";
		
        public AlertDialog create() {
        	ViewGroup vg = dialog.viewGroup;
        	Container container = ViewGroup.build(vg, true);
        	if (dialog.options.size() > 0) {
        		if (vg.getChildCount() > 0) {
        			if (vg.getChildCount() < 2) 
        				container = (Container) vg.getChildAt(0).getComponent();
        			optionPane.setMessage(container);
        		}
        		optionPane.setOptions(dialog.options.toArray());
        		dialog.setContentPane(optionPane);
        	}
        	else {
        		for (int i = 0; i < container.getComponentCount(); i++) {
					Component component = container.getComponent(i);
					dialog.getContentPane().add(component);
				}
        	}
			dialog.setTitle(title);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            return dialog;
        }

        public void addView(View view, LayoutParams params) {
        	dialog.viewGroup.addView(view, params);
    	}

		public Builder setCancelable(boolean b) {
			return this;
		}
        
        public Builder setOnCancelListener(final OnCancelListener onCancelListener) {
        	dialog.addWindowListener(new WindowAdapter() {
    			public void windowClosing(WindowEvent event) {
    				onCancelListener.onCancel(dialog);
    			}
    		});
            return this;
        }

        public Builder setIcon(int iconId) {
            if (iconId > 0) {
				ImageIcon icon = iconFrom("/images/spinner.gif");
				return setIcon(icon);
			}
            return this;
        }

        public Builder setIcon(Icon icon) {
            optionPane.setIcon(icon);
            return this;
        }

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}
        
        public Builder setInitialOption(int option) {
        	if (option < 0)
        		option = -option - 1;
        	if (option < dialog.options.size())
                optionPane.setInitialValue(dialog.options.get(option));
            return this;
        }
        
        public Builder setMessage(CharSequence message) {
        	addView(new View(new JLabel(message.toString())), null);
            return this;
        }
		
		private void addOption(final int which, String string, final OnClickListener onClickListener) {
			final JButton btn = new JButton(string);
			btn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dialog.result = btn.getText();
					if (onClickListener != null)
						onClickListener.onClick(dialog, which < 0 ? -which - 1 : which);
				}
			});
			dialog.options.add(btn);
			if (which == DialogInterface.BUTTON_NEGATIVE) {
				setInitialOption(dialog.options.indexOf(btn));
				dialog.setEnterEnabled(true);
			}
		}

		public Builder setPositiveButton(String string, OnClickListener onClickListener) {
			addOption(DialogInterface.BUTTON_POSITIVE, string, onClickListener);
			return this;
		}

		public Builder setPositiveButton(int id, OnClickListener onClickListener) {
			String string = res.getString(id);
			addOption(DialogInterface.BUTTON_POSITIVE, string, onClickListener);
			return this;
		}

		public Builder setNegativeButton(String string, OnClickListener onClickListener) {
			addOption(DialogInterface.BUTTON_NEGATIVE, string, onClickListener);
			return this;
		}

		public Builder setNegativeButton(int id, OnClickListener onClickListener) {
			String string = res.getString(id);
			addOption(DialogInterface.BUTTON_NEGATIVE, string, onClickListener);
			return this;
		}

		public Builder setNeutralButton(String string, OnClickListener onClickListener) {
			addOption(DialogInterface.BUTTON_NEUTRAL, string, onClickListener);
			return this;
		}

		public Builder setNeutralButton(int id, OnClickListener onClickListener) {
			String string = res.getString(id);
			addOption(DialogInterface.BUTTON_NEUTRAL, string, onClickListener);
			return this;
		}
        
        public Builder setView(JComponent component) {
        	return setView(new View(component));
        }
        
        public Builder setView(View view) {
        	view.setId(1);
        	if (view instanceof ViewGroup)
	    		iterateViews((ViewGroup)view, 0, 
					new Function<Object[]>() {
						public Object[] apply(Object... params) {
							View v = param(null, 0, params);
			            	addView(v, null);
							return param(null, 2, params);
						}
					}
	    		);
        	else
            	addView(view, null);
            return this;
        }

		@SuppressWarnings({ "rawtypes", "unchecked" })
        public Builder setItems(final CharSequence[] items, final OnClickListener listener) {
			final JList list = new JList(defaultListModel(asList(items)));
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) {
					dialog.result = list.getSelectedValue();
					final int which = list.getSelectedIndex();
					list.setSelectedIndex(which);
					switch (ev.getClickCount()) {
					case 1:
						new Task<Void>(null, new Job<Void>() {
							public void perform(Void t,	Object[] params) throws Exception {
								listener.onClick(dialog, which);
							}
						}, 500).execute();
						break;
					case 2:
						listener.onClick(dialog, -which - 1);
						break;
					}
				}
			});
			list.setName("list");
        	addView(new View(new JScrollPane(list)).setId(1), null);
            return this;
        }

		public Builder setSingleChoiceItems(final CharSequence[] items, int checkedItem, final OnClickListener onClickListener) {
			final JTable table = new JTable(new AbstractTableModel() {
				@Override
				public int getRowCount() {
					return items.length;
				}
				@Override
				public int getColumnCount() {
					return 1;
				}
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					return items[rowIndex];
				}
			});
			table.setTableHeader(null);
			ListSelectionModel listSelectionModel = table.getSelectionModel();
			listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listSelectionModel.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (e.getValueIsAdjusting()) return;
					int which = table.getSelectedRow();
					onClickListener.onClick(dialog, which);
				}
			});
			table.setSelectionModel(listSelectionModel);
			if (checkedItem > -1 && checkedItem < table.getModel().getRowCount())
				table.setRowSelectionInterval(checkedItem, checkedItem);
        	addView(new View(scrollableViewport(table)).setId(1), null);
			return this;
		}
        
        public Builder setMultiChoiceItems(final CharSequence[] items, final boolean[] checkedItems, final OnMultiChoiceClickListener onClickListener) {
        	final JTable table = new JTable(new AbstractTableModel() {
				@Override
				public int getRowCount() {
					return items.length;
				}
				@Override
				public int getColumnCount() {
					return 2;
				}
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					return columnIndex < 1 ? 
							items[rowIndex] : 
							checkedItems[rowIndex];
				}
				@Override
		        public Class<?> getColumnClass(int c) {
		            return getValueAt(0, c).getClass();
		        }
				@Override
				public boolean isCellEditable(int row, int col) {
					return col == 1;
				}
				@Override
				public void setValueAt(Object value, int row, int col) {
					if (col == 1)
						checkedItems[row] = (Boolean) value;
				}
			});
			table.setTableHeader(null);
			ListSelectionModel listSelectionModel = table.getSelectionModel();
			listSelectionModel.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (e.getValueIsAdjusting()) return;
					int which = table.getSelectedRow();
					onClickListener.onClick(dialog, which, checkedItems[which]);
				}
			});
			table.setSelectionModel(listSelectionModel);
        	addView(new View(scrollableViewport(table)).setId(1), null);
            return this;
        }
        
        public Builder setSingleChoiceItems(Cursor cursor, int checkedItem, String labelColumn, final OnClickListener listener) {
    		// TODO Auto-generated method stub
            return this;
        }
        
        public Builder setMultiChoiceItems(Cursor cursor, String isCheckedColumn, String labelColumn, final OnMultiChoiceClickListener listener) {
    		// TODO Auto-generated method stub
            return this;
        }
        
	}
	
	public static void alerter(Context context, String title, Exception ex) {
		EditText et = new EditText(context, 
				Resources.attributeSet(context, "android:inputType=\"textMultiLine\"", "readOnly=\"true\""));
		int id = 1;
		et.setId(id);
		et.getComponent().setPreferredSize(new Dimension(800,400));
		AlertDialog dlg = new AlertDialog.Builder(context)
	    		.setTitle(title)
				.setIcon(UIManager.getIcon("OptionPane.errorIcon"))
				.setView(et)
	            .setNeutralButton(stringValueOf(defaultOptions(3).get(0)), new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	dialog.dismiss();
	                }
	            })
				.create();
		try {
			Writer out = dlg.feed(id);
			ex.printStackTrace(new PrintWriter(out));
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "alerter", e);
		}
		dlg.open();
	}
	
	@SuppressWarnings("rawtypes")
	public static String chooser(Context context, String title, String[] values, String...defaults) {
		final AlertDialog dlg = new Builder(context)
            .setTitle(title)
            .setItems(values, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	dialog.dismiss();
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					dialog.dismiss();
				}
            })
            .create();
		if (isAvailable(0, defaults)) {
			JList list = dlg.findComponentById(1, "list");
			list.setSelectedValue(defaults[0], true);
		}
		dlg.open();
		return (String) dlg.result;
	}

}
