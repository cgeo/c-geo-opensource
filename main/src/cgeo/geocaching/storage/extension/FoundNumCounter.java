package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FoundNumCounter extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_FOUNDNUM;

    private FoundNumCounter(final DataStore.DBExtension copyFrom) {
        this.id = copyFrom.getId();
        this.key = copyFrom.getKey();
        this.long1 = copyFrom.getLong1();
    }

    public long getCounter(final boolean incrementCounter) { return incrementCounter ? getLong1() + 1 : getLong1(); }

    @Nullable
    public static FoundNumCounter load(final String serviceName) {
        final DataStore.DBExtension temp = load(type, serviceName);
        return null == temp ? null : new FoundNumCounter(temp);
    }

    public static void updateFoundNum(final String serviceName, @NonNull final long foundNum) {
        removeAll(type, serviceName);
        add(type, serviceName, foundNum, 0, "", "");
    }
}
