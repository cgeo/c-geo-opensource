package cgeo.geocaching;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.go4cache.Go4Cache;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;

public class cgGeo {

    private LocationManager geoManager = null;
    private cgUpdateLoc geoUpdate = null;
    private cgeoGeoListener geoNetListener = null;
    private cgeoGeoListener geoGpsListener = null;
    private cgeoGpsStatusListener geoGpsStatusListener = null;
    private Location locGps = null;
    private Location locNet = null;
    private long locGpsLast = 0L;
    public Location location = null;
    public LocationProviderType locationProvider = LocationProviderType.LAST;
    public Geopoint coordsNow = null;
    public Geopoint coordsBefore = null;
    public Double altitudeNow = null;
    public Float bearingNow = null;
    public Float speedNow = null;
    public Float accuracyNow = null;
    public Integer satellitesVisible = null;
    public Integer satellitesFixed = null;

    public cgGeo(cgUpdateLoc geoUpdateIn) {
        geoUpdate = geoUpdateIn;

        geoNetListener = new cgeoGeoListener();
        geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);

        geoGpsListener = new cgeoGeoListener();
        geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);

        geoGpsStatusListener = new cgeoGpsStatusListener();

        initGeo();
    }

    private void initGeo() {
        location = null;
        locationProvider = LocationProviderType.LAST;
        coordsNow = null;
        altitudeNow = null;
        bearingNow = null;
        speedNow = null;
        accuracyNow = null;
        satellitesVisible = 0;
        satellitesFixed = 0;

        if (geoManager == null) {
            geoManager = (LocationManager) cgeoapplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
        }

        lastLoc();

        geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);
        geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);
        geoManager.addGpsStatusListener(geoGpsStatusListener);

        try {
            geoManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, geoNetListener);
        } catch (Exception e) {
            Log.w(Settings.tag, "There is no NETWORK location provider");
        }

        try {
            geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, geoGpsListener);
        } catch (Exception e) {
            Log.w(Settings.tag, "There is no GPS location provider");
        }
    }

    public void closeGeo() {
        if (geoManager != null && geoNetListener != null) {
            geoManager.removeUpdates(geoNetListener);
        }
        if (geoManager != null && geoGpsListener != null) {
            geoManager.removeUpdates(geoGpsListener);
        }
        if (geoManager != null) {
            geoManager.removeGpsStatusListener(geoGpsStatusListener);
        }
    }

    public void replaceUpdate(cgUpdateLoc geoUpdateIn) {
        geoUpdate = geoUpdateIn;

        if (geoUpdate != null) {
            geoUpdate.updateLoc(this);
        }
    }

    public class cgeoGeoListener implements LocationListener {

        public String active = null;

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // nothing
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                locGps = location;
                locGpsLast = System.currentTimeMillis();
            } else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                locNet = location;
            }

            selectBest(location.getProvider());
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                if (geoManager != null && geoNetListener != null) {
                    geoManager.removeUpdates(geoNetListener);
                }
            } else if (provider.equals(LocationManager.GPS_PROVIDER)) {
                if (geoManager != null && geoGpsListener != null) {
                    geoManager.removeUpdates(geoGpsListener);
                }
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                if (geoNetListener == null) {
                    geoNetListener = new cgeoGeoListener();
                }
                geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);
            } else if (provider.equals(LocationManager.GPS_PROVIDER)) {
                if (geoGpsListener == null) {
                    geoGpsListener = new cgeoGeoListener();
                }
                geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);
            }
        }

        public void setProvider(String provider) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                if (geoManager != null && geoManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    active = provider;
                } else {
                    active = null;
                }
            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                if (geoManager != null && geoManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    active = provider;
                } else {
                    active = null;
                }
            }
        }
    }

    public class cgeoGpsStatusListener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                GpsStatus status = geoManager.getGpsStatus(null);
                Iterator<GpsSatellite> statusIterator = status.getSatellites().iterator();

                int satellites = 0;
                int fixed = 0;

                while (statusIterator.hasNext()) {
                    GpsSatellite sat = statusIterator.next();
                    if (sat.usedInFix()) {
                        fixed++;
                    }
                    satellites++;

                    /*
                     * satellite signal strength
                     * if (sat.usedInFix()) {
                     * Log.d(Settings.tag, "Sat #" + satellites + ": " + sat.getSnr() + " FIX");
                     * } else {
                     * Log.d(Settings.tag, "Sat #" + satellites + ": " + sat.getSnr());
                     * }
                     */
                }

                boolean changed = false;
                if (satellitesVisible == null || satellites != satellitesVisible) {
                    satellitesVisible = satellites;
                    changed = true;
                }
                if (satellitesFixed == null || fixed != satellitesFixed) {
                    satellitesFixed = fixed;
                    changed = true;
                }

                if (changed) {
                    selectBest(null);
                }
            }
        }
    }

    private void selectBest(String initProvider) {
        if (locNet == null && locGps != null) { // we have only GPS
            assign(locGps);
            return;
        }

        if (locNet != null && locGps == null) { // we have only NET
            assign(locNet);
            return;
        }

        if (satellitesFixed > 0) { // GPS seems to be fixed
            assign(locGps);
            return;
        }

        if (initProvider != null && initProvider.equals(LocationManager.GPS_PROVIDER)) { // we have new location from GPS
            assign(locGps);
            return;
        }

        if (locGpsLast > (System.currentTimeMillis() - 30 * 1000)) { // GPS was working in last 30 seconds
            assign(locGps);
            return;
        }

        assign(locNet); // nothing else, using NET
    }

    private void assign(final Geopoint coords) {
        if (coords == null) {
            return;
        }

        locationProvider = LocationProviderType.LAST;
        coordsNow = coords;
        altitudeNow = null;
        bearingNow = 0f;
        speedNow = 0f;
        accuracyNow = 999f;

        if (geoUpdate != null) {
            geoUpdate.updateLoc(this);
        }
    }

    private void assign(Location loc) {
        if (loc == null) {
            locationProvider = LocationProviderType.LAST;
            return;
        }

        location = loc;

        String provider = location.getProvider();
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationProviderType.GPS;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            locationProvider = LocationProviderType.NETWORK;
        } else if (provider.equalsIgnoreCase("last")) {
            locationProvider = LocationProviderType.LAST;
        }

        coordsNow = new Geopoint(location.getLatitude(), location.getLongitude());
        cgeoapplication.getInstance().setLastCoords(coordsNow);

        if (location.hasAltitude() && locationProvider != LocationProviderType.LAST) {
            altitudeNow = location.getAltitude() + Settings.getAltCorrection();
        } else {
            altitudeNow = null;
        }
        if (location.hasBearing() && locationProvider != LocationProviderType.LAST) {
            bearingNow = location.getBearing();
        } else {
            bearingNow = 0f;
        }
        if (location.hasSpeed() && locationProvider != LocationProviderType.LAST) {
            speedNow = location.getSpeed();
        } else {
            speedNow = 0f;
        }
        if (location.hasAccuracy() && locationProvider != LocationProviderType.LAST) {
            accuracyNow = location.getAccuracy();
        } else {
            accuracyNow = 999f;
        }

        if (locationProvider == LocationProviderType.GPS) {
            // save travelled distance only when location is from GPS
            if (coordsBefore != null) {
                final float dst = coordsBefore.distanceTo(coordsNow);

                if (dst > 0.005) {
                    coordsBefore = coordsNow;
                }
            } else if (coordsBefore == null) { // values aren't initialized
                coordsBefore = coordsNow;
            }
        }

        if (geoUpdate != null) {
            geoUpdate.updateLoc(this);
        }

        if (locationProvider == LocationProviderType.GPS || locationProvider == LocationProviderType.NETWORK) {
            Go4Cache.signalCoordinates(coordsNow);
        }
    }

    public void lastLoc() {
        assign(cgeoapplication.getInstance().getLastCoords());

        Location lastGps = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastGps != null) {
            lastGps.setProvider("last");
            assign(lastGps);

            Log.i(Settings.tag, "Using last location from GPS");
            return;
        }

        Location lastGsm = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (lastGsm != null) {
            lastGsm.setProvider("last");
            assign(lastGsm);

            Log.i(Settings.tag, "Using last location from NETWORK");
            return;
        }
    }
}
