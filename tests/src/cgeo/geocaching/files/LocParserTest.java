package cgeo.geocaching.files;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.R;

import android.content.res.Resources;
import android.os.Handler;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocParserTest extends InstrumentationTestCase {
    private List<cgCache> readLoc(int resourceId) throws IOException, ParserException {
        LocParser parser = new LocParser(1);
        Collection<cgCache> caches = null;
        final Resources res = getInstrumentation().getContext().getResources();
        final InputStream instream = res.openRawResource(resourceId);
        try {
            caches = parser.parse(instream, new Handler());
            assertNotNull(caches);
            assertTrue(caches.size() > 0);
        } finally {
            instream.close();
        }

        List<cgCache> cacheList = new ArrayList<cgCache>(caches);
        // TODO: may need to sort by geocode when a test imports more than one cache
        return cacheList;
    }

    public void testOCLoc() throws IOException, ParserException {
        final List<cgCache> caches = readLoc(R.raw.oc5952_loc);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);
        assertNotNull(cache);
        assertEquals("OC5952", cache.geocode);
        assertEquals("Die Schatzinsel / treasure island", cache.name);
        assertTrue(new Geopoint(48.85968, 9.18740).isEqualTo(cache.coords));
    }

    public void testGCLoc() throws IOException, ParserException {
        final List<cgCache> caches = readLoc(R.raw.gc1bkp3_loc);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);
        assertNotNull(cache);
        assertEquals("GC1BKP3", cache.geocode);
        assertEquals("Die Schatzinsel / treasure island", cache.name);
        assertTrue(new Geopoint(48.859683, 9.1874).isEqualTo(cache.coords));
        assertEquals(1.0f, cache.difficulty.floatValue());
        assertEquals(5.0f, cache.terrain.floatValue());
        assertEquals(CacheSize.MICRO, cache.size);
    }
}
