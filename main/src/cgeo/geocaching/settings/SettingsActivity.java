package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.SelectMapfileActivity;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory.NavigationAppsEnum;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.files.SimpleDirChooser;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.ui.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogTemplate;

import org.apache.commons.lang3.StringUtils;
import org.openintents.intents.FileManagerIntents;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design
 * guidelines and the <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for
 * more information on developing a Settings UI.
 *
 * @author koem (initial author)
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String INTENT_GOTO = "GOTO";
    private static final int INTENT_GOTO_SERVICES = 1;

    private EditText signatureText;

    /**
     * Enumeration for directory choosers. This is how we can retrieve information about the
     * directory and preference key in onActivityResult() easily just by knowing
     * the result code.
     */
    private enum DirChooserType {
        GPX_IMPORT_DIR(1, R.string.pref_gpxImportDir,
                Environment.getExternalStorageDirectory().getPath() + "/gpx"),
        GPX_EXPORT_DIR(2, R.string.pref_gpxExportDir,
                Environment.getExternalStorageDirectory().getPath() + "/gpx"),
        THEMES_DIR(3, R.string.pref_renderthemepath, "");
        public final int requestCode;
        public final int keyId;
        public final String defaultValue;

        DirChooserType(final int requestCode, final int keyId, final String defaultValue) {
            this.requestCode = requestCode;
            this.keyId = keyId;
            this.defaultValue = defaultValue;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(Settings.isLightSkin() ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        SettingsActivity.addPreferencesFromResource(this, R.xml.preferences);
        initPreferences();

        Intent intent = getIntent();
        int gotoPage = intent.getIntExtra(INTENT_GOTO, 0);
        if (gotoPage == INTENT_GOTO_SERVICES) {
            // start with services screen
            PreferenceScreen main = (PreferenceScreen) getPreference(R.string.pref_fakekey_main_screen);
            int index = getPreference(R.string.pref_fakekey_services_screen).getOrder();
            main.onItemClick(null, null, index, 0);
        }
    }

    @Override
    protected void onPause() {
        Compatibility.dataChanged(getPackageName());
        super.onPause();
    }

    private void initPreferences() {
        initMapSourcePreference();
        initDirChoosers();
        initDefaultNavigationPreferences();
        initBackupButtons();
        initDbLocationPreference();
        initDebugPreference();
        initBasicMemberPreferences();
        initSend2CgeoPreferences();
        initServicePreferences();

        for (int k : new int[] { R.string.pref_username, R.string.pref_password,
                R.string.pref_pass_vote, R.string.pref_signature,
                R.string.pref_mapsource, R.string.pref_renderthemepath,
                R.string.pref_gpxExportDir, R.string.pref_gpxImportDir,
                R.string.pref_mapDirectory, R.string.pref_defaultNavigationTool,
                R.string.pref_defaultNavigationTool2, R.string.pref_webDeviceName,
                R.string.pref_fakekey_preference_backup_info, }) {
            bindSummaryToStringValue(k);
        }
    }

    private void initServicePreferences() {
        getPreference(R.string.pref_connectorOCActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        getPreference(R.string.pref_connectorOCPLActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
        getPreference(R.string.pref_connectorGCActive).setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);
    }

    private static String getKey(final int prefKeyId) {
        return cgeoapplication.getInstance().getString(prefKeyId);
    }

    private Preference getPreference(final int keyId) {
        return SettingsActivity.findPreference(this, getKey(keyId));
    }

    // workaround, because OnContextItemSelected nor onMenuItemSelected is never called
    OnMenuItemClickListener TEMPLATE_CLICK = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            LogTemplate template = LogTemplateProvider.getTemplate(item.getItemId());
            if (template != null) {
                insertSignatureTemplate(template);
                return true;
            }
            return false;
        }
    };

    // workaround, because OnContextItemSelected and onMenuItemSelected are never called
    void setSignatureTextView(final EditText view) {
        this.signatureText = view;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        // context menu for signature templates
        if (v.getId() == R.id.signature_templates) {
            menu.setHeaderTitle(R.string.init_signature_template_button);
            ArrayList<LogTemplate> templates = LogTemplateProvider.getTemplates();
            for (int i = 0; i < templates.size(); ++i) {
                menu.add(0, templates.get(i).getItemId(), 0, templates.get(i).getResourceId());
                menu.getItem(i).setOnMenuItemClickListener(TEMPLATE_CLICK);
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private void insertSignatureTemplate(final LogTemplate template) {
        String insertText = "[" + template.getTemplateString() + "]";
        ActivityMixin.insertAtPosition(signatureText, insertText, true);
    }

    /**
     * Fill the choice list for map sources.
     */
    private void initMapSourcePreference() {
        ListPreference pref = (ListPreference) getPreference(R.string.pref_mapsource);

        List<MapSource> mapSources = MapProviderFactory.getMapSources();
        CharSequence[] entries = new CharSequence[mapSources.size()];
        CharSequence[] values = new CharSequence[mapSources.size()];
        for (int i = 0; i < mapSources.size(); ++i) {
            entries[i] = mapSources.get(i).getName();
            values[i] = String.valueOf(mapSources.get(i).getNumericalId());
        }
        pref.setEntries(entries);
        pref.setEntryValues(values);
    }

    /**
     * Fill the choice list for default navigation tools.
     */
    private void initDefaultNavigationPreferences() {

        final List<NavigationAppsEnum> apps = NavigationAppFactory.getInstalledDefaultNavigationApps();

        CharSequence[] entries = new CharSequence[apps.size()];
        CharSequence[] values = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); ++i) {
            entries[i] = apps.get(i).toString();
            values[i] = String.valueOf(apps.get(i).id);
        }

        ListPreference pref = (ListPreference) getPreference(R.string.pref_defaultNavigationTool);
        pref.setEntries(entries);
        pref.setEntryValues(values);
        pref = (ListPreference) getPreference(R.string.pref_defaultNavigationTool2);
        pref.setEntries(entries);
        pref.setEntryValues(values);
    }

    private void initDirChoosers() {
        for (final DirChooserType dct : DirChooserType.values()) {

            getPreference(dct.keyId).setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            startDirChooser(dct);
                            return false;
                        }
                    });
        }

        getPreference(R.string.pref_mapDirectory).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        Intent i = new Intent(SettingsActivity.this,
                                SelectMapfileActivity.class);
                        startActivityForResult(i, R.string.pref_mapDirectory);
                        return false;
                    }
                });
    }

    /**
     * Fire up a directory chooser on click on the preference.
     *
     * @see #onActivityResult() for processing of the selected directory
     *
     * @param dct
     *            type of directory to be selected
     */
    private void startDirChooser(final DirChooserType dct) {

        final String startDirectory = Settings.getString(dct.keyId, dct.defaultValue);

        try {
            final Intent dirChooser = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
            if (StringUtils.isNotBlank(startDirectory)) {
                dirChooser.setData(Uri.fromFile(new File(startDirectory)));
            }
            dirChooser.putExtra(FileManagerIntents.EXTRA_TITLE,
                    getString(R.string.simple_dir_chooser_title));
            dirChooser.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                    getString(android.R.string.ok));
            startActivityForResult(dirChooser, dct.requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            // OI file manager not available
            final Intent dirChooser = new Intent(this, SimpleDirChooser.class);
            dirChooser.putExtra(Intents.EXTRA_START_DIR, startDirectory);
            startActivityForResult(dirChooser, dct.requestCode);
        }
    }

    private void setChosenDirectory(final DirChooserType dct, final Intent data) {
        final String directory = new File(data.getData().getPath()).getAbsolutePath();
        if (StringUtils.isNotBlank(directory)) {
            Preference p = getPreference(dct.keyId);
            if (p == null) {
                return;
            }
            Settings.putString(dct.keyId, directory);
            p.setSummary(directory);
        }
    }

    public void initBackupButtons() {
        Preference backup = getPreference(R.string.pref_fakekey_preference_backup);
        backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Context context = preference.getContext();
                // avoid overwriting an existing backup with an empty database
                // (can happen directly after reinstalling the app)
                if (cgData.getAllCachesCount() == 0) {
                    ActivityMixin.helpDialog(SettingsActivity.this,
                            context.getString(R.string.init_backup),
                            context.getString(R.string.init_backup_unnecessary));
                    return false;
                }

                final ProgressDialog dialog = ProgressDialog.show(context,
                        context.getString(R.string.init_backup),
                        context.getString(R.string.init_backup_running), true, false);
                new Thread() {
                    @Override
                    public void run() {
                        final String backupFileName = cgData.backupDatabase();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                ActivityMixin.helpDialog(SettingsActivity.this,
                                        context.getString(R.string.init_backup_backup),
                                        backupFileName != null
                                                ? context.getString(R.string.init_backup_success)
                                                        + "\n" + backupFileName
                                                : context.getString(R.string.init_backup_failed));
                                VALUE_CHANGE_LISTENER.onPreferenceChange(SettingsActivity.this.getPreference(R.string.pref_fakekey_preference_backup_info), "");
                            }
                        });
                    }
                }.start();
                return true;
            }
        });

        Preference restore = getPreference(R.string.pref_fakekey_preference_restore);
        restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ((cgeoapplication) SettingsActivity.this.getApplication())
                        .restoreDatabase(SettingsActivity.this);
                return true;
            }
        });
    }

    private void initDbLocationPreference() {
        Preference p = getPreference(R.string.pref_dbonsdcard);
        p.setPersistent(false);
        p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                boolean oldValue = Settings.isDbOnSDCard();
                ((cgeoapplication) SettingsActivity.this.getApplication())
                        .moveDatabase(SettingsActivity.this);
                return oldValue != Settings.isDbOnSDCard();
            }
        });
    }

    private void initDebugPreference() {
        Preference p = getPreference(R.string.pref_debug);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                Log.setDebug((Boolean) newValue);
                return true;
            }
        });
    }

    void initBasicMemberPreferences() {
        getPreference(R.string.pref_fakekey_basicmembers_screen)
                .setEnabled(!Settings.isPremiumMember());
        getPreference(R.string.pref_loaddirectionimg)
                .setEnabled(!Settings.isPremiumMember());
        getPreference(R.string.pref_showcaptcha)
                .setEnabled(!Settings.isPremiumMember());

        redrawScreen(R.string.pref_fakekey_services_screen);
    }

    void redrawScreen(int key) {
        PreferenceScreen screen = (PreferenceScreen) getPreference(key);
        if (screen == null) {
            return;
        }
        ListAdapter adapter = screen.getRootAdapter();
        if (adapter instanceof BaseAdapter) {
            ((BaseAdapter) adapter).notifyDataSetChanged();
        }
    }

    private static void initSend2CgeoPreferences() {
        Settings.putString(R.string.pref_webDeviceName, Settings.getWebDeviceName());
    }

    public void setOcAuthTitle(int prefKeyId) {
        //TODO: Generalize!
        switch (prefKeyId) {
            case R.string.pref_fakekey_ocde_authorization:
                setOCDEAuthTitle();
                break;
            case R.string.pref_fakekey_ocpl_authorization:
                setOCPLAuthTitle();
                break;
            default:
                Log.e(String.format(Locale.ENGLISH, "Invalid key %d in SettingsActivity.setTitle()", prefKeyId));
        }
    }

    void setOCDEAuthTitle() {
        getPreference(R.string.pref_fakekey_ocde_authorization)
                .setTitle(getString(Settings.hasOCAuthorization(R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret)
                        ? R.string.init_reregister_oc_de
                        : R.string.init_register_oc_de));
    }

    void setOCPLAuthTitle() {
        getPreference(R.string.pref_fakekey_ocpl_authorization)
                .setTitle(getString(Settings.hasOCAuthorization(R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret)
                        ? R.string.init_reregister_oc_pl
                        : R.string.init_register_oc_pl));
    }

    void setTwitterAuthTitle() {
        getPreference(R.string.pref_fakekey_twitter_authorization)
                .setTitle(getString(Settings.hasTwitterAuthorization()
                        ? R.string.init_twitter_reauthorize
                        : R.string.init_twitter_authorize));
    }

    public static void jumpToServicesPage(final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, SettingsActivity.class);
        intent.putExtra(INTENT_GOTO, INTENT_GOTO_SERVICES);
        fromActivity.startActivity(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        for (DirChooserType dct : DirChooserType.values()) {
            if (requestCode == dct.requestCode) {
                setChosenDirectory(dct, data);
                return;
            }
        }

        switch (requestCode) {
            case R.string.pref_mapDirectory:
                if (data.hasExtra(Intents.EXTRA_MAP_FILE)) {
                    final String mapFile = data.getStringExtra(Intents.EXTRA_MAP_FILE);
                    Settings.setMapFile(mapFile);
                    if (!Settings.isValidMapFile(Settings.getMapFile())) {
                        ActivityMixin.showToast(this, R.string.warn_invalid_mapfile);
                    }
                }
                initMapSourcePreference();
                getPreference(R.string.pref_mapDirectory).setSummary(
                        Settings.getMapFileDirectory());
                break;
            case R.string.pref_fakekey_ocde_authorization:
                setOCDEAuthTitle();
                redrawScreen(R.string.pref_fakekey_services_screen);
                break;
            case R.string.pref_fakekey_ocpl_authorization:
                setOCPLAuthTitle();
                redrawScreen(R.string.pref_fakekey_services_screen);
                break;
            case R.string.pref_fakekey_twitter_authorization:
                setTwitterAuthTitle();
                redrawScreen(R.string.pref_fakekey_services_screen);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener VALUE_CHANGE_LISTENER = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, final Object value) {
            String stringValue = value.toString();

            if (preference instanceof EditPasswordPreference) {
                if (StringUtils.isBlank((String) value)) {
                    preference.setSummary("");
                } else {
                    preference.setSummary("\u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022 \u2022");
                }
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else if (getKey(R.string.pref_fakekey_preference_backup_info).equals(preference.getKey())) {
                File lastBackupFile = cgData.getRestoreFile();
                String text;
                if (lastBackupFile != null) {
                    text = preference.getContext().getString(R.string.init_backup_last) + " "
                            + Formatter.formatTime(lastBackupFile.lastModified())
                            + ", " + Formatter.formatDate(lastBackupFile.lastModified());
                } else {
                    text = preference.getContext().getString(R.string.init_backup_last_no);
                }
                preference.setSummary(text);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            // reset log-in if gc user or password is changed
            if (isPreference(preference, R.string.pref_username) || isPreference(preference, R.string.pref_password)) {
                if (Login.isActualLoginStatus()) {
                    Login.logout();
                }
                cgeoapplication.getInstance().checkLogin = true;
            }
            // reset log-in status if connector activation was changed
            if (isPreference(preference, R.string.pref_connectorOCActive) || isPreference(preference, R.string.pref_connectorOCPLActive) || isPreference(preference, R.string.pref_connectorGCActive)) {
                cgeoapplication.getInstance().checkLogin = true;
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #VALUE_CHANGE_LISTENER
     */
    private static void bindSummaryToValue(final Preference preference, final Object value) {
        // Set the listener to watch for value changes.
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceChangeListener(VALUE_CHANGE_LISTENER);

        // Trigger the listener immediately with the preference's
        // current value.
        VALUE_CHANGE_LISTENER.onPreferenceChange(preference, value);
    }

    /**
     * auto-care for the summary of the preference of string type with this key
     *
     * @param key
     */
    private void bindSummaryToStringValue(final int key) {

        Preference pref = getPreference(key);
        if (pref == null) {
            return;
        }

        String value = PreferenceManager
                .getDefaultSharedPreferences(pref.getContext())
                .getString(pref.getKey(), "");

        bindSummaryToValue(pref, value);
    }

    @SuppressWarnings("deprecation")
    public static Preference findPreference(final PreferenceActivity preferenceActivity, final CharSequence key) {
        return preferenceActivity.findPreference(key);
    }

    @SuppressWarnings("deprecation")
    public static void addPreferencesFromResource(final PreferenceActivity preferenceActivity, final int preferencesResId) {
        preferenceActivity.addPreferencesFromResource(preferencesResId);
    }

    private static boolean isPreference(final Preference preference, int preferenceKeyId) {
        return getKey(preferenceKeyId).equals(preference.getKey());
    }
}
