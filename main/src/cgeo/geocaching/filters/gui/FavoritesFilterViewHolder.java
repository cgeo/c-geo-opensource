package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.ui.ContinuousRangeSlider;
import cgeo.geocaching.ui.ToggleButtonGroup;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class FavoritesFilterViewHolder extends BaseFilterViewHolder<FavoritesGeocacheFilter> {

    private ContinuousRangeSlider slider;
    private ToggleButtonGroup percentage;

    private float maxValue = 1000;
    private float granularity = 1;


    @Override
    public View createView() {

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        percentage = new ToggleButtonGroup(getActivity());
        percentage.setValues(Arrays.asList(getActivity().getString(R.string.cache_filter_favorites_absolute), getActivity().getString(R.string.cache_filter_favorites_percentage)));

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(5));
        ll.addView(percentage, llp);
        percentage.setChangeListener((v, i) -> resetSliderScale());

        slider = new ContinuousRangeSlider(getActivity());
        resetSliderScale();
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(20));
        ll.addView(slider, llp);

        return ll;
    }

    private void resetSliderScale() {
        if (percentage.getSelectedValue() == 0) {
            maxValue = 1000;
            granularity = 1;
            slider.setScale(-0.2f, 1000.2f, f -> {
                maxValue = 1000;
                if (f <= 0) {
                    return "0";
                }
                if (f > 1000) {
                    return ">1000";
                }
                return "" + Math.round(f);
            }, 6);
            slider.setRange(-0.2f, 1000.2f);
        } else {
            maxValue = 1;
            granularity = 100;
            slider.setScale(-0.002f, 1.002f, f -> {
                if (f <= 0) {
                    return "0%";
                }
                if (f >= 1) {
                    return "100%";
                }
                return Math.round(f * 100) + "%";
            }, 6);
            slider.setRange(-0.002f, 1.002f);
        }
    }


    @Override
    public void setViewFromFilter(final FavoritesGeocacheFilter filter) {
        percentage.setSelectedValue(filter.isPercentage() ? 1 : 0);
        resetSliderScale();
        slider.setRange(filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue(), filter.getMaxRangeValue() == null ? 1500f : filter.getMaxRangeValue());
    }

    @Override
    public FavoritesGeocacheFilter createFilterFromView() {
        final FavoritesGeocacheFilter filter = createFilter();
        filter.setPercentage(percentage.getSelectedValue() == 1);
        final ImmutablePair<Float, Float> range = slider.getRange();
        filter.setMinMaxRange(
            range.left < 0 ? null : Math.round(range.left * granularity) / granularity,
            range.right > maxValue ? null : Math.round(range.right * granularity) / granularity);
        return filter;
    }

}
