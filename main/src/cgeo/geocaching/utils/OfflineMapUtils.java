package cgeo.geocaching.utils;

import cgeo.geocaching.storage.LocalStorage;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class OfflineMapUtils {

    private static final String INFOFILE_SUFFIX = "-cgeo.txt";

    private static final String PROP_PARSETYPE = "remote.parsetype";
    private static final String PROP_REMOTEPAGE = "remote.page";
    private static final String PROP_REMOTEFILE = "remote.file";
    private static final String PROP_REMOTEDATE = "remote.date";
    private static final String PROP_LOCALFILE = "local.file";
    private static final String PROP_DISPLAYNAME = "displayname";

    private static final String PROP_VALUE_MAPSFORGE = "1";

    public static class OfflineMapData {
        public int remoteParsetype;         // 1 = Mapsforge.org (FH Esslingen mirror)
        public String remotePage;           // eg: http://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v5/europe/germany/
        public String remoteFile;           // eg: hessen.map
        public long remoteDate;             // 2020-12-10 (as long)
        public String localFile;            // eg: map43.map (relative to map dir)
        public String displayName;          // eg: Hessen
    }

    private OfflineMapUtils() {
        // utility class
    }

    public static void writeInfo(@NonNull final String remoteUrl, @NonNull final String localFilename, @NonNull final String displayName, final long date) {
        try (OutputStream output = new FileOutputStream(LocalStorage.getOrCreateMapDirectory() + "/" + localFilename + INFOFILE_SUFFIX)) {
            final int i = remoteUrl.lastIndexOf("/");
            final String remotePage = i != -1 ? remoteUrl.substring(0, i) : remoteUrl;
            final String remoteFile = i != -1 ? remoteUrl.substring(i + 1) : localFilename;

            final Properties prop = new Properties();
            prop.setProperty(PROP_PARSETYPE, PROP_VALUE_MAPSFORGE);
            prop.setProperty(PROP_REMOTEPAGE, remotePage);
            prop.setProperty(PROP_REMOTEFILE, remoteFile);
            prop.setProperty(PROP_REMOTEDATE, CalendarUtils.yearMonthDay(date));
            prop.setProperty(PROP_LOCALFILE, localFilename);
            prop.setProperty(PROP_DISPLAYNAME, displayName);

            prop.store(output, null);
        } catch (IOException io) {
            // ignore
        }
    }

    /**
     * returns a list of downloaded offline maps which are still available in the local filesystem
     */
    public static ArrayList<OfflineMapData> availableOfflineMaps() {
        final ArrayList<OfflineMapData> result = new ArrayList<>();
        final String mapDir = LocalStorage.getOrCreateMapDirectory();

        for (File file : FileUtils.listFiles(mapDir, INFOFILE_SUFFIX)) {
            try (InputStream input = new FileInputStream(file)) {
                final OfflineMapData offlineMapData = new OfflineMapData();
                final Properties prop = new Properties();
                prop.load(input);
                offlineMapData.remoteParsetype = Integer.parseInt(prop.getProperty(PROP_PARSETYPE));
                offlineMapData.remotePage = prop.getProperty(PROP_REMOTEPAGE);
                offlineMapData.remoteFile = prop.getProperty(PROP_REMOTEFILE);
                offlineMapData.remoteDate = CalendarUtils.parseYearMonthDay(prop.getProperty(PROP_REMOTEDATE));
                offlineMapData.localFile = prop.getProperty(PROP_LOCALFILE);
                offlineMapData.displayName = prop.getProperty(PROP_DISPLAYNAME);

                if (StringUtils.isNotBlank(offlineMapData.localFile)) {
                    final File test = new File(mapDir + "/" + offlineMapData.localFile);
                    if (test.canRead() && test.isFile()) {
                        result.add(offlineMapData);
                    }
                }
            } catch (IOException | NumberFormatException e) {
                Log.d("Offline map property file error for " + file.toString() + ": " + e.getMessage());
            }
        }
        return result;
    }


    public static String getDisplayName(final String name) {
        // capitalize first letter + every first after a "-"
        String tempName = StringUtils.upperCase(name.substring(0, 1)) + name.substring(1);
        int pos = name.indexOf("-");
        while (pos > 0) {
            tempName = tempName.substring(0, pos + 1) + StringUtils.upperCase(tempName.substring(pos + 1, pos + 2)) + tempName.substring(pos + 2);
            pos = name.indexOf("-", pos + 1);
        }
        return tempName;
    }

}
