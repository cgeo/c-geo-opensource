package cgeo.geocaching.command;

import cgeo.geocaching.models.Geocache;

import java.util.HashSet;
import java.util.Set;

final class RemoveNotUniqueHelper {

    private RemoveNotUniqueHelper() {
    }

    static Set<Geocache> removeNonUniqueCaches(final Set<Geocache> caches) {
        final Set<Geocache> toBeRemoved = new HashSet<>();
        for (final Geocache geocache : caches) {
        if (geocache.getLists().size() > 1) {
          // stored on more than this list
          toBeRemoved.add(geocache);
        }
      }
      return toBeRemoved;
    }
}
