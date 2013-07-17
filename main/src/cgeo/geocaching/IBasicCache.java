/**
 *
 */
package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;

public interface IBasicCache extends ILogable, ICoordinates {

    public abstract String getGuid();

    /**
     * @return Tradi, multi etc.
     */
    public abstract CacheType getType();

    /**
     * @return Micro, small etc.
     */
    public abstract CacheSize getSize();

    /**
     * @return true if the user already found the cache
     *
     */
    public abstract boolean isFound();

    /**
     * @return true if the cache is disabled, false else
     */
    public abstract boolean isDisabled();

    /**
     * @return Difficulty assessment
     */
    public abstract float getDifficulty();

    /**
     * @return Terrain assessment
     */
    public abstract float getTerrain();



}
