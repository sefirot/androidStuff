package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.Option;

import com.applang.Util;

public class FormEditor extends JSplitPane implements TextComponent
{
	public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private static void createAndShowGUI() {
		try {
			JToolBar top = new JToolBar();
			top.setName("top");
			JToolBar bottom = new JToolBar();
			bottom.setName("bottom");
			bottom.setFloatable(false);
			JLabel label = new JLabel("");
			label.setName("mess");
			bottom.add(label);
			
			Util.Settings.load();
			final String xmlPath = Util.Settings.get("content.xml", "scripts/content.xml");
			final FormEditor formEditor = new FormEditor(new File(xmlPath).getCanonicalPath());
			
			String title = "Layout manipulation";
			JFrame frame = new JFrame(title) {
				protected void processWindowEvent(WindowEvent we) {
					if (we.getID() == WindowEvent.WINDOW_CLOSING)
						formEditor.finish(xmlPath);
					
					super.processWindowEvent(we);
				}
			};
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			Container contentPane = frame.getContentPane();
			contentPane.setPreferredSize(new Dimension(600, 300));
			
			contentPane.add(top, BorderLayout.PAGE_START);
			contentPane.add(bottom, BorderLayout.PAGE_END);

			contentPane.add(formEditor, BorderLayout.CENTER);
			
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			
			formEditor.setDivider();
		} catch (Exception e) {
			Util.handleException(e);
		}
	}
	
	public void finish(String xmlPath) {
		try {
			allMappings();
			Util.xmlTransform(xmlPath, stylePath, output, 
					"controlfile", output);
			
			Util.Settings.save();
		} catch (Exception e) {
			Util.handleException(e);
		}
	}
	
	String stylePath = Util.Settings.get("geometry.xsl", "scripts/geometry.xsl");
	String htmlPath = "/tmp/temp.html";
	String output = "/tmp/content.xml";
	
	public FormEditor(String xmlPath) {
		File dir = Util.tempDir(true, "berichtsheft");
		try {
			setupEditor();
			setTopComponent(new JScrollPane(maskPanel));
			setBottomComponent(new JScrollPane(editorPanel));
			addComponentListener(new ComponentAdapter() {

				@Override
				public void componentResized(ComponentEvent e) {
					setDivider();
					super.componentResized(e);
				}
			});
			
			Util.xmlTransform(xmlPath, stylePath, htmlPath, 
					"mode", 1);
			
			pages = dir.listFiles();
			mappings = new Util.ValMap[pages.length];
			updateSplitComponents(page = 0);
		} catch (Exception e) {
			Util.handleException(e);
		}
	}

	private void setDivider() {
		int dividerLocation = this.getWidth() / 2;
		setDividerLocation(dividerLocation);
	}

	Util.ValMap[] mappings = null;
	File[] pages = null;
	int page;
	
	private void updateSplitComponents(int page) {
		try {
			if (pages.length > 0) {
				editorPanel.setPage("file:" + pages[page].getPath());
				postData(page, null);
			}
			else
				Util.message("no mask data available");
		} catch (IOException e) {
			Util.handleException(e);
		}
	}

	MaskPanel maskPanel = new MaskPanel();
	JEditorPane editorPanel = new JEditorPane();

	private void setupEditor() {
		editorPanel.setContentType("text/html");
		editorPanel.setEditable(false);
		
		HTMLEditorKit kit = (HTMLEditorKit)editorPanel.getEditorKit();
		kit.setAutoFormSubmission(false);
		editorPanel.addHyperlinkListener(new HyperlinkListener()
		{                           
		    @Override
		    public void hyperlinkUpdate(HyperlinkEvent he)
		    {
		        if (he instanceof FormSubmitEvent)
		        {
		            FormSubmitEvent fe = (FormSubmitEvent)he;
		            postData(page, fe.getData());
					
					maskPanel.repaint();
		        }
		        else if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		    		URL url = he.getURL();
		    		String name = new File(url.getFile()).getName();
					int page = Util.toInt(0, Util.findFirstIn(name, Pattern.compile("\\w+(\\d+)\\.\\w+")).group(1));
					updateSplitComponents(page - 1);
		        }
		    }
		});
	}
	
	private void allMappings() {
		Util.clearMappings();
		for (int p = 0; p < pages.length; p++) 
			for (String key : mappings[p].keySet())
				Util.mappings.put(key, mappings[p].get(key));
	}
	
	private void postData(int page, String data) {
		try {
			mappings[page] = new Util.ValMap();
			
			if (data == null) 
				pageData(page);
			else {
				String[] parts = data.split("&|=");
				for (int i = 0; i < parts.length - 1; i+=2) {
					String key = URLDecoder.decode(parts[i], "UTF-8");
					String value = URLDecoder.decode(parts[i+1], "UTF-8");
					mappings[page].put(key, value);
				}
			}
			
			this.page = page;
			maskPanel.repaint();
		} catch (Exception e) {
			Util.handleException(e);
		}
	}
	
    private void pageData(int page) {
    	org.w3c.dom.Document doc = Util.xmlDocument(pages[page]);
    	if (doc == null)
    		return;
    	
    	org.w3c.dom.NodeList nodes = Util.evaluateXPath(doc, "//table[@id='controls']");
		if (nodes.getLength() > 0) {
			nodes = Util.evaluateXPath(nodes.item(0), ".//*[@id]");
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Element node = (org.w3c.dom.Element)nodes.item(i);
				String key = node.getAttribute("id");
				String value = node.getAttribute("value");
				mappings[page].put(key, value);
			}
		}
    }
	
    @SuppressWarnings("unused")
	private String getFormData() {
    	StringBuilder buffer = new StringBuilder();
		HTMLDocument doc = (HTMLDocument) editorPanel.getDocument();
		Element table = doc.getElement("controls");
        if (table != null) {
            ElementIterator it = new ElementIterator(table);
            Element next;

            while ((next = it.next()) != null) {
                if (next.isLeaf()) 
                	loadElementDataIntoBuffer(next, buffer);
            }
        }
        return buffer.toString();
    }
    
