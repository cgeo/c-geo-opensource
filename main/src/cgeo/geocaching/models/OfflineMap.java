package cgeo.geocaching.models;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.AbstractMapDownloader;
import cgeo.geocaching.settings.MapDownloaderMapsforge;
import cgeo.geocaching.settings.MapDownloaderOpenAndroMaps;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.OfflineMapUtils;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;

public class OfflineMap {

    private final String name;
    private final Uri uri;
    private final boolean isDir;
    private final long dateInfo;
    private final String sizeInfo;
    private String addInfo;
    private final OfflineMapType type;

    public OfflineMap(final String name, final Uri uri, final boolean isDir, final String dateISO, final String sizeInfo, final OfflineMapType type) {
        this.name = OfflineMapUtils.getDisplayName(name);
        this.uri = uri;
        this.isDir = isDir;
        this.sizeInfo = sizeInfo;
        this.addInfo = "";
        this.dateInfo = CalendarUtils.parseYearMonthDay(dateISO);
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    public boolean getIsDir() {
        return isDir;
    }

    public String getDateInfoAsString() {
        return CalendarUtils.yearMonthDay(dateInfo);
    }

    public long getDateInfo() {
        return dateInfo;
    }

    public String getSizeInfo() {
        return sizeInfo;
    }

    public void setAddInfo(final String addInfo) {
        this.addInfo = addInfo;
    }

    public String getAddInfo() {
        return addInfo;
    }

    public OfflineMapType getType() {
        return type;
    }

    public String getTypeAsString() {
        return OfflineMapType.getInstance(type.id).mapSourceName;
    }

    public enum OfflineMapType {
        // id values must not be changed as they are referenced in the database
        MAP_DOWNLOAD_TYPE_MAPSFORGE(1),
        MAP_DOWNLOAD_TYPE_OPENANDROMAPS(2);

        public final int id;
        public static final int DEFAULT = MAP_DOWNLOAD_TYPE_MAPSFORGE.id;
        private static final ArrayList<OfflineMapTypeDescriptor> offlineMapTypes = new ArrayList<>();

        OfflineMapType(final int id) {
            this.id = id;
        }

        public static ArrayList<OfflineMapTypeDescriptor> getOfflineMapTypes() {
            buildOfflineMapTypesList();
            return offlineMapTypes;
        }

        @Nullable
        public static AbstractMapDownloader getInstance(final int typeId) {
            buildOfflineMapTypesList();
            for (OfflineMapTypeDescriptor descriptor : offlineMapTypes) {
                if (descriptor.type.id == typeId) {
                    return descriptor.instance;
                }
            }
            return null;
        }

        private static void buildOfflineMapTypesList() {
            if (offlineMapTypes.size() == 0) {
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_MAPSFORGE, MapDownloaderMapsforge.getInstance(), R.string.downloadmap_source_mapsforge_name));
                offlineMapTypes.add(new OfflineMapTypeDescriptor(MAP_DOWNLOAD_TYPE_OPENANDROMAPS, MapDownloaderOpenAndroMaps.getInstance(), R.string.downloadmap_source_openandromaps_name));
            }
        }
    }

    public static class OfflineMapTypeDescriptor {
        public final OfflineMapType type;
        public final AbstractMapDownloader instance;
        public final int name;

        @NonNull
        @Override
        public String toString() {
            return CgeoApplication.getInstance().getString(name);
        }

        OfflineMapTypeDescriptor(final OfflineMapType type, final AbstractMapDownloader instance, final @StringRes int name) {
            this.type = type;
            this.instance = instance;
            this.name = name;
        }
    }
}
