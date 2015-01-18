package cgeo.geocaching.apps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.utils.ProcessUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.content.Intent;

public abstract class AbstractApp implements App {

    @Nullable private final String packageName;
    @Nullable private final String intent;
    @NonNull
    private final String name;
    /**
     * a unique id, defined in res/values/ids.xml
     */
    private final int id;

    protected AbstractApp(@NonNull final String name, final int id, @Nullable final String intent,
            @Nullable final String packageName) {
        this.name = name;
        this.id = id;
        this.intent = intent;
        this.packageName = packageName;
    }

    protected AbstractApp(@NonNull final String name, final int id, @Nullable final String intent) {
        this(name, id, intent, null);
    }

    @Override
    public boolean isInstalled() {
        if (StringUtils.isNotEmpty(packageName) && ProcessUtils.isLaunchable(packageName)) {
            return true;
        }
        return ProcessUtils.isIntentAvailable(intent);
    }

    @Nullable
    protected Intent getLaunchIntent() {
        return ProcessUtils.getLaunchIntent(packageName);
    }

    @Override
    public boolean isUsableAsDefaultNavigationApp() {
        return true;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public int getId() {
        return id;
    }

    protected static String getString(final int ressourceId) {
        return CgeoApplication.getInstance().getString(ressourceId);
    }

    @Override
    public boolean isEnabled(final Geocache cache) {
        return cache != null;
    }
}
