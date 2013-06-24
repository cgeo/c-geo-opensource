package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.Settings;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.AbstractAppFactory;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.apps.cache.CacheBeaconApp;
import cgeo.geocaching.apps.cache.GccApp;
import cgeo.geocaching.apps.cache.WhereYouGoApp;
import cgeo.geocaching.apps.cache.navi.GoogleNavigationApp.GoogleNavigationBikeApp;
import cgeo.geocaching.apps.cache.navi.GoogleNavigationApp.GoogleNavigationDrivingApp;
import cgeo.geocaching.apps.cache.navi.GoogleNavigationApp.GoogleNavigationWalkingApp;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public final class NavigationAppFactory extends AbstractAppFactory {

    public enum NavigationAppsEnum {
        /** The internal compass activity */
        COMPASS(new CompassApp(), 0),
        /** The external radar app */
        RADAR(new RadarApp(), 1),
        /** The selected map */
        INTERNAL_MAP(new InternalMap(), 2),
        /** The internal static map activity */
        STATIC_MAP(new StaticMapApp(), 3),
        /** The external Locus app */
        DOWNLOAD_STATIC_MAPS(new DownloadStaticMapsApp(), 20),
        /** The external Locus app */
        LOCUS(new LocusApp(), 4),
        /** The external RMaps app */
        RMAPS(new RMapsApp(), 5),
        /** Google Maps */
        GOOGLE_MAPS(new GoogleMapsApp(), 6),
        /** Google Navigation */
        GOOGLE_NAVIGATION(new GoogleNavigationDrivingApp(), 7),
        /** Google Streetview */
        GOOGLE_STREETVIEW(new StreetviewApp(), 8),
        /** The external OruxMaps app */
        ORUX_MAPS(new OruxMapsApp(), 9),
        /** The external navigon app */
        NAVIGON(new NavigonApp(), 10),
        /** The external Sygic app */
        SYGIC(new SygicNavigationApp(), 11),
        /**
         * Google Navigation in walking mode
         */
        GOOGLE_NAVIGATION_WALK(new GoogleNavigationWalkingApp(), 12),
        /**
         * Google Navigation in walking mode
         */
        GOOGLE_NAVIGATION_BIKE(new GoogleNavigationBikeApp(), 21),
        /**
         * Google Maps Directions
         */
        GOOGLE_MAPS_DIRECTIONS(new GoogleMapsDirectionApp(), 13),

        CACHE_BEACON(new CacheBeaconApp(), 14),
        GCC(new GccApp(), 15),
        WHERE_YOU_GO(new WhereYouGoApp(), 16);

        NavigationAppsEnum(App app, int id) {
            this.app = app;
            this.id = id;
        }

        /**
         * The app instance to use
         */
        public final App app;
        /**
         * The id - used in c:geo settings
         */
        public final int id;

        /*
         * display app name in array adapter
         *
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return app.getName();
        }
    }

    /**
     * Default way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     * <p />
     * Delegates to {@link #showNavigationMenu(Activity, cgeo.geocaching.Geocache, cgeo.geocaching.Waypoint, Geopoint, boolean, boolean)} with
     * <code>showInternalMap = true</code> and <code>showDefaultNavigation = false</code>
     *
     * @param activity
     * @param cache
     * @param waypoint
     * @param destination
     */
    public static void showNavigationMenu(final Activity activity,
            final Geocache cache, final Waypoint waypoint, final Geopoint destination) {
        showNavigationMenu(activity, cache, waypoint, destination, true, false);
    }

    /**
     * Specialized way to handle selection of navigation tool.<br />
     * A dialog is created for tool selection and the selected tool is started afterwards.
     *
     * @param activity
     * @param cache
     *            may be <code>null</code>
     * @param waypoint
     *            may be <code>null</code>
     * @param destination
     *            may be <code>null</code>
     * @param showInternalMap
     *            should be <code>false</code> only when called from within the internal map
     * @param showDefaultNavigation
     *            should be <code>false</code> by default
     *
     * @see #showNavigationMenu(Activity, cgeo.geocaching.Geocache, cgeo.geocaching.Waypoint, Geopoint)
     */
    public static void showNavigationMenu(final Activity activity,
            final Geocache cache, final Waypoint waypoint, final Geopoint destination,
            final boolean showInternalMap, final boolean showDefaultNavigation) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_navigate);
        final List<NavigationAppsEnum> items = new ArrayList<NavigationAppFactory.NavigationAppsEnum>();
        final int defaultNavigationTool = Settings.getDefaultNavigationTool();
        for (final NavigationAppsEnum navApp : getInstalledNavigationApps()) {
            if ((showInternalMap || !(navApp.app instanceof InternalMap)) &&
                    (showDefaultNavigation || defaultNavigationTool != navApp.id)) {
                boolean add = false;
                if (cache != null && navApp.app instanceof CacheNavigationApp && navApp.app.isEnabled(cache)) {
                    add = true;
                }
                if (waypoint != null && navApp.app instanceof WaypointNavigationApp && ((WaypointNavigationApp) navApp.app).isEnabled(waypoint)) {
                    add = true;
                }
                if (destination != null && navApp.app instanceof GeopointNavigationApp) {
                    add = true;
                }
                if (add) {
                    items.add(navApp);
                }
            }
        }
        /*
         * Using an ArrayAdapter with list of NavigationAppsEnum items avoids
         * handling between mapping list positions allows us to do dynamic filtering of the list based on use case.
         */
        final ArrayAdapter<NavigationAppsEnum> adapter = new ArrayAdapter<NavigationAppsEnum>(activity, android.R.layout.select_dialog_item, items);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                final NavigationAppsEnum selectedItem = adapter.getItem(item);
                final App app = selectedItem.app;
                if (cache != null) {
                    navigateCache(activity, cache, app);
                }
                else if (waypoint != null) {
                    navigateWaypoint(activity, waypoint, app);
                }
                else {
                    navigateGeopoint(activity, destination, app);
                }
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Returns all installed navigation apps.
     *
     * @return
     */
    public static List<NavigationAppsEnum> getInstalledNavigationApps() {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<NavigationAppsEnum>();
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled()) {
                installedNavigationApps.add(appEnum);
            }
        }
        return installedNavigationApps;
    }

    /**
     * Returns all installed navigation apps for default navigation.
     *
     * @return
     */
    public static List<NavigationAppsEnum> getInstalledDefaultNavigationApps() {
        final List<NavigationAppsEnum> installedNavigationApps = new ArrayList<NavigationAppsEnum>();
        for (final NavigationAppsEnum appEnum : NavigationAppsEnum.values()) {
            if (appEnum.app.isInstalled() && appEnum.app.isDefaultNavigationApp()) {
                installedNavigationApps.add(appEnum);
            }
        }
        return installedNavigationApps;
    }

    /**
     * This offset is used to build unique menu ids to avoid collisions of ids in menus
     */
    private static final int MENU_ITEM_OFFSET = 12345;


    /**
     * Adds the installed navigation tools to the given menu.
     * Use {@link #onMenuItemSelected(MenuItem, Activity, cgeo.geocaching.Geocache)} on
     * selection event to start the selected navigation tool.
     *
     * <b>Only use this way if {@link #showNavigationMenu(Activity, cgeo.geocaching.Geocache, cgeo.geocaching.Waypoint, Geopoint, boolean, boolean)} is
     * not suitable for the given usecase.</b>
     *
     * @param menu
     */
    public static void addMenuItems(final Menu menu, final Geocache cache) {
        for (final NavigationAppsEnum navApp : getInstalledNavigationApps()) {
            if (navApp.app instanceof CacheNavigationApp) {
                final CacheNavigationApp cacheApp = (CacheNavigationApp) navApp.app;
                if (cacheApp.isEnabled(cache)) {
                    menu.add(0, MENU_ITEM_OFFSET + navApp.id, 0, navApp.app.getName());
                }
            }
        }
    }

    public static void addMenuItems(final Menu menu, final Waypoint waypoint) {
        for (final NavigationAppsEnum navApp : getInstalledNavigationApps()) {
            if (navApp.app instanceof WaypointNavigationApp) {
                final WaypointNavigationApp waypointApp = (WaypointNavigationApp) navApp.app;
                if (waypointApp.isEnabled(waypoint)) {
                    menu.add(0, MENU_ITEM_OFFSET + navApp.id, 0, navApp.app.getName());
                }
            }
        }
    }

    /**
     * Handles menu selections for menu entries created with {@link #addMenuItems(Menu, cgeo.geocaching.Geocache)}.
     *
     * @param item
     * @param activity
     * @param cache
     * @return
     */
    public static boolean onMenuItemSelected(final MenuItem item, Activity activity, Geocache cache) {
        final App menuItem = getAppFromMenuItem(item);
        navigateCache(activity, cache, menuItem);
        return menuItem != null;
    }

    private static void navigateCache(Activity activity, Geocache cache, App app) {
        if (app instanceof CacheNavigationApp) {
            final CacheNavigationApp cacheApp = (CacheNavigationApp) app;
            cacheApp.navigate(activity, cache);
        }
    }

    public static boolean onMenuItemSelected(final MenuItem item, Activity activity, Waypoint waypoint) {
        final App menuItem = getAppFromMenuItem(item);
        navigateWaypoint(activity, waypoint, menuItem);
        return menuItem != null;
    }

    private static void navigateWaypoint(Activity activity, Waypoint waypoint, App app) {
        if (app instanceof WaypointNavigationApp) {
            final WaypointNavigationApp waypointApp = (WaypointNavigationApp) app;
            waypointApp.navigate(activity, waypoint);
        }
    }

    private static void navigateGeopoint(Activity activity, Geopoint destination, App app) {
        if (app instanceof GeopointNavigationApp) {
            final GeopointNavigationApp geopointApp = (GeopointNavigationApp) app;
            geopointApp.navigate(activity, destination);
        }
    }

    private static App getAppFromMenuItem(MenuItem item) {
        final int id = item.getItemId();
        for (final NavigationAppsEnum navApp : NavigationAppsEnum.values()) {
            if (MENU_ITEM_OFFSET + navApp.id == id) {
                return navApp.app;
            }
        }
        return null;
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param defaultNavigation
     *
     * @param activity
     * @param cache
     */
    public static void startDefaultNavigationApplication(int defaultNavigation, Activity activity, Geocache cache) {
        if (cache == null || cache.getCoords() == null) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }

        navigateCache(activity, cache, getDefaultNavigationApplication(defaultNavigation));
    }

    private static App getDefaultNavigationApplication(int defaultNavigation) {
        if (defaultNavigation == 2) {
            return getNavigationAppForId(Settings.getDefaultNavigationTool2());
        }
        return getNavigationAppForId(Settings.getDefaultNavigationTool());
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param activity
     * @param waypoint
     */
    public static void startDefaultNavigationApplication(int defaultNavigation, Activity activity, Waypoint waypoint) {
        if (waypoint == null || waypoint.getCoords() == null) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }
        navigateWaypoint(activity, waypoint, getDefaultNavigationApplication(defaultNavigation));
    }

    /**
     * Starts the default navigation tool if correctly set and installed or the compass app as default fallback.
     *
     * @param activity
     * @param destination
     */
    public static void startDefaultNavigationApplication(int defaultNavigation, Activity activity, final Geopoint destination) {
        if (destination == null) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_location_unknown));
            return;
        }

        navigateGeopoint(activity, destination, getDefaultNavigationApplication(defaultNavigation));
    }

    /**
     * Returns the default navigation tool if correctly set and installed or the compass app as default fallback
     *
     * @return never <code>null</code>
     */
    public static App getDefaultNavigationApplication() {
        return getDefaultNavigationApplication(1);
    }

    private static App getNavigationAppForId(final int navigationAppId) {
        final List<NavigationAppsEnum> installedNavigationApps = getInstalledNavigationApps();

        for (final NavigationAppsEnum navigationApp : installedNavigationApps) {
            if (navigationApp.id == navigationAppId) {
                return navigationApp.app;
            }
        }
        // default navigation tool wasn't set already or couldn't be found (not installed any more for example)
        return NavigationAppsEnum.COMPASS.app;
    }

}
