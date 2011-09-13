package cgeo.geocaching.apps.cache.navi;

import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

class GoogleMapsApp extends AbstractNavigationApp implements NavigationApp {

	GoogleMapsApp(final Resources res) {
		super(res.getString(R.string.cache_menu_map_ext), null);
	}

	@Override
	public boolean isInstalled(Context context) {
		return true;
	}

	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgCache cache,
			final UUID searchId, cgWaypoint waypoint, final Geopoint coords) {
		if (cache == null && waypoint == null && coords == null) {
			return false;
		}

		try {
			if (cache != null && cache.coords != null) {
				startActivity(activity, cache.coords);
			} else if (waypoint != null && waypoint.coords != null) {
				startActivity(activity, waypoint.coords);
			}

			return true;
		} catch (Exception e) {
			// nothing
		}

		Log.i(cgSettings.tag, "cgBase.runExternalMap: No maps application available.");

		if (res != null) {
			ActivityMixin.showToast(activity, res.getString(R.string.err_application_no));
		}

		return false;
	}

	private void startActivity(Activity activity, final Geopoint coords) {
		activity.startActivity(new Intent(Intent.ACTION_VIEW,
				Uri.parse("geo:" + coords.getLatitude() + "," + coords.getLongitude())));
		// INFO: q parameter works with Google Maps, but breaks cooperation with all other apps
	}

}
