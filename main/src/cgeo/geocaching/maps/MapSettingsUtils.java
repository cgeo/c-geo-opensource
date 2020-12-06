package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class MapSettingsUtils {

    private static int colorAccent;
    private static boolean isShowCircles;

    private MapSettingsUtils() {
        // utility class
    }

    public static void showSettingsPopup(final Activity activity, final Action1<Boolean> onMapSettingsPopupFinished, final Action1<RoutingMode> setRoutingValue, final Action1<Integer> setCompactIconValue) {
        colorAccent = activity.getResources().getColor(R.color.colorAccent);
        isShowCircles = Settings.isShowCircles();

        final ArrayList<SettingsCheckboxModel> settingsElementsCheckboxes = new ArrayList<>();
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_ownfound, R.drawable.ic_menu_myplaces, Settings.isExcludeMyCaches(), Settings::setExcludeMine, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_disabled, R.drawable.ic_menu_disabled, Settings.isExcludeDisabledCaches(), Settings::setExcludeDisabled, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showc_archived, R.drawable.ic_menu_archived, Settings.isExcludeArchivedCaches(), Settings::setExcludeArchived, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_original, R.drawable.ic_menu_waypoint, Settings.isExcludeWpOriginal(), Settings::setExcludeWpOriginal, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_parking, R.drawable.ic_menu_parking, Settings.isExcludeWpParking(), Settings::setExcludeWpParking, true));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_showwp_visited, R.drawable.ic_menu_visited, Settings.isExcludeWpVisited(), Settings::setExcludeWpVisited, true));
        if (StringUtils.isNotBlank(Settings.getTrackFile())) {
            settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_track, R.drawable.ic_menu_hidetrack, Settings.isHideTrack(), Settings::setHideTrack, true));
        }
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_show_circles, R.drawable.ic_menu_circle, isShowCircles, Settings::setShowCircles, false));
        settingsElementsCheckboxes.add(new SettingsCheckboxModel(R.string.map_direction, R.drawable.ic_menu_goto, Settings.isMapDirection(), Settings::setMapDirection, false));

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.map_settings_dialog, null);
        ((ListView) dialogView.findViewById(R.id.map_settings_listview)).setAdapter(new MapSettingsAdapter(activity, settingsElementsCheckboxes));

        final ArrayList<ButtonChoiceModel<Integer>> compactIconChoices = new ArrayList<>();
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_off, Settings.COMPACTICON_OFF));
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_auto, Settings.COMPACTICON_AUTO));
        compactIconChoices.add(new ButtonChoiceModel<>(R.id.compacticon_on, Settings.COMPACTICON_ON));
        final ButtonController<Integer> compactIcon = new ButtonController<Integer>(dialogView, compactIconChoices, Settings.getCompactIconMode(), setCompactIconValue);

        final ArrayList<ButtonChoiceModel<RoutingMode>> routingChoices = new ArrayList<>();
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_straight, RoutingMode.STRAIGHT));
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_walk, RoutingMode.WALK));
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_bike, RoutingMode.BIKE));
        routingChoices.add(new ButtonChoiceModel<>(R.id.routing_car, RoutingMode.CAR));
        final ButtonController<RoutingMode> routing = new ButtonController<>(dialogView, routingChoices, Settings.getRoutingMode(), setRoutingValue);

        Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setTitle(R.string.quick_settings)
            .setOnDismissListener(dialog -> {
                for (SettingsCheckboxModel item : settingsElementsCheckboxes) {
                    item.setValue();
                }
                compactIcon.setValue();
                routing.setValue();
                onMapSettingsPopupFinished.call(isShowCircles != Settings.isShowCircles());
            })
            .create()
            .show();

        compactIcon.init();
        routing.init();
    }

    private static class SettingsCheckboxModel {
        private final int resTitle;
        private final int resIcon;
        private boolean currentValue;
        private final Action1<Boolean> setValue;
        private final boolean isNegated;

        SettingsCheckboxModel(@StringRes final int resTitle, @DrawableRes final int resIcon, final boolean currentValue, final Action1<Boolean> setValue, final boolean isNegated) {
            this.resTitle = resTitle;
            this.resIcon = resIcon;
            this.currentValue = isNegated ? !currentValue : currentValue;
            this.setValue = setValue;
            this.isNegated = isNegated;
        }

        public void setValue() {
            this.setValue.call(isNegated ? !currentValue : currentValue);
        }
    }

    private static class ButtonChoiceModel<T> {
        public final int resButton;
        public final T assignedValue;
        public View button = null;

        ButtonChoiceModel(final int resButton, final T assignedValue) {
            this.resButton = resButton;
            this.assignedValue = assignedValue;
        }
    }

    private static class ButtonController<T> {
        private final View dialogView;
        private final ArrayList<ButtonChoiceModel<T>> buttons;
        private T originalValue;
        private T currentValue;
        private final Action1<T> setValue;

        ButtonController(final View dialogView, final ArrayList<ButtonChoiceModel<T>> buttons, final T currentValue, final Action1<T> setValue) {
            this.dialogView = dialogView;
            this.buttons = buttons;
            this.originalValue = currentValue;
            this.currentValue = currentValue;
            this.setValue = setValue;
        }

        public void init() {
            for (final ButtonChoiceModel<T> button : buttons) {
                button.button = dialogView.findViewById(button.resButton);
                button.button.setOnClickListener(v -> setLocalValue(button.assignedValue));
            }
            update();
        }

        public void update() {
            for (final ButtonChoiceModel<T> button : buttons) {
                if (currentValue == button.assignedValue) {
                    button.button.setBackgroundColor(colorAccent);
                } else {
                    button.button.setBackgroundColor(0x00000000);
                    button.button.setBackgroundResource(Settings.isLightSkin() ? R.drawable.action_button_light : R.drawable.action_button_dark);
                }
            }
        }

        private void setLocalValue(final T currentValue) {
            this.currentValue = currentValue;
            update();
        }

        public void setValue() {
            if (!originalValue.equals(currentValue)) {
                this.setValue.call(currentValue);
            }
        }
    }

    private static class MapSettingsAdapter extends ArrayAdapter<SettingsCheckboxModel> {

        private final ArrayList<SettingsCheckboxModel> statusList;

        MapSettingsAdapter(final Context context, final ArrayList<SettingsCheckboxModel> statusList) {
            super(context, R.layout.map_settings_dialog, statusList);
            this.statusList = statusList;
        }

        @Override
        public @NonNull View getView(final int position, final @Nullable View convertView, final @NonNull ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.map_settings_item, null);
            }

            final SettingsCheckboxModel item = statusList.get(position);
            if (item != null) {
                ((ImageView) v.findViewById(R.id.settings_item_icon)).setImageResource(item.resIcon);
                ((TextView) v.findViewById(R.id.settings_item_text)).setText(item.resTitle);
                final CheckBox checkBox = v.findViewById(R.id.settings_item_checkbox);
                checkBox.setChecked(item.currentValue);
                v.setOnClickListener(v1 -> {
                    item.currentValue = !item.currentValue;
                    checkBox.setChecked(item.currentValue);
                });
            }
            return v;
        }

    }

}
