package cgeo.geocaching.enumerations;

import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.loaders.AbstractSearchLoader.CacheListLoaderType;

import androidx.annotation.NonNull;

public enum CacheListType {
    OFFLINE(true, CacheListLoaderType.OFFLINE, AbstractBottomNavigationActivity.MENU_LIST, true),
    POCKET(false, CacheListLoaderType.POCKET, 0),
    HISTORY(true, CacheListLoaderType.HISTORY, AbstractBottomNavigationActivity.MENU_LIST, true),
    NEAREST(false, CacheListLoaderType.NEAREST, AbstractBottomNavigationActivity.MENU_NEARBY),
    COORDINATE(false, CacheListLoaderType.COORDINATE, AbstractBottomNavigationActivity.MENU_SEARCH),
    KEYWORD(false, CacheListLoaderType.KEYWORD, AbstractBottomNavigationActivity.MENU_SEARCH),
    ADDRESS(false, CacheListLoaderType.ADDRESS, AbstractBottomNavigationActivity.MENU_SEARCH),
    FINDER(false, CacheListLoaderType.FINDER, AbstractBottomNavigationActivity.MENU_SEARCH),
    OWNER(false, CacheListLoaderType.OWNER, AbstractBottomNavigationActivity.MENU_SEARCH),
    MAP(false, CacheListLoaderType.MAP, AbstractBottomNavigationActivity.MENU_MAP),
    SEARCH_FILTER(false, CacheListLoaderType.SEARCH_FILTER, AbstractBottomNavigationActivity.MENU_SEARCH);

    /**
     * whether or not this list allows switching to another list
     */
    public final boolean canSwitch;
    /**
     * The corresponding bottom navigation item which should be selected while the list is active
     *
     * assign 0 if navigation view shouldn't be displayed
     */
    public final int navigationMenuItem;
    public final boolean isStoredInDatabase;

    @NonNull public final CacheListLoaderType loaderType;

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, final int navigationMenuItem) {
        this(canSwitch, loaderType, navigationMenuItem, false);
    }

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, final int navigationMenuItem, final boolean isStoredInDatabase) {
        this.canSwitch = canSwitch;
        this.loaderType = loaderType;
        this.navigationMenuItem = navigationMenuItem;
        this.isStoredInDatabase = isStoredInDatabase;
    }

    public int getLoaderId() {
        return loaderType.getLoaderId();
    }

}
