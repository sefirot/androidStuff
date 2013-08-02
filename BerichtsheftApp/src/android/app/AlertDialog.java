package android.app;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
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
import android.widget.TextView;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;

public class AlertDialog extends Dialog implements DialogInterface
{
	protected static final String TAG = AlertDialog.class.getSimpleName();
	
	public static boolean modal = true;
	
	public AlertDialog(Context context, Object...params) {
		super(null, paramBoolean(modal, 0, params));
		setAlwaysOnTop(paramBoolean(false, 1, params));
		viewGroup = new ViewGroup(context);
	}
	
	public void cancel() {
		dismiss();		
	}

	ViewGroup viewGroup;

    public View findViewById(int id) {
    	return viewGroup.findViewById(id);
	}
	
	@SuppressWarnings("resource")
	public Writer feed(int id) throws IOException {
		final TextView tv = (TextView) findViewById(id);
		if (tv != null) {
			PipedWriter out = new PipedWriter();
			final PipedReader reader = new PipedReader(out);
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
			return out;
		}
		return null;
	}
	
	public Object result = null;

	public static class Builder
	{
		AlertDialog dialog;
		Resources res;
		JOptionPane optionPane;
		
		public Builder(Context context, Object...params) {
			dialog = new AlertDialog(context, params);
			res = context.getResources();
			optionPane = new JOptionPane();
		}
		
		public AlertDialog dialog() {
			return dialog;
		}
        
		String title = "";
		ValList options = new ValList();
		int optionType = JOptionPane.DEFAULT_OPTION;
		
        public AlertDialog create() {
        	ViewGroup vg = dialog.viewGroup;
        	if (options.size() > 0) {
        		Container container = vg.getLayout();
        		if (vg.getChildCount() > 0) {
        			if (vg.getChildCount() < 2) 
        				container = (Container) vg.getChildAt(0).getComponent();
        			else {
        				for (int i = 0; i < vg.getChildCount(); i++) {
        					container.add(vg.getChildAt(i).getComponent());
						}
        			}
    				optionPane.setMessage(container);
        		}
        		optionPane.setOptions(options.toArray());
        		dialog.setContentPane(optionPane);
        	}
        	else if (vg.getChildCount() > 0) {
        		for (int i = 0; i < vg.getChildCount(); i++)
					dialog.getContentPane().add(vg.getChildAt(i).getComponent());
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
				optionPane.setIcon(icon);
			}
            return this;
        }

		public Builder setTitle(String title) {
			this.title = title;
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
			options.add(btn);
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
        
        public Builder setInitialButton(int btn) {
        	if (btn < 0)
        		btn = -btn - 1;
        	if (btn < options.size())
                optionPane.setInitialValue(options.get(btn));
            return this;
        }
        
        public Builder setView(View view) {
        	if (view instanceof ViewGroup)
	    		iterateViews((ViewGroup)view, 
					new Function<Object[]>() {
						public Object[] apply(Object... params) {
							View v = param(null, 0, params);
			            	addView(v, null);
							return param(null, 2, params);
						}
					}, 
					0
	    		);
        	else
            	addView(view, null);
            return this;
        }

		@SuppressWarnings({ "rawtypes", "unchecked" })
        public Builder setItems(final CharSequence[] items, final OnClickListener listener) {
			final JList list = new JList(new DefaultListModel() { 
				{
					for (CharSequence item : items) 
						addElement(item);
				}
			});
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
					int which = table.getSelectedRow();
					onClickListener.onClick(dialog, which);
				}
			});
			table.setSelectionModel(listSelectionModel);
			if (checkedItem > -1 && checkedItem < table.getModel().getRowCount())
				table.setRowSelectionInterval(checkedItem, checkedItem);
        	addView(new View(new JScrollPane(table)).setId(1), null);
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
					int which = table.getSelectedRow();
					onClickListener.onClick(dialog, which, checkedItems[which]);
				}
			});
			table.setSelectionModel(listSelectionModel);
        	addView(new View(new JScrollPane(table)).setId(1), null);
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
	
	private static AlertDialog dlg = null;
	
	@SuppressWarnings("rawtypes")
	public static String chooser(Context context, String title, String[] values, String...defaults) {
		dialogResult = -1;
		dlg = new Builder(context)
            .setTitle(title)
            .setItems(values, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	if (which < 0)
                		dialogResult = -which - 1;
                    dlg.dismiss();
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					dlg.result = null;
                    dlg.cancel();
				}
            })
            .create();
		if (isAvailable(0, defaults)) {
			View vw = dlg.findViewById(1);
			Container container = vw.getComponent();
			JList list = findComponent(container, "list");
			list.setSelectedValue(defaults[0], true);
		}
		Dimension size = dlg.getSize();
		dlg.setSize(new Dimension(size.width, size.height));
		dlg.open();
		return (String) dlg.result;
	}

}
