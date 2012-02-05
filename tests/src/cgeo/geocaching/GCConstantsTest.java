package cgeo.geocaching;

import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;

import android.test.AndroidTestCase;

public class GCConstantsTest extends AndroidTestCase {
    public static void testLocation() {
        // GC37GFJ
        assertEquals("Bretagne, France", parseLocation("    <span id=\"ctl00_ContentBody_Location\">In Bretagne, France</span><br />"));
        // GCV2R9
        assertEquals("California, United States", parseLocation("<span id=\"ctl00_ContentBody_Location\">In <a href=\"/map/beta/default.aspx?lat=37.4354&lng=-122.07745&z=16\" title=\"View Map\">California, United States</a></span><br />"));
    }

    private static String parseLocation(final String html) {
        return BaseUtils.getMatch(html, GCConstants.PATTERN_LOCATION, true, "");
    }

    public static void testCacheCount() {
        assertCacheCount(149, "<strong><img src=\"/images/icons/icon_smile.png\" title=\"Caches Found\" /> 149</strong>");
        assertCacheCount(491, MockedCache.readCachePage("GC2CJPF"));
    }

    private static void assertCacheCount(final int count, final String html) {
        assertEquals(count, Integer.valueOf(BaseUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0")).intValue());
    }
}
