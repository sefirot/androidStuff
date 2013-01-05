package com.applang.berichtsheft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.gjt.sp.jedit.jEdit;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.applang.*;
import com.applang.berichtsheft.ui.JEditTextComponent;
import com.applang.berichtsheft.ui.components.TextComponent;

public class BerichtsheftApp
{
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		args = new String[] {
				"-settings=.jedit", 
				"-newview", 
				"-noserver", 
				"-nosplash" };
		jEdit.main(args);
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