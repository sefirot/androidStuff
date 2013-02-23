package com.applang.berichtsheft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;
import java.util.logging.Logger;

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

public class BerichtsheftApp
{
	public static Logger logger = Logger.getLogger(BerichtsheftApp.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		args = new String[] {
				"-settings=.jedit", 
				"-newview", 
				"-noserver", 
				"-nosplash", 
				"/tmp/.notes" };
		jEdit.main(args);
	}
	
	public static String parameters(Object... params) {
		return String.format(
			"<params>" +
				"<dbfile>%s</dbfile>" +
				"<year>%d</year>" +
				"<weekInYear>%d</weekInYear>" +
				"<dayInWeek>%s</dayInWeek>" +
			"</params>",
			Util.paramString("", 0, params),
			Util.paramInteger(2013, 1, params),
			Util.paramInteger(1, 2, params),
			Util.paramString("\\d", 3, params));
	}

	public static boolean manipContent(int phase, String vorlage, String dokument, Util.Job<File> manipulation, Object... params) {
		boolean begin = phase > -1;
		boolean end = phase < 1;
		File tempDir = Util.tempDir(begin, "berichtsheft", "odt");
		try {
			int unzipped = 0;
			if (begin) {
				File source = new File(vorlage);
				if (!source.exists())
					throw new Exception(String.format("Vorlage '%s' missing",
							vorlage));
				File archive = new File(tempDir, "Vorlage.zip");
				Util.copyFile(source, archive);
				unzipped = ZipUtil.unzipArchive(archive, new ZipUtil.UnzipJob(
						tempDir.getPath()), false);
				archive.delete();
			}
			
			if (manipulation != null)
				manipulation.dispatch(new File(tempDir, "content.xml"), params);
			
			if (end) {
				File destination = new File(dokument);
				if (destination.exists())
					destination.delete();
				int zipped = ZipUtil.zipArchive(destination, tempDir.getPath(),
						tempDir.getPath());
				if (phase == 0 && unzipped > zipped)
					throw new Exception(
							String.format(
									"Dokument '%s' is lacking some ingredient after manipulation",
									dokument));
				else if (phase == 0 && unzipped < zipped)
					throw new Exception(
							String.format(
									"Dokument '%s' has more ingredients than before manipulation",
									dokument));
				else
					logger.info(String.format("'%s' generated", dokument));
			}
			return true;
		} catch (Exception e) {
			SwingUtil.handleException(e);
			return false;
		}
		finally {
			if (end)
				Util.deleteDirectory(tempDir);
		}
	}

	public static boolean export(String vorlage, String dokument, final String databaseFilename, Object... params) {
		final Integer year = Util.paramInteger(2013, 0, params);
		final Integer weekInYear = Util.paramInteger(1, 1, params);
		final String dayInWeek = Util.paramString("\\d", 2, params);
		
		if (dokument.endsWith("_"))
			dokument = dokument + String.format("%d_%d", year, weekInYear) + ".odt";
		
		return manipContent(
				0, 
				vorlage, 
				dokument, 
				new Util.Job<File>() {
					public void dispatch(File content, Object[] params) throws Exception {
						File _content = new File(content.getParent(), "_content.xml");
						content.renameTo(_content);
						
						File database = new File(databaseFilename);
						if (!database.exists())
							throw new Exception(String.format("Database '%s' missing", database));
						
						String parameters = parameters(
							databaseFilename,
							year,
							weekInYear,
							dayInWeek);
						
						pipe(_content.getPath(), content.getPath(), new StringReader(parameters));
						
						_content.delete();
					}
				});
	}

	public static boolean pipe(String inputFilename, String outputFilename, Reader params) throws Exception {
	  	TransformerFactory tFactory = TransformerFactory.newInstance();
	  	
	    // Determine whether the TransformerFactory supports the use of SAXSource and SAXResult
	    if (!tFactory.getFeature(SAXSource.FEATURE))
			throw new Exception(String.format("TransformerFactory feature '%s' missing", SAXSource.FEATURE));
	    if (!tFactory.getFeature(SAXResult.FEATURE))
			throw new Exception(String.format("TransformerFactory feature '%s' missing", SAXResult.FEATURE));
	    
		SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);	  
		TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource(Util.getSetting("control.xsl", "scripts/control.xsl")));
		TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource(Util.getSetting("content.xsl", "scripts/content.xsl")));
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