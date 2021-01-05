package cgeo.geocaching.settings;

import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.ProcessUtils.isChromeLaunchable;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;



public class StartWebviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String url = getIntent().getDataString();
        if (url != null) {
            if (isChromeLaunchable()) {
                ShareUtils.openCustomTab(this, url);
            } else {
                // We're using "https://example.com" as we only want to query web browsers, not c:geo or other geocaching apps
                final Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("https://example.com"));

                final ResolveInfo resolveInfo = getPackageManager().queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY).get(0);

                final Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                launchIntent.setPackage(resolveInfo.activityInfo.packageName);

                startActivity(launchIntent);
            }
        }

        finish();

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
