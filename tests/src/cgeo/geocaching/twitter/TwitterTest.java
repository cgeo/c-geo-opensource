package cgeo.geocaching.twitter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class TwitterTest extends TestCase {

    public static void testTrackableMessage() {
        Trackable tb = new Trackable();
        tb.setName("Travel bug");
        tb.setGeocode("TB1234");
        assertEquals("I touched Travel bug (http://www.geocaching.com//track/details.aspx?tracker=TB1234)! #cgeo #geocaching", Twitter.getStatusMessage(tb));
    }

    public static void testCacheMessage() {
        Geocache cache = new Geocache();
        cache.setGeocode("GC1234");
        cache.setName("TwitterTest");
        assertEquals("I found TwitterTest (http://coord.info/GC1234) #cgeo #geocaching", Twitter.getStatusMessage(cache));
    }

    public static void testAvoidDuplicateTags() {
        String oldMessage = OldSettings.getCacheTwitterMessage();
        try {
            Geocache cache = new Geocache();
            cache.setGeocode("GC1234");
            cache.setName("TwitterTest");
            OldSettings.setCacheTwitterMessage("[NAME] #cgeo");
            assertEquals("TwitterTest #cgeo #geocaching", Twitter.getStatusMessage(cache));
        } finally {
            OldSettings.setCacheTwitterMessage(oldMessage);
        }
    }
}
