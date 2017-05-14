package cgeo.geocaching.command;

import java.util.HashSet;
import java.util.Set;

import cgeo.geocaching.models.Geocache;

class RemoveNotUniqueHelper {

    static Set<Geocache> removeNonUniqueCaches(Set<Geocache> caches) {
        Set<Geocache> toBeRemoved = new HashSet<>();
        for (final Geocache geocache : caches) {
        if (geocache.getLists().size() > 1) {
          // stored on more than this list
          toBeRemoved.add(geocache);
        }
      }
      return toBeRemoved;
    }
}
