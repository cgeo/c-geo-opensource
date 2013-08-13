package cgeo.geocaching.utils;

import cgeo.geocaching.connector.gc.Tile;

public class UncertainProperty<T> {

    private final T value;
    private final int certaintyLevel;

    public UncertainProperty(T value) {
        this.value = value;
        this.certaintyLevel = Tile.ZOOMLEVEL_MAX + 1;
    }

    public UncertainProperty(T value, int certaintyLevel) {
        this.value = value;
        this.certaintyLevel = certaintyLevel;
    }

    public T getValue() {
        return value;
    }

    public int getCertaintyLevel() {
        return certaintyLevel;
    }

    public UncertainProperty<T> getMergedProperty(final UncertainProperty<T> other) {
        if (other == null) {
            return this;
        }
        if (other.certaintyLevel > certaintyLevel) {
            return other;
        }

        return this;
    }

}
