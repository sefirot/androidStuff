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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;

import com.applang.BaseDirective;
import com.applang.Dialogs;

import static com.applang.Util.*;
import static com.applang.Util2.*;
import static com.applang.SwingUtil.*;
import static com.applang.ZipUtil.*;

public class BerichtsheftApp
{
	public static void loadSettings() {
		System.setProperty("settings.dir", ".jedit/plugins/berichtsheft");
		Settings.load();
	}
	/**
	 * @param args
	 */
	public static void main(String...args) {
		loadSettings();
    	File defaultFile = new File(pathCombine(System.getProperty("settings.dir"), "jedit.properties"));
    	try {
	    	File file = new File(".jedit/properties");
	    	if (!fileExists(file)) {
				copyFile(defaultFile, file);
			}
		} catch (Exception e) {
			message(String.format("'%s' could not be copied", defaultFile.getPath()));
			return;
		}
		if (nullOrEmpty(args))
			args = strings(
				"-settings=.jedit", 
				"-run=.jedit/macros/startBerichtsheft.bsh", 
				"-newview", 
				"-noserver", 
				"-nosplash" );
		jEdit.main(args);
	}

	private static final String TAG = BerichtsheftApp.class.getSimpleName();
	
	public static final String NAME = "berichtsheft";
	public static final String packageName = "com.applang.berichtsheft";

	
	private static Activity activity = null;

	public static Activity getActivity() {
		if (activity == null) {
			activity = new Activity();
			activity.setPackageInfo(packageName, "../Berichtsheft");
		}
		return activity;
	}
	
	public static org.gjt.sp.jedit.View getJEditView() {
		return (org.gjt.sp.jedit.View)Activity.frame;
	}
	
	public static String prompt(int type, String title, String message, String[] values, String...defaults) {
		switch (type) {
		case Dialogs.DIALOG_LIST:
			return AlertDialog.chooser(getActivity(), message, values, defaults);

		default:
			AlertDialog.modal = type / 100 < 1;
			Intent intent = new Intent(Dialogs.PROMPT_ACTION)
			.putExtra(BaseDirective.TYPE, type % 100)
			.putExtra(BaseDirective.TITLE, title)
			.putExtra(BaseDirective.PROMPT, message)
			.putExtra(BaseDirective.VALUES, values)
			.putExtra(BaseDirective.DEFAULTS, defaults);
			getActivity().startActivity(intent);
			String result = intent.getExtras().getString(BaseDirective.RESULT);
			return "null".equals(String.valueOf(result)) ? null : result;
		}
	}
	
	public static String parameters(Object... params) {
		return String.format(
			"<params>" +
				"<dbfile>%s</dbfile>" +
				"<year>%d</year>" +
				"<weekInYear>%d</weekInYear>" +
				"<dayInWeek>%s</dayInWeek>" +
			"</params>",
			paramString("", 0, params),
			paramInteger(2013, 1, params),
			paramInteger(1, 2, params),
			paramString("\\d", 3, params));
	}

	public static boolean manipContent(int phase, String vorlage, String dokument, Job<File> manipulation, Object... params) {
		boolean begin = phase > -1;
		boolean end = phase < 1;
		File tempDir = tempDir(begin, BerichtsheftApp.NAME, "odt");
		try {
			int unzipped = 0;
			if (begin) {
				File source = new File(vorlage);
				if (!source.exists())
					throw new Exception(String.format("Vorlage '%s' missing",
							vorlage));
				File archive = new File(tempDir, "Vorlage.zip");
				copyFile(source, archive);
				unzipped = unzipArchive(archive, new UnzipJob(
						tempDir.getPath()), false);
				archive.delete();
			}
			
			if (manipulation != null)
				manipulation.perform(new File(tempDir, "content.xml"), params);
			
			if (end) {
				File destination = new File(dokument);
				if (destination.exists())
					destination.delete();
				int zipped = zipArchive(destination, tempDir.getPath(),
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
					Log.i(TAG, String.format("'%s' generated", dokument));
			}
			return true;
		} catch (Exception e) {
			handleException(e);
			return false;
		}
		finally {
			if (end)
				deleteDirectory(tempDir);
		}
	}

	public static boolean export(String vorlage, String dokument, final String databaseFilename, Object... params) {
		final Integer year = paramInteger(2013, 0, params);
		final Integer weekInYear = paramInteger(1, 1, params);
		final String dayInWeek = paramString("\\d", 2, params);
		
		if (dokument.endsWith("_"))
			dokument = dokument + String.format("%d_%d", year, weekInYear) + ".odt";
		
		return manipContent(
				0, 
				vorlage, 
				dokument, 
				new Job<File>() {
					public void perform(File content, Object[] params) throws Exception {
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
		TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource(getSetting("control.xsl", "scripts/control.xsl")));
		TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource(getSetting("content.xsl", "scripts/content.xsl")));
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