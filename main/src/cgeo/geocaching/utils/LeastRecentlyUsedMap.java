package cgeo.geocaching.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for caching objects. Don't mix up with a geocache !
 *
 * The LeastRecentlyUsedMap is basically a LinkedHashMap which can be configured to have certain modes of operation:
 * <ul>
 * <li> LRU_CACHE means that the elements are updated in the LinkedList on every get() access,
 *      so the objects that are dropped are the ones that haven't been used the longest</li>
 * <li> BOUNDED means that objects are updated only when they are put,
 *      so the objects that are dropped are the ones that haven't been written the longest</li>
 * </ul>
 *
 * @author blafoo
 * @author Teschi
 */
public abstract class LeastRecentlyUsedMap<K, V> extends LinkedHashMap<K, V> {

    private static enum OperationModes {
        LRU_CACHE, BOUNDED
    }

    private static final long serialVersionUID = -5077882607489806620L;

    private final int maxEntries;
    private final OperationModes opMode;
    private RemoveHandler<V> removeHandler;

    // store the HashMap parameters for serialization, as we can't access the originals in the LinkedHashMap
    final int initialCapacity;
    final float loadFactor;

    protected LeastRecentlyUsedMap(int maxEntries, int initialCapacity, float loadFactor, OperationModes opMode) {
    	super(initialCapacity, loadFactor, (opMode==OperationModes.LRU_CACHE));
    	this.initialCapacity = initialCapacity;
    	this.loadFactor = loadFactor;
        this.maxEntries = maxEntries;
        this.opMode = opMode;
    }

    protected LeastRecentlyUsedMap(int maxEntries, OperationModes opMode) {
    	this(maxEntries, 16, 0.75f, opMode);
    }

    @Override
    public V put(K key, V value) {
    	// in case the underlying Map is not running with accessOrder==true, the map won't notice any changes
    	// of existing keys, so for the normal BOUNDED mode we remove and put the value to get its order updated.
    	if (opMode == OperationModes.BOUNDED && containsKey(key)) {
            // avoid trigger the remove notification
            final V oldVal = super.remove(key);
    		put(key, value);
    		return oldVal;
    	} else {
    		return super.put(key, value);
    	}
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

    public int getMaxEntries() {
		return maxEntries;
	}

    @Override
    public V remove(Object key) {

        V toBeRemoved = super.remove(key);

        if (null != toBeRemoved && null != removeHandler) {
            removeHandler.onRemove(toBeRemoved);
        }

        return toBeRemoved;
    }

    public void setRemoveHandler(RemoveHandler<V> removeHandler) {
        this.removeHandler = removeHandler;
    }

    public static class LruCache<K, V> extends LeastRecentlyUsedMap<K, V> {
        private static final long serialVersionUID = 9028478916221334454L;

        public LruCache(int maxEntries, int initialCapacity, float loadFactor) {
            super(maxEntries, initialCapacity, loadFactor, OperationModes.LRU_CACHE);
        }

        public LruCache(int maxEntries) {
            super(maxEntries, OperationModes.LRU_CACHE);
        }
    }

    public static class Bounded<K, V> extends LeastRecentlyUsedMap<K, V> {

        private static final long serialVersionUID = -1476389304214398315L;

        public Bounded(int maxEntries, int initialCapacity, float loadFactor) {
            super(maxEntries, initialCapacity, loadFactor, OperationModes.BOUNDED);
        }

        public Bounded(int maxEntries) {
            super(maxEntries, OperationModes.BOUNDED);
        }
    }

    public interface RemoveHandler<V> {

        /**
         * Method will be called on remove
         *
         * @param toBeRemoved
         */
        void onRemove(V toBeRemoved);

    }

}
