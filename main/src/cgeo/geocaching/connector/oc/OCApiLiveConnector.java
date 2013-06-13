package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.oc.OkapiClient.UserInfo;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;

public class OCApiLiveConnector extends OCApiConnector implements ISearchByCenter, ISearchByViewPort {

    private String cS;
    private UserInfo userInfo = new UserInfo(StringUtils.EMPTY, 0, false);

    public OCApiLiveConnector(String name, String host, String prefix, int cKResId, int cSResId, ApiSupport apiSupport) {
        super(name, host, prefix, CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cKResId)), apiSupport);

        cS = CryptUtils.rot13(cgeoapplication.getInstance().getResources().getString(cSResId));
    }

    @Override
    public boolean isActivated() {
        return Settings.isOCConnectorActive();
    }

    @Override
    public SearchResult searchByViewport(Viewport viewport, String[] tokens) {
        return new SearchResult(OkapiClient.getCachesBBox(viewport, this));
    }

    @Override
    public SearchResult searchByCenter(Geopoint center) {

        return new SearchResult(OkapiClient.getCachesAround(center, this));
    }

    @Override
    public OAuthLevel getSupportedAuthLevel() {
        // TODO the tokens must be available connector specific
        if (StringUtils.isNotBlank(Settings.getOCDETokenPublic()) && StringUtils.isNotBlank(Settings.getOCDETokenSecret())) {
            return OAuthLevel.Level3;
        }
        return OAuthLevel.Level1;
    }

    @Override
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

    public boolean supportsPersonalization() {
        return getSupportedAuthLevel() == OAuthLevel.Level3;
    }

    public boolean retrieveUserInfo() {
        userInfo = OkapiClient.getUserInfo(this);
        return userInfo.isRetrieveSuccessful();
    }

    public Object getUserName() {
        return userInfo.getName();
    }

    public int getCachesFound() {
        return userInfo.getFinds();
    }
}
