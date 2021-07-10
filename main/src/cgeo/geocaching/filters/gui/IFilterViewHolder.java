package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;

import android.app.Activity;
import android.view.View;

public interface IFilterViewHolder<T extends IGeocacheFilter> {

    void init(GeocacheFilterType type, Activity activity);

    GeocacheFilterType getType();

    Activity getActivity();

    void setViewFromFilter(T filter);

    View getView();

    T createFilterFromView();

    default <H extends IFilterViewHolder<?>> boolean  isOf(final Class<H> clazz) {
        return clazz.isAssignableFrom(this.getClass());
    }

    @SuppressWarnings("unchecked")
    default <H extends IFilterViewHolder<?>> H castTo(final Class<H> clazz) {
        return (H) this;
    }
}
