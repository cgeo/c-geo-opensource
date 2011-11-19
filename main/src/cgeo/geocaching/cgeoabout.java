package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import sheetrock.panda.changelog.ChangeLog;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class cgeoabout extends AbstractActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.about);
        setTitle(res.getString(R.string.about));

        init();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);

            setTitle(res.getString(R.string.about) + " (ver. " + info.versionName + ")");

            manager = null;

            ((TextView) findViewById(R.id.contributors)).setMovementMethod(LinkMovementMethod.getInstance());
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeoabout.init: Failed to obtain package version.");
        }
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void donateMore(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void donateLess(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void author(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://carnero.cc/")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void showFullChangeLog(View view) {
        new ChangeLog(this).getFullLogDialog().show();
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void support(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:support@cgeo.org")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void website(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void facebook(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/pages/cgeo/297269860090")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void twitter(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/android_gc")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void nutshellmanual(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }
}
