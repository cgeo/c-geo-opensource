package cgeo.geocaching.ui.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.lang.reflect.Field;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.Log;

/**
 * Modified progress dialog class which allows hiding the absolute numbers.
 *
 */
public class CustomProgressDialog extends ProgressDialog {

    public CustomProgressDialog(final Context context) {
        super(context, ActivityMixin.getDialogTheme());
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Field is private, make it accessible through reflection before hiding it.
            final Field field = getClass().getSuperclass().getDeclaredField("mProgressNumber");
            field.setAccessible(true);
            ((View) field.get(this)).setVisibility(View.GONE);
        } catch (final Exception e) { // no multi-catch below SDK 19
            Log.e("Failed to find the progressDialog field 'mProgressNumber'", e);
        }
    }
}
