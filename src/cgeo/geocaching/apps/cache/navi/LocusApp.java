package cgeo.geocaching.apps.cache.navi;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWarning;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractLocusApp;

class LocusApp extends AbstractLocusApp implements NavigationApp {

	LocusApp(Resources res) {
		super(res);
	}

	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res,
			cgWarning warning, cgCache cache,
			Long searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		if (cache == null && waypoint == null && latitude == null
				&& longitude == null) {
			return false;
		}
		try {
			if (isInstalled(activity)) {
				final ArrayList<cgWaypoint> waypoints = new ArrayList<cgWaypoint>();
				// get only waypoints with coordinates
				if (cache != null && cache.waypoints != null
						&& cache.waypoints.isEmpty() == false) {
					for (cgWaypoint wp : cache.waypoints) {
						if (wp.latitude != null && wp.longitude != null) {
							waypoints.add(wp);
						}
					}
				}

				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final DataOutputStream dos = new DataOutputStream(baos);

				dos.writeInt(1); // not used
				if (cache != null) {
					if (waypoints == null || waypoints.isEmpty() == true) {
						dos.writeInt(1); // cache only
					} else {
						dos.writeInt((1 + waypoints.size())); // cache and
																// waypoints
					}
				} else {
					dos.writeInt(1); // one waypoint
				}

				int icon = -1;
				if (cache != null) {
					icon = cgBase.getIcon(true, cache.type, cache.own, cache.found,
							cache.disabled || cache.archived);
				} else if (waypoint != null) {
					icon = cgBase.getIcon(false, waypoint.type, false, false, false);
				} else {
					icon = cgBase.getIcon(false, "waypoint", false, false, false);
				}

				if (icon > 0) {
					// load icon
					Bitmap bitmap = BitmapFactory.decodeResource(res, icon);
					ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos2);
					byte[] image = baos2.toByteArray();

					dos.writeInt(image.length);
					dos.write(image);
				} else {
					// no icon
					dos.writeInt(0); // no image
				}

				// name
				if (cache != null && cache.name != null
						&& cache.name.length() > 0) {
					dos.writeUTF(cache.name);
				} else if (waypoint != null && waypoint.name != null
						&& waypoint.name.length() > 0) {
					dos.writeUTF(waypoint.name);
				} else {
					dos.writeUTF("");
				}

				// description
				if (cache != null && cache.geocode != null
						&& cache.geocode.length() > 0) {
					dos.writeUTF(cache.geocode.toUpperCase());
				} else if (waypoint != null && waypoint.lookup != null
						&& waypoint.lookup.length() > 0) {
					dos.writeUTF(waypoint.lookup.toUpperCase());
				} else {
					dos.writeUTF("");
				}

				// additional data :: keyword, button title, package, activity,
				// data name, data content
				if (cache != null && cache.geocode != null
						&& cache.geocode.length() > 0) {
					dos.writeUTF("intent;c:geo;cgeo.geocaching;cgeo.geocaching.cgeodetail;geocode;"
							+ cache.geocode);
				} else if (waypoint != null && waypoint.id != null
						&& waypoint.id > 0) {
					dos.writeUTF("intent;c:geo;cgeo.geocaching;cgeo.geocaching.cgeowaypoint;id;"
							+ waypoint.id);
				} else {
					dos.writeUTF("");
				}

				if (cache != null && cache.latitude != null
						&& cache.longitude != null) {
					dos.writeDouble(cache.latitude); // latitude
					dos.writeDouble(cache.longitude); // longitude
				} else if (waypoint != null && waypoint.latitude != null
						&& waypoint.longitude != null) {
					dos.writeDouble(waypoint.latitude); // latitude
					dos.writeDouble(waypoint.longitude); // longitude
				} else {
					dos.writeDouble(latitude); // latitude
					dos.writeDouble(longitude); // longitude
				}

				// cache waypoints
				if (waypoints != null && waypoints.isEmpty() == false) {
					for (cgWaypoint wp : waypoints) {
						if (wp == null || wp.latitude == null
								|| wp.longitude == null) {
							continue;
						}

						final int wpIcon = cgBase.getIcon(false, wp.type, false,
								false, false);

						if (wpIcon > 0) {
							// load icon
							Bitmap bitmap = BitmapFactory.decodeResource(res,
									wpIcon);
							ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
							bitmap.compress(Bitmap.CompressFormat.PNG, 100,
									baos2);
							byte[] image = baos2.toByteArray();

							dos.writeInt(image.length);
							dos.write(image);
						} else {
							// no icon
							dos.writeInt(0); // no image
						}

						// name
						if (wp.lookup != null && wp.lookup.length() > 0) {
							dos.writeUTF(wp.lookup.toUpperCase());
						} else {
							dos.writeUTF("");
						}

						// description
						if (wp.name != null && wp.name.length() > 0) {
							dos.writeUTF(wp.name);
						} else {
							dos.writeUTF("");
						}

						// additional data :: keyword, button title, package,
						// activity, data name, data content
						if (wp.id != null && wp.id > 0) {
							dos.writeUTF("intent;c:geo;cgeo.geocaching;cgeo.geocaching.cgeowaypoint;id;"
									+ wp.id);
						} else {
							dos.writeUTF("");
						}

						dos.writeDouble(wp.latitude); // latitude
						dos.writeDouble(wp.longitude); // longitude
					}
				}

				final Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("menion.points:data"));
				intent.putExtra("data", baos.toByteArray());

				activity.startActivity(intent);

				return true;
			}
		} catch (Exception e) {
			// nothing
		}
		return false;
	}

}
