package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractLocusApp;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.res.Resources;

import java.util.ArrayList;

class LocusApp extends AbstractLocusApp implements NavigationApp {

    LocusApp(Resources res) {
        super(res);
    }

    /**
     * Show a single cache with waypoints or a single waypoint in Locus.
     * This method constructs a list of cache and waypoints only.
     *
     * @see AbstractLocusApp#showInLocus
     * @author koem
     */
    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res, cgCache cache,
            final cgSearch search, cgWaypoint waypoint, final Geopoint coords) {

        if (cache == null && waypoint == null && coords == null) {
            return false;
        }

        if (isInstalled(activity)) { // TODO: is this if-statement really necessary?
            final ArrayList<Object> points = new ArrayList<Object>();

            // add cache if present
            if (cache != null && cache.getCoords() != null) {
                points.add(cache);
            }

            // add waypoint if present
            if (waypoint != null && waypoint.getCoords() != null) {
                points.add(waypoint);
            }

            showInLocus(points, true, activity);

            return true;
        }

        return false;
    }

}
