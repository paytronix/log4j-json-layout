package net.logstash.log4j;

import junit.framework.Assert;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.log4j.*;
import org.apache.log4j.or.ObjectRenderer;
import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jvincent
 * Date: 12/5/12
 * Time: 12:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONEventLayoutV1Test {
    static Logger logger;
    static MockAppenderV1 appender;
    static MockAppenderV1 userFieldsAppender;
    static JSONEventLayoutV1 userFieldsLayout;
    static final String userFieldsSingle = new String("field1:value1");
    static final String userFieldsMulti = new String("field2:value2,field3:value3");
    static final String userFieldsSingleProperty = new String("field1:propval1");

    static final String[] logstashFields = new String[]{
            "message",
            "source_host",
            "timestamp"
    };

    @BeforeClass
    public static void setupTestAppender() {
        appender = new MockAppenderV1(new JSONEventLayoutV1());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappenderv1");
        appender.activateOptions();
        logger.addAppender(appender);
    }

    @After
    public void clearTestAppender() {
        NDC.clear();
        appender.clear();
        appender.close();
    }

    @Test
    public void testJSONEventLayoutIsJSON() {
        logger.info("this is an info message");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
    }

    @Test
    public void testJSONEventLayoutHasUserFieldsFromConfig() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsSingle);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
        Assert.assertEquals("Event does not contain value 'value1'", "value1", jsonObject.get("field1"));

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutUserFieldsMulti() {
        JSONEventLayoutV1 layout = (JSONEventLayoutV1) appender.getLayout();
        String prevUserData = layout.getUserFields();
        layout.setUserFields(userFieldsMulti);

        logger.info("this is an info message with user fields");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertTrue("Event does not contain field 'field2'" , jsonObject.containsKey("field2"));
        Assert.assertEquals("Event does not contain value 'value2'", "value2", jsonObject.get("field2"));
        Assert.assertTrue("Event does not contain field 'field3'" , jsonObject.containsKey("field3"));
        Assert.assertEquals("Event does not contain value 'value3'", "value3", jsonObject.get("field3"));

        layout.setUserFields(prevUserData);
    }

    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        for (String fieldName : logstashFields) {
            Assert.assertTrue("Event does not contain field: " + fieldName, jsonObject.containsKey(fieldName));
        }
    }

    @Test
    public void testJSONEventLayoutHasMDC() {
        MDC.put("foo", "bar");
        logger.warn("I should have MDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject mdc = (JSONObject) jsonObject.get("mdc");

        Assert.assertEquals("MDC is wrong","bar", mdc.get("foo"));
    }

    @Test
    public void testJSONEventLayoutHasNestedMDC() {
        HashMap nestedMdc = new HashMap<String, String>();
        nestedMdc.put("bar","baz");
        MDC.put("foo",nestedMdc);
        logger.warn("I should have nested MDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject mdc = (JSONObject) jsonObject.get("mdc");
        JSONObject nested = (JSONObject) mdc.get("foo");

        Assert.assertTrue("Event is missing foo key", mdc.containsKey("foo"));
        Assert.assertEquals("Nested MDC data is wrong", "baz", nested.get("bar"));
    }

    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = new String("shits on fire, yo");
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        Assert.assertEquals("Exception class missing", "java.lang.IllegalArgumentException", jsonObject.get("exception_class"));
        Assert.assertEquals("Exception exception message", exceptionMessage, jsonObject.get("exception_message"));
    }

    @Test
    public void testJSONEventHasLoggerName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertNotNull("LoggerName value is missing", jsonObject.get("logger_name"));
    }

    @Test
    public void testJSONEventHasThreadName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertNotNull("ThreadName value is missing", jsonObject.get("thread_name"));
    }

    @Test
    public void testDateFormat() {
        long timestamp = 1364844991207L;
        Assert.assertEquals("format does not produce expected output", "2013-04-01 15:36:31.207", JSONEventLayoutV1.dateFormat(timestamp));
    }
}
