package cgeo.geocaching.test.mock;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgImage;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GC2JVEH extends MockedCache {

    @Override
    public String getName() {
        return "Auf den Spuren des Indianer Jones Teil 1";
    }

    public GC2JVEH() {
        super(new Geopoint(52.37225, 9.73537));
    }

    @Override
    public Float getDifficulty() {
        return 5.0f;
    }

    @Override
    public Float getTerrain() {
        return 3.0f;
    }

    @Override
    public String getGeocode() {
        return "GC2JVEH";
    }

    @Override
    public String getCacheId() {
        return "1997597";
    }

    @Override
    public String getGuid() {
        return "07270e8c-72ec-4821-8cb7-b01483f94cb5";
    }

    @Override
    public String getOwner() {
        return "indianerjones, der merlyn,reflektordetektor";
    }

    @Override
    public String getOwnerReal() {
        return "indianerjones";
    }

    @Override
    public CacheSize getSize() {
        return CacheSize.SMALL;
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.MYSTERY;
    }

    @Override
    public String getShortDescription() {
        return "Aufgabe zum Start: Finde die Schattenlinie. !!!Die Skizze mit den Zahlen solltest du mitnehmen!!! Du solltest den cache so beginnen, das du station 2 in der Zeit von mo- fr von 11-19 Uhr und sa von11-16 Uhr erledigt hast.";
    }

    @Override
    public String getDescription() {
        return "<img src= \"http://img.geocaching.com/cache/1711f8a1-796a-405b-82ba-8685f2e9f024.jpg\" />";
    }

    @Override
    public String getLocation() {
        return "Niedersachsen, Germany";
    }

    @Override
    public Date getHiddenDate() {
        try {
            return cgBase.parseGcCustomDate("28/11/2010");
        } catch (ParseException e) {
        }
        return null;
    }

    @Override
    public List<String> getAttributes() {
        String[] attributes = new String[] {
                "winter_yes",
                "flashlight_yes",
                "stealth_yes",
                "parking_yes",
                "abandonedbuilding_yes",
                "hike_med_yes",
                "rappelling_yes"
        };
        return Arrays.asList(attributes);
    }

    @Override
    public Map<Integer, Integer> getLogCounts() {
        Map<Integer, Integer> logCounts = new HashMap<Integer, Integer>();
        logCounts.put(cgBase.LOG_FOUND_IT, 57);
        logCounts.put(cgBase.LOG_NOTE, 7);
        logCounts.put(cgBase.LOG_TEMP_DISABLE_LISTING, 1);
        logCounts.put(cgBase.LOG_ENABLE_LISTING, 1);
        logCounts.put(cgBase.LOG_PUBLISH_LISTING, 1);
        return logCounts;
    }

    @Override
    public Integer getFavoritePoints() {
        return new Integer(20);
    }

    @Override
    public boolean isMembersOnly() {
        return true;
    }

    @Override
    public List<cgTrackable> getInventory() {
        ArrayList<cgTrackable> inventory = new ArrayList<cgTrackable>();
        inventory.add(new cgTrackable());
        return inventory;
    }

    @Override
    public List<cgImage> getSpoilers() {
        ArrayList<cgImage> spoilers = new ArrayList<cgImage>();
        spoilers.add(new cgImage());
        spoilers.add(new cgImage());
        spoilers.add(new cgImage());
        return spoilers;
    }
}
