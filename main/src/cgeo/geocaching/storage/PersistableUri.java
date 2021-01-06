package cgeo.geocaching.storage;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.Nullable;

/** Enum listing all single-document-Uris which can be persisted */
public enum PersistableUri {

    TRACK(R.string.pref_persistableuri_track, null);
    //INDIVIDUAL_ROUTE(R.string.pref_persisteduri_individual_route, null);

    @AnyRes
    private final int prefKeyId;
    private final String mimeType;


    PersistableUri(@AnyRes final int prefKeyId, final String mimeType) {
        this.prefKeyId = prefKeyId;
        this.mimeType = mimeType;
    }

    public int getPrefKeyId() {
        return this.prefKeyId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Uri getUri() {
        final String uriString = Settings.getPersistableUri(this);
        return uriString == null ? null : Uri.parse(Settings.getPersistableUri(this));
    }

    public boolean hasValue() {
        return getUri() != null;
    }

    /** Sets a new user-defined location ("null" is allowed). Should be called ONLY by {@link ContentStorage} */
    protected void setPersistedUri(@Nullable final Uri uri) {
        Settings.setPersistableUri(this, uri == null ? null : uri.toString());
    }

    @Override
    public String toString() {
        return name() + ": " + getUri();
    }

}
