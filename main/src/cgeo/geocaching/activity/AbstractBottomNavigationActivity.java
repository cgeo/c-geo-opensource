package cgeo.geocaching.activity;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchActivity;
import cgeo.geocaching.databinding.ActivityBottomNavigationBinding;
import cgeo.geocaching.maps.DefaultMap;

import android.content.Intent;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.jetbrains.annotations.NotNull;


public abstract class AbstractBottomNavigationActivity extends AbstractActionBarActivity implements BottomNavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemReselectedListener {
    public static final @IdRes
    int MENU_MAP = R.id.page_map;
    public static final @IdRes
    int MENU_LIST = R.id.page_list;
    public static final @IdRes
    int MENU_SEARCH = R.id.page_search;
    public static final @IdRes
    int MENU_NEARBY = R.id.page_nearby;
    public static final @IdRes
    int MENU_MORE = R.id.page_more;


    private ActivityBottomNavigationBinding wrapper = null;
    private boolean doubleBackToExitPressedOnce = false;


    @Override
    public void setContentView(final View contentView) {
        wrapper = ActivityBottomNavigationBinding.inflate(getLayoutInflater());
        wrapper.activityContent.addView(contentView);
        updateSelectedItemId();
        // add item selected listener only after item selection has been updated, as it would otherwise directly call the listener
        wrapper.activityBottomNavigation.setOnNavigationItemSelectedListener(this);

        super.setContentView(wrapper.getRoot());
    }

    @Override
    public void setContentView(final int layoutResID) {
        final View view = getLayoutInflater().inflate(layoutResID, null);
        setContentView(view);
    }

    @Override
    public void onBackPressed() {
        if (!isTaskRoot() || doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.touch_again_to_exit, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    /**
     * WARNING: This triggers {@link AbstractBottomNavigationActivity#onNavigationItemSelected} while changing the current selection.
     */
    private void updateSelectedItemId() {
        final int menuId = getSelectedBottomItemId();

        if (menuId == 0) {
            wrapper.activityBottomNavigation.setVisibility(View.GONE);
        } else {
            wrapper.activityBottomNavigation.setVisibility(View.VISIBLE);
            wrapper.activityBottomNavigation.setSelectedItemId(menuId);
        }
    }

    /**
     *
     * @return the menu item id or 0 to hide the bottom navigation
     */
    public abstract @IdRes
    int getSelectedBottomItemId();

    @Override
    public void onNavigationItemReselected(final @NonNull @NotNull MenuItem item) {
        // do nothing by default. Can be overridden by subclasses.
    }

    @Override
    public boolean onNavigationItemSelected(final @NonNull @NotNull MenuItem item) {
        final int id = item.getItemId();

        if (id == getSelectedBottomItemId()) {
            onNavigationItemReselected(item);
            return true;
        }

        if (id == MENU_MAP) {
            startActivity(DefaultMap.getLiveMapIntent(this));
        } else if (id == MENU_LIST) {
            CacheListActivity.startActivityOffline(this);
        } else if (id == MENU_SEARCH) {
            startActivity(new Intent(this, SearchActivity.class));
        } else if (id == MENU_NEARBY) {
            startActivity(CacheListActivity.getNearestIntent(this));
        } else if (id == MENU_MORE) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            throw new IllegalStateException("unknown navigation item selected"); // should never happen
        }
        // avoid weired transitions
        ActivityMixin.overrideTransitionToFade(this);

        finish();
        return true;
    }


// TODO: 14.06.2021 do we need the cache counter badge at all?
//    private void updateCacheCounter() {
//        AndroidRxUtils.bindActivity(activity, DataStore.getAllCachesCountObservable()).subscribe(count -> {
//            if (count != 0) {
//                final BadgeDrawable badge = wrapper.activityBottomNavigation.getOrCreateBadge(MENU_LIST);
//                badge.setNumber(count);
//            } else {
//                wrapper.activityBottomNavigation.removeBadge(MENU_LIST);
//            }
//        }, throwable -> Log.e("Unable to add bubble count", throwable));
//    }
}
