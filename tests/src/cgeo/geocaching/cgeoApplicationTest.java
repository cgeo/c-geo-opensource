package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCBase;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.network.Login;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.GC2JVEH;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.tuple.ImmutablePair;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.Date;

import junit.framework.Assert;

/**
 * The c:geo application test. It can be used for tests that require an
 * application and/or context.
 */

public class cgeoApplicationTest extends ApplicationTestCase<cgeoapplication> {

    public cgeoApplicationTest() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init environment
        createApplication();
        cgBase.initialize(getApplication());
    }

    /**
     * The name 'test preconditions' is a convention to signal that if this test
     * doesn't pass, the test case was not set up properly and it might explain
     * any and all failures in other tests. This is not guaranteed to run before
     * other tests, as junit uses reflection to find the tests.
     */
    @SuppressWarnings("static-method")
    @SmallTest
    public void testPreconditions() {
        assertEquals(StatusCode.NO_ERROR, Login.login());
    }

    /**
     * Test {@link cgBase#searchTrackable(String, String, String)}
     */
    @MediumTest
    public static void testSearchTrackableNotExisting() {
        cgTrackable tb = cgBase.searchTrackable("123456", null, null);
        assertNotNull(tb);
    }

    /**
     * Test {@link cgBase#searchTrackable(String, String, String)}
     */
    @MediumTest
    public static void testSearchTrackable() {
        cgTrackable tb = cgBase.searchTrackable("TB2J1VZ", null, null);
        // fix data
        assertEquals("aefffb86-099f-444f-b132-605436163aa8", tb.getGuid());
        assertEquals("TB2J1VZ", tb.getGeocode());
        assertEquals("http://www.geocaching.com/images/wpttypes/21.gif", tb.getIconUrl());
        assertEquals("blafoo's Children Music CD", tb.getName());
        assertEquals("Travel Bug Dog Tag", tb.getType());
        assertEquals(new Date(2009 - 1900, 8 - 1, 24), tb.getReleased());
        assertEquals("Niedersachsen, Germany", tb.getOrigin());
        assertEquals("blafoo", tb.getOwner());
        assertEquals("0564a940-8311-40ee-8e76-7e91b2cf6284", tb.getOwnerGuid());
        assertEquals("Kinder erfreuen.<br /><br />Make children happy.", tb.getGoal());
        assertTrue(tb.getDetails().startsWith("Auf der CD sind"));
        assertEquals("http://img.geocaching.com/track/display/38382780-87a7-4393-8393-78841678ee8c.jpg", tb.getImage());
        // Following data can change over time
        assertTrue(tb.getDistance() >= 10617.8f);
        assertTrue(tb.getLogs().size() >= 10);
        assertTrue(cgTrackable.SPOTTED_CACHE == tb.getSpottedType() || cgTrackable.SPOTTED_USER == tb.getSpottedType());
        // no assumption possible: assertEquals("faa2d47d-19ea-422f-bec8-318fc82c8063", tb.getSpottedGuid());
        // no assumption possible: assertEquals("Nice place for a break cache", tb.getSpottedName());
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static cgCache testSearchByGeocode(final String geocode) {
        final SearchResult search = cgBase.searchByGeocode(geocode, null, 0, true, null);
        assertNotNull(search);
        if (Settings.isPremiumMember() || search.error == null) {
            assertEquals(1, search.getGeocodes().size());
            assertTrue(search.getGeocodes().contains(geocode));
            return cgeoapplication.getInstance().loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        assertEquals(0, search.getGeocodes().size());
        return null;
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByGeocodeNotExisting() {
        final SearchResult search = cgBase.searchByGeocode("GC123456", null, 0, true, null);
        assertNotNull(search);
        assertEquals(search.error, StatusCode.UNPUBLISHED_CACHE);
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByGeocodeNotLoggedIn() {
        ImmutablePair<String, String> login = Settings.getLogin();
        String memberStatus = Settings.getMemberStatus();

        try {
            // non premium cache
            MockedCache cache = new GC2CJPF();

            deleteCacheFromDBAndLogout(cache.getGeocode());

            SearchResult search = cgBase.searchByGeocode(cache.getGeocode(), null, StoredList.TEMPORARY_LIST_ID, true, null);
            assertNotNull(search);
            assertEquals(1, search.getGeocodes().size());
            assertTrue(search.getGeocodes().contains(cache.getGeocode()));
            cgCache searchedCache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            // coords must be null if the user is not logged in
            assertNull(searchedCache.getCoords());

            // premium cache. Not visible to guests
            cache = new GC2JVEH();

            deleteCacheFromDBAndLogout(cache.getGeocode());

            search = cgBase.searchByGeocode(cache.getGeocode(), null, StoredList.TEMPORARY_LIST_ID, true, null);
            assertNotNull(search);
            assertEquals(0, search.getGeocodes().size());

        } finally {
            // restore user and password
            Settings.setLogin(login.left, login.right);
            Settings.setMemberStatus(memberStatus);
            Login.login();
        }
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchErrorOccured() {
        ImmutablePair<String, String> login = Settings.getLogin();
        String memberStatus = Settings.getMemberStatus();

        try {
            // non premium cache
            MockedCache cache = new GC1ZXX2();

            deleteCacheFromDBAndLogout(cache.getGeocode());

            SearchResult search = cgBase.searchByGeocode(cache.getGeocode(), null, StoredList.TEMPORARY_LIST_ID, true, null);
            assertNotNull(search);
            assertEquals(0, search.getGeocodes().size());

        } finally {
            // restore user and password
            Settings.setLogin(login.left, login.right);
            Settings.setMemberStatus(memberStatus);
            Login.login();
        }
    }

    /**
     * Test {@link cgBase#searchByCoords(cgSearchThread, Geopoint, String, int, boolean)}
     */
    @MediumTest
    public static void testSearchByCoords() {
        final SearchResult search = cgBase.searchByCoords(null, new Geopoint("N 52° 24.972 E 009° 35.647"), CacheType.MYSTERY, false);
        assertNotNull(search);
        assertTrue(18 <= search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC1RMM2"));
    }

    /**
     * Test {@link cgBase#searchByOwner(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByOwner() {
        final SearchResult search = cgBase.searchByOwner(null, "blafoo", CacheType.MYSTERY, false);
        assertNotNull(search);
        assertEquals(3, search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC36RT6"));
    }

    /**
     * Test {@link cgBase#searchByUsername(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByUsername() {
        final SearchResult search = cgBase.searchByUsername(null, "blafoo", CacheType.WEBCAM, false);
        assertNotNull(search);
        assertEquals(3, search.totalCnt);
        assertTrue(search.getGeocodes().contains("GCP0A9"));
    }

    /**
     * Test {@link cgBase#searchByViewport(String, Viewport)}
     */
    @MediumTest
    public static void testSearchByViewport() {

        Strategy strategy = Settings.getLiveMapStrategy();

        try {
            GC2CJPF mockedCache = new GC2CJPF();
            deleteCacheFromDB(mockedCache.getGeocode());

            final String tokens[] = GCBase.getTokens();
            final Viewport viewport = new Viewport(mockedCache.getCoords(), 0.003, 0.003);

            // check coords for DETAILED
            Settings.setLiveMapStrategy(Strategy.DETAILED);
            SearchResult search = ConnectorFactory.searchByViewport(viewport, tokens);
            assertNotNull(search);
            assertTrue(search.getGeocodes().contains(mockedCache.getGeocode()));

            cgCache parsedCache = cgeoapplication.getInstance().loadCache(mockedCache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);

            assertEquals(Settings.isPremiumMember(), mockedCache.getCoords().isEqualTo(parsedCache.getCoords()));
            assertEquals(Settings.isPremiumMember(), parsedCache.isReliableLatLon());

            // check update after switch strategy to FAST
            Settings.setLiveMapStrategy(Strategy.FAST);
            GCBase.removeFromTileCache(mockedCache.getCoords());
            search = ConnectorFactory.searchByViewport(viewport, tokens);
            assertNotNull(search);
            assertTrue(search.getGeocodes().contains(mockedCache.getGeocode()));

            parsedCache = cgeoapplication.getInstance().loadCache(mockedCache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);

            assertEquals(Settings.isPremiumMember(), mockedCache.getCoords().isEqualTo(parsedCache.getCoords()));
            assertEquals(Settings.isPremiumMember(), parsedCache.isReliableLatLon());

        } finally {
            Settings.setLiveMapStrategy(strategy);
        }
    }

    /**
     * Test {@link cgBase#searchByViewport(String, Viewport)}
     */
    @MediumTest
    public static void testSearchByViewportNotLoggedIn() {

        ImmutablePair<String, String> login = Settings.getLogin();
        String memberStatus = Settings.getMemberStatus();
        Strategy strategy = Settings.getLiveMapStrategy();
        Strategy testStrategy = Strategy.FAST; // FASTEST, FAST or DETAILED for tests
        Settings.setLiveMapStrategy(testStrategy);

        try {

            final String tokens[] = null; // without a valid token we are "logged off"

            // non premium cache
            MockedCache cache = new GC2CJPF();
            deleteCacheFromDBAndLogout(cache.getGeocode());

            Viewport viewport = new Viewport(cache.getCoords(), 0.003, 0.003);
            GCBase.removeFromTileCache(cache.getCoords());
            SearchResult search = ConnectorFactory.searchByViewport(viewport, tokens);

            assertNotNull(search);
            assertTrue(search.getGeocodes().contains(cache.getGeocode()));
            // coords differ
            cgCache cacheFromViewport = cgeoapplication.getInstance().loadCache(cache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            Log.d(Settings.tag, "cgeoApplicationTest.testSearchByViewportNotLoggedIn: Coords expected = " + cache.getCoords());
            Log.d(Settings.tag, "cgeoApplicationTest.testSearchByViewportNotLoggedIn: Coords actual = " + cacheFromViewport.getCoords());
            assertFalse(cache.getCoords().isEqualTo(cacheFromViewport.getCoords(), 1e-3));
            // depending on the chosen strategy the coords can be reliable or not
            assertEquals(testStrategy == Strategy.DETAILED, cacheFromViewport.isReliableLatLon());

            // premium cache
            cache = new GC2JVEH();
            deleteCacheFromDBAndLogout(cache.getGeocode());

            viewport = new Viewport(cache.getCoords(), 0.003, 0.003);
            search = ConnectorFactory.searchByViewport(viewport, tokens);

            assertNotNull(search);
            // depending on the chosen strategy the cache is part of the search or not
            assertEquals(testStrategy == Strategy.DETAILED, search.getGeocodes().contains(cache.getGeocode()));

        } finally {
            // restore user and password
            Settings.setLogin(login.left, login.right);
            Settings.setMemberStatus(memberStatus);
            Login.login();
            Settings.setLiveMapStrategy(strategy);
        }
    }

    /**
     * Test cache parsing. Esp. useful after a GC.com update
     */
    public static void testSearchByGeocodeBasis() {
        for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
            mockedCache.setMockedDataUser(Settings.getUsername());
            cgCache parsedCache = cgeoApplicationTest.testSearchByGeocode(mockedCache.getGeocode());
            if (null != parsedCache) {
                cgBaseTest.testCompareCaches(mockedCache, parsedCache, true);
            }
        }
    }

    /**
     * Caches that are good test cases
     */
    public static void testSearchByGeocodeSpecialties() {
        cgCache GCV2R9 = cgeoApplicationTest.testSearchByGeocode("GCV2R9");
        Assert.assertEquals("California, United States", GCV2R9.getLocation());

        cgCache GC1ZXEZ = cgeoApplicationTest.testSearchByGeocode("GC1ZXEZ");
        Assert.assertEquals("Ms.Marple/Mr.Stringer", GC1ZXEZ.getOwnerReal());
    }

    /** Remove cache from DB and cache to ensure that the cache is not loaded from the database */
    private static void deleteCacheFromDB(String geocode) {
        cgeoapplication.getInstance().removeCache(geocode, LoadFlags.REMOVE_ALL);
    }

    /** Remove cache from DB and cache to ensure that the cache is not loaded from the database */
    private static void deleteCacheFromDBAndLogout(String geocode) {
        deleteCacheFromDB(geocode);

        Login.logout();
        // Modify login data to avoid an automatic login again
        Settings.setLogin("c:geo", "c:geo");
        Settings.setMemberStatus("Basic member");
    }

}

