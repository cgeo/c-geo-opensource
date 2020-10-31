package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.DistanceDrawer;
import cgeo.geocaching.maps.ScaleDrawer;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.storage.DataStore;

import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.view.View;

import java.util.ArrayList;

public class MapsforgePositionAndHistory implements GeneralOverlay, PositionAndHistory {
    private OverlayImpl ovlImpl = null;

    PositionDrawer positionDrawer = null;
    ScaleDrawer scaleDrawer = null;
    DirectionDrawer directionDrawer = null;
    DistanceDrawer distanceDrawer = null;

    public MapsforgePositionAndHistory(final View root, final OverlayImpl ovlImpl, final Geopoint coords, final String geocode, final boolean showBothDistances) {
        this.ovlImpl = ovlImpl;
        positionDrawer = new PositionDrawer();
        scaleDrawer = new ScaleDrawer();

        if (coords != null) {
            directionDrawer = new DirectionDrawer(coords, realDistance -> distanceDrawer.setRealDistance(realDistance));
            distanceDrawer = new DistanceDrawer(root, coords, showBothDistances);
        } else if (geocode != null) {
            final Viewport bounds = DataStore.getBounds(geocode);
            if (bounds != null) {
                directionDrawer = new DirectionDrawer(bounds.center, realDistance -> distanceDrawer.setRealDistance(realDistance));
                distanceDrawer = new DistanceDrawer(root, bounds.center, showBothDistances);
            }
        }
    }

    @Override
    public void setCoordinates(final Location coordinatesIn) {
        positionDrawer.setCoordinates(coordinatesIn);
        if (directionDrawer != null) {
            directionDrawer.setCoordinates(coordinatesIn);
            distanceDrawer.setCoordinates(coordinatesIn);
        }

    }

    @Override
    public Location getCoordinates() {
        return positionDrawer.getCoordinates();
    }

    @Override
    public void setHeading(final float bearingNow) {
        positionDrawer.setHeading(bearingNow);
    }

    @Override
    public float getHeading() {
        return positionDrawer.getHeading();
    }

    @Override
    public void updateManualRoute(final Route route) {
        // do nothing - functionality not implemented for old Mapsforge map
    }


    @Override
    public void drawOverlayBitmap(final Canvas canvas, final Point drawPosition,
            final MapProjectionImpl projection, final byte drawZoomLevel) {

        drawInternal(canvas, projection, getOverlayImpl().getMapViewImpl());
    }

    @Override
    public void draw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection(), mapView);
    }

    private void drawInternal(final Canvas canvas, final MapProjectionImpl projection, final MapViewImpl mapView) {
        if (directionDrawer != null) {
            directionDrawer.drawDirection(canvas, projection);
        }
        positionDrawer.drawPosition(canvas, projection);
        scaleDrawer.drawScale(canvas, mapView);
        if (distanceDrawer != null) {
            distanceDrawer.drawDistance(canvas);
        }
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }

    @Override
    public ArrayList<TrailHistoryElement> getHistory() {
        return positionDrawer.getHistory();
    }

    @Override
    public void setHistory(final ArrayList<TrailHistoryElement> history) {
        positionDrawer.setHistory(history);
    }

    @Override
    public void repaintRequired() {
        ovlImpl.getMapViewImpl().repaintRequired(this);
    }

    @Override
    public void updateMapRotation() {
    }
}
