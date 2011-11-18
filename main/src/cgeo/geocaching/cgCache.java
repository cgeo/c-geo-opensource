package cgeo.geocaching;

import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Spannable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal c:geo representation of a "cache"
 */
public class cgCache implements ICache {

    /**
     * Cache loading parameters
     */
    final public static int LOADATTRIBUTES = 1 << 0;
    final public static int LOADWAYPOINTS = 1 << 1;
    final public static int LOADSPOILERS = 1 << 2;
    final public static int LOADLOGS = 1 << 3;
    final public static int LOADINVENTORY = 1 << 4;
    final public static int LOADOFFLINELOG = 1 << 5;
    final public static int LOADALL = LOADATTRIBUTES | LOADWAYPOINTS | LOADSPOILERS | LOADLOGS | LOADINVENTORY | LOADOFFLINELOG;

    private Long updated = null;
    private Long detailedUpdate = null;
    private Long visitedDate = null;
    private Integer reason = 0;
    private boolean detailed = false;
    private String geocode = "";
    private String cacheId = "";
    private String guid = "";
    private CacheType cacheType = CacheType.UNKNOWN;
    private String name = "";
    private Spannable nameSp = null;
    private String owner = "";
    private String ownerReal = "";
    private Date hidden = null;
    private String hint = "";
    private CacheSize size = null;
    private Float difficulty = Float.valueOf(0);
    private Float terrain = Float.valueOf(0);
    private Float direction = null;
    private Float distance = null;
    private String latlon = "";
    private String location = "";
    private Geopoint coords = null;
    private boolean reliableLatLon = false;
    private Double elevation = null;
    private String personalNote = null;
    private String shortdesc = "";
    private String description = null;
    private boolean disabled = false;
    private boolean archived = false;
    private boolean members = false;
    private boolean found = false;
    private boolean favourite = false;
    private boolean own = false;
    private Integer favouriteCnt = null;
    private Float rating = null;
    private Integer votes = null;
    private Float myVote = null;
    private int inventoryItems = 0;
    private boolean onWatchlist = false;
    private List<String> attributes = null;
    private List<cgWaypoint> waypoints = null;
    private ArrayList<cgImage> spoilers = null;
    private List<cgLog> logs = null;
    private List<cgTrackable> inventory = null;
    private Map<Integer, Integer> logCounts = new HashMap<Integer, Integer>();
    private boolean logOffline = false;
    // temporary values
    private boolean statusChecked = false;
    private boolean statusCheckedView = false;
    private String directionImg = "";
    private String nameForSorting;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    /**
     * Gather missing information from another cache object.
     *
     * @param other
     *            the other version, or null if non-existent
     */
    public void gatherMissingFrom(final cgCache other) {
        if (other == null) {
            return;
        }

        updated = System.currentTimeMillis();
        if (!detailed && other.detailed) {
            detailed = true;
            detailedUpdate = updated;
        }

        if (visitedDate == null || visitedDate == 0) {
            visitedDate = other.getVisitedDate();
        }
        if (reason == null || reason == 0) {
            reason = other.reason;
        }
        if (StringUtils.isBlank(geocode)) {
            geocode = other.getGeocode();
        }
        if (StringUtils.isBlank(cacheId)) {
            cacheId = other.cacheId;
        }
        if (StringUtils.isBlank(guid)) {
            guid = other.getGuid();
        }
        if (null == cacheType || CacheType.UNKNOWN == cacheType) {
            cacheType = other.getType();
        }
        if (StringUtils.isBlank(name)) {
            name = other.getName();
        }
        if (StringUtils.isBlank(nameSp)) {
            nameSp = other.nameSp;
        }
        if (StringUtils.isBlank(owner)) {
            owner = other.getOwner();
        }
        if (StringUtils.isBlank(ownerReal)) {
            ownerReal = other.getOwnerReal();
        }
        if (hidden == null) {
            hidden = other.hidden;
        }
        if (StringUtils.isBlank(hint)) {
            hint = other.hint;
        }
        if (size == null) {
            size = other.size;
        }
        if (difficulty == null || difficulty == 0) {
            difficulty = other.getDifficulty();
        }
        if (terrain == null || terrain == 0) {
            terrain = other.getTerrain();
        }
        if (direction == null) {
            direction = other.direction;
        }
        if (distance == null) {
            distance = other.getDistance();
        }
        if (StringUtils.isBlank(latlon)) {
            latlon = other.latlon;
        }
        if (StringUtils.isBlank(location)) {
            location = other.location;
        }
        if (coords == null) {
            coords = other.getCoords();
        }
        if (elevation == null) {
            elevation = other.elevation;
        }
        if (personalNote == null) { // don't use StringUtils.isBlank. Otherwise we cannot recognize a note which was deleted on GC
            personalNote = other.personalNote;
        }
        if (StringUtils.isBlank(shortdesc)) {
            shortdesc = other.getShortdesc();
        }
        if (StringUtils.isBlank(description)) {
            description = other.description;
        }
        if (favouriteCnt == null) {
            favouriteCnt = other.getFavouriteCnt();
        }
        if (rating == null) {
            rating = other.getRating();
        }
        if (votes == null) {
            votes = other.votes;
        }
        if (myVote == null) {
            myVote = other.getMyVote();
        }
        if (attributes == null) {
            attributes = other.getAttributes();
        }
        if (waypoints == null) {
            waypoints = other.getWaypoints();
        }
        else {
            cgWaypoint.mergeWayPoints(waypoints, other.getWaypoints());
        }
        if (spoilers == null) {
            spoilers = other.spoilers;
        }
        if (inventory == null) {
            // If inventoryItems is 0, it can mean both
            // "don't know" or "0 items". Since we cannot distinguish
            // them here, only populate inventoryItems from
            // old data when we have to do it for inventory.
            inventory = other.inventory;
            inventoryItems = other.inventoryItems;
        }
        if (CollectionUtils.isEmpty(logs)) { // keep last known logs if none
            logs = other.logs;
        }
    }

