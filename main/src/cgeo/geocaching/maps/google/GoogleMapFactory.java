package cgeo.geocaching.maps.google;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapFactory;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import com.google.android.maps.MapActivity;

import android.content.Context;

public class GoogleMapFactory implements MapFactory {

    @Override
    public Class<? extends MapActivity> getMapClass() {
        return GoogleMapActivity.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.map;
    }

    @Override
    public int getMapLayoutId() {
        return R.layout.map_google;
    }

    @Override
    public GeoPointImpl getGeoPointBase(final Geopoint coords) {
        return new GoogleGeoPoint(coords.getLatitudeE6(), coords.getLongitudeE6());
    }

    @Override
    public CachesOverlayItemImpl getCachesOverlayItem(cgCoord coordinate, String type) {
        GoogleCacheOverlayItem baseItem = new GoogleCacheOverlayItem(coordinate, type);
        return baseItem;
    }

    @Override
    public OtherCachersOverlayItemImpl getOtherCachersOverlayItemBase(Context context, cgUser userOne) {
        GoogleOtherCachersOverlayItem baseItem = new GoogleOtherCachersOverlayItem(context, userOne);
        return baseItem;
    }

}
