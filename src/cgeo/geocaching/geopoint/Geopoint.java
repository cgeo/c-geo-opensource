package cgeo.geocaching.geopoint;

import android.location.Location;

/**
 * Abstraction of geographic point.
 */
public class Geopoint
{
    public static final double kmInMiles = 1 / 1.609344;
    public static final double deg2rad   = Math.PI / 180;
    public static final double rad2deg   = 180 / Math.PI;
    public static final float  erad      = 6371.0f;

    private double latitude;
    private double longitude;

    /**
     * Creates new Geopoint with given latitude and longitude (both degree).
     *
     * @param lat latitude
     * @param lon longitude
     */
    public Geopoint(final double lat, final double lon)
    {
        setLatitude(lat);
        setLongitude(lon);
    }

    /**
     * Creates new Geopoint with given latitude and longitude (both microdegree).
     *
     * @param lat latitude
     * @param lon longitude
     */
    public Geopoint(final int lat, final int lon)
    {
        setLatitude(lat * 1E-6);
        setLongitude(lon * 1E-6);
    }

    /**
     * Creates new Geopoint with latitude and longitude parsed from string.
     *
     * @param text string to parse
     * @see GeopointParser.parse()
     */
    public Geopoint(final String text)
    {
        setLatitude(GeopointParser.parseLatitude(text));
        setLongitude(GeopointParser.parseLongitude(text));
    }

    /**
     * Creates new Geopoint with given Geopoint. This is similar to clone().
     *
     * @param gp the Geopoint to clone
     */
    public Geopoint(final Geopoint gp)
    {
        this(gp.getLatitude(), gp.getLongitude());
    }

    /**
     * Creates new Geopoint with given Location.
     *
     * @param gp the Location to clone
     */
    public Geopoint(final Location loc) {
        this(loc.getLatitude(), loc.getLongitude());
    }

	/**
     * Set latitude in degree.
     *
     * @param lat latitude
     * @return this
     * @throws MalformedCoordinateException if not -90 <= lat <= 90
     */
    private void setLatitude(final double lat)
    {
        if (lat <= 90 && lat >= -90)
        {
            latitude  = lat;
        }
        else
        {
            throw new MalformedCoordinateException("malformed latitude: " + lat);
        }
    }

    /**
     * Get latitude in degree.
     *
     * @return latitude
     */
    public double getLatitude()
    {
        return latitude;
    }

    /**
     * Get latitude in microdegree.
     *
     * @return latitude
     */
    public int getLatitudeE6()
    {
        return (int) (latitude * 1E6);
    }

    /**
     * Set longitude in degree.
     *
     * @param lon longitude
     * @return this
     * @throws MalformedCoordinateException if not -180 <= lon <= 180
     */
    private void setLongitude(final double lon)
    {
        if (lon <= 180 && lon >=-180)
        {
            longitude = lon;
        }
        else
        {
            throw new MalformedCoordinateException("malformed longitude: " + lon);
        }
    }

    /**
     * Get longitude in degree.
     *
     * @return longitude
     */
    public double getLongitude()
    {
        return longitude;
    }

    /**
     * Get longitude in microdegree.
     *
     * @return longitude
     */
    public int getLongitudeE6()
    {
        return (int) (longitude * 1E6);
    }

    /**
     * Get distance and bearing from the current point to a target.
     *
     * @param target The target
     * @return An array of floats: the distance in meters, then the bearing in degrees
     */
    private float[] pathTo(final Geopoint target) {
        float[] results = new float[2];
        android.location.Location.distanceBetween(getLatitude(), getLongitude(), target.getLatitude(), target.getLongitude(), results);
        return results;
    }

    /**
     * Calculates distance to given Geopoint in km.
     *
     * @param gp target
     * @return distance in km
     * @throws GeopointException if there is an error in distance calculation
     */
    public float distanceTo(final Geopoint gp)
    {
        return pathTo(gp)[0] / 1000;
    }

    /**
     * Calculates bearing to given Geopoint in degree.
     *
     * @param gp target
     * @return bearing in degree.
     */
    public float bearingTo(final Geopoint gp)
    {
        return pathTo(gp)[1];
    }

    /**
     * Calculates geopoint from given bearing and distance.
     *
     * @param bearing bearing in degree
     * @param distance distance in km
     * @return the projected geopoint
     */
    public Geopoint project(final double bearing, final double distance)
    {
        final double rlat1     = latitude * deg2rad;
        final double rlon1     = longitude * deg2rad;
        final double rbearing  = bearing * deg2rad;
        final double rdistance = distance / erad;

        final double rlat = Math.asin(Math.sin(rlat1) * Math.cos(rdistance) + Math.cos(rlat1) * Math.sin(rdistance) * Math.cos(rbearing));
        final double rlon = rlon1 + Math.atan2(Math.sin(rbearing) * Math.sin(rdistance) * Math.cos(rlat1), Math.cos(rdistance) - Math.sin(rlat1) * Math.sin(rlat));

        return new Geopoint(rlat * rad2deg, rlon * rad2deg);
    }

    /**
     * Checks if given Geopoint is identical with this Geopoint.
     *
     * @param gp Geopoint to check
     * @return true if identical, false otherwise
     */
    public boolean isEqualTo(Geopoint gp)
    {
        return null != gp && gp.getLatitude() == latitude && gp.getLongitude() == longitude;
    }

    /**
     * Checks if given Geopoint is similar to this Geopoint with tolerance.
     *
     * @param gp Geopoint to check
     * @param tolerance tolerance in km
     * @return true if similar, false otherwise
     */
    public boolean isEqualTo(Geopoint gp, double tolerance)
    {
        return null != gp && distanceTo(gp) <= tolerance;
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(GeopointFormatter format)
    {
        return format.format(this);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(String format)
    {
        return GeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(GeopointFormatter.Format format)
    {
        return GeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates with default format.
     * Default format is decimalminutes, e.g. N 52° 36.123 E 010° 03.456
     *
     * @return formatted coordinates
     */
    public String toString()
    {
        return format(GeopointFormatter.Format.LAT_LON_DECMINUTE);
    }

    public static class GeopointException
        extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public GeopointException(String msg)
        {
            super(msg);
        }
    }

    public static class MalformedCoordinateException
        extends GeopointException
    {
        private static final long serialVersionUID = 1L;

        public MalformedCoordinateException(String msg)
        {
            super(msg);
        }
    }
}
