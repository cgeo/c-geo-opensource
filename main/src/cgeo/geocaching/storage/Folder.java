package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a concrete definite folder / directory on disk.
 *
 * Folders can have different types as defined by {@link FolderType}.
 * Depending on that type, different attributes are filled and different handling is necessary
 * to interact with actual data in that folder. Those different handlings are implemented by {@link FolderStorage}.
 *
 * Instances of this class are immutable. They can be serialized/deserialized to/from a String using {@link #toConfig()} and {@link #fromConfig(String)}
 *
 * Instances support the usage in Maps ({@link #equals(Object)} and {@link #hashCode()}). Note however that
 * two FolderLocation instances pointing to the same actual folder on disk but using two different FolderTypes
 * are NOT considered equal!
 */
public class Folder {


    public enum FolderType {
        /** a 'classic' folder based on a file. Folder locations for this type are immutable */
        FILE,
        /** Folder based on Storage Access Frameworks and retrieved by {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE}. Folder locations for this type are immutable */
        DOCUMENT,
        /** (Volatile type) A Folder based on a ConfigurableFolder. Folder locations for this type can change when based folder is reconfigured */
        CONFIGURABLE_FOLDER,
    }

    /** cGeo's private internal Files directory */
    public static final Folder CGEO_PRIVATE_FILES = Folder.fromFile(CgeoApplication.getInstance().getApplicationContext().getFilesDir());

    /** Root folder for documents (deprecated since API29 but still works somehow) */
    public static final Folder DOCUMENTS_FOLDER_DEPRECATED = Folder.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));

    /** Legacy public root folder of c:geo until API29 (will no longer work in API30) */
    public static final Folder LEGACY_CGEO_PUBLIC_ROOT = Folder.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "cgeo"));

    private static final String CONFIG_SEP = "::";

    private final FolderType type;
    private final Uri uri;

    private final ConfigurableFolder configurableFolder; //needed for type PUBLIC_FOLDER

    private final List<String> subfolders; //each type may have subfolders
    private final String subfolderString;

    private Folder(final FolderType type, final Uri uri, final ConfigurableFolder configurableFolder, final List<String> subfolders) {
        this.type = type;
        this.uri = uri;

        this.configurableFolder = configurableFolder;

        this.subfolders = subfolders == null ? Collections.emptyList() : subfolders;
        this.subfolderString = CollectionStream.of(this.subfolders).toJoinedString("/");

    }

    /** registers a listener which is fired each time the actual location of this folder changes */
    public void registerChangeListener(final Object lifecycleRef, final Consumer<ConfigurableFolder> listener) {

        //currently, this folders location can only change if it is based on a Public Folder
        if (getRootConfigurableFolder() != null) {
            getRootConfigurableFolder().registerChangeListener(lifecycleRef, listener);
        }
    }

    /** returns this folder's type. This value is immutable */
    public FolderType getType() {
        return type;
    }

    /** returns this folder's current BaseUri (below all subfolders). This value is volatile if this folder's type is volatile (e.g. {@link FolderType#CONFIGURABLE_FOLDER}) */
    @NonNull
    public Uri getBaseUri() {
        return configurableFolder != null ? configurableFolder.getFolder().getBaseUri() : this.uri;
    }

    /** The current base type, which is always an immutable type (e.g. may never be {@link FolderType#CONFIGURABLE_FOLDER}). Return value is volatile */
    @NonNull
    public FolderType getBaseType() {
        if (configurableFolder != null) {
            return configurableFolder.getFolder().getBaseType();
        }
        return getType();
    }

    /** Gets all subdirs down to the base folder's baseUri. This value is volatile if this folder's type is volatile (e.g. {@link FolderType#CONFIGURABLE_FOLDER}) */
    public List<String> getSubdirsToBase() {
        final List<String > result = configurableFolder != null ? configurableFolder.getFolder().getSubdirsToBase() : new ArrayList<>();
        result.addAll(subfolders);
        return result;
    }

    /** If this instance is of type {@link FolderType#CONFIGURABLE_FOLDER}, then this configurablefolder is returned. Otherwise null is returned */
    @Nullable
    public ConfigurableFolder getRootConfigurableFolder() {
        return configurableFolder;
    }

    /** Returns a representation of this folder's location fit to show to an end user. This value is volatile if this folder's type is volatile (e.g. {@link FolderType#CONFIGURABLE_FOLDER}) */
    @NonNull
    public String toUserDisplayableString() {
        return UriUtils.toUserDisplayableString(UriUtils.appendPath(getBaseUri(), CollectionStream.of(getSubdirsToBase()).toJoinedString("/")));
    }

    @Nullable
    public static Folder fromFile(final File file) {
        return fromFile(file, null);
    }

    @Nullable
    public static Folder fromFile(final File file, final String subfolders) {

        if (file == null) {
            return null;
        }
        return new Folder(FolderType.FILE, Uri.fromFile(file), null, toFolderNames(subfolders));
    }

    @Nullable
    public static Folder fromDocumentUri(final Uri uri) {
        return fromDocumentUri(uri, null);
    }

    @Nullable
    public static Folder fromDocumentUri(final Uri uri, final String subfolders) {

        if (uri == null) {
            return null;
        }
        return new Folder(FolderType.DOCUMENT, uri, null, toFolderNames(subfolders));
    }

    @Nullable
    public static Folder fromFolder(final Folder folder, final String subfolder) {
        if (folder == null) {
            return null;
        }
        final List<String> newSubfolders = new ArrayList<>(folder.subfolders);
        newSubfolders.addAll(toFolderNames(subfolder));
        return new Folder(folder.type, folder.uri, folder.configurableFolder, newSubfolders);
    }

    @Nullable
    public static Folder fromConfigurableFolder(final ConfigurableFolder configurableFolder, final String subfolder) {
        if (configurableFolder == null) {
            return null;
        }
        return new Folder(FolderType.CONFIGURABLE_FOLDER, null, configurableFolder, toFolderNames(subfolder));
    }

    /** Creates Folder instance from a previously deserialized representation using {@link Folder#toConfig()}. */
    @Nullable
    public static Folder fromConfig(final String config) {
        if (config == null) {
            return null;
        }

        final Folder result = fromConfigStrict(config);
        if (result != null) {
            return result;
        }

        //try parse as an Uri
        final Uri uri = UriUtils.parseUri(config);
        if (UriUtils.isFileUri(uri)) {
            return Folder.fromFile(UriUtils.toFile(uri));
        }
        if (UriUtils.isContentUri(uri)) {
            //we suspect that it is a documentUri in this case
            return Folder.fromDocumentUri(uri);
        }

        //we did our best, giving up now
        return null;

    }

    /** porses config strictly according to #toConfig */
    private static Folder fromConfigStrict(@NonNull final String config) {
        final String[] tokens = config.split(CONFIG_SEP, -1);
        if (tokens.length != 3) {
            return null;
        }
        final FolderType type = EnumUtils.getEnum(FolderType.class, tokens[0]);
        if (type == null) {
            return null;
        }
        switch (type) {
            case DOCUMENT:
                return Folder.fromDocumentUri(Uri.parse(tokens[1]), tokens[2]);
            case FILE:
                return Folder.fromFile(UriUtils.toFile(Uri.parse(tokens[1])), tokens[2]);
            case CONFIGURABLE_FOLDER:
                final ConfigurableFolder configFolder = EnumUtils.getEnum(ConfigurableFolder.class, tokens[1]);
                if (configFolder == null) {
                    return null;
                }
                return Folder.fromConfigurableFolder(configFolder, tokens[2]);
            default:
                return null;
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Folder)) {
            return false;
        }

        return this.toConfig(true).equals(((Folder) other).toConfig(true));
    }

    @Override
    public int hashCode() {
        return this.toConfig(true).hashCode();
    }

    /** returns a config string for this foleer fit for reconstrucvting it using {@link Folder#fromConfig(String)}. This value is ALWAYS immutable even whne folder type is volatile */
    public String toConfig() {
        return toConfig(false);
    }

    private String toConfig(final boolean forEquals) {

        final StringBuilder configString = new StringBuilder(type.toString()).append(CONFIG_SEP);
        switch (type) {
            case CONFIGURABLE_FOLDER:
                configString.append(this.configurableFolder.name());
                break;
            case FILE:
            case DOCUMENT:
            default:
                if (forEquals) {
                    configString.append(UriUtils.toCompareString(uri));
                } else {
                    configString.append(uri);
                }
                break;
        }
        //Important: do NOT output getSubdirsToBase() here!
        //For folders based on other folders with subdirs, this woul lead to false reconstruction with doubled up subfolders
        configString.append(CONFIG_SEP).append(this.subfolderString);
        return configString.toString();
    }

    @NotNull
    @Override
    public String toString() {
        //We can't print the REAL Uri this Folder points to since this would require a call to FolderStorage
        return toUserDisplayableString() +
            "[" +
            getType() +
            (getRootConfigurableFolder() == null ? "" : "(" + getRootConfigurableFolder().name() + ")") +
            "#" + subfolders.size() +
            ":" + UriUtils.getPseudoUriString(getBaseUri(), getSubdirsToBase(), -1) +
            "]";
    }

    @NonNull
    private static List<String> toFolderNames(final String names) {
        if (names == null) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>();
        for (String token : names.split("/")) {
            if (!StringUtils.isBlank(token)) {
                result.add(token.replaceAll("[^a-zA-Z0-9-_.]", "-").trim());
            }
        }
        return result;
    }
}
