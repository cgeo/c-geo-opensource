package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.maps.mapsforge.v6.layers.ITileLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.MultiRendererLayer;
import cgeo.geocaching.maps.mapsforge.v6.layers.RendererLayer;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PublicLocalFolder;
import cgeo.geocaching.storage.PublicLocalStorage;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;


public final class MapsforgeMapProvider extends AbstractMapProvider {

    private static final String OFFLINE_MAP_DEFAULT_ATTRIBUTION = "---";
    private static final Map<Uri, String> OFFLINE_MAP_ATTRIBUTIONS = new HashMap<>();

    private MapItemFactory mapItemFactory = new MapsforgeMapItemFactory();


    private MapsforgeMapProvider() {
        final Resources resources = CgeoApplication.getInstance().getResources();

        //register fixed maps
        registerMapSource(new OsmMapSource(this, resources.getString(R.string.map_source_osm_mapnik)));
        registerMapSource(new OsmdeMapSource(this, resources.getString(R.string.map_source_osm_osmde)));
        registerMapSource(new CyclosmMapSource(this, resources.getString(R.string.map_source_osm_cyclosm)));

        //initiale offline maps (necessary here in constructor only so that initial setMapSource will succeed)
        updateOfflineMaps();
    }

    private static final class Holder {
        private static final MapsforgeMapProvider INSTANCE = new MapsforgeMapProvider();
    }

    public static MapsforgeMapProvider getInstance() {
        return Holder.INSTANCE;
    }

    public static List<ImmutablePair<String, Uri>> getOfflineMaps() {
        //TODO
        return PublicLocalStorage.get().list(PublicLocalFolder.OFFLINE_MAPS);

    }

