package cgeo.geocaching.maps.google;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class GoogleCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
    private String cacheType = null;
    private cgCoord coord;

    public GoogleCacheOverlayItem(cgCoord coordinate, String type) {
        super(new GeoPoint(coordinate.coords.getLatitudeE6(), coordinate.coords.getLongitudeE6()), coordinate.name, "");

        this.cacheType = type;
        this.coord = coordinate;
    }

    public cgCoord getCoord() {
        return coord;
    }

    public String getType() {
        return cacheType;
    }

}
