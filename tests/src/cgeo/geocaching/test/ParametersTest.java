package cgeo.geocaching.test;

import cgeo.geocaching.Parameters;

import android.test.AndroidTestCase;

import java.security.InvalidParameterException;

import junit.framework.Assert;

@SuppressWarnings("static-method")
public class ParametersTest extends AndroidTestCase {

    public void testException() {
        try {
            @SuppressWarnings("unused")
            final Parameters params = new Parameters("aaa", "AAA", "bbb");
            Assert.fail("Exception not raised");
        } catch (InvalidParameterException e) {
            // Ok
        }
        try {
            final Parameters params = new Parameters("aaa", "AAA");
            params.put("bbb", "BBB", "ccc");
            Assert.fail("Exception not raised");
        } catch (InvalidParameterException e) {
            // Ok
        }
    }

    public void testMultipleValues() {
        final Parameters params = new Parameters("aaa", "AAA", "bbb", "BBB");
        params.put("ccc", "CCC", "ddd", "DDD");
        Assert.assertEquals("aaa=AAA&bbb=BBB&ccc=CCC&ddd=DDD", params.toString());
    }

    public void testSort() {
        final Parameters params = new Parameters();
        params.put("aaa", "AAA");
        params.put("ccc", "CCC");
        params.put("bbb", "BBB");
        Assert.assertEquals("aaa=AAA&ccc=CCC&bbb=BBB", params.toString());
        params.sort();
        Assert.assertEquals("aaa=AAA&bbb=BBB&ccc=CCC", params.toString());
    }

    public void testToString() {
        final Parameters params = new Parameters();
        params.put("name", "foo&bar");
        params.put("type", "moving");
        Assert.assertEquals("name=foo%26bar&type=moving", params.toString());
    }

}
