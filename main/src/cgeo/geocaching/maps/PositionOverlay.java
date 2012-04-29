package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public class PositionOverlay implements GeneralOverlay {
    private Location coordinates = null;
    private GeoPointImpl location = null;
    private Float heading = 0f;
    private Paint accuracyCircle = null;
    private Paint historyLine = null;
    private Paint historyLineShadow = null;
    private Point center = new Point();
    private Point left = new Point();
    private Bitmap arrow = null;
    private int widthArrowHalf = 0;
    private int heightArrowHalf = 0;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private Location historyRecent = null;
    private List<Location> history = new ArrayList<Location>();
    private Point historyPointN = new Point();
    private Point historyPointP = new Point();
    private Activity activity;
    private MapProvider mapProvider = null;
    private MapItemFactory mapItemFactory = null;
    private OverlayImpl ovlImpl = null;

    public PositionOverlay(Activity activity, OverlayImpl ovlImpl) {
        this.activity = activity;
        this.mapProvider = Settings.getMapProvider();
        this.mapItemFactory = this.mapProvider.getMapItemFactory();
        this.ovlImpl = ovlImpl;
    }

    public void setCoordinates(Location coordinatesIn) {
        coordinates = coordinatesIn;
        location = mapItemFactory.getGeoPointBase(new Geopoint(coordinates));
    }

    public void setHeading(Float bearingNow) {
        heading = bearingNow;
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {

        drawInternal(canvas, projection);
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection());
    }

    private void drawInternal(Canvas canvas, MapProjectionImpl projection) {

        if (coordinates == null || location == null) {
            return;
        }

        if (accuracyCircle == null) {
            accuracyCircle = new Paint();
            accuracyCircle.setAntiAlias(true);
            accuracyCircle.setStrokeWidth(1.0f);
        }

        if (historyLine == null) {
            historyLine = new Paint();
            historyLine.setAntiAlias(true);
            historyLine.setStrokeWidth(3.0f);
            historyLine.setColor(0xFFFFFFFF);
        }

        if (historyLineShadow == null) {
            historyLineShadow = new Paint();
            historyLineShadow.setAntiAlias(true);
            historyLineShadow.setStrokeWidth(7.0f);
            historyLineShadow.setColor(0x66000000);
        }

        if (setfil == null) {
            setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
        }
        if (remfil == null) {
            remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);
        }

        canvas.setDrawFilter(setfil);

        double latitude = coordinates.getLatitude();
        double longitude = coordinates.getLongitude();
        float accuracy = coordinates.getAccuracy();

        float[] result = new float[1];

        Location.distanceBetween(latitude, longitude, latitude, longitude + 1, result);
        float longitudeLineDistance = result[0];

        final Geopoint leftCoords = new Geopoint(latitude, longitude - accuracy / longitudeLineDistance);
        GeoPointImpl leftGeo = mapItemFactory.getGeoPointBase(leftCoords);
        projection.toPixels(leftGeo, left);
        projection.toPixels(location, center);
        int radius = center.x - left.x;

        accuracyCircle.setColor(0x66000000);
        accuracyCircle.setStyle(Style.STROKE);
        canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

        accuracyCircle.setColor(0x08000000);
        accuracyCircle.setStyle(Style.FILL);
        canvas.drawCircle(center.x, center.y, radius, accuracyCircle);

        if (coordinates.getAccuracy() < 50f && ((historyRecent != null && historyRecent.distanceTo(coordinates) > 5.0) || historyRecent == null)) {
            if (historyRecent != null) {
                history.add(historyRecent);
            }
            historyRecent = coordinates;

            int toRemove = history.size() - 700;

            if (toRemove > 0) {
                for (int cnt = 0; cnt < toRemove; cnt++) {
                    history.remove(cnt);
                }
            }
        }

        if (Settings.isMapTrail()) {
            int size = history.size();
            if (size > 1) {
                int alpha;
                int alphaCnt = size - 201;
                if (alphaCnt < 1) {
                    alphaCnt = 1;
                }

                for (int cnt = 1; cnt < size; cnt++) {
                    Location prev = history.get(cnt - 1);
                    Location now = history.get(cnt);

                    if (prev != null && now != null) {
                        projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(prev)), historyPointP);
                        projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(now)), historyPointN);

                        if ((alphaCnt - cnt) > 0) {
                            alpha = 255 / (alphaCnt - cnt);
                        }
                        else {
                            alpha = 255;
                        }

                        historyLineShadow.setAlpha(alpha);
                        historyLine.setAlpha(alpha);

                        canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLineShadow);
                        canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLine);
                    }
                }
            }

            if (size > 0) {
                Location prev = history.get(size - 1);
                Location now = coordinates;

                if (prev != null && now != null) {
                    projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(prev)), historyPointP);
                    projection.toPixels(mapItemFactory.getGeoPointBase(new Geopoint(now)), historyPointN);

                    historyLineShadow.setAlpha(255);
                    historyLine.setAlpha(255);

                    canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLineShadow);
                    canvas.drawLine(historyPointP.x, historyPointP.y, historyPointN.x, historyPointN.y, historyLine);
                }
            }
        }

        if (arrow == null) {
            arrow = BitmapFactory.decodeResource(activity.getResources(), R.drawable.my_location_chevron);
            widthArrowHalf = arrow.getWidth() / 2;
            heightArrowHalf = arrow.getHeight() / 2;
        }

        int marginLeft;
        int marginTop;

        marginLeft = center.x - widthArrowHalf;
        marginTop = center.y - heightArrowHalf;

        Matrix matrix = new Matrix();
        matrix.setRotate(heading, widthArrowHalf, heightArrowHalf);
        matrix.postTranslate(marginLeft, marginTop);

        canvas.drawBitmap(arrow, matrix, null);

        canvas.setDrawFilter(remfil);

        //super.draw(canvas, mapView, shadow);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }
}
