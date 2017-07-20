package net.logstash.log4j;

import net.logstash.log4j.data.HostData;
import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class JSONEventLayoutV1 extends Layout {

    private boolean locationInfo = false;
    private String customUserFields;

    private boolean ignoreThrowable = false;

    private boolean activeIgnoreThrowable = ignoreThrowable;
    private String hostname = new HostData().getHostName();
    private String threadName;
    private long timestamp;
    private Map mdc;
    private HashMap<String, Object> exceptionInformation;


    private JSONObject logstashEvent;

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");
    public static final String ADDITIONAL_DATA_PROPERTY = "net.logstash.log4j.JSONEventLayoutV1.UserFields";

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONEventLayoutV1() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONEventLayoutV1(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String format(LoggingEvent loggingEvent) {
        threadName = loggingEvent.getThreadName();
        timestamp = loggingEvent.getTimeStamp();
        exceptionInformation = new HashMap<String, Object>();
        mdc = loggingEvent.getProperties();

        logstashEvent = new JSONObject();
        String whoami = this.getClass().getSimpleName();

        /**
         * All v1 of the event format requires is
         * "@timestamp" and "@version"
         * Every other field is arbitrary
         */
        logstashEvent.put("timestamp", dateFormat(timestamp));

        /**
         * Extract and add fields from log4j config, if defined
         */
        if (getUserFields() != null) {
            String userFlds = getUserFields();
            addUserFields(userFlds);
        }

        /**
         * Now we start injecting our own stuff.
         */
        logstashEvent.put("source_host", hostname);
        logstashEvent.put("message", loggingEvent.getRenderedMessage());

        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if (throwableInformation.getThrowable().getClass().getCanonicalName() != null) {
                addEventData("exception_class", throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                addEventData("exception_message", throwableInformation.getThrowable().getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(), "\n");
                addEventData("stacktrace", stackTrace);
            }
        }

        addEventData("logger_name", loggingEvent.getLoggerName());
        addEventData("mdc", mdc);
        addEventData("level", loggingEvent.getLevel().toString());
        addEventData("thread_name", threadName);

        return logstashEvent.toString() + "\n";
    }

    public boolean ignoresThrowable() {
        return ignoreThrowable;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String getUserFields() { return customUserFields; }
    public void setUserFields(String userFields) { this.customUserFields = userFields; }

    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }

    private void addUserFields(String data) {
        if (null != data) {
            String[] pairs = data.split(",");
            for (String pair : pairs) {
                String[] userField = pair.split(":", 2);
                if (userField[0] != null) {
                    String key = userField[0];
                    String val = userField[1];
                    addEventData(key, val);
                }
            }
        }
    }
    private void addEventData(String keyname, Object keyval) {
        if (null != keyval) {
            logstashEvent.put(keyname, keyval);
        }
    }
}
