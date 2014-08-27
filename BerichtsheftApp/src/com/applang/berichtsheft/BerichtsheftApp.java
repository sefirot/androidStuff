package com.applang.berichtsheft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.applang.PluginUtils;

import static com.applang.Util.*;
import static com.applang.Util1.*;
import static com.applang.Util2.*;
import static com.applang.PluginUtils.insideJEdit;
import static com.applang.SwingUtil.*;
import static com.applang.ZipUtil.*;

public class BerichtsheftApp
{
	public static String absolutePath(String relPath) {
		String absPath = insideJEdit() ? 
				jEdit.getSettingsDirectory() : 
				Resources.getCodeSourceLocation(PluginUtils.class).getPath();
		int index = absPath.indexOf(".jedit");
		String part = index > -1 ? 
				absPath.substring(0, index) : 
				relativePath();
		absPath = pathCombine(part, relPath);
		return absPath;
	}
	
	public static void loadSettings() {
		System.setProperty("plugin.props", "BerichtsheftPlugin.props");
		String path = absolutePath(".jedit");
		System.setProperty("jedit.settings.dir", path);
		String path2 = pathCombine(path, "jars/sqlite4java");
		System.setProperty("sqlite4java.library.path", path2);
		path2 = pathCombine(path, "plugins/" + NAME);
		System.setProperty("settings.dir", path2);
		Settings.load(pathCombine(path2, NAME + ".properties"));
		Log.logConsoleHandling(Log.INFO);
		path2 = pathCombine(System.getProperty("user.home"), "work/test");
		Environment.setDataDir(path2);
	}
	/**
	 * @param args
	 */
	public static void main(String...args) {
		loadSettings();
    	File defaultFile = new File(applicationDataPath("jedit.properties"));
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
	
	public static View getJEditView() {
		return (View)Activity.frame;
	}
	
	public static String applicationDataPath(String...parts) {
		String settingsDir = System.getProperty("settings.dir", "");
		parts = arrayappend(strings(settingsDir), parts);
		return pathCombine(parts);
	}
	
	public static String parameters(Object... params) {
		String[] dbFiles = param(strings(), 0, params);
		return String.format(
			"<params>" +
				"<dbfile>%s</dbfile>" +
				"<dbfile2>%s</dbfile2>" +
				"<year>%d</year>" +
				"<weekInYear>%d</weekInYear>" +
				"<dayInWeek>%s</dayInWeek>" +
			"</params>",
			param("", 0, dbFiles),
			param("", 1, dbFiles),
			param_Integer(2013, 1, params),
			param_Integer(1, 2, params),
			param_String("\\d", 3, params));
	}

	public static boolean manipContent(int phase, String vorlage, String dokument, Job<File> manipulation, Object...params) {
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
			boolean keep = phase == 0 && param_Boolean(false, 0, params);
			if (end && !keep)
				deleteDirectory(tempDir);
		}
	}

