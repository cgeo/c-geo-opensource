package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.CancellableHandler;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.Date;

import junit.framework.Assert;

public class cgBaseTest extends AndroidTestCase {

    public static void testReplaceWhitespaces() {
        Assert.assertEquals("foo bar baz ", BaseUtils.replaceWhitespace(new String("  foo\n\tbar   \r   baz  ")));
    }

    public static void testElevation() {
        Assert.assertEquals(125.663703918457, cgBase.getElevation(new Geopoint(48.0, 2.0)), 0.1);
    }

    public static void testCompareCaches(ICache expected, cgCache actual) {
        Assert.assertEquals(expected.getGeocode(), actual.getGeocode());
        Assert.assertTrue(expected.getType() == actual.getType());
        Assert.assertEquals(expected.getOwner(), actual.getOwner());
        Assert.assertEquals(expected.getDifficulty(), actual.getDifficulty());
        Assert.assertEquals(expected.getTerrain(), actual.getTerrain());
        Assert.assertEquals(expected.getLatitude(), actual.getLatitude());
        Assert.assertEquals(expected.getLongitude(), actual.getLongitude());
        assertTrue(actual.isReliableLatLon());
        Assert.assertEquals(expected.isDisabled(), actual.isDisabled());
        Assert.assertEquals(expected.isOwn(), actual.isOwn());
        Assert.assertEquals(expected.isArchived(), actual.isArchived());
        Assert.assertEquals(expected.isMembersOnly(), actual.isMembersOnly());
        Assert.assertEquals(expected.getOwnerReal(), actual.getOwnerReal());
        Assert.assertEquals(expected.getSize(), actual.getSize());
        Assert.assertEquals(expected.getHint(), actual.getHint());
        Assert.assertTrue(actual.getDescription().startsWith(expected.getDescription()));
        Assert.assertEquals(expected.getShortDescription(), actual.getShortDescription());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getCacheId(), actual.getCacheId());
        Assert.assertEquals(expected.getGuid(), actual.getGuid());
        Assert.assertEquals(expected.getLocation(), actual.getLocation());
        Assert.assertEquals(expected.getPersonalNote(), actual.getPersonalNote());
        Assert.assertEquals(expected.isFound(), actual.isFound());
        Assert.assertEquals(expected.isFavorite(), actual.isFavorite());
        Assert.assertEquals(expected.getFavoritePoints(), actual.getFavoritePoints());
        Assert.assertEquals(expected.isWatchlist(), actual.isWatchlist());
        Date date1 = expected.getHiddenDate();
        Date date2 = actual.getHiddenDate();
        Assert.assertEquals(date1.toString(), date2.toString());
        for (String attribute : expected.getAttributes()) {
            Assert.assertTrue(actual.getAttributes().contains(attribute));
        }
        for (LogType logType : expected.getLogCounts().keySet()) {
            Assert.assertTrue(actual.getLogCounts().get(logType) >= expected.getLogCounts().get(logType));
        }

        int actualInventorySize = null != actual.getInventory() ? actual.getInventory().size() : 0;
        int expectedInventorySize = null != expected.getInventory() ? expected.getInventory().size() : 0;
        Assert.assertEquals(expectedInventorySize, actualInventorySize);

        int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
        int expectedSpoilersSize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
        Assert.assertEquals(expectedSpoilersSize, actualSpoilersSize);
    }

    /**
     * Test {@link cgBase#parseCacheFromText(String, int, CancellableHandler)} with "mocked" data
     *
     */
    @MediumTest
    public static void testParseCacheFromTextWithMockedData() {
        String gcCustomDate = Settings.getGcCustomDate();
        for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
            // to get the same results we have to use the date format used when the mocked data was created
            Settings.setGcCustomDate(mockedCache.getDateFormat());
            ParseResult parseResult = cgBase.parseCacheFromText(mockedCache.getData(), 0, null);
            cgCache parsedCache = parseResult.cacheList.get(0);
            cgBaseTest.testCompareCaches(mockedCache, parsedCache);
        }
        Settings.setGcCustomDate(gcCustomDate);
    }

    public static void testHumanDistance() {
        assertEquals("?", cgBase.getHumanDistance(null));
        assertEquals("123 km", cgBase.getHumanDistance(123.456f));
    }
}