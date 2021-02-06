package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.utils.Log;

import android.net.Uri;
import android.os.Bundle;

/* Receives a download URL via "mf-v4-map" scheme, e. g. from openandromaps.org */
class MapDownloaderReceiverSchemeMap extends AbstractActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Uri uri = getIntent().getData();
        final String host = uri.getHost();
        final String path = uri.getPath();
        Log.i("MapDownloaderReceiverSchemeMap: host=" + host + ", path=" + path);

        // check for OpenAndroMaps
        if (host.equals("download.openandromaps.org") && path.startsWith("/mapsV4/") && path.endsWith(".zip")) {
            // remap Uri to their ftp server
            final Uri newUri = Uri.parse(getString(R.string.mapserver_openandromaps_downloadurl) + path.substring(8));
            MapDownloaderUtils.triggerDownload(this, OfflineMap.OfflineMapType.MAP_DOWNLOAD_TYPE_OPENANDROMAPS.id, newUri, "", System.currentTimeMillis(), this::callback);
        } else {
            // generic map download
            Log.w("MapDownloaderReceiverSchemeMap: Received map download intent from unknown source: " + uri.toString());
            finish();
        }
    }

    public void callback() {
        finish();
    }
}