	public static boolean export(String vorlage, String dokument, final String[] databaseFilenames, Object...params) {
		final Integer year = param_Integer(2013, 0, params);
		final Integer weekInYear = param_Integer(1, 1, params);
		final String dayInWeek = param_String("\\d", 2, params);
		boolean keep = param_Boolean(false, 3, params);
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
					for (int i = 0; i < databaseFilenames.length; i++) {
						databaseFilenames[i] = dbPath(databaseFilenames[i]);
						File database = new File(databaseFilenames[i]);
						if (!database.exists())
							throw new Exception(
									String.format("Database '%s' missing", database.getPath()));
					}
					String parameters = parameters(
						databaseFilenames,
						year,
						weekInYear,
						dayInWeek);
					pipe(_content.getPath(), content.getPath(), new StringReader(parameters));
					boolean keep = param_Boolean(false, 0, params);
					if (keep) {
						File destDir = _content.getParentFile().getParentFile();
						copyFile(_content, new File(destDir, "_content.xml"));
					}
					_content.delete();
				}
			}, keep) && piped;
	}
	
	private static boolean piped = true;

	public static boolean pipe(String inputFilename, String outputFilename, Reader params) throws Exception {
		piped = true;
		String className = "org.apache.xalan.processor.TransformerFactoryImpl";
//		System.setProperty("javax.xml.transform.TransformerFactory", className);
		Class.forName(className);
		TransformerFactory tFactory = TransformerFactory.newInstance();
	    // Determine whether the TransformerFactory supports the use of SAXSource and SAXResult
	    if (!tFactory.getFeature(SAXSource.FEATURE))
			throw new Exception(String.format("TransformerFactory feature '%s' missing", SAXSource.FEATURE));
	    if (!tFactory.getFeature(SAXResult.FEATURE))
			throw new Exception(String.format("TransformerFactory feature '%s' missing", SAXResult.FEATURE));
		SAXTransformerFactory saxTFactory = ((SAXTransformerFactory) tFactory);	  
		String controlStyleSheet = applicationDataPath("Skripte/control.xsl");
		String contentStyleSheet = applicationDataPath("Skripte/content.xsl");
		TransformerHandler tHandler1 = saxTFactory.newTransformerHandler(new StreamSource(controlStyleSheet));
		TransformerHandler tHandler2 = saxTFactory.newTransformerHandler(new StreamSource(contentStyleSheet));
		tHandler2.getTransformer().setParameter("inputfile", inputFilename);
		tHandler2.getTransformer().setErrorListener(new ErrorListener() {
			@Override
			public void warning(TransformerException exception) throws TransformerException {
				Log.w(TAG, exception.getMessage());
			}
			@Override
			public void fatalError(TransformerException exception) throws TransformerException {
				Log.e(TAG, exception.getMessage());
				piped = false;
			}
			@Override
			public void error(TransformerException exception) throws TransformerException {
				Log.e(TAG, exception.getMessage());
				piped = false;
			}
		});
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
		return piped;
	}

	public static String odtVorlagePath(String name) {
		return applicationDataPath("Vorlagen/" + name + ".odt");
	}

	public static String odtDokumentPath(String name, int...weekDate) {
		if (isAvailable(1, weekDate))
			name += String.format("_%d_%d", weekDate[1], weekDate[0]);
		return applicationDataPath("Dokumente/" + name + ".odt");
	}

	public static void doIndirection(Element element, String attr, String tag, Job<Element> job, Object...params) {
		int no = toInt(0, element.getAttribute(attr));
		if (no > 0) {
			NodeList nodeList = evaluateXPath(element.getOwnerDocument(), "//" + tag);
			if (nodeList.getLength() >= no)
				try {
					job.perform( (Element) nodeList.item(no - 1), params );
				}
				catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
		}
	}

	public static void performQueries(String controlFileName) throws Exception {
		final Context context = BerichtsheftActivity.getInstance();
		File file = new File(controlFileName);
		Document doc = xmlDocument(file);
		NodeList nodeList = evaluateXPath(doc.getDocumentElement(), "*[@query]");
		for (int i = 0; i < nodeList.getLength(); i++) {
			final Element element = (Element) nodeList.item(i);
			doIndirection(element, "query", "QUERY", new Job<Element>() {
				public void perform(Element el, Object[] parms) throws Exception {
					doIndirection(el, "dbinfo", "DBINFO", new Job<Element>() {
						public void perform(Element el, Object[] parms) throws Exception {
							NodeList nodeList = evaluateXPath(el, "dburl");
							String url = nodeList.item(0).getTextContent();
							String path = url.substring(url.lastIndexOf(":") + 1);
							Uri uri = fileUri(path, null);
							Element query = param(null, 0, parms);
							if (query != null) {
								ValList list = vlist();
								list.add(query.getAttribute("statement"));
								for (int j = 1; j < element.getAttributes().getLength(); j++) {
									String param = element.getAttribute("param" + j);
									if (notNullOrEmpty(param))
										list.add(param);
								}
								Cursor cursor = context.getContentResolver().rawQuery(uri, toStrings(list));
								list = vlist();
								traverse(cursor, new Job<Cursor>() {
									public void perform(Cursor c, Object[] parms) throws Exception {
										ValList list = param(null, 0, parms);
										list.add(c.getString(0));
									}
								}, list);
								String text = join(NEWLINE, list.toArray());
								element.setTextContent(text);
								element.setAttribute("query", "0");
							}
						}
					}, objects(el));
				}
			});
		}
		xmlNodeToFile(doc, true, file);
	}
}