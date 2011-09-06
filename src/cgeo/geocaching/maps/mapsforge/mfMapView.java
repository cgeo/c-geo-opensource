package cgeo.geocaching.maps.mapsforge;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapDatabase;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;
import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.Projection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.OtherCachersOverlay;
import cgeo.geocaching.maps.PositionOverlay;
import cgeo.geocaching.maps.ScaleOverlay;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OnDragListener;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl.overlayType;

public class mfMapView extends MapView implements MapViewImpl {
	private GestureDetector gestureDetector;
	private OnDragListener onDragListener;

	public mfMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		gestureDetector = new GestureDetector(context, new GestureListener());
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
		return new mfMapController(getController(), getMaxZoomLevel());
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
	public CachesOverlay createAddMapOverlay(cgSettings settings,
			Context context, Drawable drawable, boolean fromDetailIntent) {
		
		mfCacheOverlay ovl = new mfCacheOverlay(settings, context, drawable, fromDetailIntent);
		getOverlays().add(ovl);
		return ovl.getBase();
	}
	
	@Override
	public OtherCachersOverlay createAddUsersOverlay(Context context, Drawable markerIn) {
		mfUsersOverlay ovl = new mfUsersOverlay(context, markerIn);
		getOverlays().add(ovl);
		return ovl.getBase();
	}

	@Override
	public PositionOverlay createAddPositionOverlay(Activity activity,
			cgSettings settingsIn) {
		mfOverlay ovl = new mfOverlay(activity, settingsIn, overlayType.PositionOverlay);
		getOverlays().add(ovl);
		return (PositionOverlay) ovl.getBase();
	}

	@Override
	public ScaleOverlay createAddScaleOverlay(Activity activity,
			cgSettings settingsIn) {
		mfOverlay ovl = new mfOverlay(activity, settingsIn, overlayType.ScaleOverlay);
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

	@Override
	public int getMapZoomLevel() {
		return getZoomLevel()+1;
	}

	@Override
	public boolean needsScaleOverlay() {
		return false;
	}

	@Override
	public void setBuiltinScale(boolean b) {
		setScaleBar(b);
	}
	
	@Override
	public void setMapSource(cgSettings settings) {
		
		MapViewMode viewMode = mfMapProvider.getMapViewMode(settings.getMapSourceId());
		
		if (MapViewMode.CANVAS_RENDERER == viewMode) {
			if (MapDatabase.isValidMapFile(settings.getMapFile())) {
				setMapViewMode(MapViewMode.CANVAS_RENDERER);
				super.setMapFile(settings.getMapFile());
			} else {
				setMapViewMode(MapViewMode.MAPNIK_TILE_DOWNLOAD);
			}
		} else {
			setMapViewMode(viewMode);
		}
	}

	@Override
	public void repaintRequired(GeneralOverlay overlay) {
		
		try {
			mfOverlay ovl = (mfOverlay) overlay.getOverlayImpl();
			
			if (ovl != null) {
				ovl.requestRedraw();
			}
			
		} catch (Exception e) {
			Log.e(cgSettings.tag, "mfMapView.repaintRequired: " + e.toString());
		}
	}

	@Override
	public boolean needsDarkScale() {
		return false;
	}

	@Override
	public void setOnDragListener(OnDragListener onDragListener) {
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
}