    @Override
    public boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return source1.getNumericalId() == source2.getNumericalId() || (!(source1 instanceof OfflineMapSource) && !(source2 instanceof OfflineMapSource));
    }

    @Override
    public Class<? extends Activity> getMapClass() {
        mapItemFactory = new MapsforgeMapItemFactory();
        return NewMap.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.mfmapv5;
    }

    @Override
    public int getMapLayoutId() {
        return R.layout.map_mapsforge_v6;
    }

    @Override
    public int getMapAttributionViewId() {
        return R.id.map_attribution;
    }

    @Override
    public MapItemFactory getMapItemFactory() {
        return mapItemFactory;
    }

    /**
     * Offline maps use the hash of the filename as ID. That way changed files can easily be detected. Also we do no
     * longer need to differentiate between internal map sources and offline map sources, as they all just have an
     * numerical ID (based on the hash code).
     */
    public static final class OfflineMapSource extends AbstractMapsforgeMapSource {

        private final Uri mapUri;

        public OfflineMapSource(final Uri mapUri, final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
            this.mapUri = mapUri;
        }

        public Uri getMapUri() {
            return mapUri;
        }

        @Override
        @NonNull
        public String getId() {
            return super.getId() + ":" + mapUri.getLastPathSegment();
        }

        @Override
        public boolean isAvailable() {
            return MapsforgeMapProvider.isValidMapFile(mapUri);
        }

        /** Create new render layer, if mapfile exists */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final MapFile mf = createMapFile(this.mapUri);
            if (mf != null) {
                MapProviderFactory.setLanguages(mf.getMapLanguages());
                return new RendererLayer(tileCache, mf, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
            }
            MapsforgeMapProvider.getInstance().invalidateMapUri(mapUri);
            return null;
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(MapsforgeMapProvider.getInstance().getAttributionFor(this.mapUri), true);
        }

   }

    public static final class CyclosmMapSource extends AbstractMapsforgeMapSource {

        public CyclosmMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceCyclosm.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_cyclosm_html), false);
        }

    }

    public static final class OsmMapSource extends AbstractMapsforgeMapSource {

        public OsmMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, OpenStreetMapMapnik.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false);
        }

    }


    public static final class OsmdeMapSource extends AbstractMapsforgeMapSource {

        public OsmdeMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, TileSourceOsmde.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
            return new ImmutablePair<>(ctx.getString(R.string.map_attribution_openstreetmapde_html), false);
        }

    }

    public static final class OfflineMultiMapSource extends AbstractMapsforgeMapSource {
        private final List<ImmutablePair<String, Uri>> mapUris;

        public OfflineMultiMapSource(final List<ImmutablePair<String, Uri>> mapUris, final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
            this.mapUris = mapUris;
        }

        @Override
        public boolean isAvailable() {
            boolean isValid = true;
            for (ImmutablePair<String, Uri> mapUri : mapUris) {
                isValid &= MapsforgeMapProvider.getInstance().isValidMapFile(mapUri.right);
            }
            return isValid;
        }

        /**
         * Create new render layer, if mapfiles exist
         */
        @Override
        public ITileLayer createTileLayer(final TileCache tileCache, final IMapViewPosition mapViewPosition) {
            final List<MapFile> mapFiles = new ArrayList<>();
            for (ImmutablePair<String, Uri> fileName : mapUris) {
                final MapFile mf = createMapFile(fileName.right);
                if (mf != null) {
                    mapFiles.add(mf);
                } else {
                    MapsforgeMapProvider.getInstance().invalidateMapUri(fileName.right);
                }
            }

            return new MultiRendererLayer(tileCache, mapFiles, mapViewPosition, false, true, false, AndroidGraphicFactory.INSTANCE);
        }

        @Override
        public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {

            final StringBuilder attributation = new StringBuilder();
            for (ImmutablePair<String, Uri> mapUri : mapUris) {
                attributation.append("<p><b>" + mapUri.left + "</b>:<br>");
                attributation.append(MapsforgeMapProvider.getInstance().getAttributionFor(mapUri.right));
                attributation.append("</p>");
            }
            return new ImmutablePair<>(attributation.toString(), true);
        }

     }

    @NonNull
    public String getAttributionFor(final Uri filePath) {
        final String att = getAttributionIfValidFor(filePath);
        return att == null ? OFFLINE_MAP_DEFAULT_ATTRIBUTION : att;
    }

    /**
     * checks whether the given Uri is a valid map file.
     * Thie methos uses cached results from previous checks
     * Note: this method MUST be static because it is indirectly used in MapsforgeMapProvider-constructur
     */
    public static boolean isValidMapFile(final Uri filePath) {
        return getAttributionIfValidFor(filePath) != null;
    }

    private static String getAttributionIfValidFor(final Uri filePath) {

        if (OFFLINE_MAP_ATTRIBUTIONS.containsKey(filePath)) {
            return OFFLINE_MAP_ATTRIBUTIONS.get(filePath);
        }
        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, readAttributionFromMapFileIfValid(filePath));
        return OFFLINE_MAP_ATTRIBUTIONS.get(filePath);
    }

    private void invalidateMapUri(final Uri filePath) {
        OFFLINE_MAP_ATTRIBUTIONS.put(filePath, null);
    }

    /**
     * Tries to open given uri as a mapfile.
     * If mapfile is invalid in any way (not available, not readable, wrong version, ...), then null is returned.
     * If mapfile is valid, then its attribution is read and returned (or a default attribution value in case attribution is null)
     */
    @Nullable
    private static String readAttributionFromMapFileIfValid(final Uri mapUri) {

        MapFile mapFile = null;
        try {
            mapFile = createMapFile(mapUri);
            if (mapFile != null && mapFile.getMapFileInfo() != null && mapFile.getMapFileInfo().fileVersion <= 5) {
                if (!StringUtils.isBlank(mapFile.getMapFileInfo().comment)) {
                    return mapFile.getMapFileInfo().comment;
                }
                if (!StringUtils.isBlank(mapFile.getMapFileInfo().createdBy)) {
                    return mapFile.getMapFileInfo().createdBy;
                }
                //map file is valid but has no attribution -> return default value
                return OFFLINE_MAP_DEFAULT_ATTRIBUTION;
            }
        }  catch (MapFileException ex) {
            Log.w(String.format("Exception reading mapfile '%s'", mapUri.toString()), ex);
        } finally {
            closeMapFileQuietly(mapFile);
        }
        return null;
    }

    private static MapFile createMapFile(final Uri mapUri) {
        if (mapUri == null) {
            return null;
        }

        final InputStream fis = PublicLocalStorage.get().openForRead(mapUri);
        if (fis != null) {
            try {
                return new MapFile((FileInputStream) fis, 0, MapProviderFactory.getLanguage(Settings.getMapLanguage()));
            } catch (MapFileException mfe) {
                Log.e("Problem opening map file '" + mapUri + "'", mfe);
            }
        }
        return null;
    }

    private static void closeMapFileQuietly(final MapFile mapFile) {
        if (mapFile != null) {
            mapFile.close();
        }
    }

    public void updateOfflineMaps() {
        updateOfflineMaps(null);
    }

    public void updateOfflineMaps(final Uri offlineMapToSet) {
        MapSource msToSet = null;
        MapProviderFactory.deleteOfflineMapSources();
        final Resources resources = CgeoApplication.getInstance().getResources();
        final List<ImmutablePair<String, Uri>> offlineMaps =
            CollectionStream.of(getOfflineMaps()).filter(mu -> isValidMapFile(mu.right)).toList();
        if (offlineMaps.size() > 1) {
            registerMapSource(new OfflineMultiMapSource(offlineMaps, this, resources.getString(R.string.map_source_osm_offline_combined)));
        }
        for (final ImmutablePair<String, Uri> mapFile : offlineMaps) {
            final String mapName = StringUtils.capitalize(StringUtils.substringBeforeLast(mapFile.left, "."));
            final OfflineMapSource offlineMapSource = new OfflineMapSource(mapFile.right, this, mapName + " (" + resources.getString(R.string.map_source_osm_offline) + ")");
            registerMapSource(offlineMapSource);
            if (offlineMapToSet != null && offlineMapToSet.equals(offlineMapSource.getMapUri())) {
                msToSet = offlineMapSource;
            }
        }
        if (msToSet != null) {
            Settings.setMapSource(msToSet);
        }
    }

}
