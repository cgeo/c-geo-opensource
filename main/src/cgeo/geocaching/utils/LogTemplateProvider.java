package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.ui.Formatter;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * provides all the available templates for logging
 *
 */
public class LogTemplateProvider {

    /**
     * Context aware data container for log templates.
     * <p>
     * Some log templates need additional information. To provide that information, it can be encapsulated in this log
     * context.
     * </p>
     *
     */
    public static class LogContext {
        private Geocache cache;
        private Trackable trackable;
        private boolean offline = false;

        public LogContext(final Geocache cache) {
            this(cache, false);
        }

        public LogContext(final Trackable trackable) {
            this.trackable = trackable;
        }

        public LogContext(boolean offline) {
            this(null, offline);
        }

        public LogContext(final Geocache cache, boolean offline) {
            this.cache = cache;
            this.offline = offline;
        }

        public Geocache getCache() {
            return cache;
        }

        public Trackable getTrackable() {
            return trackable;
        }

        public boolean isOffline() {
            return offline;
        }
    }

    public static abstract class LogTemplate {
        private final String template;
        private final int resourceId;

        protected LogTemplate(String template, int resourceId) {
            this.template = template;
            this.resourceId = resourceId;
        }

        abstract public String getValue(LogContext context);

        public int getResourceId() {
            return resourceId;
        }

        public int getItemId() {
            return template.hashCode();
        }

        public String getTemplateString() {
            return template;
        }

        protected String apply(String input, LogContext context) {
            if (input.contains("[" + template + "]")) {
                return StringUtils.replace(input, "[" + template + "]", getValue(context));
            }
            return input;
        }
    }

    public static ArrayList<LogTemplate> getTemplates() {
        ArrayList<LogTemplate> templates = new ArrayList<LogTemplateProvider.LogTemplate>();
        templates.add(new LogTemplate("DATE", R.string.init_signature_template_date) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatFullDate(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("TIME", R.string.init_signature_template_time) {

            @Override
            public String getValue(final LogContext context) {
                return Formatter.formatTime(System.currentTimeMillis());
            }
        });
        templates.add(new LogTemplate("DATETIME", R.string.init_signature_template_datetime) {

            @Override
            public String getValue(final LogContext context) {
                final long currentTime = System.currentTimeMillis();
                return Formatter.formatFullDate(currentTime) + " " + Formatter.formatTime(currentTime);
            }
        });
        templates.add(new LogTemplate("USER", R.string.init_signature_template_user) {

            @Override
            public String getValue(final LogContext context) {
                return Settings.getUsername();
            }
        });
        templates.add(new LogTemplate("NUMBER", R.string.init_signature_template_number) {

            @Override
            public String getValue(final LogContext context) {
                int current = Login.getActualCachesFound();
                if (current == 0) {
                    if (context.isOffline()) {
                        return "";
                    }
                    final String page = Network.getResponseData(Network.getRequest("http://www.geocaching.com/email/"));
                    current = parseFindCount(page);
                }

                String findCount = "";
                if (current >= 0) {
                    findCount = String.valueOf(current + 1);
                }
                return findCount;
            }
        });
        templates.add(new LogTemplate("OWNER", R.string.init_signature_template_owner) {

            @Override
            public String getValue(final LogContext context) {
                Trackable trackable = context.getTrackable();
                if (trackable != null) {
                    return trackable.getOwner();
                }
                Geocache cache = context.getCache();
                if (cache != null) {
                    return cache.getOwnerDisplayName();
                }
                return "";
            }
        });
        return templates;
    }

    public static LogTemplate getTemplate(int itemId) {
        for (LogTemplate template : getTemplates()) {
            if (template.getItemId() == itemId) {
                return template;
            }
        }
        return null;
    }

    public static String applyTemplates(String signature, LogContext context) {
        if (signature == null) {
            return "";
        }
        String result = signature;
        for (LogTemplate template : getTemplates()) {
            result = template.apply(result, context);
        }
        return result;
    }

    private static int parseFindCount(String page) {
        if (StringUtils.isBlank(page)) {
            return -1;
        }

        try {
            return Integer.parseInt(TextUtils.getMatch(page, GCConstants.PATTERN_CACHES_FOUND, true, "-1").replaceAll("[,.]", ""));
        } catch (NumberFormatException e) {
            Log.e("parseFindCount", e);
            return -1;
        }
    }
}
