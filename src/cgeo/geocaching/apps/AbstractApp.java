package cgeo.geocaching.apps;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import cgeo.geocaching.cgSettings;

public abstract class AbstractApp implements App {

	protected String packageName;

	private String intent;
	private String name;

	protected AbstractApp(final String name, final String intent,
			final String packageName) {
		this.name = name;
		this.intent = intent;
		this.packageName = packageName;
	}

	protected AbstractApp(final String name, final String intent) {
		this(name, intent, null);
	}

	protected Intent getLaunchIntent(Context context) {
		if (packageName == null) {
			return null;
		}
		PackageManager packageManager = context.getPackageManager();
		try {
			// This can throw an exception where the exception type is only defined on API Level > 3
			// therefore surround with try-catch
			Intent intent = packageManager.getLaunchIntentForPackage(packageName);
			return intent;
		} catch (Exception e) {
			return null;
		}
	}

	public boolean isInstalled(final Context context) {
		if (getLaunchIntent(context) != null) {
			return true;
		}
		return isIntentAvailable(context, intent);
	}

	private static boolean isIntentAvailable(Context context, String action) {
		final Intent intent = new Intent(action);

		return isIntentAvailable(context, intent);
	}

	protected static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManager = context.getPackageManager();
		final List<ResolveInfo> list = packageManager.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);

		return list.size() > 0;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getId() {
		return getName().hashCode();
	}

	protected static cgSettings getSettings(Activity activity) {
		return new cgSettings(activity,
				activity.getSharedPreferences(cgSettings.preferences, Context.MODE_PRIVATE));
	}
}
