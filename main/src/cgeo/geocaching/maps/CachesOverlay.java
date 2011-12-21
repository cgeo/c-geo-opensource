package cgeo.geocaching.maps;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCoord;
import cgeo.geocaching.cgeopopup;
import cgeo.geocaching.cgeowaypoint;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.ItemizedOverlayImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapViewImpl;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CachesOverlay extends AbstractItemizedOverlay {

    private List<CachesOverlayItemImpl> items = new ArrayList<CachesOverlayItemImpl>();
    private Context context = null;
    private boolean fromDetail = false;
    private boolean displayCircles = false;
    private ProgressDialog waitDialog = null;
    private Point center = new Point();
    private Point left = new Point();
    private Paint blockedCircle = null;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private MapProvider mapProvider = null;

    public CachesOverlay(ItemizedOverlayImpl ovlImpl, Context contextIn, boolean fromDetailIn) {
        super(ovlImpl);

        populate();

        context = contextIn;
        fromDetail = fromDetailIn;

        mapProvider = Settings.getMapProvider();
    }

    public void updateItems(CachesOverlayItemImpl item) {
        List<CachesOverlayItemImpl> itemsPre = new ArrayList<CachesOverlayItemImpl>();
        itemsPre.add(item);

        updateItems(itemsPre);
    }

    public void updateItems(List<CachesOverlayItemImpl> itemsPre) {
        if (itemsPre == null) {
            return;
        }

        for (CachesOverlayItemImpl item : itemsPre) {
            item.setMarker(boundCenterBottom(item.getMarker(0)));
        }

        // ensure no interference between the draw and content changing routines
        getOverlayImpl().lock();
        try {
            items = new ArrayList<CachesOverlayItemImpl>(itemsPre);

            setLastFocusedItemIndex(-1); // to reset tap during data change
            populate();
        } finally {
            getOverlayImpl().unlock();
        }
    }

    public boolean getCircles() {
        return displayCircles;
    }

    public void switchCircles() {
        displayCircles = !displayCircles;
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {

        drawInternal(canvas, mapView.getMapProjection());

        super.draw(canvas, mapView, false);
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {

        drawInternal(canvas, projection);

        super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
    }

    private void drawInternal(Canvas canvas, MapProjectionImpl projection) {

        // prevent content changes
        getOverlayImpl().lock();
        try {
            if (displayCircles) {
                if (blockedCircle == null) {
                    blockedCircle = new Paint();
                    blockedCircle.setAntiAlias(true);
                    blockedCircle.setStrokeWidth(1.0f);
                    blockedCircle.setARGB(127, 0, 0, 0);
                    blockedCircle.setPathEffect(new DashPathEffect(new float[] { 3, 2 }, 0));
                }

                if (setfil == null) {
                    setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
                }
                if (remfil == null) {
                    remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);
                }

                canvas.setDrawFilter(setfil);

                for (CachesOverlayItemImpl item : items) {
                    final cgCoord itemCoord = item.getCoord();
                    float[] result = new float[1];

                    Location.distanceBetween(itemCoord.getCoords().getLatitude(), itemCoord.getCoords().getLongitude(),
                            itemCoord.getCoords().getLatitude(), itemCoord.getCoords().getLongitude() + 1, result);
                    final float longitudeLineDistance = result[0];

                    GeoPointImpl itemGeo = mapProvider.getGeoPointBase(itemCoord.getCoords());

                    final Geopoint leftCoords = new Geopoint(itemCoord.getCoords().getLatitude(),
                            itemCoord.getCoords().getLongitude() - 161 / longitudeLineDistance);
                    GeoPointImpl leftGeo = mapProvider.getGeoPointBase(leftCoords);

                    projection.toPixels(itemGeo, center);
                    projection.toPixels(leftGeo, left);
                    int radius = center.x - left.x;

                    final CacheType type = item.getType();
                    if (type == null || type == CacheType.MULTI || type == CacheType.MYSTERY || type == CacheType.VIRTUAL) {
                        blockedCircle.setColor(0x66000000);
                        blockedCircle.setStyle(Style.STROKE);
                        canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                    } else {
                        blockedCircle.setColor(0x66BB0000);
                        blockedCircle.setStyle(Style.STROKE);
                        canvas.drawCircle(center.x, center.y, radius, blockedCircle);

                        blockedCircle.setColor(0x44BB0000);
                        blockedCircle.setStyle(Style.FILL);
                        canvas.drawCircle(center.x, center.y, radius, blockedCircle);
                    }
                }
                canvas.setDrawFilter(remfil);
            }
        } finally {
            getOverlayImpl().unlock();
        }
    }

    @Override
    public boolean onTap(int index) {

        try {
            if (items.size() <= index) {
                return false;
            }

            if (waitDialog == null) {
                waitDialog = new ProgressDialog(context);
                waitDialog.setMessage("loading details...");
                waitDialog.setCancelable(false);
            }
            waitDialog.show();

            CachesOverlayItemImpl item = null;

            // prevent concurrent changes
            getOverlayImpl().lock();
            try {
                if (index < items.size()) {
                    item = items.get(index);
                }
            } finally {
                getOverlayImpl().unlock();
            }

            if (item == null) {
                return false;
            }

            cgCoord coordinate = item.getCoord();

            if (StringUtils.isNotBlank(coordinate.getCoordType()) && coordinate.getCoordType().equalsIgnoreCase("cache") && StringUtils.isNotBlank(coordinate.getGeocode())) {
                Intent popupIntent = new Intent(context, cgeopopup.class);

                popupIntent.putExtra("fromdetail", fromDetail);
                popupIntent.putExtra("geocode", coordinate.getGeocode());

                context.startActivity(popupIntent);
            } else if (coordinate.getCoordType() != null && coordinate.getCoordType().equalsIgnoreCase("waypoint") && coordinate.getId() != null && coordinate.getId() > 0) {
                Intent popupIntent = new Intent(context, cgeowaypoint.class);

                popupIntent.putExtra("waypoint", coordinate.getId());
                popupIntent.putExtra("geocode", coordinate.getGeocode());

                context.startActivity(popupIntent);
            } else {
                waitDialog.dismiss();
                return false;
            }

            waitDialog.dismiss();
        } catch (Exception e) {
            Log.e(Settings.tag, "cgMapOverlay.onTap: " + e.toString());
        }

        return false;
    }

    @Override
    public CachesOverlayItemImpl createItem(int index) {
        try {
            return items.get(index);
        } catch (Exception e) {
            Log.e(Settings.tag, "cgMapOverlay.createItem: " + e.toString());
        }

        return null;
    }

    @Override
    public int size() {
        try {
            return items.size();
        } catch (Exception e) {
            Log.e(Settings.tag, "cgMapOverlay.size: " + e.toString());
        }

        return 0;
    }
}
