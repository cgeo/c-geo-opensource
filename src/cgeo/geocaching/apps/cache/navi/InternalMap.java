package cgeo.geocaching.apps.cache.navi;

import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;

class InternalMap extends AbstractInternalMap implements
		NavigationApp {

	InternalMap(Resources res) {
		super(res.getString(R.string.cache_menu_map), null);
	}

	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgCache cache,
			final UUID searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		cgSettings settings = getSettings(activity);
		Intent mapIntent = new Intent(activity, settings.getMapFactory().getMapClass());
		if (cache != null) {
			mapIntent.putExtra("detail", false);
			mapIntent.putExtra("geocode", cache.geocode);
		}
		if (searchId != null) {
			mapIntent.putExtra("detail", true);
			mapIntent.putExtra("searchid", searchId.toString());
		}
		if (waypoint != null) {
			mapIntent.putExtra("latitude", waypoint.latitude);
			mapIntent.putExtra("longitude", waypoint.longitude);
			mapIntent.putExtra("wpttype", waypoint.type);
		}

		activity.startActivity(mapIntent);
		return true;
	}

}
