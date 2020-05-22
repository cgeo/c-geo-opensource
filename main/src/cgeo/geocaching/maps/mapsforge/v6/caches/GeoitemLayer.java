package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;
import cgeo.geocaching.utils.DisplayUtils;

import android.util.DisplayMetrics;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;

public class GeoitemLayer extends Marker {

    private static final double tapSpanInches = 0.12; // 3mm as inches
    private static final double tapSpanRadius;

    static {

        final DisplayMetrics metrics = DisplayUtils.getDisplayMetrics();
        tapSpanRadius = metrics.densityDpi * tapSpanInches / 2.0;
    }

    private final GeoitemRef item;
    private final TapHandler tapHandler;
    private final double halfXSpan;
    private final double halfYSpan;
    private GeoitemCircle circle;

    private static final float radius = (float) (528.0 * IConversion.FEET_TO_KILOMETER * 1000.0);

    private static final Paint strokePaint;
    private static final Paint fillPaint;

    static {
        strokePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        strokePaint.setStrokeWidth(2.0f);
        strokePaint.setDashPathEffect(new float[] { 3, 2 });
        strokePaint.setColor(0x66BB0000);
        strokePaint.setStyle(Style.STROKE);

        fillPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        fillPaint.setColor(0x44BB0000);
        fillPaint.setStyle(Style.FILL);
    }

    public GeoitemLayer(final GeoitemRef item, final boolean hasCircle, final TapHandler tapHandler, final LatLong latLong, final Bitmap bitmap, final int horizontalOffset, final int verticalOffset) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);

        this.item = item;
        this.tapHandler = tapHandler;
        this.halfXSpan = getBitmap().getWidth() / 2.0;
        this.halfYSpan = getBitmap().getHeight() / 2.0;

        if (hasCircle) {
            circle = new GeoitemCircle(latLong, radius, fillPaint, strokePaint);
        } else {
            circle = null;
        }
    }

    public GeoitemRef getItem() {
        return item;
    }

    public String getItemCode() {
        return item.getItemCode();
    }

    public Layer getCircle() {
        return circle;
    }

    @Override
    public boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
        tapHandler.setMode(false);
        if (isHit(layerXY, tapXY)) {
            tapHandler.setHit(item);
        }
        return super.onTap(tapLatLong, layerXY, tapXY);
    }

    @Override
    public boolean onLongPress(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
        tapHandler.setMode(true);
        if (isHit(layerXY, tapXY)) {
            tapHandler.setHit(item);
        }
        return super.onLongPress(tapLatLong, layerXY, tapXY);
    }

    private boolean isHit(final Point layerXY, final Point tapXY) {
        final Rectangle rect = new Rectangle(layerXY.x + getHorizontalOffset() - halfXSpan, layerXY.y + getVerticalOffset() - halfYSpan, layerXY.x + getHorizontalOffset() + halfXSpan, layerXY.y + getVerticalOffset() + halfYSpan);

        return rect.intersectsCircle(tapXY.x, tapXY.y, tapSpanRadius);
    }
}
