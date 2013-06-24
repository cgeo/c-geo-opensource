package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.utils.MemorySubject;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class DirectionProvider extends MemorySubject<Float> implements SensorEventListener {

    private final SensorManager sensorManager;

    // Previous values signaled to observers to avoid re-sending the same value when the
    // device doesn't change orientation. The orientation is usually given with a 1 degree
    // precision by Android, so it is not uncommon to obtain exactly the same value several
    // times.
    private float previous = -1;

    public DirectionProvider(final Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

    }

    @Override
    protected void onFirstObserver() {
        @SuppressWarnings("deprecation")
        // This will be removed when using a new location service. Until then, it is okay to be used.
        final Sensor defaultSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, defaultSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onLastObserver() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        /*
         * There is a bug in Android, which apparently causes this method to be called every
         * time the sensor _value_ changed, even if the _accuracy_ did not change. So logging
         * this event leads to the log being flooded with multiple entries _per second_,
         * which I experienced when running cgeo in a building (with GPS and network being
         * unreliable).
         *
         * See for example https://code.google.com/p/android/issues/detail?id=14792
         */

        //Log.i(Settings.tag, "Compass' accuracy is low (" + accuracy + ")");
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        final float direction = event.values[0];
        if (direction != previous) {
            notifyObservers(direction);
            previous = direction;
        }
    }

    /**
     * Take the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param activity the activity to consider when computing the rotation
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */
    public static float getDirectionNow(final Activity activity, final float direction) {
        return Compatibility.getDirectionNow(direction, activity);
    }

}
