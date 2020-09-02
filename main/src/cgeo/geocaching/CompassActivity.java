package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotificationByCoords;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.log.LoggingUI;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.DirectionData;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.GnssStatusProvider.Status;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.SpeechService;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CompassView;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.WaypointSelectionActionProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import org.apache.commons.lang3.StringUtils;

public class CompassActivity extends AbstractActionBarActivity {

    @BindView(R.id.nav_type) protected TextView navType;
    @BindView(R.id.nav_accuracy) protected TextView navAccuracy;
    @BindView(R.id.nav_satellites) protected TextView navSatellites;
    @BindView(R.id.nav_location) protected TextView navLocation;
    @BindView(R.id.distance) protected TextView distanceView;
    @BindView(R.id.heading) protected TextView headingView;
    @BindView(R.id.rose) protected CompassView compassView;
    @BindView(R.id.destination) protected TextView destinationTextView;
    @BindView(R.id.cacheinfo) protected TextView cacheInfoView;
    @BindView(R.id.use_compass) protected ToggleButton useCompassSwitch;
    @BindView(R.id.device_heading) protected TextView deviceHeading;
    @BindView(R.id.device_orientation_label) protected TextView deviceOrientationLabel;
    @BindView(R.id.device_orientation_azimuth) protected TextView deviceOrientationAzimuth;
    @BindView(R.id.device_orientation_pitch) protected TextView deviceOrientationPitch;
    @BindView(R.id.device_orientation_roll) protected TextView deviceOrientationRoll;

