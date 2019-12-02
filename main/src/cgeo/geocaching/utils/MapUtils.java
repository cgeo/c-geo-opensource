package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class MapUtils {

    private MapUtils() {
        // utility class
    }

    public static void showInvalidMapfileMessage(final Context context) {
        Dialogs.messageNeutral((Activity) context, context.getString(R.string.warn_invalid_mapfile), R.string.more_information, (dialog, which) -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setDataAndType(Uri.parse(context.getString(R.string.settings_offline_maps_url)), "text/html");
            context.startActivity(intent);
        });
    }
}
