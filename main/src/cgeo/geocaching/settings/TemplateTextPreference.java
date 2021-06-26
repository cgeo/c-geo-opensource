package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TemplateTextPreference extends DialogPreference {

    /**
     * default value, if none is given in the preference XML.
     */
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    private SettingsActivity settingsActivity;
    private EditText editText;
    private String initialValue;

    public TemplateTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.template_preference_dialog);
    }

    @Override
    protected void onBindDialogView(final View view) {
        settingsActivity = (SettingsActivity) this.getContext();

        editText = view.findViewById(R.id.signature_dialog_text);
        editText.setText(getPersistedString(initialValue != null ? initialValue : StringUtils.EMPTY));
        // @todo yet another workaround to be dismissed after migration of settings to PreferenceFragment
        setCursorDrawableColor(editText, Settings.isLightSkin(getContext()) ? 0xff000000 : 0xffffffff);
        Dialogs.moveCursorToEnd(editText);

        final Button button = view.findViewById(R.id.signature_templates);
        button.setOnClickListener(button1 -> {
            final AlertDialog.Builder alert = Dialogs.newBuilder(TemplateTextPreference.this.getContext());
            alert.setTitle(R.string.init_signature_template_button);
            final List<LogTemplate> templates = LogTemplateProvider.getTemplatesWithoutSignature();
            final String[] items = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                items[i] = settingsActivity.getString(templates.get(i).getResourceId());
            }
            alert.setItems(items, (dialog, position) -> {
                dialog.dismiss();
                final LogTemplate template = templates.get(position);
                insertSignatureTemplate(template);
            });
            final AlertDialog dialog = alert.create();
            // @todo yet another workaround to be dismissed after migration of settings to PreferenceFragment
            final int c = Build.VERSION.SDK_INT > 22 ? getContext().getColor(Settings.isLightSkin(getContext()) ? R.color.settings_colorDialogBackgroundLight : R.color.settings_colorDialogBackgroundDark) : CgeoApplication.getInstance().getResources().getColor(R.color.colorBackgroundSelected);
            dialog.getListView().setBackgroundColor(c);
            dialog.show();
        });

        super.onBindDialogView(view);
    }

    // @todo the whole method is yet another workaround to be dismissed after migration of settings to PreferenceFragment
    @SuppressLint("UseCompatLoadingForDrawables")
    public static void setCursorDrawableColor(final EditText editText, final int color) {
        // adapted from https://stackoverflow.com/questions/11554078/set-textcursordrawable-programmatically
        if (Build.VERSION.SDK_INT >= 29) {
            editText.setTextCursorDrawable(R.drawable.cursor);
        } else {
            try {
                final Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                fCursorDrawableRes.setAccessible(true);
                final int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
                final Field fEditor = TextView.class.getDeclaredField("mEditor");
                fEditor.setAccessible(true);
                final Object editor = fEditor.get(editText);
                final Field fCursorDrawable = editor.getClass().getDeclaredField("mCursorDrawable");
                fCursorDrawable.setAccessible(true);
                final Drawable[] drawables = new Drawable[2];
                drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
                drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
                drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                fCursorDrawable.set(editor, drawables);
            } catch (Throwable ignored) {
            }
        }
        editText.setTextColor(color);
    }

    private void insertSignatureTemplate(final LogTemplate template) {
        final String insertText = "[" + template.getTemplateString() + "]";
        ActivityMixin.insertAtPosition(editText, insertText, true);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            final String text = editText.getText().toString();
            persistString(text);
            callChangeListener(text);
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            initialValue = this.getPersistedString(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            initialValue = defaultValue.toString();
            persistString(initialValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray array, final int index) {
        return array.getString(index);
    }
}