    public boolean hasTrackables() {
        return inventoryItems > 0;
    }

    public boolean canBeAddedToCalendar() {
        // is event type?
        if (!isEventCache()) {
            return false;
        }
        // has event date set?
        if (hidden == null) {
            return false;
        }
        // is in future?
        Date today = new Date();
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);
        if (hidden.compareTo(today) <= 0) {
            return false;
        }
        return true;
    }

    /**
     * checks if a page contains the guid of a cache
     *
     * @param cache
     *            the cache to look for
     * @param page
     *            the page to search in
     *
     * @return true: page contains guid of cache, false: otherwise
     */
    boolean isGuidContainedInPage(final String page) {
        // check if the guid of the cache is anywhere in the page
        if (StringUtils.isBlank(guid)) {
            return false;
        }
        Pattern patternOk = Pattern.compile(guid, Pattern.CASE_INSENSITIVE);
        Matcher matcherOk = patternOk.matcher(page);
        if (matcherOk.find()) {
            Log.i(Settings.tag, "cgCache.isGuidContainedInPage: guid '" + guid + "' found");
            return true;
        } else {
            Log.i(Settings.tag, "cgCache.isGuidContainedInPage: guid '" + guid + "' not found");
            return false;
        }
    }

    public boolean isEventCache() {
        return CacheType.EVENT == cacheType || CacheType.MEGA_EVENT == cacheType
                || CacheType.CITO == cacheType || CacheType.LOSTANDFOUND == cacheType;
    }

    public boolean logVisit(IAbstractActivity fromActivity) {
        if (StringUtils.isBlank(cacheId)) {
            fromActivity.showToast(((Activity) fromActivity).getResources().getString(R.string.err_cannot_log_visit));
            return true;
        }
        Intent logVisitIntent = new Intent((Activity) fromActivity, VisitCacheActivity.class);
        logVisitIntent.putExtra(VisitCacheActivity.EXTRAS_ID, cacheId);
        logVisitIntent.putExtra(VisitCacheActivity.EXTRAS_GEOCODE, geocode.toUpperCase());
        logVisitIntent.putExtra(VisitCacheActivity.EXTRAS_FOUND, found);

        ((Activity) fromActivity).startActivity(logVisitIntent);

        return true;
    }

    public boolean logOffline(final IAbstractActivity fromActivity, final int logType, final cgBase base) {
        String log = "";
        if (StringUtils.isNotBlank(Settings.getSignature())
                && Settings.isAutoInsertSignature()) {
            log = LogTemplateProvider.applyTemplates(Settings.getSignature(), base, true);
        }
        logOffline(fromActivity, log, Calendar.getInstance(), logType);
        return true;
    }

    void logOffline(final IAbstractActivity fromActivity, final String log, Calendar date, final int logType) {
        if (logType <= 0) {
            return;
        }
        cgeoapplication app = (cgeoapplication) ((Activity) fromActivity).getApplication();
        final boolean status = app.saveLogOffline(geocode, date.getTime(), logType, log);

        Resources res = ((Activity) fromActivity).getResources();
        if (status) {
            fromActivity.showToast(res.getString(R.string.info_log_saved));
            app.saveVisitDate(geocode);
        } else {
            fromActivity.showToast(res.getString(R.string.err_log_post_failed));
        }
    }

    public List<Integer> getPossibleLogTypes() {
        boolean isOwner = owner != null && owner.equalsIgnoreCase(Settings.getUsername());
        List<Integer> types = new ArrayList<Integer>();
        if (isEventCache()) {
            types.add(cgBase.LOG_WILL_ATTEND);
            types.add(cgBase.LOG_NOTE);
            types.add(cgBase.LOG_ATTENDED);
            types.add(cgBase.LOG_NEEDS_ARCHIVE);
            if (isOwner) {
                types.add(cgBase.LOG_ANNOUNCEMENT);
            }
        } else if (CacheType.WEBCAM == cacheType) {
            types.add(cgBase.LOG_WEBCAM_PHOTO_TAKEN);
            types.add(cgBase.LOG_DIDNT_FIND_IT);
            types.add(cgBase.LOG_NOTE);
            types.add(cgBase.LOG_NEEDS_ARCHIVE);
            types.add(cgBase.LOG_NEEDS_MAINTENANCE);
        } else {
            types.add(cgBase.LOG_FOUND_IT);
            types.add(cgBase.LOG_DIDNT_FIND_IT);
            types.add(cgBase.LOG_NOTE);
            types.add(cgBase.LOG_NEEDS_ARCHIVE);
            types.add(cgBase.LOG_NEEDS_MAINTENANCE);
        }
        if (isOwner) {
            types.add(cgBase.LOG_OWNER_MAINTENANCE);
            types.add(cgBase.LOG_TEMP_DISABLE_LISTING);
            types.add(cgBase.LOG_ENABLE_LISTING);
            types.add(cgBase.LOG_ARCHIVE);
            types.remove(Integer.valueOf(cgBase.LOG_UPDATE_COORDINATES));
        }
        return types;
    }

    public void openInBrowser(Activity fromActivity) {
        fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getCacheUrl())));
    }

    private String getCacheUrl() {
        return getConnector().getCacheUrl(this);
    }

    private IConnector getConnector() {
        return ConnectorFactory.getConnector(this);
    }

    public boolean canOpenInBrowser() {
        return getCacheUrl() != null;
    }

    public boolean supportsRefresh() {
        return getConnector().supportsRefreshCache(this);
    }

    public boolean supportsWatchList() {
        return getConnector().supportsWatchList();
    }

    public boolean supportsLogging() {
        return getConnector().supportsLogging();
    }

    @Override
    public Float getDifficulty() {
        return difficulty;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    @Override
    public String getLatitude() {
        return coords != null ? coords.format(GeopointFormatter.Format.LAT_DECMINUTE) : null;
    }

    @Override
    public String getLongitude() {
        return coords != null ? coords.format(GeopointFormatter.Format.LON_DECMINUTE) : null;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public CacheSize getSize() {
        return size;
    }

    @Override
    public Float getTerrain() {
        return terrain;
    }

    @Override
    public boolean isArchived() {
        return archived;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public boolean isMembersOnly() {
        return members;
    }

    @Override
    public boolean isOwn() {
        return own;
    }

    @Override
    public String getOwnerReal() {
        return ownerReal;
    }

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public String getDescription() {
        if (description == null) {
            description = StringUtils.defaultString(cgeoapplication.getInstance().getCacheDescription(geocode));
        }
        return description;
    }

    @Override
    public String getShortDescription() {
        return shortdesc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCacheId() {
        // TODO: Only valid for GC-cache
        if (StringUtils.isBlank(cacheId)) {
            return CryptUtils.convertToGcBase31(geocode);
        }

        return cacheId;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getPersonalNote() {
        return personalNote;
    }

    public boolean supportsUserActions() {
        return getConnector().supportsUserActions();
    }

    public boolean supportsCachesAround() {
        return getConnector().supportsCachesAround();
    }

    public void shareCache(Activity fromActivity, Resources res) {
        if (geocode == null) {
            return;
        }

        StringBuilder subject = new StringBuilder("Geocache ");
        subject.append(geocode.toUpperCase());
        if (StringUtils.isNotBlank(name)) {
            subject.append(" - ").append(name);
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
        intent.putExtra(Intent.EXTRA_TEXT, getUrl());

        fromActivity.startActivity(Intent.createChooser(intent, res.getText(R.string.action_bar_share_title)));
    }

    public String getUrl() {
        return getConnector().getCacheUrl(this);
    }

    public boolean supportsGCVote() {
        return StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean isFound() {
        return found;
    }

    @Override
    public boolean isFavorite() {
        return favourite;
    }

    @Override
    public boolean isWatchlist() {
        return onWatchlist;
    }

    @Override
    public Date getHiddenDate() {
        return hidden;
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }

    @Override
    public List<cgTrackable> getInventory() {
        return inventory;
    }

    @Override
    public ArrayList<cgImage> getSpoilers() {
        return spoilers;
    }

    @Override
    public Map<Integer, Integer> getLogCounts() {
        return logCounts;
    }

    @Override
    public Integer getFavoritePoints() {
        return favouriteCnt;
    }

    @Override
    public String getNameForSorting() {
        if (null == nameForSorting) {
            final Matcher matcher = NUMBER_PATTERN.matcher(name);
            if (matcher.find()) {
                nameForSorting = name.replace(matcher.group(), StringUtils.leftPad(matcher.group(), 6, '0'));
            }
            else {
                nameForSorting = name;
            }
        }
        return nameForSorting;
    }

    public boolean isVirtual() {
        return CacheType.VIRTUAL == cacheType || CacheType.WEBCAM == cacheType
                || CacheType.EARTH == cacheType;
    }

    public boolean showSize() {
        return !((isEventCache() || isVirtual()) && size == CacheSize.NOT_CHOSEN);
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    public Long getDetailedUpdate() {
        return detailedUpdate;
    }

    public void setDetailedUpdate(Long detailedUpdate) {
        this.detailedUpdate = detailedUpdate;
    }

    public Long getVisitedDate() {
        return visitedDate;
    }

    public void setVisitedDate(Long visitedDate) {
        this.visitedDate = visitedDate;
    }

    public Integer getReason() {
        return reason;
    }

    public void setReason(Integer reason) {
        this.reason = reason;
    }

    public boolean getDetailed() {
        return detailed;
    }

    public void setDetailed(boolean detailed) {
        this.detailed = detailed;
    }

    public Spannable getNameSp() {
        return nameSp;
    }

    public void setNameSp(Spannable nameSp) {
        this.nameSp = nameSp;
    }

    public Date getHidden() {
        return hidden;
    }

    public void setHidden(Date hidden) {
        this.hidden = hidden;
    }

    public Float getDirection() {
        return direction;
    }

    public void setDirection(Float direction) {
        this.direction = direction;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public String getLatlon() {
        return latlon;
    }

    public void setLatlon(String latlon) {
        this.latlon = latlon;
    }

    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(Geopoint coords) {
        this.coords = coords;
    }

    public boolean isReliableLatLon() {
        return reliableLatLon;
    }

    public void setReliableLatLon(boolean reliableLatLon) {
        this.reliableLatLon = reliableLatLon;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public String getShortdesc() {
        return shortdesc;
    }

    public void setShortdesc(String shortdesc) {
        this.shortdesc = shortdesc;
    }

    public boolean isMembers() {
        return members;
    }

    public void setMembers(boolean members) {
        this.members = members;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public Integer getFavouriteCnt() {
        return favouriteCnt;
    }

    public void setFavouriteCnt(Integer favouriteCnt) {
        this.favouriteCnt = favouriteCnt;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    public Integer getVotes() {
        return votes;
    }

    public void setVotes(Integer votes) {
        this.votes = votes;
    }

    public Float getMyVote() {
        return myVote;
    }

    public void setMyVote(Float myVote) {
        this.myVote = myVote;
    }

    public int getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(int inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public boolean isOnWatchlist() {
        return onWatchlist;
    }

    public void setOnWatchlist(boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    public List<cgWaypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<cgWaypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public List<cgLog> getLogs() {
        return logs;
    }

    public void setLogs(List<cgLog> logs) {
        this.logs = logs;
    }

    public boolean isLogOffline() {
        return logOffline;
    }

    public void setLogOffline(boolean logOffline) {
        this.logOffline = logOffline;
    }

    public boolean isStatusChecked() {
        return statusChecked;
    }

    public void setStatusChecked(boolean statusChecked) {
        this.statusChecked = statusChecked;
    }

    public boolean isStatusCheckedView() {
        return statusCheckedView;
    }

    public void setStatusCheckedView(boolean statusCheckedView) {
        this.statusCheckedView = statusCheckedView;
    }

    public String getDirectionImg() {
        return directionImg;
    }

    public void setDirectionImg(String directionImg) {
        this.directionImg = directionImg;
    }

    public void setGeocode(String geocode) {
        this.geocode = geocode;
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setOwnerReal(String ownerReal) {
        this.ownerReal = ownerReal;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public void setSize(CacheSize size) {
        this.size = size;
    }

    public void setDifficulty(Float difficulty) {
        this.difficulty = difficulty;
    }

    public void setTerrain(Float terrain) {
        this.terrain = terrain;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPersonalNote(String personalNote) {
        this.personalNote = personalNote;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public void setOwn(boolean own) {
        this.own = own;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public void setSpoilers(ArrayList<cgImage> spoilers) {
        this.spoilers = spoilers;
    }

    public void setInventory(List<cgTrackable> inventory) {
        this.inventory = inventory;
    }

    public void setLogCounts(Map<Integer, Integer> logCounts) {
        this.logCounts = logCounts;
    }

    public void setNameForSorting(String nameForSorting) {
        this.nameForSorting = nameForSorting;
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.IBasicCache#getType()
     *
     * @returns Never null
     */
    @Override
    public CacheType getType() {
        return cacheType;
    }

    public void setType(CacheType cacheType) {
        if (cacheType == null || CacheType.ALL == cacheType) {
            throw new IllegalArgumentException("Illegal cache type");
        }
        this.cacheType = cacheType;
    }

    public boolean hasDifficulty() {
        return difficulty != null && difficulty > 0f;
    }

    public boolean hasTerrain() {
        return terrain != null && terrain > 0f;
    }

}
