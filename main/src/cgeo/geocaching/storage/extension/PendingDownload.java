package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PendingDownload extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_PENDING_DOWNLOAD;

    private PendingDownload(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public String getFilename() {
        return getString1();
    }

    public String getRemoteUrl() {
        return getString2();
    }

    public long getDate() {
        return getLong1();
    }

    public int getOfflineMapTypeId() {
        return (int) getLong2();
    }

    @Nullable
    public static PendingDownload load(final long pendingDownload) {
        final DataStore.DBExtension temp = load(type, String.valueOf(pendingDownload));
        return null == temp ? null : new PendingDownload(temp);
    }

    public static void add(final long pendingDownload, @NonNull final String filename, @NonNull final String remoteUrl, final long date, final int offlineMapTypeId) {
        final String key = String.valueOf(pendingDownload);
        removeAll(type, key);
        add(type, key, date, offlineMapTypeId, 0, 0, filename, remoteUrl, "", "");
    }

    public static void remove(final long pendingDownload) {
        removeAll(type, String.valueOf(pendingDownload));
    }
}
