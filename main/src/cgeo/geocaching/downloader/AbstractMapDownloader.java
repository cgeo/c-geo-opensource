package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.FileUtils;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.StringRes;

import java.util.List;

abstract class AbstractMapDownloader extends AbstractDownloader {

    public static final int ICONRES_MAP = R.drawable.ic_menu_mapmode;

    AbstractMapDownloader(final Download.DownloadType offlineMapType, final @StringRes int mapBase, final @StringRes int mapSourceName, final @StringRes int mapSourceInfo, final @StringRes int projectUrl, final @StringRes int likeItUrl) {
        super(offlineMapType, mapBase, mapSourceName, mapSourceInfo, projectUrl, likeItUrl, PersistableFolder.OFFLINE_MAPS);
        this.iconRes = ICONRES_MAP;
        this.forceExtension = FileUtils.MAP_FILE_EXTENSION;
    }

    @Override
    protected void onSuccessfulReceive(final Uri result) {
        // update offline maps
        MapsforgeMapProvider.getInstance().updateOfflineMaps(result);
    }

    // check if any of the given file exists in the given path
    // if none exists: confirm to & download first
    protected void findOrDownload(final Activity activity, final String[] filenames, final String baseUrl, final Download.DownloadType offlineMapType, final Runnable callback) {
        boolean anyFileFound = false;
        final List<ContentStorage.FileInformation> dirContent = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_THEMES);
        for (ContentStorage.FileInformation fi : dirContent) {
            for (String filename : filenames) {
                if (fi.name.equals(filename)) {
                    anyFileFound = true;
                    break;
                }
            }
        }

        if (!anyFileFound) {
            Dialogs.confirm(activity, activity.getString(R.string.downloadmap_install_theme_title), activity.getString(R.string.downloadmap_install_theme_info), activity.getString(android.R.string.ok), (d, w) -> {
                final Uri newUri = Uri.parse(baseUrl + filenames[0]);
                DownloaderUtils.triggerDownload(activity, R.string.download_title, offlineMapType.id, newUri, "", "", System.currentTimeMillis(), callback);
            }, dialog -> callback.run());
        } else {
            callback.run();
        }
    }

}
