package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.connector.gc.GCParser;

import android.content.Context;

public class UsernameGeocacheListLoader extends AbstractSearchLoader {

    private final String username;

    public UsernameGeocacheListLoader(Context context, String username) {
        super(context);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return GCParser.searchByUsername(username, OldSettings.getCacheType(), OldSettings.isShowCaptcha(), this);
    }

}
