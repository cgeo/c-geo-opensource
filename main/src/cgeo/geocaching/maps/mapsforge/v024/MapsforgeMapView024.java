package cgeo.geocaching.maps.mapsforge.v024;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.OtherCachersOverlay;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl.overlayType;
import cgeo.geocaching.utils.Log;

import org.mapsforge.android.mapsold.GeoPoint;
import org.mapsforge.android.mapsold.MapDatabase;
import org.mapsforge.android.mapsold.MapView;
import org.mapsforge.android.mapsold.MapViewMode;
import org.mapsforge.android.mapsold.Overlay;
import org.mapsforge.android.mapsold.Projection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.ArrayList;
public class MapsforgeMapView024 extends MapView implements MapViewImpl {
    private GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;
    private final MapsforgeMapController mapController = new MapsforgeMapController(getController(), getMaxZoomLevel());

    public MapsforgeMapView024(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            // Google Maps and OSM Maps use different zoom levels for the same view.
            // Here we don't want the Google Maps compatible zoom level, but the actual one.
            if (getActualMapZoomLevel() > 22) { // to avoid too close zoom level (mostly on Samsung Galaxy S series)
                getController().setZoom(22);
            }

            super.draw(canvas);
        } catch (Exception e) {
            Log.e("MapsforgeMapView.draw: " + e.toString());
        }
    }

    @Override
    public void displayZoomControls(boolean takeFocus) {
        // nothing to do here
    }

    @Override
    public MapControllerImpl getMapController() {
        return mapController;
    }

    @Override
    public GeoPointImpl getMapViewCenter() {
        GeoPoint point = getMapCenter();
        return new MapsforgeGeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
    }

    @Override
    public Viewport getViewport() {
        return new Viewport(getMapViewCenter(), getLatitudeSpan() / 1e6, getLongitudeSpan() / 1e6);
    }

    @Override
    public void addOverlay(OverlayImpl ovl) {
        getOverlays().add((Overlay) ovl);
    }

    @Override
    public void clearOverlays() {
        getOverlays().clear();
    }

    @Override
    public MapProjectionImpl getMapProjection() {
        return new MapsforgeMapProjection(getProjection());
    }

    @Override
    public CachesOverlay createAddMapOverlay(Context context, Drawable drawable) {

        MapsforgeCacheOverlay ovl = new MapsforgeCacheOverlay(context, drawable);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
        MapsforgeOtherCachersOverlay ovl = new MapsforgeOtherCachersOverlay(context, markerIn);
        getOverlays().add(ovl);
        return ovl.getBase();
    }

    @Override
    public PositionOverlay createAddPositionOverlay(Activity activity) {
        MapsforgeOverlay ovl = new MapsforgeOverlay(activity, overlayType.PositionOverlay);
        getOverlays().add(ovl);
        return (PositionOverlay) ovl.getBase();
    }

    @Override
    public ScaleOverlay createAddScaleOverlay(Activity activity) {
        MapsforgeOverlay ovl = new MapsforgeOverlay(activity, overlayType.ScaleOverlay);
        getOverlays().add(ovl);
        return (ScaleOverlay) ovl.getBase();
    }

    @Override
    public int getLatitudeSpan() {

        int span = 0;

        Projection projection = getProjection();

        if (projection != null && getHeight() > 0) {

            GeoPoint low = projection.fromPixels(0, 0);
            GeoPoint high = projection.fromPixels(0, getHeight());

            if (low != null && high != null) {
                span = Math.abs(high.getLatitudeE6() - low.getLatitudeE6());
            }
        }

        return span;
    }

    @Override
    public int getLongitudeSpan() {

        int span = 0;

        Projection projection = getProjection();

        if (projection != null && getWidth() > 0) {
            GeoPoint low = projection.fromPixels(0, 0);
            GeoPoint high = projection.fromPixels(getWidth(), 0);

            if (low != null && high != null) {
                span = Math.abs(high.getLongitudeE6() - low.getLongitudeE6());
            }
        }

        return span;
    }

    @Override
    public void preLoad() {
        // Nothing to do here
    }

    /**
     * Get the map zoom level which is compatible with Google Maps.
     *
     * @return the current map zoom level +1
     */
    @Override
    public int getMapZoomLevel() {
        // Google Maps and OSM Maps use different zoom levels for the same view.
        // All OSM Maps zoom levels are offset by 1 so they match Google Maps.
        return getZoomLevel() + 1;
    }

    /**
     * Get the actual map zoom level
     *
     * @return the current map zoom level with no adjustments
     */
    private int getActualMapZoomLevel() {
        return getZoomLevel();
    }

    @Override
    public void setMapSource() {
        setMapViewMode(MapViewMode.CANVAS_RENDERER);
        setMapFile(Settings.getMapFile());
        if (!MapDatabase.isValidMapFile(Settings.getMapFile())) {
            Log.e("MapsforgeMapView024: Invalid map file");
        }
        Toast.makeText(
                getContext(),
                getContext().getResources().getString(R.string.warn_deprecated_mapfile),
                Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void repaintRequired(GeneralOverlay overlay) {

        if (null == overlay) {
            invalidate();
        } else {
            try {
                Overlay ovl = (Overlay) overlay.getOverlayImpl();

                if (ovl != null) {
                    ovl.requestRedraw();
                }

            } catch (Exception e) {
                Log.e("MapsforgeMapView.repaintRequired: " + e.toString());
            }
        }
    }

    @Override
    public void setOnDragListener(OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    @Override
    public boolean needsInvertedColors() {
        return false;
    }

    @Override
    public boolean isMapDatabaseSwitchSupported() {
        //Not supported so return false
        return false;
    }

    @Override
    public ArrayList<String> getMapDatabaseList() {
        //Not supported so return null
        return null;
    }

    @Override
    public String getCurrentMapDatabase() {
        //Not supported so return null
        return null;
    }

    @Override
    public void setMapDatabase(String s) {
        //Not supported so do nothing

    }
}
