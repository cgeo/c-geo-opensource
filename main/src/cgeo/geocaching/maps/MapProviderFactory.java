package cgeo.geocaching.maps;

import cgeo.geocaching.maps.google.GoogleMapProvider;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.view.Menu;

import java.util.SortedMap;
import java.util.TreeMap;

public class MapProviderFactory {

    private final static int GOOGLEMAP_BASEID = 30;
    private final static int MFMAP_BASEID = 40;

    private static MapProviderFactory instance = null;

    private MapProvider[] mapProviders;
    private SortedMap<Integer, String> mapSources;

    private MapProviderFactory() {
        mapProviders = new MapProvider[] { new GoogleMapProvider(GOOGLEMAP_BASEID), new MapsforgeMapProvider(MFMAP_BASEID) };
        mapSources = new TreeMap<Integer, String>();
        for (MapProvider mp : mapProviders) {
            mapSources.putAll(mp.getMapSources());
        }
    }

    private static synchronized void initInstance() {
        if (null == instance) {
            instance = new MapProviderFactory();
        }
    }

    private static MapProviderFactory getInstance() {
        if (null == instance) {
            initInstance();
        }
        return instance;
    }

    public static SortedMap<Integer, String> getMapSources() {
        return getInstance().mapSources;
    }

    public static boolean IsValidSourceId(int sourceId) {
        return getInstance().mapSources.containsKey(sourceId);
    }

    public static boolean IsSameProvider(int sourceId1, int sourceId2) {
        for (MapProvider mp : getInstance().mapProviders) {
            if (mp.IsMySource(sourceId1) && mp.IsMySource(sourceId2)) {
                return true;
            }
        }
        return false;
    }

    public static MapProvider getMapProvider(int sourceId) {
        for (MapProvider mp : getInstance().mapProviders) {
            if (mp.IsMySource(sourceId)) {
                return mp;
            }
        }
        return getInstance().mapProviders[0];
    }

    public static int getSourceOrdinalFromId(int sourceId) {

        int sourceOrdinal = 0;
        for (int key : getInstance().mapSources.keySet()) {
            if (sourceId == key) {
                return sourceOrdinal;
            }
            sourceOrdinal++;
        }
        return 0;
    }

    public static int getSourceIdFromOrdinal(int sourceOrdinal) {
        int count = 0;
        for (int key : getInstance().mapSources.keySet()) {
            if (sourceOrdinal == count) {
                return key;
            }
            count++;
        }
        return getInstance().mapSources.firstKey();
    }

    public static void addMapviewMenuItems(Menu parentMenu, int groupId, int currentSource) {

        SortedMap<Integer, String> mapSources = getInstance().mapSources;

        for (int key : mapSources.keySet()) {
            parentMenu.add(groupId, key, 0, mapSources.get(key)).setCheckable(true).setChecked(key == currentSource);
        }
    }

    public static int getMapSourceFromMenuId(int menuId) {
        return menuId;
    }
}
