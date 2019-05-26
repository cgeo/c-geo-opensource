package cgeo.geocaching.location;

import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.settings.Settings;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class ProximityNotification {
    // marker: don't play a tone
    protected static final int TONE_NONE = 0;

    // marker: no last distance set
    protected static final long DISTANCE_RESET = Settings.DISTANCE_BEEP_MAX + 1;

    // minimum increase in distance, before distance gets reset
    protected static final long MIN_DISTANCE_DELTA = 10;

    // repeat tone 1/2 every x ms (if still close enough)
    protected static final long MIN_TIME_DELTA1 = 10000;
    protected static final long MIN_TIME_DELTA2 = 5000;

    // distance beep activated?
    protected boolean distanceBeep = false;

    // max distances for single or double beep
    protected long distanceSingleBeep = 200;
    protected long distanceDoubleBeep = 50;

    // last distance / last timestamp
    protected long lastDistanceInM = DISTANCE_RESET;
    protected long lastTimestamp = 0;

    // reference point to calculate distance to
    // needs to be set by inheriting class by calling setReferencePoint()
    private Geopoint referencePointForBeep = null;

    public ProximityNotification() {
        distanceSingleBeep = Settings.getDistanceBeepThreshold(true);
        distanceDoubleBeep = Settings.getDistanceBeepThreshold(false);
        distanceBeep = Settings.getDistanceBeep() & (distanceSingleBeep > 0) && (distanceDoubleBeep > 0);
    }

    public void setReferencePoint(final Geopoint referencePoint) {
        referencePointForBeep = referencePoint;
    }

    public void onUpdateGeoData(final GeoData geo) {
        // send acoustic signal depending on distance (if activated)
        if (distanceBeep && null != referencePointForBeep) {
            final long distanceInM = (long) (1000 * geo.getCoords().distanceTo(referencePointForBeep));
            final long currentTimestamp = System.currentTimeMillis();

            int playTone = TONE_NONE;
            if (distanceInM > (lastDistanceInM + MIN_DISTANCE_DELTA)) {
                lastDistanceInM = DISTANCE_RESET;
            }
            if ((distanceInM <= distanceDoubleBeep) && ((lastDistanceInM > distanceDoubleBeep) || ((currentTimestamp - lastTimestamp) > MIN_TIME_DELTA2))) {
                playTone = ToneGenerator.TONE_PROP_BEEP2;
            } else if ((distanceInM <= distanceSingleBeep) && ((lastDistanceInM > distanceSingleBeep) || ((currentTimestamp - lastTimestamp) > MIN_TIME_DELTA1))) {
                playTone = ToneGenerator.TONE_PROP_BEEP;
            }
            lastDistanceInM = distanceInM;
            if (playTone != TONE_NONE) {
                final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
                toneG.startTone(playTone);
                lastTimestamp = currentTimestamp;
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    toneG.release();
                }, 350);
            }
        }
    }

}
