package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

public interface NavigationApp extends App {
    public boolean invoke(final Activity activity,
            final cgCache cache, final cgWaypoint waypoint,
            final Geopoint coords);

    boolean isEnabled(final cgWaypoint waypoint);

    boolean isEnabled(final Geopoint geopoint);
}
