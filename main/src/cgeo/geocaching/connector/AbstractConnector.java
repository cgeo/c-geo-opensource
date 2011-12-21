package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CancellableHandler;

public abstract class AbstractConnector implements IConnector {

    @Override
    public boolean canHandle(String geocode) {
        return false;
    }

    @Override
    public boolean supportsRefreshCache(cgCache cache) {
        return false;
    }

    @Override
    public boolean supportsWatchList() {
        return false;
    }

    @Override
    public boolean supportsLogging() {
        return false;
    }

    @Override
    public String getLicenseText(final cgCache cache) {
        return null;
    }

    @Override
    public boolean supportsUserActions() {
        return false;
    }

    @Override
    public boolean supportsCachesAround() {
        return false;
    }

    @Override
    public cgSearch searchByCoordinate(Geopoint center) {
        return null;
    }

    @Override
    public cgSearch searchByGeocode(String geocode, String guid, cgeoapplication app, cgSearch search, int reason, CancellableHandler handler) {
        return null;
    }

    protected static boolean isNumericId(final String string) {
        try {
            return Integer.valueOf(string) > 0;
        } catch (NumberFormatException e) {
        }
        return false;
    }
}