//	taken from javax.swing.text.html.FormView class
    
    private void loadElementDataIntoBuffer(Element elem, StringBuilder buffer) {
        AttributeSet attr = elem.getAttributes();
        String name = (String)attr.getAttribute(HTML.Attribute.NAME);
        if (name == null) {
            return;
        }
        String value = null;
        HTML.Tag tag = (HTML.Tag)elem.getAttributes().getAttribute
                                  (StyleConstants.NameAttribute);

        if (tag == HTML.Tag.INPUT) {
            value = getInputElementData(attr);
        } else if (tag ==  HTML.Tag.TEXTAREA) {
            value = getTextAreaData(attr);
        } else if (tag == HTML.Tag.SELECT) {
            loadSelectData(attr, buffer);
        }

        if (name != null && value != null) {
            appendBuffer(buffer, name, value);
        }
    }

    @SuppressWarnings("deprecation")
	private void appendBuffer(StringBuilder buffer, String name, String value) {
        if (buffer.length() > 0) {
            buffer.append('&');
        }
        String encodedName = URLEncoder.encode(name);
        buffer.append(encodedName);
        buffer.append('=');
        String encodedValue = URLEncoder.encode(value);
        buffer.append(encodedValue);
    }

    private String getInputElementData(AttributeSet attr) {

        Object model = attr.getAttribute(StyleConstants.ModelAttribute);
        String type = (String) attr.getAttribute(HTML.Attribute.TYPE);
        String value = null;

        if (type.equals("text") || type.equals("password")) {
            Document doc = (Document)model;
            try {
                value = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                value = null;
            }
        } else if (type.equals("submit") || type.equals("hidden")) {
            value = (String) attr.getAttribute(HTML.Attribute.VALUE);
            if (value == null) {
                value = "";
            }
        } else if (type.equals("radio") || type.equals("checkbox")) {
            ButtonModel m = (ButtonModel)model;
            if (m.isSelected()) {
                value = (String) attr.getAttribute(HTML.Attribute.VALUE);
                if (value == null) {
                    value = "on";
                }
            }
        } else if (type.equals("file")) {
            Document doc = (Document)model;
            String path;

            try {
                path = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                path = null;
            }
            if (path != null && path.length() > 0) {
                value = path;
            }
        }
        return value;
    }

    private String getTextAreaData(AttributeSet attr) {
        Document doc = (Document)attr.getAttribute(StyleConstants.ModelAttribute);
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
	private void loadSelectData(AttributeSet attr, StringBuilder buffer) {

        String name = (String)attr.getAttribute(HTML.Attribute.NAME);
        if (name == null) {
            return;
        }
        Object m = attr.getAttribute(StyleConstants.ModelAttribute);
        /*if (m instanceof OptionListModel) {
            OptionListModel model = (OptionListModel)m;

            for (int i = 0; i < model.getSize(); i++) {
                if (model.isSelectedIndex(i)) {
                    Option option = (Option) model.getElementAt(i);
                    appendBuffer(buffer, name, option.getValue());
                }
            }
        } else */if (m instanceof ComboBoxModel) {
            ComboBoxModel model = (ComboBoxModel)m;
            Option option = (Option)model.getSelectedItem();
            if (option != null) {
                appendBuffer(buffer, name, option.getValue());
            }
        }
    }

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDirty(boolean dirty) {
		// TODO Auto-generated method stub

	}

	@Override
	public void spellcheck() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setText(String t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	public class MaskPanel extends JPanel
	{
		public MaskPanel() {
			setBackground(Color.white);
		}

		TreeSet<String> controls;
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			
			g2.setColor(Color.black);
			
			Util.ValMap map = mappings[page];
			if (map == null)
				return;
			
			controls = new TreeSet<String>();
			for (String key : map.keySet()) 
				controls.add(key.substring(0, key.indexOf('_')));
			
			for (String control : controls) {
				String _x = map.get(control + "_x").toString();
				String _y = map.get(control + "_y").toString();
				String _width = map.get(control + "_width").toString();
				String _height = map.get(control + "_height").toString();
//				72 units in user space equals 1 inch in device space
				float x = 72 * Util.toFloat(Float.NaN, _x);
				float y = 72 * Util.toFloat(Float.NaN, _y);
				float width = 72 * Util.toFloat(Float.NaN, _width);
				float height = 72 * Util.toFloat(Float.NaN, _height);
				
		        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
				g2.draw(rect);
				g2.drawString(control, x, y + height);
			}
		}
		
	}
}
