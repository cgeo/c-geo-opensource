package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.Image;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgData;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.text.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final public class OkapiClient {
    private static final String CACHE_SIZE = "size";
    private static final String CACHE_VOTES = "rating_votes";
    private static final String CACHE_NOTFOUNDS = "notfounds";
    private static final String CACHE_FOUNDS = "founds";
    private static final String CACHE_HIDDEN = "date_hidden";
    private static final String CACHE_LATEST_LOGS = "latest_logs";
    private static final String CACHE_IMAGE_URL = "url";
    private static final String CACHE_IMAGE_CAPTION = "caption";
    private static final String CACHE_IMAGE_IS_SPOILER = "is_spoiler";
    private static final String CACHE_IMAGES = "images";
    private static final String CACHE_HINT = "hint";
    private static final String CACHE_DESCRIPTION = "description";
    private static final String CACHE_RECOMMENDATIONS = "recommendations";
    private static final String CACHE_RATING = "rating";
    private static final String CACHE_TERRAIN = "terrain";
    private static final String CACHE_DIFFICULTY = "difficulty";
    private static final String CACHE_OWNER = "owner";
    private static final String CACHE_STATUS = "status";
    private static final String CACHE_TYPE = "type";
    private static final String CACHE_LOCATION = "location";
    private static final String CACHE_NAME = "name";
    private static final String CACHE_CODE = "code";

    private static final String LOG_TYPE = "type";
    private static final String LOG_COMMENT = "comment";
    private static final String LOG_DATE = "date";
    private static final String LOG_USER = "user";

    private static final String USER_USERNAME = "username";

    private static final String SERVICE_CACHE = "/okapi/services/caches/geocache";

    private static final String SERVICE_CACHE_FIELDS = "code|name|location|type|status|owner|founds|notfounds|is_found|size|difficulty|terrain|rating|rating_votes|recommendations|description|hint|images|latest_logs|date_hidden";
    private static final String SERVICE_CACHE_SMALL_FIELDS = "code|name|location|type|status|difficulty|terrain|size|is_found";

    private static final String SERVICE_SEARCH_AND_RETRIEVE = "/okapi/services/caches/shortcuts/search_and_retrieve";

    private static final String METHOD_SEARCH_NEAREST = "services/caches/search/nearest";
    private static final String METHOD_SEARCH_BBOX = "services/caches/search/bbox";
    private static final String METHOD_RETRIEVE_CACHES = "services/caches/geocaches";

    public static Geocache getCache(final String geoCode) {
        final Parameters params = new Parameters("cache_code", geoCode, "fields", SERVICE_CACHE_FIELDS);
        final JSONObject data = request(ConnectorFactory.getConnector(geoCode), SERVICE_CACHE, params);

        if (data == null) {
            return null;
        }

        return parseCache(data);
    }

    public static List<Geocache> getCachesAround(final Geopoint center, IConnector connector) {
        String centerString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, center) + "|" + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, center);
        final Parameters params = new Parameters("search_method", METHOD_SEARCH_NEAREST);
        final Map<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put("center", centerString);
        valueMap.put("limit", "20");

        addFilterParams(valueMap);

        params.add("search_params", new JSONObject(valueMap).toString());
        params.add("retr_method", METHOD_RETRIEVE_CACHES);
        params.add("retr_params", "{\"fields\": \"" + SERVICE_CACHE_SMALL_FIELDS + "\"}");
        params.add("wrap", "true");
        final JSONObject data = request(connector, SERVICE_SEARCH_AND_RETRIEVE, params);

        if (data == null) {
            return new ArrayList<Geocache>();
        }

        return parseCaches(data);
    }

    public static List<Geocache> getCachesBBox(final Viewport viewport, IConnector connector) {

        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return new ArrayList<Geocache>();
        }

        String bboxString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, viewport.bottomLeft)
                + "|" + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, viewport.bottomLeft)
                + "|" + GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, viewport.topRight)
                + "|" + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, viewport.topRight);
        final Parameters params = new Parameters("search_method", METHOD_SEARCH_BBOX);
        final Map<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put("bbox", bboxString);

        addFilterParams(valueMap);

        params.add("search_params", new JSONObject(valueMap).toString());
        params.add("retr_method", "services/caches/geocaches");
        params.add("retr_params", "{\"fields\": \"code|name|location|type|status|difficulty|terrain|size|is_found\"}");
        params.add("wrap", "true");
        final JSONObject data = request(connector, SERVICE_SEARCH_AND_RETRIEVE, params);

        if (data == null) {
            return new ArrayList<Geocache>();
        }

        return parseCaches(data);
    }

    private static List<Geocache> parseCaches(final JSONObject response) {
        try {
            // Check for empty result
            final String result = response.getString("results");
            if (result.equals("[]")) {
                return new ArrayList<Geocache>();
            }

            // Get and iterate result list
            final JSONObject cachesResponse = response.getJSONObject("results");
            if (cachesResponse != null) {
                List<Geocache> caches = new ArrayList<Geocache>(cachesResponse.length());
                @SuppressWarnings("unchecked")
                Iterator<String> keys = cachesResponse.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Geocache cache = parseSmallCache(cachesResponse.getJSONObject(key));
                    if (cache != null) {
                        caches.add(cache);
                    }
                }
                return caches;
            }
        } catch (JSONException e) {
            Log.e("OkapiClient.parseCachesResult", e);
        }
        return new ArrayList<Geocache>();
    }

    private static Geocache parseSmallCache(final JSONObject response) {
        final Geocache cache = new Geocache();
        cache.setReliableLatLon(true);
        try {
            cache.setGeocode(response.getString(CACHE_CODE));
            cache.setName(response.getString(CACHE_NAME));
            // not used: names
            setLocation(cache, response.getString(CACHE_LOCATION));
            cache.setType(getCacheType(response.getString(CACHE_TYPE)));

            final String status = response.getString(CACHE_STATUS);
            cache.setDisabled(status.equalsIgnoreCase("Temporarily unavailable"));
            cache.setArchived(status.equalsIgnoreCase("Archived"));

            cache.setSize(getCacheSize(response));
            cache.setDifficulty((float) response.getDouble(CACHE_DIFFICULTY));
            cache.setTerrain((float) response.getDouble(CACHE_TERRAIN));

            cache.setFound(response.getBoolean("is_found"));

            cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_CACHE));
        } catch (JSONException e) {
            Log.e("OkapiClient.parseCache", e);
        }
        return cache;
    }

    private static Geocache parseCache(final JSONObject response) {
        final Geocache cache = new Geocache();
        cache.setReliableLatLon(true);
        try {
            cache.setGeocode(response.getString(CACHE_CODE));
            cache.setName(response.getString(CACHE_NAME));
            // not used: names
            setLocation(cache, response.getString(CACHE_LOCATION));
            cache.setType(getCacheType(response.getString(CACHE_TYPE)));

            final String status = response.getString(CACHE_STATUS);
            cache.setDisabled(status.equalsIgnoreCase("Temporarily unavailable"));
            cache.setArchived(status.equalsIgnoreCase("Archived"));

            // not used: url
            final JSONObject owner = response.getJSONObject(CACHE_OWNER);
            cache.setOwnerDisplayName(parseUser(owner));

            cache.getLogCounts().put(LogType.FOUND_IT, response.getInt(CACHE_FOUNDS));
            cache.getLogCounts().put(LogType.DIDNT_FIND_IT, response.getInt(CACHE_NOTFOUNDS));
            cache.setSize(getCacheSize(response));
            cache.setDifficulty((float) response.getDouble(CACHE_DIFFICULTY));
            cache.setTerrain((float) response.getDouble(CACHE_TERRAIN));
            if (!response.isNull(CACHE_RATING)) {
                cache.setRating((float) response.getDouble(CACHE_RATING));
            }
            cache.setVotes(response.getInt(CACHE_VOTES));

            cache.setFound(response.getBoolean("is_found"));

            cache.setFavoritePoints(response.getInt(CACHE_RECOMMENDATIONS));
            // not used: req_password
            cache.setDescription(response.getString(CACHE_DESCRIPTION));
            cache.setHint(Html.fromHtml(response.getString(CACHE_HINT)).toString());
            // not used: hints

            final JSONArray images = response.getJSONArray(CACHE_IMAGES);
            if (images != null) {
                for (int i = 0; i < images.length(); i++) {
                    JSONObject imageResponse = images.getJSONObject(i);
                    if (imageResponse.getBoolean(CACHE_IMAGE_IS_SPOILER)) {
                        final String title = imageResponse.getString(CACHE_IMAGE_CAPTION);
                        final String url = absoluteUrl(imageResponse.getString(CACHE_IMAGE_URL), cache.getGeocode());
                        cache.addSpoiler(new Image(url, title));
                    }
                }
            }

            // not used: attrnames
            cache.setLogs(parseLogs(response.getJSONArray(CACHE_LATEST_LOGS)));
            cache.setHidden(parseDate(response.getString(CACHE_HIDDEN)));

            cache.setDetailedUpdatedNow();
            // save full detailed caches
            cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
        } catch (JSONException e) {
            Log.e("OkapiClient.parseCache", e);
        }
        return cache;
    }

    private static String absoluteUrl(String url, String geocode) {
        final Uri uri = Uri.parse(url);

        if (!uri.isAbsolute()) {
            final IConnector connector = ConnectorFactory.getConnector(geocode);
            final String host = connector.getHost();
            if (StringUtils.isNotBlank(host)) {
                return "http://" + host + "/" + url;
            }
        }
        return url;
    }

    private static String parseUser(JSONObject user) throws JSONException {
        return user.getString(USER_USERNAME);
    }

    private static List<LogEntry> parseLogs(JSONArray logsJSON) {
        List<LogEntry> result = null;
        for (int i = 0; i < logsJSON.length(); i++) {
            try {
                JSONObject logResponse = logsJSON.getJSONObject(i);
                LogEntry log = new LogEntry(
                        parseUser(logResponse.getJSONObject(LOG_USER)),
                        parseDate(logResponse.getString(LOG_DATE)).getTime(),
                        parseLogType(logResponse.getString(LOG_TYPE)),
                        logResponse.getString(LOG_COMMENT).trim());
                if (result == null) {
                    result = new ArrayList<LogEntry>();
                }
                result.add(log);
            } catch (JSONException e) {
                Log.e("OkapiClient.parseLogs", e);
            }
        }
        return result;
    }

    private static LogType parseLogType(String logType) {
        if ("Found it".equalsIgnoreCase(logType)) {
            return LogType.FOUND_IT;
        }
        if ("Didn't find it".equalsIgnoreCase(logType)) {
            return LogType.DIDNT_FIND_IT;
        }
        return LogType.NOTE;
    }

    private static Date parseDate(final String date) {
        final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        final String strippedDate = date.replaceAll("\\+0([0-9]){1}\\:00", "+0$100");
        try {
            return ISO8601DATEFORMAT.parse(strippedDate);
        } catch (ParseException e) {
            Log.e("OkapiClient.parseDate", e);
        }
        return null;
    }

    private static void setLocation(final Geocache cache, final String location) {
        final String latitude = StringUtils.substringBefore(location, "|");
        final String longitude = StringUtils.substringAfter(location, "|");
        cache.setCoords(new Geopoint(latitude, longitude));
    }

    private static CacheSize getCacheSize(final JSONObject response) {
        if (response.isNull(CACHE_SIZE)) {
            return CacheSize.NOT_CHOSEN;
        }
        double size = 0;
        try {
            size = response.getDouble(CACHE_SIZE);
        } catch (JSONException e) {
            Log.e("OkapiClient.getCacheSize", e);
        }
        switch ((int) Math.round(size)) {
            case 1:
                return CacheSize.MICRO;
            case 2:
                return CacheSize.SMALL;
            case 3:
                return CacheSize.REGULAR;
            case 4:
                return CacheSize.LARGE;
            case 5:
                return CacheSize.LARGE;
            default:
                break;
        }
        return CacheSize.NOT_CHOSEN;
    }

    private static CacheType getCacheType(final String cacheType) {
        if (cacheType.equalsIgnoreCase("Traditional")) {
            return CacheType.TRADITIONAL;
        }
        if (cacheType.equalsIgnoreCase("Multi")) {
            return CacheType.MULTI;
        }
        if (cacheType.equalsIgnoreCase("Quiz")) {
            return CacheType.MYSTERY;
        }
        if (cacheType.equalsIgnoreCase("Virtual")) {
            return CacheType.VIRTUAL;
        }
        return CacheType.UNKNOWN;
    }

    private static JSONObject request(final IConnector connector, final String service, final Parameters params) {
        if (connector == null) {
            return null;
        }
        if (!(connector instanceof OCApiConnector)) {
            return null;
        }

        final String host = connector.getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }

        params.add("langpref", getPreferredLanguage());

        OAuth.signOAuth(host, service, "GET", false, params, Settings.getOCDETokenPublic(), Settings.getOCDETokenSecret(), ((OCApiLiveConnector) connector).getCK(), ((OCApiLiveConnector) connector).getCS());

        final String uri = "http://" + host + service;
        return Network.requestJSON(uri, params);
    }

    private static String getPreferredLanguage() {
        final String code = Locale.getDefault().getCountry();
        if (StringUtils.isNotBlank(code)) {
            return StringUtils.lowerCase(code) + "|en";
        }
        return "en";
    }

    private static void addFilterParams(final Map<String, String> valueMap) {
        if (!Settings.isExcludeDisabledCaches()) {
            valueMap.put("status", "Available|Temporarily unavailable");
        }
        if (Settings.isExcludeMyCaches()) {
            valueMap.put("exclude_my_own", "true");
            valueMap.put("found_status", "notfound_only");
        }
        if (Settings.getCacheType() != CacheType.ALL) {
            valueMap.put("type", getFilterFromType(Settings.getCacheType()));
        }
    }

    private static String getFilterFromType(CacheType cacheType) {
        switch (cacheType) {
            case EVENT:
                return "Event";
            case MULTI:
                return "Multi";
            case MYSTERY:
                return "Quiz";
            case TRADITIONAL:
                return "Traditional";
            case VIRTUAL:
                return "Virtual";
            case WEBCAM:
                return "Webcam";
            default:
                return "";
        }
    }

}
