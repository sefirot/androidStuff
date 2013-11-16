package android.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.applang.Util.*;

/**
 * API for sending log output.
 *
 * <p>Generally, use the Log.v() Log.d() Log.i() Log.w() and Log.e()
 * methods.
 *
 * <p>The order in terms of verbosity, from least to most is
 * ERROR, WARN, INFO, DEBUG, VERBOSE.  Verbose should never be compiled
 * into an application except during development.  Debug logs are compiled
 * in but stripped at runtime.  Error, warning and info logs are always kept.
 *
 * <p><b>Tip:</b> A good convention is to declare a <code>TAG</code> constant
 * in your class:
 *
 * <pre>private static final String TAG = "MyActivity";</pre>
 *
 * and use that in subsequent calls to the log methods.
 * </p>
 *
 * <p><b>Tip:</b> Don't forget that when you make a call like
 * <pre>Log.v(TAG, "index=" + i);</pre>
 * that when you're building the string to pass into Log.d, the compiler uses a
 * StringBuilder and at least three allocations occur: the StringBuilder
 * itself, the buffer, and the String object.  Realistically, there is also
 * another buffer allocation and copy, and even more pressure on the gc.
 * That means that if your log message is filtered out, you might be doing
 * significant work and incurring significant overhead.
 */
public final class Log
{
	public static final Logger logger = Logger.getLogger("com.applang");
    
