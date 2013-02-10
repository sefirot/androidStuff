package com.applang.berichtsheft.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.TreeSet;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.applang.Util;

public class FormEditor extends JSplitPane implements TextComponent
{
	public static void main(String[] args) throws Exception {
        JToolBar top = new JToolBar();
        JToolBar bottom = new JToolBar();
        bottom.setFloatable(false);
        final JLabel label = new JLabel("");
        label.setName("mess");
        bottom.add(label);
		
		Util.Settings.load();
		final String xmlPath = Util.Settings.get("content.xml", "scripts/content.xml");
		final FormEditor formEditor = new FormEditor(new File(xmlPath).getCanonicalPath());
		
		String title = "Layout manipulation";
        JFrame frame = new JFrame(title) {
			protected void processWindowEvent(WindowEvent we) {
				if (we.getID() == WindowEvent.WINDOW_CLOSING)
					formEditor.finish(label, xmlPath);
				
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
	}
	
	public void finish(final JLabel label, String xmlPath) {
		try {
			Util.xmlTransform(xmlPath, stylePath, "/tmp/content.xml", 
					"controlfile", htmlPath);
			
			Util.Settings.save();
		} catch (Exception e) {
			label.setText(e.getMessage());
		}
	}
	
	String stylePath = Util.Settings.get("geometry.xsl", "scripts/geometry.xsl");
	String htmlPath = Util.Settings.get("geometry.html", "/tmp/temp.html");
	
	public FormEditor(String xmlName) {
		try {
			Util.xmlTransform(xmlName, stylePath, htmlPath, 
					"mode", 1);
			
			setBottomComponent(editorComponent(htmlPath));
			setTopComponent(graphicComponent());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	final MaskPanel maskPanel = new MaskPanel();
	
	private Component graphicComponent() {
		return new JScrollPane(maskPanel);
	}

	private Component editorComponent(String htmlName) throws IOException {
		JEditorPane jep = new JEditorPane("file:" + htmlName);
		jep.setContentType("text/html");
		jep.setEditable(false);
		
        HTMLEditorKit kit = (HTMLEditorKit)jep.getEditorKit();
        kit.setAutoFormSubmission(false);
        jep.addHyperlinkListener(new HyperlinkListener()
        {                           
            @Override
            public void hyperlinkUpdate(HyperlinkEvent he)
            {
                if (he instanceof FormSubmitEvent)
                {
                    FormSubmitEvent fe = (FormSubmitEvent)he;
					Util.posting2mappings(fe.getData());
                }
            }
        });
        
		return new JScrollPane(jep);
	}

	void testDocument(JEditorPane jep) {
		HTMLDocument doc = (HTMLDocument) jep.getDocument();
		Element input = doc.getElement("control1_x");
        AttributeSet set = input.getAttributes();
        Enumeration<?> names = set.getAttributeNames();
        String value = "";
        while (names.hasMoreElements()) {
        	Object el = names.nextElement();
			value += el.toString() + " : " + set.getAttribute(el).toString() + "\n";
        }
		System.out.println(value);
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
			
			controls = new TreeSet<String>();
			for (String key : Util.mappings.keySet()) 
				controls.add(key.substring(0, key.indexOf('_')));
			
			for (String control : controls) {
				String _x = Util.getMapping(control + "_x");
				String _y = Util.getMapping(control + "_y");
				String _width = Util.getMapping(control + "_width");
				String _height = Util.getMapping(control + "_height");
//				72 units in user space equals 1 inch in device space
				double x = 72 * Util.toDouble(Double.NaN, _x);
				double y = 72 * Util.toDouble(Double.NaN, _y);
				double width = 72 * Util.toDouble(Double.NaN, _width);
				double height = 72 * Util.toDouble(Double.NaN, _height);
				
		        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
				g2.draw(rect);
			}
		}
		
	}
}
