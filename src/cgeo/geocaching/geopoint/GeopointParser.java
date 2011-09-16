package cgeo.geocaching.geopoint;

import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint.GeopointException;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse coordinates.
 */
public class GeopointParser
{
    private static final Pattern patternLat = Pattern.compile("([NS])\\s*(\\d+)°?(\\s*(\\d+)([\\.,](\\d+)|'?\\s*(\\d+)(''|\")?)?)?");
    private static final Pattern patternLon = Pattern.compile("([WE])\\s*(\\d+)°?(\\s*(\\d+)([\\.,](\\d+)|'?\\s*(\\d+)(''|\")?)?)?");

    private enum LatLon
    {
        LAT,
        LON
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of a String.
     * Accepts following formats and combinations of it:
     * X DD
     * X DD°
     * X DD° MM
     * X DD° MM.MMM
     * X DD° MM SS
     *
     * as well as:
     * DD.DDDDDDD
     *
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param text
     *            the string to parse
     * @return an Geopoint with parsed latitude and longitude
     * @throws ParseException
     *             if lat or lon could not be parsed
     */
    public static Geopoint parse(final String text)
    {

        double lat = parseLatitude(text);
        double lon = parseLongitude(text);

        return new Geopoint(lat, lon);
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of a String.
     * Accepts following formats and combinations of it:
     * X DD
     * X DD°
     * X DD° MM
     * X DD° MM.MMM
     * X DD° MM SS
     *
     * as well as:
     * DD.DDDDDDD
     *
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param latitude
     *            the latitude string to parse
     * @param longitude
     *            the longitude string to parse
     * @return an Geopoint with parsed latitude and longitude
     * @throws ParseException
     *             if lat or lon could not be parsed
     */
    public static Geopoint parse(final String latitude, final String longitude)
    {
        final double lat = parseLatitude(latitude);
        final double lon = parseLongitude(longitude);

        return new Geopoint(lat, lon);
    }

    /*
     * (non JavaDoc)
     * Helper for coordinates-parsing.
     */
    private static double parseHelper(final String text, final LatLon latlon)
    {

        Matcher matcher;

        if (LatLon.LAT == latlon)
        {
            matcher = patternLat.matcher(text);
        }
        else
        {
            matcher = patternLon.matcher(text);
        }

        if (matcher.find())
        {
            int sign = 1;
            int degree = 0;
            int minutes = 0;
            int seconds = 0;

            if (matcher.group(1).equalsIgnoreCase("S") || matcher.group(1).equalsIgnoreCase("W"))
            {
                sign = -1;
            }

            degree = Integer.parseInt(matcher.group(2));

            if (null != matcher.group(4))
            {
                minutes = Integer.parseInt(matcher.group(4));

                if (null != matcher.group(6))
                {
                    seconds = Math.round(Float.parseFloat("0." + matcher.group(6)) * 60);
                }
                else if (null != matcher.group(7))
                {
                    seconds = Integer.parseInt(matcher.group(7));
                }
            }

            return (double) sign * ((double) degree + (double) minutes / 60 + (double) seconds / 3600);
        }
        else // Nothing found with "N 52...", try to match string as decimaldegree
        {
            try {
                final String[] items = StringUtils.split(text.trim());
                final int index = latlon == LatLon.LON ? items.length - 1 : 0;
                return Double.parseDouble(items[index]);
            } catch (NumberFormatException e) {
                // The right exception will be raised below.
            }
        }

        throw new ParseException("Could not parse coordinates as " + latlon + ": \"" + text + "\"", latlon);
    }

    /**
     * Parses latitude out of a given string.
     *
     * @see parse()
     * @param text
     *            the string to be parsed
     * @return the latitude as decimaldegree
     * @throws ParseException
     *             if latitude could not be parsed
     */
    public static double parseLatitude(final String text)
    {
        return parseHelper(text, LatLon.LAT);
    }

    /**
     * Parses longitude out of a given string.
     *
     * @see parse()
     * @param text
     *            the string to be parsed
     * @return the longitude as decimaldegree
     * @throws ParseException
     *             if longitude could not be parsed
     */
    public static double parseLongitude(final String text)
    {
        return parseHelper(text, LatLon.LON);
    }

    public static class ParseException
            extends GeopointException
    {
        private static final long serialVersionUID = 1L;
        public final int resource;

        public ParseException(final String msg, final LatLon faulty)
        {
            super(msg);
            resource = faulty == LatLon.LAT ? R.string.err_parse_lat : R.string.err_parse_lon;
        }
    }
}
