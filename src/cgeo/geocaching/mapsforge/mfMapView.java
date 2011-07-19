package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapDatabase;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;
import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.Projection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapOverlay;
import cgeo.geocaching.mapcommon.cgUsersOverlay;
import cgeo.geocaching.mapinterfaces.GeoPointImpl;
import cgeo.geocaching.mapinterfaces.MapControllerImpl;
import cgeo.geocaching.mapinterfaces.MapProjectionImpl;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OverlayImpl;

public class mfMapView extends MapView implements MapViewImpl {

	public mfMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void draw(Canvas canvas) {
		try {
			if (getMapZoomLevel() >= 22) { // to avoid too close zoom level (mostly on Samsung Galaxy S series)
				getController().setZoom(22);
			}

			super.draw(canvas);
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgMapView.draw: " + e.toString());
		}
	}

	@Override
	public void displayZoomControls(boolean takeFocus) {
		// nothing to do here
	}

	@Override
	public MapControllerImpl getMapController() {
		return new mfMapController(getController());
	}

	@Override
	public GeoPointImpl getMapViewCenter() {
		GeoPoint point = getMapCenter();
		return new mfGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
	}

	@Override
	public void addOverlay(OverlayImpl ovl) {
		getOverlays().add((Overlay)ovl);
	}

	@Override
	public void clearOverlays() {
		getOverlays().clear();
	}

	@Override
	public MapProjectionImpl getMapProjection() {
		return new mfMapProjection(getProjection());
	}

	@Override
	public cgMapOverlay createAddMapOverlay(cgSettings settings,
			Context context, Drawable drawable, boolean fromDetailIntent) {
		
		mfCacheOverlay ovl = new mfCacheOverlay(settings, context, drawable, fromDetailIntent);
		getOverlays().add(ovl);
		return ovl.getBase();
	}
	
	@Override
	public cgUsersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
		mfUsersOverlay ovl = new mfUsersOverlay(context, markerIn);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public int getLatitudeSpan() {

		Projection projection = getProjection();
		
		GeoPoint low = projection.fromPixels(0, 0);
		GeoPoint high = projection.fromPixels(0, getHeight());

		return Math.abs(high.getLatitudeE6() - low.getLatitudeE6());
	}

	@Override
	public int getLongitudeSpan() {
		Projection projection = getProjection();
		
		GeoPoint low = projection.fromPixels(0, 0);
		GeoPoint high = projection.fromPixels(getWidth(), 0);

		return Math.abs(high.getLongitudeE6() - low.getLongitudeE6());
	}

	@Override
	public boolean isSatellite() {
		return false;
	}

	@Override
	public void preLoad() {
		// Nothing to do here
	}

	@Override
	public void setSatellite(boolean b) {
		// Nothing to do here
	}

	@Override
	public int getMapZoomLevel() {
		return getZoomLevel();
	}

	@Override
	public boolean needsScaleOverlay() {
		return false;
	}

	@Override
	public void setBuiltinScale(boolean b) {
		setScaleBar(b);
	}
	
	/**
	 * Checks whether a given file is a valid map file.
	 * 
	 * @param file
	 *            the path to the map file that should be tested.
	 * @return true if the file is a valid map file, false otherwise.
	 */
	public static boolean isValidMapFile(String file) {
		return MapDatabase.isValidMapFile(file);
	}


	@Override
	public void setMapSource(cgSettings settings) {

		setMapViewMode(MapViewMode.MAPNIK_TILE_DOWNLOAD);
		
		switch(settings.mapProvider) {
			case mapsforgeMapnik:
				// is default
				break;
			case mapsforgeOsmarender:
				setMapViewMode(MapViewMode.OSMARENDER_TILE_DOWNLOAD);
				break;
			case mapsforgeOffline:
				if (isValidMapFile(settings.getMapFile())) {
					setMapViewMode(MapViewMode.CANVAS_RENDERER);
					super.setMapFile(settings.getMapFile());
				}
		}
	}
}
