package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.connector.gc.GCParser;

import android.content.Context;

public class OwnerGeocacheListLoader extends AbstractSearchLoader {

    private final String username;

    public OwnerGeocacheListLoader(Context context, String username) {
        super(context);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByOwner(username, Settings.getCacheType(), Settings.isShowCaptcha(), this);
    }

}
