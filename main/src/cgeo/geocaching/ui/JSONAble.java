package cgeo.geocaching.ui;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONAble {
    JSONObject toJSON() throws JSONException;
}
