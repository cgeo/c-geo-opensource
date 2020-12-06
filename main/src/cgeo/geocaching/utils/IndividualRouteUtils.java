package cgeo.geocaching.utils;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectIndividualRouteFileActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.export.IndividualRouteExport;
import cgeo.geocaching.files.GPXIndividualRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.RouteSortActivity;
import cgeo.geocaching.models.ManualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action2;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import static android.app.Activity.RESULT_OK;

import java.io.File;

public class IndividualRouteUtils {

    private static final int REQUEST_SORT_INDIVIDUAL_ROUTE = 4712;
    private static final int REQUEST_CODE_GET_INDIVIDUALROUTEFILE = 47122;

    private IndividualRouteUtils() {
        // utility class
    }

    /**
     * Enable/disable track related menu entries
     *
     * @param menu menu to be configured
     */
    public static void onPrepareOptionsMenu(final Menu menu, final ManualRoute route, final boolean targetIsSet) {
        final boolean isVisible = route != null && route.getNumSegments() > 0;
        menu.findItem(R.id.menu_sort_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_center_on_route).setVisible(isVisible);
        menu.findItem(R.id.menu_export_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_clear_individual_route).setVisible(isVisible);
        menu.findItem(R.id.menu_clear_targets).setVisible(targetIsSet || Settings.isAutotargetIndividualRoute());
    }

    /**
     * Check if selected menu entry is regarding individual routes
     *
     * @param activity calling activity
     * @param id       menu entry id
     * @return true, if selected menu entry is individual route related and consumed / false else
     */
    public static boolean onOptionsItemSelected(final Activity activity, final int id, final ManualRoute route, final Runnable clearIndividualRoute, final Route.CenterOnPosition centerOnPosition, final Action2<Geopoint, String> setTarget) {
        if (id == R.id.menu_load_individual_route) {
            if (null == route || route.getNumSegments() == 0) {
                startIndividualRouteFileSelector(activity);
            } else {
                Dialogs.confirm(activity, R.string.map_load_individual_route, R.string.map_load_individual_route_confirm, (dialog, which) -> startIndividualRouteFileSelector(activity));
            }
        } else if (id == R.id.menu_sort_individual_route) {
            activity.startActivityForResult(new Intent(activity, RouteSortActivity.class), REQUEST_SORT_INDIVIDUAL_ROUTE);
        } else if (id == R.id.menu_center_on_route) {
            route.setCenter(centerOnPosition);
        } else if (id == R.id.menu_export_individual_route) {
            new IndividualRouteExport(activity, route);
        } else if (id == R.id.menu_clear_individual_route) {
            Dialogs.confirm(activity, R.string.map_clear_individual_route, R.string.map_clear_individual_route_confirm, (dialog, which) -> {
                clearIndividualRoute.run();
                ActivityMixin.invalidateOptionsMenu(activity);
            });
        } else if (id == R.id.menu_autotarget_individual_route) {
            Settings.setAutotargetIndividualRoute(!Settings.isAutotargetIndividualRoute());
            route.triggerTargetUpdate(!Settings.isAutotargetIndividualRoute());
            ActivityMixin.invalidateOptionsMenu(activity);
        } else if (id == R.id.menu_clear_targets) {
            if (setTarget != null) {
                setTarget.call(null, null);
            }
            Settings.setAutotargetIndividualRoute(false);
            route.triggerTargetUpdate(true);
            ActivityMixin.invalidateOptionsMenu(activity);
            ActivityMixin.showToast(activity, R.string.map_manual_targets_cleared);
        } else {
            return false;
        }
        return true;
    }

    private static void startIndividualRouteFileSelector(final Activity activity) {
        final Intent intent = new Intent(activity, SelectIndividualRouteFileActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_GET_INDIVIDUALROUTEFILE);
    }

    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data, final Runnable reloadIndividualRoute) {
        if (requestCode == REQUEST_SORT_INDIVIDUAL_ROUTE) {
            reloadIndividualRoute.run();
            return true;
        } else if (requestCode == REQUEST_CODE_GET_INDIVIDUALROUTEFILE && resultCode == RESULT_OK) {
            final String filename = data.getStringExtra(Intents.EXTRA_GPX_FILE);
            if (null != filename) {
                final File file = new File(filename);
                if (!file.isDirectory()) {
                    GPXIndividualRouteImporter.doImport(activity, file);
                    reloadIndividualRoute.run();
                }
            }
            return true;
        }
        return false;
    }

}
