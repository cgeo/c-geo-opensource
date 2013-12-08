package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.PeriodicHandler;
import cgeo.geocaching.utils.PeriodicHandler.PeriodicHandlerListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.View;

public class CompassView extends View implements PeriodicHandlerListener {

    private Context context = null;
    private Bitmap compassUnderlay = null;
    private Bitmap compassRose = null;
    private Bitmap compassArrow = null;
    private Bitmap compassOverlay = null;
    /**
     * North direction currently SHOWN on compass (not measured)
     */
    private float azimuthShown = 0;
    /**
     * cache direction currently SHOWN on compass (not measured)
     */
    private float cacheHeadingShown = 0;
    /**
     * cache direction measured from device, or 0.0
     */
    private float cacheHeadingMeasured = 0;
    /**
     * North direction measured from device, or 0.0
     */
    private float northMeasured = 0;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private int compassUnderlayWidth = 0;
    private int compassUnderlayHeight = 0;
    private int compassRoseWidth = 0;
    private int compassRoseHeight = 0;
    private int compassArrowWidth = 0;
    private int compassArrowHeight = 0;
    private int compassOverlayWidth = 0;
    private int compassOverlayHeight = 0;
    private boolean initialDisplay;
    private final PeriodicHandler redrawHandler = new PeriodicHandler(40, this);

    public CompassView(Context contextIn) {
        super(contextIn);
        context = contextIn;
    }

    public CompassView(Context contextIn, AttributeSet attrs) {
        super(contextIn, attrs);
        context = contextIn;
    }

    @Override
    public void onAttachedToWindow() {
        compassUnderlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_underlay);
        compassRose = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_rose);
        compassArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_arrow);
        compassOverlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_overlay);

        compassUnderlayWidth = compassUnderlay.getWidth();
        compassUnderlayHeight = compassUnderlay.getWidth();
        compassRoseWidth = compassRose.getWidth();
        compassRoseHeight = compassRose.getWidth();
        compassArrowWidth = compassArrow.getWidth();
        compassArrowHeight = compassArrow.getWidth();
        compassOverlayWidth = compassOverlay.getWidth();
        compassOverlayHeight = compassOverlay.getWidth();

        setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
        remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);

        synchronized (this) {
            initialDisplay = true;
        }
        redrawHandler.start();
    }

    @Override
    public void onDetachedFromWindow() {
        redrawHandler.stop();
        super.onDetachedFromWindow();

        if (compassUnderlay != null) {
            compassUnderlay.recycle();
        }

        if (compassRose != null) {
            compassRose.recycle();
        }

        if (compassArrow != null) {
            compassArrow.recycle();
        }

        if (compassOverlay != null) {
            compassOverlay.recycle();
        }
    }

    public synchronized void updateNorth(float northHeadingIn, float cacheHeadingIn) {
        if (initialDisplay) {
            // We will force the compass to move brutally if this is the first
            // update since it is visible.
            azimuthShown = northHeadingIn;
            cacheHeadingShown = cacheHeadingIn;

            // it may take some time to get an initial direction measurement for the device
            if (northHeadingIn != 0.0) {
                initialDisplay = false;
            }
        }
        northMeasured = northHeadingIn;
        cacheHeadingMeasured = cacheHeadingIn;
    }

    /**
     * Compute the new value, moving by small increments.
     *
     * @param goal
     *            the goal to reach
     * @param actual
     *            the actual value
     * @return the new value
     */
    static protected float smoothUpdate(float goal, float actual) {
        final float diff = AngleUtils.difference(actual, goal);

        float offset = 0;

        // If the difference is smaller than 1 degree, do nothing as it
        // causes the arrow to vibrate. Round away from 0.
        if (diff > 1.0) {
            offset = FloatMath.ceil(diff / 10.0f); // for larger angles, rotate faster
        } else if (diff < 1.0) {
            offset = FloatMath.floor(diff / 10.0f);
        }

        return AngleUtils.normalize(actual + offset);
    }

    @Override
    public synchronized void onPeriodic() {
        final float newAzimuthShown = smoothUpdate(northMeasured, azimuthShown);
        final float newCacheHeadingShown = smoothUpdate(cacheHeadingMeasured, cacheHeadingShown);
        if (Math.abs(AngleUtils.difference(azimuthShown, newAzimuthShown)) >= 2 ||
                Math.abs(AngleUtils.difference(cacheHeadingShown, newCacheHeadingShown)) >= 2) {
            synchronized(this) {
                azimuthShown = newAzimuthShown;
                cacheHeadingShown = newCacheHeadingShown;
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // use local synchronized variables to avoid them being changed from the device during drawing
        float azimuthDrawn;
        float headingDrawn;

        synchronized (this) {
            azimuthDrawn = azimuthShown;
            headingDrawn = cacheHeadingShown;
        }

        final float azimuthTemp = azimuthDrawn;
        final float azimuthRelative = AngleUtils.normalize(azimuthTemp - headingDrawn);

        // compass margins
        final int canvasCenterX = (compassRoseWidth / 2) + ((getWidth() - compassRoseWidth) / 2);
        final int canvasCenterY = (compassRoseHeight / 2) + ((getHeight() - compassRoseHeight) / 2);

        super.onDraw(canvas);

        canvas.save();
        canvas.setDrawFilter(setfil);

        int marginLeftTemp = (getWidth() - compassUnderlayWidth) / 2;
        int marginTopTemp = (getHeight() - compassUnderlayHeight) / 2;

        canvas.drawBitmap(compassUnderlay, marginLeftTemp, marginTopTemp, null);

        marginLeftTemp = (getWidth() - compassRoseWidth) / 2;
        marginTopTemp = (getHeight() - compassRoseHeight) / 2;

        canvas.rotate(-azimuthTemp, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassRose, marginLeftTemp, marginTopTemp, null);
        canvas.rotate(azimuthTemp, canvasCenterX, canvasCenterY);

        marginLeftTemp = (getWidth() - compassArrowWidth) / 2;
        marginTopTemp = (getHeight() - compassArrowHeight) / 2;

        canvas.rotate(-azimuthRelative, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassArrow, marginLeftTemp, marginTopTemp, null);
        canvas.rotate(azimuthRelative, canvasCenterX, canvasCenterY);

        marginLeftTemp = (getWidth() - compassOverlayWidth) / 2;
        marginTopTemp = (getHeight() - compassOverlayHeight) / 2;

        canvas.drawBitmap(compassOverlay, marginLeftTemp, marginTopTemp, null);

        canvas.setDrawFilter(remfil);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }

        final int desired = compassArrow.getWidth() + getPaddingLeft() + getPaddingRight();
        if (specMode == MeasureSpec.AT_MOST) {
            return Math.min(desired, specSize);
        }

        return desired;
    }

    private int measureHeight(int measureSpec) {
        // The duplicated code in measureHeight and measureWidth cannot be avoided.
        // Those methods must be efficient, therefore we cannot extract the code differences and unify the remainder.
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }

        final int desired = compassArrow.getHeight() + getPaddingTop() + getPaddingBottom();
        if (specMode == MeasureSpec.AT_MOST) {
            return Math.min(desired, specSize);
        }

        return desired;
    }
}
