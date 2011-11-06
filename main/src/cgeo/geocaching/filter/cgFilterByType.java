package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

public class cgFilterByType extends cgFilter {
    private CacheType cacheType;

    public cgFilterByType(final CacheType cacheType) {
        super(cacheType.id);
        this.cacheType = cacheType;
    }

    @Override
    boolean applyFilter(final cgCache cache) {
        return cacheType == cache.getCacheType();
    }

    @Override
    public String getFilterName() {
        return cacheType.getL10n();
    }
}
