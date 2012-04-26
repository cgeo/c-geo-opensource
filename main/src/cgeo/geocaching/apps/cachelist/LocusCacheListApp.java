package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractLocusApp;
import cgeo.geocaching.cgCache;

import org.apache.commons.collections.CollectionUtils;

import android.app.Activity;
import android.content.Intent;

import java.util.List;

class LocusCacheListApp extends AbstractLocusApp implements CacheListApp {

    private boolean export;

    public LocusCacheListApp(boolean export) {
        super(getString(export ? R.string.caches_map_locus_export : R.string.caches_map_locus), Intent.ACTION_VIEW);
        this.export = export;
    }

    /**
     * show caches in Locus
     *
     * @see AbstractLocusApp#showInLocus
     * @author koem
     */
    @Override
    public boolean invoke(IGeoData geo, List<cgCache> cacheList, Activity activity, final SearchResult search) {
        if (CollectionUtils.isEmpty(cacheList)) {
            return false;
        }

        showInLocus(cacheList, false, export, activity);

        return true;
    }

}
