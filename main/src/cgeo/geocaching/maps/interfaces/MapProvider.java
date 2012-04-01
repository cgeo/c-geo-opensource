package cgeo.geocaching.maps.interfaces;

import cgeo.geocaching.IWaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.go4cache.Go4CacheUser;

import android.app.Activity;
import android.content.Context;

import java.util.Map;

/**
 * Defines functions of a factory class to get implementation specific objects
 * (GeoPoints, OverlayItems, ...)
 *
 * @author rsudev
 *
 */
public interface MapProvider {

    public Map<Integer, String> getMapSources();

    public boolean isMySource(int sourceId);

    public Class<? extends Activity> getMapClass();

    public int getMapViewId();

    public int getMapLayoutId();

    public GeoPointImpl getGeoPointBase(final Geopoint coords);

    public CachesOverlayItemImpl getCachesOverlayItem(final IWaypoint iWaypoint, final CacheType type);

    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context,
            Go4CacheUser userOne);

}
