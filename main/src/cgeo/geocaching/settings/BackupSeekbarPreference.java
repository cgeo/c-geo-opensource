package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;


public class BackupSeekbarPreference extends SeekbarPreference {

    private SeekBar seekBar;
    private TextView valueView;


    public BackupSeekbarPreference(final Context context) {
        super(context);
    }

    public BackupSeekbarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BackupSeekbarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getValueString(final int progress) {

        if (progress == minProgress) {
            return context.getString(R.string.init_backup_history_length_min);
        } else if (progress == maxProgress) {
            return context.getString(R.string.init_backup_history_length_max);
        }
        return super.getValueString(progress + 1);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);
        valueView = v.findViewById(R.id.preference_seekbar_value_view);
        valueView.setSingleLine(false);
        final LayoutParams params = (LayoutParams) valueView.getLayoutParams();
        params.weight = 3.0f;
        valueView.setMinLines(2);
        valueView.setOnClickListener(null);

        seekBar = v.findViewById(R.id.preference_seekbar);

        return v;

    }

    public void setValue(final int value) {
        seekBar.setProgress(value);
        valueView.setText(getValueString(value));
        saveSetting(value);
    }
}
