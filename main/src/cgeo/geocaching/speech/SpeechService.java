package cgeo.geocaching.speech;

import cgeo.geocaching.DirectionProvider;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import java.util.Locale;

/**
 * Service to speak the compass directions.
 *
 */
public class SpeechService extends Service implements OnInitListener {

    private static final int SPEECH_MINPAUSE_SECONDS = 5;
    private static final int SPEECH_MAXPAUSE_SECONDS = 30;
    private static final String EXTRA_TARGET_COORDS = "target";
    private static Activity startingActivity;
    private static boolean isRunning = false;
    /**
     * Text to speech API of Android
     */
    private TextToSpeech tts;
    /**
     * TTS has been initialized and we can speak.
     */
    private boolean initialized = false;
    protected float direction;
    protected Geopoint position;
    protected boolean directionInitialized = !Settings.isUseCompass(); // don't wait for magnetometer, if it shall not be used
    protected boolean positionInitialized = false;

    GeoDirHandler geoHandler = new GeoDirHandler() {
        @Override
        protected void updateDirection(float newDirection) {
            if (cgeoapplication.getInstance().currentGeo().getSpeed() <= 5) {
                direction = DirectionProvider.getDirectionNow(startingActivity, newDirection);
                directionInitialized = true;
                updateCompass();
            }
        }

        @Override
        protected void updateGeoData(cgeo.geocaching.IGeoData newGeo) {
            position = newGeo.getCoords();
            positionInitialized = true;
            if (!Settings.isUseCompass() || newGeo.getSpeed() > 5) {
                direction = newGeo.getBearing();
                directionInitialized = true;
            }
            updateCompass();
        }
    };
    /**
     * remember when we talked the last time
     */
    private long lastSpeechTime = 0;
    private float lastSpeechDistance = 0.0f;
    private Geopoint target;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void updateCompass() {
        // make sure we have both sensor values before talking
        if (!positionInitialized || !directionInitialized) {
            return;
        }

        // avoid any calculation, if the delay since the last output is not long enough
        final long now = System.currentTimeMillis();
        if (now - lastSpeechTime <= SPEECH_MINPAUSE_SECONDS * 1000) {
            return;
        }

        // to speak, we want max pause to have elapsed or distance to geopoint to have changed by a given amount
        final float distance = position.distanceTo(target);
        if (now - lastSpeechTime <= SPEECH_MAXPAUSE_SECONDS * 1000) {
            if (Math.abs(lastSpeechDistance - distance) < getDeltaForDistance(distance)) {
                return;
            }
        }

        final String text = TextFactory.getText(position, target, direction);
        if (StringUtils.isNotEmpty(text)) {
            lastSpeechTime = System.currentTimeMillis();
            lastSpeechDistance = distance;
            speak(text);
        }
    }

    /**
     * Return distance required to be moved based on overall distance.<br>
     *
     * @param distance
     *            in km
     * @return delta in km
     */
    private static float getDeltaForDistance(final float distance) {
        if (distance > 1.0) {
            return 0.2f;
        }
        if (distance > 0.05) {
            return distance / 5.0f;
        }
        return 0f;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onDestroy() {
        geoHandler.stopGeoAndDir();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        // The text to speech system takes some time to initialize.
        if (status != TextToSpeech.SUCCESS) {
            Log.e("Text to speech cannot be initialized.");
            return;
        }

        int switchLocale = tts.setLanguage(Locale.getDefault());

        if (switchLocale == TextToSpeech.LANG_MISSING_DATA
                || switchLocale == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("Current languge not supported by text to speech.");
            return;
        }

        initialized = true;

        if (Settings.isUseCompass()) {
            geoHandler.startGeoAndDir();
        } else {
            geoHandler.startGeo();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            target = intent.getParcelableExtra(EXTRA_TARGET_COORDS);
        }
        return START_NOT_STICKY;
    }

    private void speak(final String text) {
        if (!initialized) {
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public static void startService(final Activity activity, Geopoint dstCoords) {
        isRunning = true;
        startingActivity = activity;
        Intent talkingService = new Intent(activity, SpeechService.class);
        talkingService.putExtra(EXTRA_TARGET_COORDS, dstCoords);
        activity.startService(talkingService);
    }

    public static void stopService(final Activity activity) {
        isRunning = false;
        activity.stopService(new Intent(activity, SpeechService.class));
    }

    public static boolean isRunning() {
        return isRunning;
    }

}
