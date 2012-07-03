package cgeo.geocaching.export;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringEscapeUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class GpxExport extends AbstractExport {
    private static final File exportLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gpx");
    private static final SimpleDateFormat dateFormatZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    protected GpxExport() {
        super(getString(R.string.export_gpx));
    }

    @Override
    public void export(final List<cgCache> caches, final Activity activity) {
        if (null == activity) {
            // No activity given, so no user interaction possible.
            // Start export with default parameters.
            new ExportTask(caches, activity).execute((Void) null);

        } else {
            // Show configuration dialog
            new ExportOptionsDialog(caches, activity).show();
        }
    }

    /**
     * A dialog to allow the user to set options for the export.
     *
     * Currently available option is: opening of share menu after successful export
     */
    private class ExportOptionsDialog extends AlertDialog {
        public ExportOptionsDialog(final List<cgCache> caches, final Activity activity) {
            super(activity);

            View layout = activity.getLayoutInflater().inflate(R.layout.gpx_export_dialog, null);
            setView(layout);

            final CheckBox shareOption = (CheckBox) layout.findViewById(R.id.share);

            shareOption.setChecked(Settings.getShareAfterExport());

            shareOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Settings.setShareAfterExport(shareOption.isChecked());
                }
            });

            ((Button) layout.findViewById(R.id.export)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    new ExportTask(caches, activity).execute((Void) null);
                }
            });
        }
    }

    private class ExportTask extends AsyncTask<Void, Integer, Boolean> {
        private final List<cgCache> caches;
        private final Activity activity;
        private final Progress progress = new Progress();
        private File exportFile;
        private Writer gpx;

        /**
         * Instantiates and configures the task for exporting field notes.
         *
         * @param caches
         *            The {@link List} of {@link cgCache} to be exported
         * @param activity
         *            optional: Show a progress bar and toasts
         */
        public ExportTask(final List<cgCache> caches, final Activity activity) {
            this.caches = caches;
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            if (null != activity) {
                progress.show(activity, null, getString(R.string.export) + ": " + getName(), ProgressDialog.STYLE_HORIZONTAL, null);
                progress.setMaxProgressAndReset(caches.size());
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // quick check for being able to write the GPX file
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return false;
            }

            try {
                exportLocation.mkdirs();

                final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                exportFile = new File(exportLocation.toString() + File.separatorChar + "export_" + fileNameDateFormat.format(new Date()) + ".gpx");

                gpx = new BufferedWriter(new FileWriter(exportFile));

                gpx.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                gpx.write("<gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd http://www.groundspeak.com/cache/1/0/1 http://www.groundspeak.com/cache/1/0/1/cache.xsd\">");

                for (int i = 0; i < caches.size(); i++) {
                    // reload the cache. otherwise logs, attributes and other detailed information is not available
                    final cgCache cache = cgeoapplication.getInstance().loadCache(caches.get(i).getGeocode(), LoadFlags.LOAD_ALL_DB_ONLY);

                    gpx.write("<wpt ");
                    gpx.write("lat=\"");
                    gpx.write(Double.toString(cache.getCoords().getLatitude()));
                    gpx.write("\" ");
                    gpx.write("lon=\"");
                    gpx.write(Double.toString(cache.getCoords().getLongitude()));
                    gpx.write("\">");

                    gpx.write("<time>");
                    gpx.write(StringEscapeUtils.escapeXml(dateFormatZ.format(cache.getHiddenDate())));
                    gpx.write("</time>");

                    gpx.write("<name>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getGeocode()));
                    gpx.write("</name>");

                    gpx.write("<desc>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.write("</desc>");

                    gpx.write("<url>");
                    gpx.write(cache.getUrl());
                    gpx.write("</url>");

                    gpx.write("<urlname>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.write("</urlname>");

                    gpx.write("<sym>");
                    gpx.write(cache.isFound() ? "Geocache Found" : "Geocache");
                    gpx.write("</sym>");

                    gpx.write("<type>");
                    gpx.write(StringEscapeUtils.escapeXml("Geocache|" + cache.getType().pattern));
                    gpx.write("</type>");

                    gpx.write("<groundspeak:cache ");
                    gpx.write("id=\"");
                    gpx.write(cache.getCacheId());
                    gpx.write("\" available=\"");
                    gpx.write(!cache.isDisabled() ? "True" : "False");
                    gpx.write("\" archived=\"");
                    gpx.write(cache.isArchived() ? "True" : "False");
                    gpx.write("\" ");
                    gpx.write("xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\">");

                    gpx.write("<groundspeak:name>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getName()));
                    gpx.write("</groundspeak:name>");

                    gpx.write("<groundspeak:placed_by>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getOwnerDisplayName()));
                    gpx.write("</groundspeak:placed_by>");

                    gpx.write("<groundspeak:owner>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getOwnerUserId()));
                    gpx.write("</groundspeak:owner>");

                    gpx.write("<groundspeak:type>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getType().pattern));
                    gpx.write("</groundspeak:type>");

                    gpx.write("<groundspeak:container>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getSize().id));
                    gpx.write("</groundspeak:container>");

                    writeAttributes(cache);

                    gpx.write("<groundspeak:difficulty>");
                    gpx.write(Float.toString(cache.getDifficulty()));
                    gpx.write("</groundspeak:difficulty>");

                    gpx.write("<groundspeak:terrain>");
                    gpx.write(Float.toString(cache.getTerrain()));
                    gpx.write("</groundspeak:terrain>");

                    gpx.write("<groundspeak:country>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getLocation()));
                    gpx.write("</groundspeak:country>");

                    gpx.write("<groundspeak:state></groundspeak:state>"); // c:geo cannot manage 2 separate fields, so we export as country

                    gpx.write("<groundspeak:short_description html=\"");
                    if (BaseUtils.containsHtml(cache.getShortDescription())) {
                        gpx.write("True");
                    } else {
                        gpx.write("False");
                    }
                    gpx.write("\">");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getShortDescription()));
                    gpx.write("</groundspeak:short_description>");

                    gpx.write("<groundspeak:long_description html=\"");
                    if (BaseUtils.containsHtml(cache.getDescription())) {
                        gpx.write("True");
                    } else {
                        gpx.write("False");
                    }
                    gpx.write("\">");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getDescription()));
                    gpx.write("</groundspeak:long_description>");

                    gpx.write("<groundspeak:encoded_hints>");
                    gpx.write(StringEscapeUtils.escapeXml(cache.getHint()));
                    gpx.write("</groundspeak:encoded_hints>");

                    writeLogs(cache);

                    gpx.write("</groundspeak:cache>");

                    gpx.write("</wpt>");

                    writeWaypoints(cache);

                    publishProgress(i + 1);
                }

                gpx.write("</gpx>");

                gpx.close();
            } catch (Exception e) {
                Log.e("GpxExport.ExportTask export", e);

                if (gpx != null) {
                    try {
                        gpx.close();
                    } catch (IOException ee) {
                    }
                }

                // delete partial gpx file on error
                if (exportFile.exists()) {
                    exportFile.delete();
                }

                return false;
            }

            return true;
        }

        private void writeWaypoints(final cgCache cache) throws IOException {
            List<cgWaypoint> waypoints = cache.getWaypoints();
            List<cgWaypoint> ownWaypoints = new ArrayList<cgWaypoint>(waypoints.size());
            List<cgWaypoint> originWaypoints = new ArrayList<cgWaypoint>(waypoints.size());
            for (cgWaypoint wp : cache.getWaypoints()) {
                if (wp.isUserDefined()) {
                    ownWaypoints.add(wp);
                } else {
                    originWaypoints.add(wp);
                }
            }
            int maxPrefix = 0;
            for (cgWaypoint wp : originWaypoints) {
                String prefix = wp.getPrefix();
                try {
                    maxPrefix = Math.max(Integer.parseInt(prefix), maxPrefix);
                } catch (NumberFormatException ex) {
                    Log.e("Unexpected origin waypoint prefix='" + prefix + "'", ex);
                }
                writeCacheWaypoint(wp, prefix);
            }
            for (cgWaypoint wp : ownWaypoints) {
                maxPrefix++;
                String prefix = String.valueOf(maxPrefix);
                if (prefix.length() == 1) {
                    prefix = "0" + prefix;
                }
                writeCacheWaypoint(wp, prefix);
            }
        }

        /**
         * Writes one waypoint entry for cache waypoint.
         *
         * @param cache
         *            The
         * @param wp
         * @param prefix
         * @throws IOException
         */
        private void writeCacheWaypoint(final cgWaypoint wp, final String prefix) throws IOException {
            gpx.write("<wpt lat=\"");
            final Geopoint coords = wp.getCoords();
            gpx.write(coords != null ? Double.toString(coords.getLatitude()) : ""); // TODO: check whether is the best way to handle unknown waypoint coordinates
            gpx.write("\" lon=\"");
            gpx.write(coords != null ? Double.toString(coords.getLongitude()) : "");
            gpx.write("\">");

            gpx.write("<name>");
            gpx.write(StringEscapeUtils.escapeXml(prefix));
            gpx.write(StringEscapeUtils.escapeXml(wp.getGeocode().substring(2)));
            gpx.write("</name>");

            gpx.write("<cmt>");
            gpx.write(StringEscapeUtils.escapeXml(wp.getNote()));
            gpx.write("</cmt>");

            gpx.write("<desc>");
            gpx.write(StringEscapeUtils.escapeXml(wp.getName()));
            gpx.write("</desc>");

            gpx.write("<sym>");
            gpx.write(StringEscapeUtils.escapeXml(wp.getWaypointType().toString())); //TODO: Correct identifier string
            gpx.write("</sym>");

            gpx.write("<type>Waypoint|");
            gpx.write(StringEscapeUtils.escapeXml(wp.getWaypointType().toString())); //TODO: Correct identifier string
            gpx.write("</type>");

            gpx.write("</wpt>");
        }

        private void writeLogs(final cgCache cache) throws IOException {
            if (cache.getLogs().size() <= 0) {
                return;
            }
            gpx.write("<groundspeak:logs>");

            for (LogEntry log : cache.getLogs()) {
                gpx.write("<groundspeak:log id=\"");
                gpx.write(Integer.toString(log.id));
                gpx.write("\">");

                gpx.write("<groundspeak:date>");
                gpx.write(StringEscapeUtils.escapeXml(dateFormatZ.format(new Date(log.date))));
                gpx.write("</groundspeak:date>");

                gpx.write("<groundspeak:type>");
                gpx.write(StringEscapeUtils.escapeXml(log.type.type));
                gpx.write("</groundspeak:type>");

                gpx.write("<groundspeak:finder id=\"\">");
                gpx.write(StringEscapeUtils.escapeXml(log.author));
                gpx.write("</groundspeak:finder>");

                gpx.write("<groundspeak:text encoded=\"False\">");
                gpx.write(StringEscapeUtils.escapeXml(log.log));
                gpx.write("</groundspeak:text>");

                gpx.write("</groundspeak:log>");
            }

            gpx.write("</groundspeak:logs>");
        }

        private void writeAttributes(final cgCache cache) throws IOException {
            if (!cache.hasAttributes()) {
                return;
            }
            //TODO: Attribute conversion required: English verbose name, gpx-id
            gpx.write("<groundspeak:attributes>");

            for (String attribute : cache.getAttributes()) {
                final CacheAttribute attr = CacheAttribute.getByGcRawName(CacheAttribute.trimAttributeName(attribute));
                final boolean enabled = CacheAttribute.isEnabled(attribute);

                gpx.write("<groundspeak:attribute id=\"");
                gpx.write(Integer.toString(attr.id));
                gpx.write("\" inc=\"");
                if (enabled) {
                    gpx.write('1');
                } else {
                    gpx.write('0');
                }
                gpx.write("\">");
                gpx.write(StringEscapeUtils.escapeXml(attr.getL10n(enabled)));
                gpx.write("</groundspeak:attribute>");
            }

            gpx.write("</groundspeak:attributes>");
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (null != activity) {
                progress.dismiss();
                if (result) {
                    ActivityMixin.showToast(activity, getName() + ' ' + getString(R.string.export_exportedto) + ": " + exportFile.toString());
                    if (Settings.getShareAfterExport()) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(exportFile));
                        shareIntent.setType("application/xml");
                        activity.startActivity(Intent.createChooser(shareIntent, getString(R.string.export_gpx_to)));
                    }
                } else {
                    ActivityMixin.showToast(activity, getString(R.string.export_failed));
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... status) {
            if (null != activity) {
                progress.setProgress(status[0]);
            }
        }
    }
}
