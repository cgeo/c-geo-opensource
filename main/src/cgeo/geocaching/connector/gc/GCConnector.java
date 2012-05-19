package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class GCConnector extends AbstractConnector implements ISearchByGeocode, ISearchByCenter {

    private static GCConnector instance;
    private static final Pattern gpxZipFilePattern = Pattern.compile("\\d{7,}(_.+)?\\.zip", Pattern.CASE_INSENSITIVE);

    private GCConnector() {
        // singleton
    }

    public static GCConnector getInstance() {
        if (instance == null) {
            instance = new GCConnector();
        }
        return instance;
    }

    @Override
    public boolean canHandle(String geocode) {
        if (geocode == null) {
            return false;
        }
        return GCConstants.PATTERN_GC_CODE.matcher(geocode).matches() || GCConstants.PATTERN_TB_CODE.matcher(geocode).matches();
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        // it would also be possible to use "http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode();
        return "http://coord.info/" + cache.getGeocode();
    }

    @Override
    public boolean supportsOwnCoordinates() {
        return true;
    }

    @Override
    public boolean supportsWatchList() {
        return true;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public String getName() {
        return "GeoCaching.com";
    }

    @Override
    public String getHost() {
        return "www.geocaching.com";
    }

    @Override
    public boolean supportsUserActions() {
        return true;
    }

    @Override
    public SearchResult searchByGeocode(final String geocode, final String guid, final CancellableHandler handler) {

        CancellableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = GCParser.requestHtmlPage(geocode, guid, "y", String.valueOf(GCConstants.NUMBER_OF_LOGS));

        if (StringUtils.isEmpty(page)) {
            final SearchResult search = new SearchResult();
            if (cgeoapplication.getInstance().isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i("Loading old cache from cache.");

                    search.addGeocode(cgeoapplication.getInstance().getGeocode(guid));
                } else {
                    search.addGeocode(geocode);
                }
                search.setError(StatusCode.NO_ERROR);
                return search;
            }

            Log.e("cgeoBase.searchByGeocode: No data from server");
            search.setError(StatusCode.COMMUNICATION_ERROR);
            return search;
        }

        final SearchResult searchResult = GCParser.parseCache(page, handler);

        if (searchResult == null || CollectionUtils.isEmpty(searchResult.getGeocodes())) {
            Log.e("cgeoBase.searchByGeocode: No cache parsed");
            return searchResult;
        }

        return searchResult.filterSearchResults(false, false, Settings.getCacheType());
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        return GCMap.searchByViewport(viewport, tokens);
    }

    @Override
    public boolean isZippedGPXFile(final String fileName) {
        return gpxZipFilePattern.matcher(fileName).matches();
    }

    @Override
    public boolean isReliableLatLon(boolean cacheHasReliableLatLon) {
        return cacheHasReliableLatLon;
    }

    public static boolean addToWatchlist(cgCache cache) {
        final boolean added = GCParser.addToWatchlist(cache);
        if (added) {
            cgeoapplication.getInstance().updateCache(cache);
        }
        return added;
    }

    public static boolean removeFromWatchlist(cgCache cache) {
        final boolean removed = GCParser.removeFromWatchlist(cache);
        if (removed) {
            cgeoapplication.getInstance().updateCache(cache);
        }
        return removed;
    }

    public static boolean addToFavorites(cgCache cache) {
        final boolean added = GCParser.addToFavorites(cache);
        if (added) {
            cgeoapplication.getInstance().updateCache(cache);
        }
        return added;
    }

    public static boolean removeFromFavorites(cgCache cache) {
        final boolean removed = GCParser.removeFromFavorites(cache);
        if (removed) {
            cgeoapplication.getInstance().updateCache(cache);
        }
        return removed;
    }

    public static boolean uploadModifiedCoordinates(cgCache cache, Geopoint wpt) {
        final boolean uploaded = GCParser.uploadModifiedCoordinates(cache, wpt);
        if (uploaded) {
            cgeoapplication.getInstance().updateCache(cache);
        }
        return uploaded;
    }

    public static boolean deleteModifiedCoordinates(cgCache cache) {
        final boolean deleted = GCParser.deleteModifiedCoordinates(cache);
        if (deleted) {
            cgeoapplication.getInstance().updateCache(cache);
        }
        return deleted;
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {
        // TODO make search by coordinate use this method. currently it is just a marker that this connector supports search by center
        return null;
    }

    @Override
    public boolean supportsFavoritePoints() {
        return true;
    }

}
