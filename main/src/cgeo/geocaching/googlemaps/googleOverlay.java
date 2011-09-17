package cgeo.geocaching.googlemaps;

import cgeo.geocaching.cgSettings;
import cgeo.geocaching.mapcommon.cgMapMyOverlay;
import cgeo.geocaching.mapcommon.cgOverlayScale;
import cgeo.geocaching.mapinterfaces.MapViewImpl;
import cgeo.geocaching.mapinterfaces.OverlayBase;
import cgeo.geocaching.mapinterfaces.OverlayImpl;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import android.app.Activity;
import android.graphics.Canvas;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class googleOverlay extends Overlay implements OverlayImpl {

    private OverlayBase overlayBase = null;
    private Lock lock = new ReentrantLock();

    public googleOverlay(Activity activityIn, cgSettings settingsIn, overlayType ovlType) {
        switch (ovlType) {
            case PositionOverlay:
                overlayBase = new cgMapMyOverlay(settingsIn, activityIn, this);
                break;
            case ScaleOverlay:
                overlayBase = new cgOverlayScale(activityIn, settingsIn, this);
        }
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);

        if (overlayBase != null) {
            overlayBase.draw(canvas, (MapViewImpl) mapView, shadow);
        }
    }

    public OverlayBase getBase() {
        return overlayBase;
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
