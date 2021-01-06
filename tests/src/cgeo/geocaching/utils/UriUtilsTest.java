package cgeo.geocaching.utils;

import android.net.Uri;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UriUtilsTest {

    //Example for folder /cgeo
    private static final String DOC_URI_EXAMPLE = "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fcgeo/document/primary%3ADocuments%2Fcgeo%2Flogfiles%2Flogcat_2020-12-28_17-22-20-2.txt";
    //Example for folder /Documents/cgeo
    private static final String DOC_URI_EXAMPLE_2 = "content://com.android.externalstorage.documents/tree/home%3Acgeo";
    //Example for folder /Downloads/cgeo
    private static final String DOC_URI_EXAMPLE_3 = "content://com.android.externalstorage.documents/tree/primary%3ADownload%2Fcgeo";

    //complicated example for a document in /Documents/cgeo/gpx
    private static final String DOC_URI_COMPLICATED = "content://com.android.externalstorage.documents/tree/home%3Acgeo/document/home%3Acgeo%2Fgpx%2Froute_2021-01-06_17-14-08-4.gpx";

    private static final String DOC_URI_EXAMPLE_DECODED = "content://com.android.externalstorage.documents/tree/primary:Documents/cgeo/document/primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt";

    private static final String FILE_URI_EXAMPLE = "file:///storage/emulated/0/cgeo/test.txt";

    @Test
    public void getUserDisplayableUri() {
        assertThat(UriUtils.toUserDisplayableString(Uri.parse(FILE_URI_EXAMPLE))).isEqualTo("/storage/emulated/0/cgeo/test.txt");
        assertThat(UriUtils.toUserDisplayableString(Uri.parse(DOC_URI_EXAMPLE))).isEqualTo("…/Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt");
        assertThat(UriUtils.toUserDisplayableString(null)).isEqualTo("");

        assertThat(UriUtils.toUserDisplayableString(Uri.parse(DOC_URI_EXAMPLE_2))).isEqualTo("…/[Documents]/cgeo");
        assertThat(UriUtils.toUserDisplayableString(Uri.parse(DOC_URI_EXAMPLE_3))).isEqualTo("…/Download/cgeo");
        assertThat(UriUtils.toUserDisplayableString(Uri.parse(DOC_URI_COMPLICATED))).isEqualTo("…/[Documents]/cgeo/gpx/route_2021-01-06_17-14-08-4.gpx");
    }

    @Test
    public void getLastPathSegment() {

        //A typical example of a non-trivial SAF Document Uri as returned by the framework
        //for tjis ecxample. Uri.getLastPathSegment gives you: primary:Documents/cgeo/logfiles/logcat_2020-12-28_17-22-20-2.txt
        final Uri uri = Uri.parse(DOC_URI_EXAMPLE);
        assertThat(UriUtils.getLastPathSegment(uri)).isEqualTo("logcat_2020-12-28_17-22-20-2.txt");

        //a little stability test
        assertThat(UriUtils.getLastPathSegment(null)).isNull();
    }

    @Test
    public void filesAndUris() throws IOException {

        //A non-file uri
        final Uri nonFileUri = Uri.parse("http://www.cgeo.org");
        assertThat(UriUtils.isFileUri(nonFileUri)).isFalse();
        assertThat(UriUtils.toFile(nonFileUri)).isNull();

        //a File Uri
        final File someFile = File.createTempFile("tempfileforunittest", "tmp");
        final Uri fileUri = Uri.fromFile(someFile);
        assertThat(UriUtils.isFileUri(fileUri)).isTrue();
        assertThat(UriUtils.toFile(fileUri)).isEqualTo(someFile);

        assertThat(someFile.delete()).isTrue();
    }

    @Test
    public void contentAndUris() {
        assertThat(UriUtils.isContentUri(Uri.parse(DOC_URI_EXAMPLE))).isTrue();
        assertThat(UriUtils.isContentUri(Uri.parse("/eins/zwei/drei"))).isFalse();
        assertThat(UriUtils.isContentUri(Uri.parse("http://www.cgeo.org"))).isFalse();
    }

    @Test
    public void appendPath() {
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "eins/zwei").toString()).isEqualTo(DOC_URI_EXAMPLE + "/eins/zwei");
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "/eins/zwei/").toString()).isEqualTo(DOC_URI_EXAMPLE + "/eins/zwei");
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "  /eins/zwei  ").toString()).isEqualTo(DOC_URI_EXAMPLE + "/eins/zwei");
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "   ").toString()).isEqualTo(DOC_URI_EXAMPLE);
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), null).toString()).isEqualTo(DOC_URI_EXAMPLE);
        assertThat(UriUtils.appendPath(Uri.parse(DOC_URI_EXAMPLE), "  ///  ").toString()).isEqualTo(DOC_URI_EXAMPLE);
    }

    @Test
    public void toStringDecoded() {
        assertThat(UriUtils.toCompareString(Uri.parse(DOC_URI_EXAMPLE))).isEqualTo(DOC_URI_EXAMPLE_DECODED);
    }

}