	public static class LogFormatter extends Formatter
	{
		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			sb.append(formatDate(now()));
			sb.append(String.format(" %s : ", record.getLevel()));
			sb.append(formatMessage(record));
			sb.append("\n");
			return sb.toString();
		}
	}
    
    public static void logConsoleHandling(int priority) {
		try {
			Logger rootLogger = Logger.getLogger("");
	    	for (Handler handler : rootLogger.getHandlers()) 
	    		if (handler instanceof ConsoleHandler) {
	    			((ConsoleHandler) handler).setFormatter(new LogFormatter());
	    			break;
	    		}
	    	Log.changeLogLevel(priority);
		} catch (Exception ex) {
			new ErrorManager().error("logConsoleHandling", ex, ErrorManager.GENERIC_FAILURE);
		}
    }
    
    public static void logFileHandling(String pattern, int limit, int count, boolean append) {
//    	logFileHandling("%t/" + TAG + "-log.%u-%g.txt", 8192, 5, false)
		try {
			Logger rootLogger = Logger.getLogger("");
	    	for (Handler handler : rootLogger.getHandlers()) 
	    		if (handler instanceof ConsoleHandler) {
	    			rootLogger.removeHandler(handler);
	    			break;
	    		}
			FileHandler fileHandler = new FileHandler(pattern, limit, count, append);
			fileHandler.setFormatter(new LogFormatter());
			rootLogger.addHandler(fileHandler);
		} catch (Exception ex) {
			new ErrorManager().error("logFileHandling", ex, ErrorManager.GENERIC_FAILURE);
		}
    }

    /*
     * set different levels for the logger plus a handler in order to take effect !!!
     */
    public static void changeLogLevel(int priority) {
    	Level level = toLevel(priority);
		Log.logger.setLevel(level);
		Logger rootLogger = Logger.getLogger("");
		rootLogger.getHandlers()[0].setLevel(level);
    }

    /**
     * Priority constant for the println method; use Log.v.
     */
    public static final int VERBOSE = 2;

    /**
     * Priority constant for the println method; use Log.d.
     */
    public static final int DEBUG = 3;

    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int INFO = 4;

    /**
     * Priority constant for the println method; use Log.w.
     */
    public static final int WARN = 5;

    /**
     * Priority constant for the println method; use Log.e.
     */
    public static final int ERROR = 6;

    /**
     * Priority constant for the println method.
     */
    public static final int ASSERT = 7;

    /**
     * Exception class used to capture a stack trace in {@link #wtf()}.
     */
    private static class TerribleFailure extends Exception {
        TerribleFailure(String msg, Throwable cause) { super(msg, cause); }
    }

    /**
     * Interface to handle terrible failures from {@link #wtf()}.
     *
     * @hide
     */
    public interface TerribleFailureHandler {
        void onTerribleFailure(String tag, TerribleFailure what);
    }

    private static TerribleFailureHandler sWtfHandler = new TerribleFailureHandler() {
            public void onTerribleFailure(String tag, TerribleFailure what) {
                wtf(tag, what);
            }
        };

    private Log() {
    }

    /**
     * Send a {@link #VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        return println(LOG_ID_MAIN, VERBOSE, tag, msg);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        return println(LOG_ID_MAIN, DEBUG, tag, msg);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send an {@link #INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        return println(LOG_ID_MAIN, INFO, tag, msg);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, INFO, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a {@link #WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        return println(LOG_ID_MAIN, WARN, tag, msg);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, WARN, tag, msg + '\n' + getStackTraceString(tr));
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        return println(LOG_ID_MAIN, WARN, tag, getStackTraceString(tr));
    }

    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        return println(LOG_ID_MAIN, ERROR, tag, msg);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, ERROR, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     */
    public static int wtf(String tag, String msg) {
        return wtf(tag, msg, null);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, String)}, with an exception to log.
     * @param tag Used to identify the source of a log message.
     * @param tr An exception to log.
     */
    public static int wtf(String tag, Throwable tr) {
        return wtf(tag, tr.getMessage(), tr);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, Throwable)}, with a message as well.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     * @param tr An exception to log.  May be null.
     */
    public static int wtf(String tag, String msg, Throwable tr) {
        TerribleFailure what = new TerribleFailure(msg, tr);
        int bytes = println(LOG_ID_MAIN, ASSERT, tag, msg + '\n' + getStackTraceString(tr));
        sWtfHandler.onTerribleFailure(tag, what);
        return bytes;
    }

    /**
     * Sets the terrible failure handler, for testing.
     *
     * @return the old handler
     *
     * @hide
     */
    public static TerribleFailureHandler setWtfHandler(TerribleFailureHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler == null");
        }
        TerribleFailureHandler oldHandler = sWtfHandler;
        sWtfHandler = handler;
        return oldHandler;
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Low-level logging call.
     * @param priority The priority/type of this log message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int bufId, int priority, String tag, String msg) {
    	if (!isLoggable(tag, priority))
    		return -1;
    	
        StringBuilder s = new StringBuilder("[" + tag + "] ");
        if (msg != null)
        	s.append(msg);
        
        logger.log(toLevel(priority), s.toString());
    	return 0;
    }

    /** @hide */ public static final int LOG_ID_MAIN = 0;
    /** @hide */ public static final int LOG_ID_RADIO = 1;
    /** @hide */ public static final int LOG_ID_EVENTS = 2;
    /** @hide */ public static final int LOG_ID_SYSTEM = 3;

    /**
     * Checks to see whether or not a log for the specified tag is loggable at the specified level.
     *
     *  The default level of any tag is set to INFO. This means that any level above and including
     *  INFO will be logged. Before you make any calls to a logging method you should check to see
     *  if your tag should be logged. You can change the default level by setting a system property:
     *      'setprop log.tag.&lt;YOUR_LOG_TAG> &lt;LEVEL>'
     *  Where level is either VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS. SUPPRESS will
     *  turn off all logging for your tag. You can also create a local.prop file that with the
     *  following in it:
     *      'log.tag.&lt;YOUR_LOG_TAG>=&lt;LEVEL>'
     *  and place that in /data/local.prop.
     *
     * @param tag The tag to check.
     * @param level The level to check.
     * @return Whether or not that this is allowed to be logged.
     * @throws IllegalArgumentException is thrown if the tag.length() > 23.
     */
    public static /*native*/ boolean isLoggable(String tag, int priority) {
    	Level lvl = toLevel(priority);
        return logger.isLoggable(lvl);
    }
    
    static Level toLevel(int priority) {
    	switch (priority) {
		case VERBOSE:	
			return Level.FINER;
		case DEBUG:	
			return Level.FINE;
		case INFO:	
			return Level.INFO;
		case WARN:	
			return Level.WARNING;
		case ERROR:	
			return Level.SEVERE;
		case ASSERT:	
			return Level.FINEST;
		default:
			return Level.OFF;
		}
    }
}
