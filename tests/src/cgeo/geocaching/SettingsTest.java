package cgeo.geocaching;

import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

@TargetApi(8)
public class SettingsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public SettingsTest() {
        super(MainActivity.class);
    }

    /**
     * access settings.
     * this should work fine without an exception (once there was an exception because of the empty map file string)
     */
    public static void testSettingsException() {
        final String mapFile = OldSettings.getMapFile();
        // We just want to ensure that it does not throw any exception but we do not know anything about the result
        MapsforgeMapProvider.isValidMapFile(mapFile);
        assertTrue(true);
    }

    public static void testSettings() {
        // unfortunately, several other tests depend on being a premium member and will fail if run by a basic member
        assertEquals(GCConstants.MEMBER_STATUS_PM, OldSettings.getMemberStatus());
    }

    public static void testDeviceHasNormalLogin() {
        // if the unit tests were interrupted in a previous run, the device might still have the "temporary" login data from the last tests
        assertFalse("c:geo".equals(OldSettings.getUsername()));
    }
}
