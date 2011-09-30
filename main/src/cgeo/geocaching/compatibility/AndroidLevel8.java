package cgeo.geocaching.compatibility;

import cgeo.geocaching.cgSettings;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.util.Log;
import android.view.Display;

public class AndroidLevel8 {

    static public int getRotation(final Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    static public void dataChanged(final String name) {
        Log.i(cgSettings.tag, "Requesting settings backup with settings manager");
        BackupManager.dataChanged(name);
    }
}
