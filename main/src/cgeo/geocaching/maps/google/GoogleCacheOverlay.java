package cgeo.geocaching.maps.google;

import cgeo.geocaching.cgSettings;
import cgeo.geocaching.maps.CachesOverlay;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Google specific implementation of the itemized cache overlay
 *
 * @author rsudev
 *
 */
public class GoogleCacheOverlay extends ItemizedOverlay<GoogleCacheOverlayItem> implements ItemizedOverlayImpl {

    private CachesOverlay base;
    private Lock lock = new ReentrantLock();

    public GoogleCacheOverlay(cgSettings settingsIn, Context contextIn, Drawable markerIn, Boolean fromDetailIn) {
        super(boundCenterBottom(markerIn));
        base = new CachesOverlay(settingsIn, this, contextIn, fromDetailIn);
    }

    @Override
    public CachesOverlay getBase() {
        return base;
    }

    @Override
    protected GoogleCacheOverlayItem createItem(int i) {
        if (base == null)
            return null;

        return (GoogleCacheOverlayItem) base.createItem(i);
    }

    @Override
    public int size() {
        if (base == null)
            return 0;

        return base.size();
    }

    @Override
    protected boolean onTap(int arg0) {
        if (base == null)
            return false;

        return base.onTap(arg0);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        base.draw(canvas, (MapViewImpl) mapView, shadow);
    }

    @Override
    public void superPopulate() {
        populate();
    }

    @Override
    public Drawable superBoundCenter(Drawable markerIn) {
        return super.boundCenter(markerIn);
    }

    @Override
    public Drawable superBoundCenterBottom(Drawable marker) {
        return super.boundCenterBottom(marker);
    }

    @Override
    public void superSetLastFocusedItemIndex(int i) {
        super.setLastFocusedIndex(i);
    }

    @Override
    public boolean superOnTap(int index) {
        return super.onTap(index);
    }

    @Override
    public void superDraw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        super.draw(canvas, (MapView) mapView, shadow);
    }

    @Override
    public void superDrawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {
        // Nothing to do here...
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

}
