package cgeo.geocaching.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import cgeo.geocaching.ICache;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;

/**
 * The c:geo application test.
 * It can be used for tests that require an application and/or context.
 */

public class cgeoApplicationTest extends ApplicationTestCase<cgeoapplication> {
	
	private cgSettings settings = null;
	private cgBase base = null; 

    public cgeoApplicationTest() {
        super(cgeoapplication.class);
      }

      @Override
      protected void setUp() throws Exception {
          super.setUp();
          
       	  // init environment
    	  createApplication();
    	  Context context = this.getContext();
    	  SharedPreferences prefs = context.getSharedPreferences(cgSettings.preferences, Context.MODE_PRIVATE);
    	  
    	  // create required c:geo objects
    	  settings = new cgSettings(context, prefs);
    	  base = new cgBase(this.getApplication(), settings, prefs);
      }

      /**
       * The name 'test preconditions' is a convention to signal that if this
       * test doesn't pass, the test case was not set up properly and it might
       * explain any and all failures in other tests.  This is not guaranteed
       * to run before other tests, as junit uses reflection to find the tests.
       */
      @SmallTest
      public void testPreconditions() {
      }

      /**
       * Test {@link cgBase#searchByGeocode(HashMap, int, boolean)}
       * @param base
       */
      @MediumTest
      public void testSearchByGeocode() {
    	  HashMap<String, String> params = new HashMap<String, String>();
    	  params.put("geocode", "GC1RMM2");
    	  
    	  Long id = base.searchByGeocode(params, 0, true);
    	  Assert.assertNotNull(id);
      }
      
      /**
       * Test {@link cgBase#parseCache(String, int) with "mocked" data
       * @param base
       */
      @MediumTest
      public void testParseCache() {
    	  List<ICache> cachesToTest = new ArrayList<ICache>();
    	  cachesToTest.add(new GC2CJPF());
    	  cachesToTest.add(new GC1ZXX2());

    	  for (ICache cache : cachesToTest) {
	    	  cgCacheWrap caches = base.parseCache(cache.getData(),0);
	    	  cgCache cacheParsed = caches.cacheList.get(0);
	    	  Assert.assertEquals(cacheParsed.getGeocode(), cache.getGeocode());
	    	  Assert.assertEquals(cacheParsed.getType(), cache.getType());
	    	  Assert.assertEquals(cacheParsed.getOwner(), cache.getOwner());
	    	  Assert.assertEquals(cacheParsed.getDifficulty(), cache.getDifficulty());
	    	  Assert.assertEquals(cacheParsed.getTerrain(), cache.getTerrain());
	    	  Assert.assertEquals(cacheParsed.getLatitute(), cache.getLatitute());
	    	  Assert.assertEquals(cacheParsed.getLongitude(), cache.getLongitude());
	    	  Assert.assertEquals(cacheParsed.isDisabled(), cache.isDisabled());
	    	  Assert.assertEquals(cacheParsed.isOwn(), cache.isOwn());
	    	  Assert.assertEquals(cacheParsed.isArchived(), cache.isArchived());
	    	  Assert.assertEquals(cacheParsed.isMembersOnly(), cache.isMembersOnly());
	    	  Assert.assertEquals(cacheParsed.getOwnerReal(), cache.getOwnerReal());
	    	  Assert.assertEquals(cacheParsed.getSize(), cache.getSize());
	    	  Assert.assertEquals(cacheParsed.getHint(), cache.getHint());
	    	  Assert.assertTrue(cacheParsed.getDescription().startsWith(cache.getDescription()));
	    	  Assert.assertEquals(cacheParsed.getShortDescription(), cache.getShortDescription());
	    	  Assert.assertEquals(cacheParsed.getName(), cache.getName());
    	  }
      }

}