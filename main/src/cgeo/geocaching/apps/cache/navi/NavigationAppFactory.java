package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.ArrayUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public final class NavigationAppFactory extends AbstractAppFactory {
    private static NavigationApp[] apps = new NavigationApp[] {};

    private static NavigationApp[] getNavigationApps(Resources res) {
        if (ArrayUtils.isEmpty(apps)) {
            apps = new NavigationApp[] {
                    // compass
                    new RadarApp(res),
                    new InternalMap(res),
                    new StaticMapApp(res),
                    new LocusApp(res),
                    new RMapsApp(res),
                    new GoogleMapsApp(res),
                    new GoogleNavigationApp(res),
                    new StreetviewApp(res),
                    new OruxMapsApp(res) };
        }
        return apps;
    }

    public static void addMenuItems(final Menu menu, final Activity activity,
            final Resources res) {
        addMenuItems(menu, activity, res, true);
    }

    public static void addMenuItems(final Menu menu, final Activity activity,
            final Resources res, final boolean showInternalMap) {
        for (NavigationApp app : getNavigationApps(res)) {
            if (app.isInstalled(activity)) {
                if (showInternalMap || !(app instanceof InternalMap)) {
                    menu.add(0, app.getId(), 0, app.getName());
                }
            }
        }
    }

    public static boolean onMenuItemSelected(final MenuItem item,
            final cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final cgSearch search, cgWaypoint waypoint, final Geopoint destination) {
        NavigationApp app = (NavigationApp) getAppFromMenuItem(item, apps);
        if (app != null) {
            try {
                return app.invoke(geo, activity, res, cache,
                        search, waypoint, destination);
            } catch (Exception e) {
                Log.e(Settings.tag, "NavigationAppFactory.onMenuItemSelected: " + e.toString());
            }
        }
        return false;
    }

}
