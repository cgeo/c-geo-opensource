package cgeo.geocaching.utils;

import cgeo.geocaching.storage.LocalStorage;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public final class Log {

    private static final String TAG = "cgeo";

    public enum LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, NONE }

    /** Name of File containing Log properties which will be searched for in logfiles-directory */
    private static final String LOGPROPERTY_FILENAME = "log.properties";
    /** Minimum log level. Value should be one of {@link LogLevel} in textual form */
    public static final String PROP_MIN_LOG_LEVEL = "logging.minlevel";
    /** Minimum log level to add callerinfo to log message. Value should be one of {@link LogLevel} in textual form */
    public static final String PROP_MIN_CALLERINFO_LEVEL = "logging.mincallerinfolevel";
    /** Whether to throw an exception when an error is logged. Value should be true or false */
    public static final String PROP_THROW_ON_ERROR_LOG = "logging.throwonerror";

    /**
     * If the debug flag is set then minimum log level is debug AND an exception is thrown on error logging
     * The debug flag is cached here so that we don't need to access the settings every time we have to evaluate it.
     */
    private static boolean isDebug = true;

    private static LogLevel minLogLevel = LogLevel.WARN;
    private static boolean logThrowExceptionOnError = false;
    private static LogLevel minLogAddCallerINfo = LogLevel.NONE;

    private static final boolean[] SETTING_DO_LOGGING = new boolean[LogLevel.values().length];
    private static boolean settingThrowExceptionOnError = true;
    private static final boolean[] SETTING_ADD_CLASSINFO = new boolean[LogLevel.values().length];

    static {
        try {
            final File propFile = new File(LocalStorage.getLogfilesDirectory(), LOGPROPERTY_FILENAME);
            if (!propFile.exists()) {
                adjustSettings();
                android.util.Log.i(TAG, "[Log] No logging config found at " + propFile + ", using defaults");
            } else {
                android.util.Log.i(TAG, "[Log] Logging config found at " + propFile + ", try to apply");
                final Properties logProps = new Properties();
                logProps.load(new FileReader(propFile));
                setProperties(logProps);
            }
        } catch (Exception ex) {
            android.util.Log.e(TAG, "[Log] Failed to set up Logging", ex);
        }
    }

    private Log() {
        //utility class
    }

    public static boolean isDebug() {
        return isDebug;
    }

    /**
     * Save a copy of the debug flag from the settings for performance reasons.
     *
     */
    public static void setDebug(final boolean isDebug) {
        Log.isDebug = isDebug;
        adjustSettings();
    }

    public static void setProperties(final Properties logProps) {
        if (logProps != null) {
            LogLevel level = readLogLevel(logProps, PROP_MIN_LOG_LEVEL);
            if (level != null) {
                minLogLevel = level;
            }
            level = readLogLevel(logProps, PROP_MIN_CALLERINFO_LEVEL);
            if (level != null) {
                minLogAddCallerINfo = level;
            }
            logThrowExceptionOnError = "true".equalsIgnoreCase(logProps.getProperty(PROP_THROW_ON_ERROR_LOG));
            adjustSettings();

        }
    }

    public static LogLevel readLogLevel(final Properties logProps, final String propName) {
        if (!logProps.containsKey(propName)) {
            return null;
        }
        try {
            return LogLevel.valueOf(logProps.getProperty(propName).toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private static void adjustSettings() {
        setLevel(SETTING_DO_LOGGING, isDebug() && minLogLevel.ordinal() > LogLevel.DEBUG.ordinal() ? LogLevel.DEBUG : minLogLevel);
        setLevel(SETTING_ADD_CLASSINFO, minLogAddCallerINfo);
        settingThrowExceptionOnError = logThrowExceptionOnError || isDebug;
        android.util.Log.i(TAG, "[Log] Logging set: minLevel=" + minLogLevel + ", minAddCallerInfo=" +  minLogAddCallerINfo + ", throwOnError=" + logThrowExceptionOnError);
    }

    private static void setLevel(final boolean[] settings, final LogLevel level) {
        for (int i = 0; i < settings.length; i++) {
            settings[i] = level.ordinal() <= i;
        }
    }

    private static String adjustMessage(final String msg, final LogLevel level) {
        //thread
        final String threadName = Thread.currentThread().getName();
        final String shortName = threadName.startsWith("OkHttp") ? "OkHttp" : threadName;

        //callerinfo
        if (SETTING_ADD_CLASSINFO[level.ordinal()]) {
            return "[" + shortName + "]{" + getCallerInfo() + "} " + msg;
        }
        return "[" + shortName + "] " + msg;


    }

    public static void v(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.VERBOSE.ordinal()]) {
            android.util.Log.v(TAG, adjustMessage(msg, LogLevel.VERBOSE));
        }
    }

    public static void v(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.VERBOSE.ordinal()]) {
            android.util.Log.v(TAG, adjustMessage(msg, LogLevel.VERBOSE), t);
        }
    }

    public static void d(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.DEBUG.ordinal()]) {
            android.util.Log.d(TAG, adjustMessage(msg, LogLevel.DEBUG));
        }
    }

    public static void d(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.DEBUG.ordinal()]) {
            android.util.Log.d(TAG, adjustMessage(msg, LogLevel.DEBUG), t);
        }
    }

    public static void i(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.INFO.ordinal()]) {
            android.util.Log.i(TAG, adjustMessage(msg, LogLevel.INFO));
        }
    }

    public static void i(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.INFO.ordinal()]) {
            android.util.Log.i(TAG, adjustMessage(msg, LogLevel.INFO), t);
        }
    }

    public static void w(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.WARN.ordinal()]) {
            android.util.Log.w(TAG, adjustMessage(msg, LogLevel.WARN));
        }
    }

    public static void w(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.WARN.ordinal()]) {
            android.util.Log.w(TAG, adjustMessage(msg, LogLevel.WARN), t);
        }
    }

    public static void e(final String msg) {
        if (SETTING_DO_LOGGING[LogLevel.ERROR.ordinal()]) {
            android.util.Log.e(TAG, adjustMessage(msg, LogLevel.ERROR));
            if (settingThrowExceptionOnError) {
                throw new RuntimeException("Aborting on Log.e()");
            }
        }
    }

    public static void e(final String msg, final Throwable t) {
        if (SETTING_DO_LOGGING[LogLevel.ERROR.ordinal()]) {
            android.util.Log.e(TAG, adjustMessage(msg, LogLevel.ERROR), t);
            if (settingThrowExceptionOnError) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new RuntimeException("Aborting on Log.e()", t);
            }
        }
    }

    /**
     * Returns compact info about caller of Log method. Note: this method is considerably slow
     * and shall be used with care (only when explicitely turned on)
     */
    private static String getCallerInfo() {
        final String logClassName = Log.class.getName();
        for (final StackTraceElement st : new RuntimeException().getStackTrace()) {
            if (!st.getClassName().equals(logClassName)) {
                String shortClassName = st.getClassName();
                final int idx = shortClassName.lastIndexOf(".");
                if (idx >= 0) {
                    shortClassName = shortClassName.substring(idx + 1);
                }
                return shortClassName + "." + st.getMethodName() + ":" + st.getLineNumber();
            }
        }
        return "<none>";
    }

}
