package cgeo.geocaching.connector;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.lang.ref.WeakReference;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

public class UserAction {

    @StringRes public final int displayResourceId;
    @NonNull private final Action1<Context> runnable;

    public static class Context {
        @NonNull
        public final String userName;
        public final String userId;
        public WeakReference<Activity> activityRef;

        public Context(@NonNull final String userName, @NonNull final String userId) {
            this.userName = userName;
            this.userId = userId;
        }

        public void startActivity(final Intent intent) {
            final Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            try {
                activity.startActivity(intent);
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e);
            }
        }

        public void setActivity(final Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        public Activity getActivity() {
            return activityRef.get();
        }
    }

    public UserAction(@StringRes final int displayResourceId, @NonNull final Action1<Context> runnable) {
        this.displayResourceId = displayResourceId;
        this.runnable = runnable;
    }

    public void run(@NonNull final Context context) {
        runnable.call(context);
    }
}
