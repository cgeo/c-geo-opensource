package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CryptUtils;

import android.app.Activity;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort {

    private String cS;

    public OCApiLiveConnector(String name, String host, String prefix, int cKResId, int cSResId) {
        super(name, host, prefix, CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cKResId)));

        cS = CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cSResId));
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        return new SearchResult(OkapiClient.getCachesBBox(viewport, this));
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {

        return new SearchResult(OkapiClient.getCachesAround(center, this));
    }

    public String getCS() {
        return CryptUtils.rot13(cS);
    }

    @Override
    public boolean supportsWatchList() {
        return true;
    }

    @Override
    public boolean addToWatchlist(Geocache cache) {
        final boolean added = OkapiClient.setWatchState(cache, true, this);

        if (added) {
            cgData.saveChangedCache(cache);
        }

        return added;
    }

    @Override
    public boolean removeFromWatchlist(Geocache cache) {
        final boolean removed = OkapiClient.setWatchState(cache, false, this);

        if (removed) {
            cgData.saveChangedCache(cache);
        }

        return removed;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public ILoggingManager getLoggingManager(Activity activity, Geocache cache) {
        return new OkapiLoggingManager(activity, this, cache);
    }

    @Override
    public boolean canLog(Geocache cache) {
        return true;
    }
}