    /**
     * Destination cache, may be null
     */
    private Geocache cache = null;
    /**
     * Destination waypoint, may be null
     */
    private Waypoint waypoint = null;
    private Geopoint dstCoords = null;
    private float cacheHeading = 0;
    private String description;
    private ProximityNotificationByCoords proximityNotification = null;
    private final TextSpinner<DirectionData.DeviceOrientation> deviceOrientationMode = new TextSpinner<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.compass_activity);
        ButterKnife.bind(this);

        deviceOrientationMode
            .setValues(Arrays.asList(new DirectionData.DeviceOrientation[]{DirectionData.DeviceOrientation.AUTO, DirectionData.DeviceOrientation.FLAT, DirectionData.DeviceOrientation.UPRIGHT}))
            .setDisplayMapper(d -> getString(R.string.device_orientation) + ": " + getString(d.resId))
            .setCheckedMapper(d -> d == DirectionData.DeviceOrientation.AUTO)
            .setTextClickThrough(true)
            .setChangeListener(d -> Settings.setDeviceOrientationMode(d));

        reconfigureGui();

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        // prepare proximity notification
        proximityNotification = Settings.isSpecificProximityNotificationActive() ? new ProximityNotificationByCoords() : null;
        if (null != proximityNotification) {
            proximityNotification.setTextNotifications(this);
        }

        // cache must exist, except for "any point navigation"
        final String geocode = extras.getString(Intents.EXTRA_GEOCODE);
        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }

        // find the wanted navigation target
        if (extras.containsKey(Intents.EXTRA_WAYPOINT_ID)) {
            final int waypointId = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
            final Waypoint waypoint = DataStore.loadWaypoint(waypointId);
            if (waypoint != null) {
                setTarget(waypoint, cache);
            }
        } else if (extras.containsKey(Intents.EXTRA_COORDS)) {
            setTarget(extras.getParcelable(Intents.EXTRA_COORDS), extras.getString(Intents.EXTRA_COORD_DESCRIPTION));
        } else if (cache != null) {
            setTarget(cache);
        } else {
            Log.w("CompassActivity.onCreate: no cache was found for geocode " + geocode);
            finish();
        }

        // set activity title just once, independent of what target is switched to
        if (cache != null) {
            setCacheTitleBar(cache);
        } else {
            setTitle(StringUtils.defaultIfBlank(extras.getString(Intents.EXTRA_NAME), res.getString(R.string.navigation)));
        }


        // make sure we can control the TTS volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        super.onResume();

        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this,
                new RestartLocationPermissionGrantedCallback(PermissionRequestContext.CompassActivity) {

                    @Override
                    public void executeAfter() {
                        resumeDisposables(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR),
                                Sensors.getInstance().gpsStatusObservable().observeOn(AndroidSchedulers.mainThread()).subscribe(gpsStatusHandler));
                    }
                });


        forceRefresh();
    }

    @Override
    public void onDestroy() {
        compassView.destroyDrawingCache();
        SpeechService.stopService(this);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.compass_activity);
        ButterKnife.bind(this);
        setTarget(dstCoords, description);

        forceRefresh();
    }

    private void forceRefresh() {
        reconfigureGui();

        final Sensors sensors = Sensors.getInstance();
        geoDirHandler.updateGeoDir(sensors.currentGeo(), sensors.currentDirection());
    }

    /** refresh GUI elements which can be changed e.g. after a new configuration was issued (e.g. turning from portrait into landscape and vice versa */
    private void reconfigureGui() {
        // Force a refresh of location and direction when data is available.
        final Sensors sensors = Sensors.getInstance();

        // reset the visibility of the compass toggle button if the device does not support it.
        if (sensors.hasCompassCapabilities()) {
            useCompassSwitch.setChecked(Settings.isUseCompass());
            useCompassSwitch.setOnClickListener(view -> {
                Settings.setUseCompass(((ToggleButton) view).isChecked());
                findViewById(R.id.device_orientation_mode).setVisibility(sensors.hasCompassCapabilities() && useCompassSwitch.isChecked() ? View.VISIBLE : View.GONE);
            });
        } else {
            useCompassSwitch.setVisibility(View.GONE);
        }
        deviceOrientationMode.setTextView(findViewById(R.id.device_orientation_mode)).set(Settings.getDeviceOrientationMode());
        findViewById(R.id.device_orientation_mode).setVisibility(sensors.hasCompassCapabilities() && useCompassSwitch.isChecked() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.compass_activity_options, menu);
        if (cache != null) {
            LoggingUI.addMenuItems(this, menu, cache);
            initializeTargetActionProvider(menu);
        }
        return true;
    }

    private void initializeTargetActionProvider(final Menu menu) {
        final MenuItem destinationMenu = menu.findItem(R.id.menu_select_destination);
        WaypointSelectionActionProvider.initialize(destinationMenu, cache, new WaypointSelectionActionProvider.Callback() {

            @Override
            public void onWaypointSelected(final Waypoint waypoint) {
                setTarget(waypoint, null);
            }

            @Override
            public void onGeocacheSelected(final Geocache geocache) {
                setTarget(geocache);
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_hint).setVisible(cache != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.menu_map:
                if (waypoint != null) {
                    DefaultMap.startActivityCoords(this, waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
                } else if (cache != null) {
                    DefaultMap.startActivityGeoCode(this, cache.getGeocode());
                } else {
                    DefaultMap.startActivityCoords(this, dstCoords, null, null);
                }
                return true;
            case R.id.menu_tts_toggle:
                SpeechService.toggleService(this, dstCoords);
                return true;
            case R.id.menu_hint:
                cache.showHintToast(this);
                return true;
            default:
                if (LoggingUI.onMenuItemSelected(item, this, cache, null)) {
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTarget(@NonNull final Geopoint coords, final String newDescription) {
        setDestCoords(coords);
        setTargetDescription(newDescription);
        updateDistanceInfo(Sensors.getInstance().currentGeo());

        Log.d("destination set: " + newDescription + " (" + dstCoords + ")");
    }

    private void setTarget(@NonNull final Waypoint waypointIn, @Nullable final Geocache cache) {
        waypoint = waypointIn;
        final Geopoint coordinates = waypointIn.getCoords();
        if (coordinates != null) { // handled by WaypointSelectionActionProvider, but the compiler doesn't know
            setTarget(coordinates, waypointIn.getName() + (null != cache ? Formatter.SEPARATOR + Formatter.formatCacheInfoShort(cache) : ""));
        }
    }

    private void setTarget(@NonNull final Geocache cache) {
        setTarget(cache.getCoords(), Formatter.formatCacheInfoShort(cache));
    }

    private void setDestCoords(final Geopoint coords) {
        dstCoords = coords;
        if (null != proximityNotification) {
            proximityNotification.setReferencePoint(coords);
        }
        if (dstCoords == null) {
            return;
        }

        destinationTextView.setText(dstCoords.toString());
    }

    private void setTargetDescription(@Nullable final String newDescription) {
        description = newDescription;
        if (description == null) {
            cacheInfoView.setVisibility(View.GONE);
            return;
        }
        cacheInfoView.setVisibility(View.VISIBLE);
        cacheInfoView.setText(description);
    }

    @SuppressLint("SetTextI18n")
    private void updateDistanceInfo(final GeoData geo) {
        if (dstCoords == null) {
            return;
        }

        cacheHeading = geo.getCoords().bearingTo(dstCoords);
        distanceView.setText(Units.getDistanceFromKilometers(geo.getCoords().distanceTo(dstCoords)));
        headingView.setText(Math.round(cacheHeading) + "°");
    }

    @SuppressLint("SetTextI18n")
    private final Consumer<Status> gpsStatusHandler = new Consumer<Status>() {
        @Override
        public void accept(final Status gpsStatus) {
            if (gpsStatus.satellitesVisible >= 0) {
                navSatellites.setText(res.getString(R.string.loc_sat) + ": " + gpsStatus.satellitesFixed + "/" + gpsStatus.satellitesVisible);
            } else {
                navSatellites.setText("");
            }
        }
    };

    private static final double[] altitudeReadings = { 0.0d, 0.0d, 0.0d, 0.0d, 0.0d };
    private static int altitudeReadingPos = 0;

    @SuppressLint("SetTextI18n")
    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoDirData(@NonNull final GeoData geo, final DirectionData dir) {
            try {
                navType.setText(res.getString(geo.getLocationProvider().resourceId));

                if (geo.getAccuracy() >= 0) {
                    navAccuracy.setText("±" + Units.getDistanceFromMeters(geo.getAccuracy()));
                } else {
                    navAccuracy.setText(null);
                }

                // remember new altitude reading, and calculate average from past MAX_READINGS readings
                if (geo.hasAltitude() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || geo.getVerticalAccuracyMeters() > 0.0)) {
                    altitudeReadings[altitudeReadingPos] = geo.getAltitude();
                    altitudeReadingPos = (++altitudeReadingPos) % altitudeReadings.length;
                }
                double averageAltitude = altitudeReadings[0];
                for (int i = 1; i < altitudeReadings.length; i++) {
                    averageAltitude += altitudeReadings[i];
                }
                if (altitudeReadings.length > 0) {
                    averageAltitude /= (double) altitudeReadings.length;
                }
                navLocation.setText(geo.getCoords().toString() + Formatter.SEPARATOR + Units.getDistanceFromMeters((float) averageAltitude));

                updateDistanceInfo(geo);

                updateNorthHeading(AngleUtils.getDirectionNow(dir.getDirection()));

                updateDeviceHeadingAndOrientation(dir);

                if (null != proximityNotification) {
                    proximityNotification.onUpdateGeoData(geo);
                }
            } catch (final RuntimeException e) {
                Log.w("Failed to update location", e);
            }
        }
    };

    private void updateNorthHeading(final float northHeading) {
        if (compassView != null) {
            compassView.updateNorth(northHeading, cacheHeading);
        }
    }

    private void updateDeviceHeadingAndOrientation(final DirectionData dir) {

        if (dir.hasOrientation()) {
            deviceOrientationAzimuth.setText(formatDecimalFloat(dir.getAzimuth()) + "° /");
            deviceOrientationPitch.setText(formatDecimalFloat(dir.getPitch()) + "° /");
            deviceOrientationRoll.setText(formatDecimalFloat(dir.getRoll()).substring(1) + "°");

            deviceOrientationLabel.setVisibility(View.VISIBLE);
            deviceOrientationAzimuth.setVisibility(View.VISIBLE);
            deviceOrientationPitch.setVisibility(View.VISIBLE);
            deviceOrientationRoll.setVisibility(View.VISIBLE);
        } else {
            deviceOrientationLabel.setVisibility(View.INVISIBLE);
            deviceOrientationAzimuth.setVisibility(View.INVISIBLE);
            deviceOrientationPitch.setVisibility(View.INVISIBLE);
            deviceOrientationRoll.setVisibility(View.INVISIBLE);
        }

        float direction = dir.getDirection();
        while (direction < 0f) {
            direction += 360f;
        }
        while (direction >= 360f) {
            direction -= 360f;
        }
        deviceHeading.setText(String.format("%3.1f°", direction));

        if (deviceOrientationMode.get() == DirectionData.DeviceOrientation.AUTO) {
            deviceOrientationMode.setTextDisplayMapper(d -> getString(R.string.device_orientation) + ": " + getString(dir.getDeviceOrientation().resId) + " (" + getString(DirectionData.DeviceOrientation.AUTO.resId) + ")");
        } else {
            deviceOrientationMode.setTextDisplayMapper(d -> getString(R.string.device_orientation) + ": " + getString(d.resId));
        }
    }

    /** formats a float to a decimal with length 4 and no places behind comma. Handles "-0" case. */
    private static String formatDecimalFloat(final float value) {
        final String formattedValue = String.format("% 4.0f", value);
        if (formattedValue.endsWith("-0")) {
            return "   0";
        }
        return formattedValue;
    }

    public static void startActivityWaypoint(final Context context, final Waypoint waypoint) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(Intents.EXTRA_GEOCODE, waypoint.getGeocode());
        navigateIntent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypoint.getId());
        context.startActivity(navigateIntent);
    }

    public static void startActivityPoint(final Context context, final Geopoint coords, final String displayedName) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(Intents.EXTRA_COORDS, coords);
        navigateIntent.putExtra(Intents.EXTRA_NAME, displayedName);
        context.startActivity(navigateIntent);
    }

    public static void startActivityCache(final Context context, final Geocache cache) {
        final Intent navigateIntent = new Intent(context, CompassActivity.class);
        navigateIntent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
        context.startActivity(navigateIntent);
    }

}
