package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class CacheSizeTest extends AndroidTestCase {
    public static void testOrder() {
        assertTrue(CacheSize.MICRO.comparable < CacheSize.SMALL.comparable);
        assertTrue(CacheSize.SMALL.comparable < CacheSize.REGULAR.comparable);
        assertTrue(CacheSize.REGULAR.comparable < CacheSize.LARGE.comparable);
    }
}
