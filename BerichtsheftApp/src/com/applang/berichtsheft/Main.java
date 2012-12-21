package com.applang.berichtsheft;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import ui.SpellChecker;
import ui.Util.Bounds;
import ui.Util.ComponentFunction;
import ui.Util.Settings;

import com.applang.*;
import com.applang.berichtsheft.ui.BerichtsheftTextArea;
import com.applang.berichtsheft.ui.components.NotePicker;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (Util.isAvailable(0, args) && args[0].equals("-jedit")) 
			org.gjt.sp.jedit.jEdit.main(new String[]{"-settings=.jedit", "-reuseview", "-nosplash"});
		else {
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
					show(null);
		        }
		    });
		}
	}

	protected static void show(Component relative, Object... params) {
		UIManager.put("swing.boldMetal", Boolean.FALSE);	//	Turn off metal's use of bold fonts
		
		Settings.load();
		final String title = Util.paramString("Berichtsheft", 0, params);
		final boolean save = Util.paramBoolean(true, 1, params);
		
		final BerichtsheftTextArea textArea = new BerichtsheftTextArea();
//		textArea.createPopupMenu();
		
		ui.Util.showFrame(null, 
			new ComponentFunction<Component[]>() {
				public Component[] apply(Component component, Object[] parms) {
					final JFrame frame = (JFrame)component;
					
					NotePicker notePicker = new NotePicker(textArea, 
							null,
							"Berichtsheft database");
					
					Container cp = frame.getContentPane();
					cp.add(notePicker, BorderLayout.NORTH);
					cp.add(new JScrollPane(textArea), BorderLayout.CENTER);
			        
			        frame.addWindowListener(new WindowAdapter() {
			        	public void windowClosing(WindowEvent event) {
			        		Bounds.save(frame, "frame", title);
			        		if (save)
			        			Settings.save();
			        	}
			        });
					
					return null;
				}
			},
			title, false);		
	}

	public static boolean spell(JTextComponent textArea) {
		new SpellChecker(textArea).show(
				textArea, 
				textArea, 
				"Check spelling", 
				"");
		
		return false;
	}

	public static boolean merge(String vorlage, String dokument, String databaseFilename, Object... params) {
		File tempDir = Util.tempDir("berichtsheft");
		try {
			File source = new File(vorlage);
			if (!source.exists())
				throw new Exception(String.format("Vorlage '%s' missing", vorlage));
			
			File database = new File(databaseFilename);
			if (!database.exists())
				throw new Exception(String.format("Database '%s' missing", database));
			
			File archive = new File(tempDir, "Vorlage.zip");
			Util.copyFile(source, archive);
			int unzipped = ZipUtil.unzipArchive(archive, 
					new ZipUtil.UnzipJob(tempDir.getPath()), 
					false);
			archive.delete();

			String content = Util.pathCombine(tempDir.getPath(), "content.xml");
			String _content = Util.pathCombine(tempDir.getPath(), "_content.xml");
			new File(content).renameTo(new File(_content));
			
			String parameters = String.format(
				"<params>" +
					"<dbfile>%s</dbfile>" +
					"<year>%d</year>" +
					"<weekInYear>%d</weekInYear>" +
					"<dayInWeek>%s</dayInWeek>" +
				"</params>",
				databaseFilename,
				Util.paramInteger(2013, 0, params),
				Util.paramInteger(1, 1, params),
				Util.paramString("\\d", 2, params));
			pipe(_content, content, new StringReader(parameters));
			
			new File(_content).delete();
			
			File destination = new File(dokument);
			if (destination.exists())
				destination.delete();
			
			int zipped = ZipUtil.zipArchive(destination, tempDir.getPath(), tempDir.getPath());
			if (unzipped != zipped)
				throw new Exception(String.format("Dokument '%s' lacking some ingredient", dokument));
	    	
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		finally {
			Util.deleteDirectory(tempDir);
		}
	}

	public static boolean pipe(String inputFilename, String outputFilename, Reader params) throws Exception {
	  	TransformerFactory tFactory = TransformerFactory.newInstance();
	  	
	    // Determine whether the TransformerFactory supports the use of SAXSource and SAXResult
	    if (!tFactory.getFeature(SAXSource.FEATURE))
			throw new Exception(String.format("TransformerFactory feature '%s' missing", SAXSource.FEATURE));
	    if (!tFactory.getFeature(SAXResult.FEATURE))
			throw new Exception(String.format("TransformerFactory feature '%s' missing", SAXResult.FEATURE));
	    
		SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);	  
		TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource("scripts/control.xsl"));
		TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource("scripts/content.xsl"));
		tHandler2.getTransformer().setParameter("inputfile", inputFilename);
		tHandler1.setResult(new SAXResult(tHandler2));
		
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setContentHandler(tHandler1);
		reader.setProperty("http://xml.org/sax/properties/lexical-handler", tHandler1);
		
		Properties xmlProps = OutputPropertiesFactory.getDefaultMethodProperties("xml");
		xmlProps.setProperty("indent", "no");
		xmlProps.setProperty("standalone", "no");
		Serializer serializer = SerializerFactory.getSerializer(xmlProps);
		OutputStream out = new FileOutputStream(outputFilename);
		serializer.setOutputStream(out);
		tHandler2.setResult(new SAXResult(serializer.asContentHandler()));
		
		reader.parse(new InputSource(params));
		
		return true;
	}

}