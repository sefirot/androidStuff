package com.applang.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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

import static com.applang.SwingUtil.*;
import static com.applang.Util.*;
import static com.applang.Util2.*;

import com.applang.SwingUtil.Behavior;
import com.applang.Util2;
import com.applang.berichtsheft.BerichtsheftApp;

public class FormEditor extends JSplitPane
{
	public static void main(String[] args) throws Exception {
		final String inputPath = param(BerichtsheftApp.odtVorlagePath("Tagesberichte"), 0, args);
		final String outputPath = param(BerichtsheftApp.odtDokumentPath("Tagesberichte"), 1, args);
        SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				perform(inputPath, outputPath, false);
			}
		});
	}

	public static boolean perform(final String inputPath, final String outputPath, final boolean deadline, Object... params) {
		try {
			BerichtsheftApp.loadSettings();
			final Job<File> finish = new Job<File>() {
				public void perform(File _content, Object[] params) throws Exception {
					_content.delete();
					BerichtsheftApp.manipContent(-1, inputPath, outputPath, null);
					Settings.save();
				}
			};
			boolean ok = BerichtsheftApp.manipContent(1, inputPath, outputPath, 
					new Job<File>() {
						public void perform(final File content, final Object[] params) throws Exception {
							inputDir = content.getParentFile();
							final File _content = new File(inputDir, "_content.xml");
							content.renameTo(_content);
							if (!generateMask(_content.getCanonicalPath()))
								return;
							final FormEditor formEditor = new FormEditor();
							showFrame(null, 
									"Layout manipulation", 
									new UIFunction() {
										public Component[] apply(Component comp, Object[] parms) {
											JFrame frame = (JFrame)comp;
											Container contentPane = frame.getContentPane();
											northToolBar(contentPane);
											southStatusBar(contentPane);
											contentPane.add(formEditor, BorderLayout.CENTER);
											return null;
										}
									}, 
									new UIFunction() {
										public Component[] apply(Component comp, Object[] parms) {
											JFrame frame = (JFrame)comp;
											Bounds.load(frame, "frame", frame.getTitle());
											formEditor.setDivider();
											if (isAvailable(0, params)) {
												try {
													Job<FormEditor> job = param(null, 0, params);
													job.perform(formEditor, null);
												} catch (Exception e) {
													handleException(e);
												}
											}
											return null;
										}
									}, 
									new UIFunction() {
										public Component[] apply(Component comp, Object[] parms) {
											JFrame frame = (JFrame)comp;
											Bounds.save(frame, "frame", frame.getTitle());
											unmask(_content.getPath(), content.getPath());
											try {
												if (!deadline)
													finish.perform(_content, null);
											} catch (Exception e) {
												handleException(e);
											}
											return null;
										}
									}, 
									deadline ? Behavior.TIMEOUT : 0);
							if (deadline) {
								if (isAvailable(1, params)) {
									Job<Void> job = param(null, 1, params);
									job.perform(null, new Object[] {_content.getPath(), content.getPath()});
								}
								try {
									finish.perform(_content, null);
								} catch (Exception e) {
									handleException(e);
								}
							}
						}
					}, params);
			return ok;
		} catch (Exception e) {
			handleException(e);
			return false;
		}
	}
	
	static void unmask(String inputPath, String outputPath) {
		try {
			String stylePath = BerichtsheftApp.applicationDataPath("Skripte/mask.xsl");
			xmlTransform(inputPath, stylePath, outputPath, "mode", 2);
		} catch (Exception e) {
			handleException(e);
		}
	}
	
	static File inputDir = null;
	
	static boolean generateMask(String contentXml) {
		try {
			String stylePath = BerichtsheftApp.applicationDataPath("Skripte/mask.xsl");
			String dummy = tempPath() + "/temp.html";
			xmlTransform(contentXml, stylePath, dummy, "mode", 1);
			File dir = tempDir(false, BerichtsheftApp.NAME);
			pages = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.matches("page\\d+\\.html");
				}
			});
			images = new Image[pages.length];
			pageLayoutProperties = getElement("styles.xml", 
					"/document-styles" +
					"/automatic-styles" +
					"/page-layout" +
					"/page-layout-properties");
			return true;
		} catch (Exception e) {
			handleException(e);
			return false;
		}
	}
	
	static org.w3c.dom.Element getElement(String fileName, String xpath) {
		File file = new File(inputDir, fileName);
		if (fileExists(file)) {
			org.w3c.dom.Document doc = xmlDocument(file);
			org.w3c.dom.NodeList nodes = evaluateXPath(doc, xpath);
			if (nodes.getLength() > 0) 
				return (org.w3c.dom.Element) nodes.item(0);
		}
		return null;
	}
	
	static Image loadImage(String path) {
		try {
			Image image = ImageIO.read(new File(path));
			return image;
		} catch (Exception e) {
			handleException(e);
			return null;
		}
	}

	public static File[] pages = null;
	public static org.w3c.dom.Element pageLayoutProperties = null;
	public static Image[] images = null;
	
	public FormEditor() {
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
			
			mappings = new ValMap[pages.length];
			updateSplitComponents(page = 0, null);
		} catch (Exception e) {
			handleException(e);
		}
	}

	private void setDivider() {
		int dividerLocation = this.getWidth() / 2;
		setDividerLocation(dividerLocation);
	}

	ValMap[] mappings = null;
	int page;
	
	public void updateSplitComponents(int page, String data) {
		try {
			if (pages.length > 0 && page < pages.length) {
				updateMask(page, data);
				String url = "file:" + pages[page].getPath();
				editorPanel.setPage(url);
			}
			else
				message("no mask data available");
		} catch (Exception e) {
			handleException(e);
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
		    	URL url = he.getURL();
		        if (he instanceof FormSubmitEvent)
		        {
		            FormSubmitEvent fe = (FormSubmitEvent)he;
		            String data = fe.getData();
					
		            updateSplitComponents(page, data);
		        }
		        else if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		    		String name = new File(url.getFile()).getName();
					int page = toInt(0, findFirstIn(name, Pattern.compile("\\w+(\\d+)\\.\\w+")).group(1));
					updateSplitComponents(page - 1, null);
		        }
		    }
		});
	}
	
	private void forceReload() {
		Document doc = editorPanel.getDocument();
		doc.putProperty(Document.StreamDescriptionProperty, null);
	}
	
	@SuppressWarnings("unused")
	private ValMap allMappings() {
		ValMap map = vmap();
		for (int p = 0; p < pages.length; p++) {
			getPageData(p);
			for (String key : mappings[p].keySet())
				map.put(key, mappings[p].get(key));
		}
		return map;
	}
	
	private void updateMask(int page, String data) {
		try {
			getPageData(page);
			
			if (notNullOrEmpty(data)) {
				ValMap map = vmap();
				String[] parts = data.split("&|=");
				for (int i = 0; i < parts.length - 1; i+=2) {
					String key = URLDecoder.decode(parts[i], "UTF-8");
					String value = URLDecoder.decode(parts[i+1], "UTF-8");
					map.put(key, value);
				}
				boolean change = false;
				int action = 0;
				for (String key : map.keySet()) {
					String value = map.get(key).toString();
					if (key.startsWith("control") && value.toLowerCase().equals("on")) {
						change |= updateMapping(page, key, "x", map);
						change |= updateMapping(page, key, "y", map);
						change |= updateMapping(page, key, "width", map);
						change |= updateMapping(page, key, "height", map);
					}
					else if (key.startsWith("action"))
						action = toInt(0, findFirstIn(key, Pattern.compile("\\d+")).group());
				}
				switch (action) {
				case 1:
					if (change) {
						putPageData(page);
						forceReload();
					}
					break;
				}
			}
			
			this.page = page;
			maskPanel.update(null, null);
		} catch (Exception e) {
			handleException(e);
		}
	}
	
    private boolean updateMapping(int page, String key1, String key2, ValMap map) {
    	boolean retval = false;
    	Object value = map.get(key2);
		if (value != null) {
			float val = toFloat(Float.NaN, value.toString());
			if (!Float.isNaN(val)) {
				mappings[page].put(key1 + "_" + key2, val);
				retval = true;
			}
		}
		return retval;
	}

	private void putPageData(int page) {
		org.w3c.dom.Document doc = map2page(mappings[page], page, false);
		if (doc != null)
			xmlNodeToFile(doc, true, pages[page]);
	}

	public static org.w3c.dom.Document map2page(ValMap map, int page, boolean reverse) {
    	org.w3c.dom.Document doc = xmlDocument(pages[page]);
    	if (doc == null)
    		return null;
    	
		org.w3c.dom.NodeList nodes = evaluateXPath(doc, "//table[@id='controls']");
		if (nodes.getLength() > 0) {
			nodes = evaluateXPath(nodes.item(0), ".//*[@name]");
			for (int i = 0; i < nodes.getLength(); i++) {
				org.w3c.dom.Element node = (org.w3c.dom.Element)nodes.item(i);
				String key = node.getAttribute("name");
				if (key.contains("_")) {
					boolean inputTag = key.endsWith("image");
					if (reverse) {
						String value = inputTag ? node.getAttribute("value") : node.getTextContent();
						map.put(key, value);
						if (inputTag)
							images[page] = loadImage(new File(inputDir, value).getPath());
					} else {
						Object value = map.get(key);
						if (value != null)
							if (inputTag)
								node.setAttribute("value", value.toString());
							else
								node.setTextContent(value.toString());
					}
				}
			}
		}
		
		return doc;
	}
	
    private void getPageData(int page) {
    	mappings[page] = vmap();
    	map2page(mappings[page], page, true);
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
	
    private void createMaskPanelPopupMenu(final MaskPanel maskPanel) {
    	final String[] factors = strings("offsetX","offsetY","scaleX","scaleY");
    	final JPanel pnl = new JPanel();
    	final Observer observer = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				for (int i = 0, j = 0; i < factors.length; i++, j=i%2) {
					JTextField field = findFirstComponent(pnl, factors[i]);
					if (o instanceof MaskPanel.Scale && factors[i].startsWith("scale")) {
						MaskPanel.Scale scale = (MaskPanel.Scale) o;
						field.setText("" + round(scale.getDim(j), 3));
					}
					if (o instanceof MaskPanel.Offset && factors[i].startsWith("offset")) {
						MaskPanel.Offset offset = (MaskPanel.Offset) o;
						field.setText("" + round(offset.getCoord(j), 3));
					}
				}
			}
		};
        maskPanel.addMouseListener(newPopupAdapter(
        	new Object[] {"Mask fitting ...", 
        		new ActionListener() {
		        	public void actionPerformed(ActionEvent ae) {
		        		showDialog(maskPanel, maskPanel, 
		        			"Mask fitting", 
		        			new UIFunction() {
								public Component[] apply(Component dlg, Object[] parms) {
									pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));
									Dimension fieldSize = new Dimension(160,20);
									for (int i = 0; i < factors.length; i++) {
										if (findFirstComponent(pnl, factors[i]) == null) {
											final JTextField field = new JTextField();
											field.setName(factors[i]);
											field.setPreferredSize(fieldSize);
											field.setHorizontalAlignment(JTextField.CENTER);
											JLabel label = new JLabel(factors[i]);
											label.setLabelFor(field);
											label.setPreferredSize(fieldSize);
											label.setHorizontalAlignment(SwingConstants.CENTER);
											pnl.add(label);
											label.setAlignmentX(Component.CENTER_ALIGNMENT);
											pnl.add(field);
											field.setAlignmentX(Component.CENTER_ALIGNMENT);
											field.addActionListener(new ActionListener() {
												public void actionPerformed(
														ActionEvent e) {
													String name = field
															.getName();
													int i = asList(
															factors).indexOf(
															name) % 2;
													String text = field
															.getText();
													if (name.startsWith("offset")) {
														double d = maskPanel.offset
																.getCoord(i);
														d = toDouble(d,
																text);
														maskPanel.offset
																.setCoord(i, d);
													}
													if (name.startsWith("scale")) {
														double d = maskPanel.scale
																.getDim(i);
														d = toDouble(d,
																text);
														maskPanel.scale.setDim(
																i, d);
													}
												}
											});
										}
									}
									return components(pnl);
								}
		        			}, 
		        			new UIFunction() {
								public Component[] apply(Component dlg, Object[] parms) {
									maskPanel.scale.addObserver(observer);
									observer.update(maskPanel.scale, null);
									maskPanel.offset.addObserver(observer);
									observer.update(maskPanel.offset, null);
									return null;
								}
		        			}, 
		        			new UIFunction() {
								public Component[] apply(Component dlg, Object[] parms) {
									maskPanel.scale.deleteObserver(observer);
									maskPanel.offset.deleteObserver(observer);
									return null;
								}
		        			}, 
		        			Behavior.NONE);
		        	}
		        }, "scale", "change scale factors of the mask"}
        ));
    }

	public class MaskPanel extends JPanel implements Observer
	{
		public class Scale extends Observable
		{
			public void setDim(int i, double d) {
				double[] dd = new double[]{dim.getWidth(),dim.getHeight()};
				dd[i] = d;
				dim.setSize(dd[0], dd[1]);
				setChanged();
				notifyObservers(this);
			}
			
			public double getDim(int i) {
				double[] dd = new double[]{dim.getWidth(),dim.getHeight()};
				return dd[i];
			}

			public Dimension2D dim = new Dimension2D() {
				double width, height;
				@Override
				public void setSize(double width, double height) {
					this.width = width;
					this.height = height;
				}
				@Override
				public double getWidth() {
					return width;
				}
				@Override
				public double getHeight() {
					return height;
				}
			};

			public String toString() {
				Writer writer = write(new StringWriter(), "[");
				writer = write_assoc(writer, "width", dim.getWidth());
				writer = write_assoc(writer, "height", dim.getHeight(), 1);
				return write(writer, "]").toString();
			}
		}
		public class Offset extends Observable
		{
			public void setCoord(int i, double d) {
				double[] cc = new double[]{point.getX(),point.getY()};
				cc[i] = d;
				point.setLocation(cc[0], cc[1]);
				setChanged();
				notifyObservers(this);
			}
			
			public double getCoord(int i) {
				double[] cc = new double[]{point.getX(),point.getY()};
				return cc[i];
			}

			public Point2D point = new Point2D() {
				double x, y;
				@Override
				public void setLocation(double x, double y) {
					this.x = x;
					this.y = y;
				}
				@Override
				public double getY() {
					return y;
				}
				@Override
				public double getX() {
					return x;
				}
			};

			public String toString() {
				Writer writer = write(new StringWriter(), "[");
				writer = write_assoc(writer, "x", point.getX());
				writer = write_assoc(writer, "y", point.getY(), 1);
				return write(writer, "]").toString();
			}
		}
		
		public Scale scale;
		public Offset offset;
		 
		class MouseHandler extends MouseAdapter {
			private int offsetX;
			private int offsetY;
	 
			public void mousePressed(MouseEvent e) {
				offsetX = e.getX();
				offsetY = e.getY();
			}
	 
			public void mouseDragged(MouseEvent e) {
				int deltaX = e.getX() - offsetX;
				int deltaY = e.getY() - offsetY;
	 
				offsetX += deltaX;
				offsetY += deltaY;
	 
				offset.setCoord(0, offset.getCoord(0) + deltaX);
				offset.setCoord(1, offset.getCoord(1) + deltaY);
			}
			
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					double delta = .005 * e.getWheelRotation();
					
					if (e.isShiftDown()) 
						scale.setDim(1, Math.max(0.00001, scale.getDim(1) + delta));
					
					if (e.isControlDown()) 
						scale.setDim(0, Math.max(0.00001, scale.getDim(0) + delta));
				}
			}
		}
		
		public MaskPanel() {
			setBackground(Color.white);
			
			offset = new Offset();
			offset.point.setLocation(0.0, 0.0);
			offset.addObserver(this);
			
			scale = new Scale();
			scale.dim.setSize(1.0, 1.0);
			scale.addObserver(this);
			
			MouseHandler mouseHandler = new MouseHandler();
			addMouseListener(mouseHandler);
			addMouseMotionListener(mouseHandler);
			addMouseWheelListener(mouseHandler);
			
			createMaskPanelPopupMenu(this);
			
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					resizeMask();
					update(null, null);
				}
			});
		}

		@Override
		public void update(Observable o, Object arg) {
//			println("update offset : %s\tscale : %s", offset, scale);
			repaint();
		}

		//	72 units in user space equals 1 inch in device space
		private static final int device2userSpace = 72;
		
		private Double pageLayout(String name) {
			if (pageLayoutProperties == null)
				return null;
			
			String attribute = pageLayoutProperties.getAttribute(name);
			return toDouble(null, stripUnits(attribute));
		}
		
		Double[] page_dims = new Double[] {
				pageLayout("fo:page-width"), 
				pageLayout("fo:page-height"), 
		};
		
		Double[] margins = new Double[] {
				pageLayout("fo:margin-left"), 
				pageLayout("fo:margin-top"), 
		};
		
		public double getDim(int i) {
			double[] dd = new double[]{this.getWidth(),this.getHeight()};
			return dd[i];
		}
		
		private void resizeMask() {
			for (int i = 0; i < 2; i++) {
				double user = this.getDim(i);
				Double dim = page_dims[i];
				if (dim == null) {
					Object frameItem = frameItem(page, i==0 ? "width" : "height");
					if (frameItem != null) 
						dim = toDouble(
								new Double(1 / device2userSpace), 
								frameItem.toString());
				}
				scale.setDim(i, user / device2userSpace / dim);
				offset.setCoord(i, margins[i] == null ? 0 : device2userSpace * margins[i] * scale.getDim(i));
			}
		}
	
		private Object frameItem(int page, String item) {
			return mappings[page].get("frame" + (page+1) + "_" + item);
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);			
			g2.setColor(Color.black);
			
			ValMap map = mappings[page];
			if (map == null)
				return;
			
			if (isAvailable(page, images)) 
				g2.drawImage(images[page], 0, 0, this.getWidth(), this.getHeight(), null);
			
			AffineTransform tx = new AffineTransform();
			tx.translate(offset.point.getX(), offset.point.getY());
			tx.scale(scale.dim.getWidth(), scale.dim.getHeight());
			g2.setTransform(tx);
			
			TreeSet<String> controls = new TreeSet<String>();
			for (String key : map.keySet()) {
				int underscore = key.indexOf('_');
				if (underscore > 0 && key.startsWith("control"))
					controls.add(key.substring(0, underscore));
			}
			
			for (String control : controls) {
				String _x = map.get(control + "_x").toString();
				String _y = map.get(control + "_y").toString();
				String _width = map.get(control + "_width").toString();
				String _height = map.get(control + "_height").toString();
				float x = device2userSpace * toFloat(Float.NaN, _x);
				float y = device2userSpace * toFloat(Float.NaN, _y);
				float width = device2userSpace * toFloat(Float.NaN, _width);
				float height = device2userSpace * toFloat(Float.NaN, _height);
				
		        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
				g2.draw(rect);
				g2.drawString(control, x, y + height);
			}
		}
		
	}
}
