package cgeo.geocaching.storage;

import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a concrete definite folder location.
 *
 * Folder locations can have different types as defined by {@link FolderType}.
 * Depending on that type, different attributes are filled and different handling is necessary
 * to interact with actual data in that folder. Those different handlings are implemented by {@link PublicLocalStorage}.
 *
 * Instances of this class are immutable. They can be serialized/deserialized to/from a String.
 *
 * Instances support the usage in Maps ({@link #equals(Object)} and {@link #hashCode()}). Note however that
 * two FolderLocation instances pointing to the same actual folder on disk but using two different FolderTypes
 * are NOT considered equal!
 */
public class FolderLocation {


    public enum FolderType {
        /** a 'classic' folder based on a file */
        FILE,
        /** Folder based on Storage Access Frameworks and retrieved by {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE} */
        DOCUMENT,
        /** A subfolder of another FolderLocation */
        SUBFOLDER,
    }

    //some base file locations for usage

   /** Root folder for documents (deprecated since API29 but still works somehow) */
    public static final File DOCUMENTS_FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);


    private static final String EMPTY = "---";
    private static final String CONFIG_SEP = "::";

    private final FolderType type;
    private final Uri uri;

    private final PublicLocalFolder subfolderBase; //needed for type SUBFOLDER
    private final String subfolder; //needed for type SUBFOLDER


    private FolderLocation(final FolderType type, final Uri uri, final PublicLocalFolder subfolderBase, final String subfolder) {
        this.type = type;
        this.uri = uri;

        this.subfolderBase = subfolderBase;
        this.subfolder = toFolderName(subfolder);
    }


    public FolderType getType() {
        return type;
    }

    public FolderType getBaseType() {
        if (type.equals(FolderType.SUBFOLDER)) {
            return this.subfolderBase.getLocation().getBaseType();
        }
        return type;
    }

    /** Uri associated with this folder */
    @Nullable
    public Uri getUri() {
        if (this.type.equals(FolderType.SUBFOLDER)) {
            return Uri.withAppendedPath(this.subfolderBase.getLocation().getUri(), this.subfolder);
        }
        return this.uri;
    }

    /** The base Uri (below all subfolders)) */
    @NonNull
    public Uri getBaseUri() {
        switch (this.type) {
            case SUBFOLDER:
                return this.subfolderBase.getLocation().getBaseUri();
            case DOCUMENT:
            case FILE:
            default:
                return getUri();
        }
    }

    public List<String> getSubdirsToBase() {
        if (!getType().equals(FolderType.SUBFOLDER)) {
            return new ArrayList<>();
        }
        final List<String > result = this.subfolderBase.getLocation().getSubdirsToBase();
        result.add(subfolder);
        return result;

    }

    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String getUserDisplayableName() {
        return getUri() == null ? EMPTY : getUri().getPath();
    }

    public static FolderLocation fromFile(final File file) {
        if (file == null) {
            return null;
        }
        return new FolderLocation(FolderType.FILE, Uri.fromFile(file), null, null);
    }

    @Nullable
    public static FolderLocation fromDocumentUri(final Uri uri) {
        if (uri == null) {
            return null;
        }
        return new FolderLocation(FolderType.DOCUMENT, uri, null, null);
    }

    public static FolderLocation fromSubfolder(final PublicLocalFolder publicLocalFolder, final String subfolder) {
        if (publicLocalFolder == null) {
            return null;
        }
        return new FolderLocation(FolderType.SUBFOLDER, null, publicLocalFolder, subfolder);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof FolderLocation)) {
            return false;
        }

        return this.toComparableString(true).equals(((FolderLocation) other).toComparableString(true));
    }

    @Override
    public int hashCode() {
        return this.toComparableString(true).hashCode();
    }

    private String toComparableString(final boolean unifiedUri) {

        final StringBuilder configString = new StringBuilder(type.toString()).append(CONFIG_SEP);
        switch (type) {
            case SUBFOLDER:
                configString.append(this.subfolderBase.name()).append(CONFIG_SEP).append(this.subfolder);
                break;
            case FILE:
            case DOCUMENT:
            default:
                configString.append(uriToComparableString(this.uri, unifiedUri));
                break;
        }
        return configString.toString();
    }

    private static String uriToComparableString(final Uri uri, final boolean unifiedUri) {
        if (uri == null) {
            return null;
        }
        if (!unifiedUri) {
            return uri.toString();
        }
        return uri.toString().replaceAll("%2F", "/");
    }

    @NotNull
    @Override
    public String toString() {
        return getUserDisplayableName() + "[" + toComparableString(false) + "]";
    }

    @NonNull
    private static String toFolderName(final String name) {
        String folderName = name == null ? "default" : name.replaceAll("[^a-zA-Z0-9-_.]", "-").trim();
        if (StringUtils.isBlank(folderName)) {
            folderName = "default";
        }
        return folderName;
    }
}
