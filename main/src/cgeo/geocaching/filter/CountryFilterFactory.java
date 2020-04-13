package cgeo.geocaching.filter;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryFilterFactory implements IFilterFactory {


    static class CountryFilter extends AbstractFilter {
        private String location = null;

        public static final Creator<CountryFilter> CREATOR
                = new Creator<CountryFilter>() {

            @Override
            public CountryFilter createFromParcel(final Parcel in) {
                return new CountryFilter(in);
            }

            @Override
            public CountryFilter[] newArray(final int size) {
                return new CountryFilter[size];
            }
        };

        CountryFilter(final String location, final String title) {
            super(title);
            this.location = location;
        }

        protected CountryFilter(final Parcel in) {
            super(in);
            location = in.readString();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(location);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            final @NonNull String location = cache.getLocation();

            return location != null && location.endsWith(this.location);
        }

    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {

        final Map<String, IFilter> filters = new HashMap<>();

        final String separator = ", ";

        for (final String location : DataStore.getStoredLocations()) {

            final int indexOfSeparator = location.lastIndexOf(separator);
            final String countryKey = location.substring(indexOfSeparator == -1 ? 0 : indexOfSeparator + separator.length());

            if (!filters.containsKey(countryKey)) {
                filters.put(countryKey, new CountryFilter(countryKey, countryKey));
            }
        }

        return new ArrayList<>(filters.values());
    }

}
