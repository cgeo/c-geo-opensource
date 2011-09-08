package cgeo.geocaching.maps.google;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import cgeo.geocaching.cgCoord;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;

public class googleCacheOverlayItem extends OverlayItem implements CachesOverlayItemImpl {
	private String cacheType = null;
	private cgCoord coord;

	public googleCacheOverlayItem(cgCoord coordinate, String type) {
		super(new GeoPoint((int)(coordinate.latitude * 1e6), (int)(coordinate.longitude * 1e6)), coordinate.name, "");

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
