package cgeo.geocaching.network;

import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.utils.MemorySubject;
import cgeo.geocaching.utils.PeriodicHandler;
import cgeo.geocaching.utils.PeriodicHandler.PeriodicHandlerListener;
import cgeo.geocaching.utils.Version;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Looper;

import java.util.Locale;

public class StatusUpdater extends MemorySubject<StatusUpdater.Status> implements Runnable, PeriodicHandlerListener {

    static public class Status {
        final public String message;
        final public String messageId;
        final public String icon;
        final public String url;

        Status(final String message, final String messageId, final String icon, final String url) {
            this.message = message;
            this.messageId = messageId;
            this.icon = icon;
            this.url = url;
        }

        final static public Status closeoutStatus =
                new Status("", "status_closeout_warning", "attribute_abandonedbuilding", "http://www.cgeo.org/closeout/");

        final static public Status defaultStatus() {
            return VERSION.SDK_INT < VERSION_CODES.ECLAIR_MR1 ? closeoutStatus : null;
        }
    }

    @Override
    public void onPeriodic() {
        final JSONObject response =
                Network.requestJSON("http://status.cgeo.org/api/status.json",
                        new Parameters("version_code", String.valueOf(Version.getVersionCode(cgeoapplication.getInstance())),
                                "version_name", Version.getVersionName(cgeoapplication.getInstance()),
                                "locale", Locale.getDefault().toString()));
        if (response != null) {
            notifyObservers(new Status(get(response, "message"), get(response, "message_id"), get(response, "icon"), get(response, "url")));
        }
    }

    private static String get(final JSONObject json, final String key) {
        try {
            return json.getString(key);
        } catch (final JSONException e) {
            return null;
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        new PeriodicHandler(1800000L, this).start();
        Looper.loop();
    }

}
