package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;

import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractFilter implements IFilter {
    private final String name;

    protected AbstractFilter(final String name) {
        this.name = name;
    }

    @Override
    public void filter(@NonNull final List<Geocache> list) {
        final List<Geocache> itemsToRemove = new ArrayList<>();
        for (final Geocache item : list) {
            if (!accepts(item)) {
                itemsToRemove.add(item);
            }
        }
        list.removeAll(itemsToRemove);
    }

    @Override
    public String getName() {
        return name;
    }

    /*
     * show name in array adapter
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
}
