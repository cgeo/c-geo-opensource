package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.URLEncoder;
import java.util.Arrays;

public class cgeotrackable extends AbstractActivity {
    public cgTrackable trackable = null;
    public String geocode = null;
    public String name = null;
    public String guid = null;
    public String id = null;
    private String contextMenuUser = null;
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private Handler loadTrackableHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            RelativeLayout itemLayout;
            TextView itemName;
            TextView itemValue;

            if (trackable != null && trackable.getErrorRetrieve() != null) {
                showToast(res.getString(R.string.err_tb_details_download) + " " + trackable.getErrorRetrieve().getErrorString(res) + ".");

                finish();
                return;
            }

            if (trackable != null && StringUtils.isNotBlank(trackable.getError())) {
                showToast(res.getString(R.string.err_tb_details_download) + " " + trackable.getError() + ".");

                finish();
                return;
            }

            if (trackable == null) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                }

                if (StringUtils.isNotBlank(geocode)) {
                    showToast(res.getString(R.string.err_tb_find) + " " + geocode + ".");
                } else {
                    showToast(res.getString(R.string.err_tb_find_that));
                }

                finish();
                return;
            }

            try {
                inflater = getLayoutInflater();
                geocode = trackable.getGeocode().toUpperCase();

                if (StringUtils.isNotBlank(trackable.getName())) {
                    setTitle(Html.fromHtml(trackable.getName()).toString());
                } else {
                    setTitle(trackable.getName().toUpperCase());
                }

                ((ScrollView) findViewById(R.id.details_list_box)).setVisibility(View.VISIBLE);
                LinearLayout detailsList = (LinearLayout) findViewById(R.id.details_list);

                // actiobar icon
                if (StringUtils.isNotBlank(trackable.getIconUrl())) {
                    final tbIconHandler iconHandler = new tbIconHandler(((TextView) findViewById(R.id.actionbar_title)));
                    final tbIconThread iconThread = new tbIconThread(trackable.getIconUrl(), iconHandler);
                    iconThread.start();
                }

                // trackable name
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);

                itemName.setText(res.getString(R.string.trackable_name));
                if (StringUtils.isNotBlank(trackable.getName())) {
                    itemValue.setText(Html.fromHtml(trackable.getName()).toString());
                } else {
                    itemValue.setText(res.getString(R.string.trackable_unknown));
                }
                detailsList.addView(itemLayout);

                // trackable type
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);

                String tbType = null;
                if (StringUtils.isNotBlank(trackable.getType())) {
                    tbType = Html.fromHtml(trackable.getType()).toString();
                } else {
                    tbType = res.getString(R.string.trackable_unknown);
                }
                itemName.setText(res.getString(R.string.trackable_type));
                itemValue.setText(tbType);
                detailsList.addView(itemLayout);

                // trackable geocode
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);

                itemName.setText(res.getString(R.string.trackable_code));
                itemValue.setText(trackable.getGeocode().toUpperCase());
                detailsList.addView(itemLayout);

                // trackable owner
                itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                itemName = (TextView) itemLayout.findViewById(R.id.name);
                itemValue = (TextView) itemLayout.findViewById(R.id.value);

                itemName.setText(res.getString(R.string.trackable_owner));
                if (StringUtils.isNotBlank(trackable.getOwner())) {
                    itemValue.setText(Html.fromHtml(trackable.getOwner()), TextView.BufferType.SPANNABLE);
                    itemLayout.setOnClickListener(new userActions());
                } else {
                    itemValue.setText(res.getString(R.string.trackable_unknown));
                }
                detailsList.addView(itemLayout);

                // trackable spotted
                if (StringUtils.isNotBlank(trackable.getSpottedName()) ||
                        trackable.getSpottedType() == cgTrackable.SPOTTED_UNKNOWN ||
                        trackable.getSpottedType() == cgTrackable.SPOTTED_OWNER
                ) {
                    itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                    itemName = (TextView) itemLayout.findViewById(R.id.name);
                    itemValue = (TextView) itemLayout.findViewById(R.id.value);

                    itemName.setText(res.getString(R.string.trackable_spotted));
                    String text = null;

                    if (trackable.getSpottedType() == cgTrackable.SPOTTED_CACHE) {
                        text = res.getString(R.string.trackable_spotted_in_cache) + " " + Html.fromHtml(trackable.getSpottedName()).toString();
                    } else if (trackable.getSpottedType() == cgTrackable.SPOTTED_USER) {
                        text = res.getString(R.string.trackable_spotted_at_user) + " " + Html.fromHtml(trackable.getSpottedName()).toString();
                    } else if (trackable.getSpottedType() == cgTrackable.SPOTTED_UNKNOWN) {
                        text = res.getString(R.string.trackable_spotted_unknown_location);
                    } else if (trackable.getSpottedType() == cgTrackable.SPOTTED_OWNER) {
                        text = res.getString(R.string.trackable_spotted_owner);
                    } else {
                        text = "N/A";
                    }

                    itemValue.setText(text);
                    itemLayout.setClickable(true);
                    if (cgTrackable.SPOTTED_CACHE == trackable.getSpottedType()) {
                        itemLayout.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View arg0) {
                                Intent cacheIntent = new Intent(cgeotrackable.this, cgeodetail.class);
                                cacheIntent.putExtra("guid", trackable.getSpottedGuid());
                                cacheIntent.putExtra("name", trackable.getSpottedName());
                                startActivity(cacheIntent);
                            }
                        });
                    } else if (cgTrackable.SPOTTED_USER == trackable.getSpottedType()) {
                        itemLayout.setOnClickListener(new userActions());
                        //activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?guid=" + trackable.spottedGuid)));
                    }

                    detailsList.addView(itemLayout);
                }

                // trackable origin
                if (StringUtils.isNotBlank(trackable.getOrigin())) {
                    itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                    itemName = (TextView) itemLayout.findViewById(R.id.name);
                    itemValue = (TextView) itemLayout.findViewById(R.id.value);

                    itemName.setText(res.getString(R.string.trackable_origin));
                    itemValue.setText(Html.fromHtml(trackable.getOrigin()), TextView.BufferType.SPANNABLE);
                    detailsList.addView(itemLayout);
                }

                // trackable released
                if (trackable.getReleased() != null) {
                    itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                    itemName = (TextView) itemLayout.findViewById(R.id.name);
                    itemValue = (TextView) itemLayout.findViewById(R.id.value);

                    itemName.setText(res.getString(R.string.trackable_released));
                    itemValue.setText(base.formatDate(trackable.getReleased().getTime()));
                    detailsList.addView(itemLayout);
                }

                // trackable distance
                if (trackable.getDistance() != null) {
                    itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
                    itemName = (TextView) itemLayout.findViewById(R.id.name);
                    itemValue = (TextView) itemLayout.findViewById(R.id.value);

                    itemName.setText(res.getString(R.string.trackable_distance));
                    itemValue.setText(cgBase.getHumanDistance(trackable.getDistance()));
                    detailsList.addView(itemLayout);
                }

                // trackable goal
                if (StringUtils.isNotBlank(trackable.getGoal())) {
                    ((LinearLayout) findViewById(R.id.goal_box)).setVisibility(View.VISIBLE);
                    TextView descView = (TextView) findViewById(R.id.goal);
                    descView.setVisibility(View.VISIBLE);
                    descView.setText(Html.fromHtml(trackable.getGoal(), new cgHtmlImg(cgeotrackable.this, geocode, true, 0, false), null), TextView.BufferType.SPANNABLE);
                    descView.setMovementMethod(LinkMovementMethod.getInstance());
                }

                // trackable details
                if (StringUtils.isNotBlank(trackable.getDetails())) {
                    ((LinearLayout) findViewById(R.id.details_box)).setVisibility(View.VISIBLE);
                    TextView descView = (TextView) findViewById(R.id.details);
                    descView.setVisibility(View.VISIBLE);
                    descView.setText(Html.fromHtml(trackable.getDetails(), new cgHtmlImg(cgeotrackable.this, geocode, true, 0, false), null), TextView.BufferType.SPANNABLE);
                    descView.setMovementMethod(LinkMovementMethod.getInstance());
                }

                // trackable image
                if (StringUtils.isNotBlank(trackable.getImage())) {
                    ((LinearLayout) findViewById(R.id.image_box)).setVisibility(View.VISIBLE);
                    LinearLayout imgView = (LinearLayout) findViewById(R.id.image);

                    final ImageView trackableImage = (ImageView) inflater.inflate(R.layout.trackable_image, null);

                    trackableImage.setImageResource(R.drawable.image_not_loaded);
                    trackableImage.setClickable(true);
                    trackableImage.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View arg0) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trackable.getImage())));
                        }
                    });

                    // try to load image
                    final Handler handler = new Handler() {

                        @Override
                        public void handleMessage(Message message) {
                            BitmapDrawable image = (BitmapDrawable) message.obj;
                            if (image != null) {
                                trackableImage.setImageDrawable((BitmapDrawable) message.obj);
                            }
                        }
                    };

                    new Thread() {

                        @Override
                        public void run() {
                            BitmapDrawable image = null;
                            try {
                                cgHtmlImg imgGetter = new cgHtmlImg(cgeotrackable.this, geocode, true, 0, false);

                                image = imgGetter.getDrawable(trackable.getImage());
                                Message message = handler.obtainMessage(0, image);
                                handler.sendMessage(message);
                            } catch (Exception e) {
                                Log.e(Settings.tag, "cgeospoilers.onCreate.onClick.run: " + e.toString());
                            }
                        }
                    }.start();

                    imgView.addView(trackableImage);
                }
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeotrackable.loadTrackableHandler: " + e.toString() + Arrays.toString(e.getStackTrace()));
            }

            displayLogs();

            if (waitDialog != null) {
                waitDialog.dismiss();
            }
        }
    };

    public cgeotrackable() {
        super("c:geo-trackable-details");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.trackable_detail);
        setTitle(res.getString(R.string.trackable));

        // get parameters
        Bundle extras = getIntent().getExtras();
        Uri uri = getIntent().getData();

        // try to get data from extras
        if (extras != null) {
            geocode = extras.getString("geocode");
            name = extras.getString("name");
            guid = extras.getString("guid");
            id = extras.getString("id");
        }

        // try to get data from URI
        if (geocode == null && guid == null && id == null && uri != null) {
            String uriHost = uri.getHost().toLowerCase();
            if (uriHost.contains("geocaching.com")) {
                geocode = uri.getQueryParameter("tracker");
                guid = uri.getQueryParameter("guid");
                id = uri.getQueryParameter("id");

                if (StringUtils.isNotBlank(geocode)) {
                    geocode = geocode.toUpperCase();
                    guid = null;
                    id = null;
                } else if (StringUtils.isNotBlank(guid)) {
                    geocode = null;
                    guid = guid.toLowerCase();
                    id = null;
                } else if (StringUtils.isNotBlank(id)) {
                    geocode = null;
                    guid = null;
                    id = id.toLowerCase();
                } else {
                    showToast(res.getString(R.string.err_tb_details_open));
                    finish();
                    return;
                }
            } else if (uriHost.contains("coord.info")) {
                String uriPath = uri.getPath().toLowerCase();
                if (uriPath != null && uriPath.startsWith("/tb")) {
                    geocode = uriPath.substring(1).toUpperCase();
                    guid = null;
                    id = null;
                } else {
                    showToast(res.getString(R.string.err_tb_details_open));
                    finish();
                    return;
                }
            }
        }

        // no given data
        if (geocode == null && guid == null && id == null) {
            showToast(res.getString(R.string.err_tb_display));
            finish();
            return;
        }

        if (StringUtils.isNotBlank(name)) {
            waitDialog = ProgressDialog.show(this, Html.fromHtml(name).toString(), res.getString(R.string.trackable_details_loading), true);
        } else if (StringUtils.isNotBlank(geocode)) {
            waitDialog = ProgressDialog.show(this, geocode.toUpperCase(), res.getString(R.string.trackable_details_loading), true);
        } else {
            waitDialog = ProgressDialog.show(this, res.getString(R.string.trackable), res.getString(R.string.trackable_details_loading), true);
        }
        waitDialog.setCancelable(true);

        loadTrackable thread;
        thread = new loadTrackable(loadTrackableHandler, geocode, guid, id);
        thread.start();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        final int viewId = view.getId();

        if (viewId == R.id.author) { // Log item author
            contextMenuUser = ((TextView) view).getText().toString();
        } else { // Trackable owner, and user holding trackable now
            RelativeLayout itemLayout = (RelativeLayout) view;
            TextView itemName = (TextView) itemLayout.findViewById(R.id.name);

            String selectedName = itemName.getText().toString();
            if (selectedName.equals(res.getString(R.string.trackable_owner))) {
                contextMenuUser = trackable.getOwner();
            } else if (selectedName.equals(res.getString(R.string.trackable_spotted))) {
                contextMenuUser = trackable.getSpottedName();
            }
        }

        menu.setHeaderTitle(res.getString(R.string.user_menu_title) + " " + contextMenuUser);
        menu.add(viewId, 1, 0, res.getString(R.string.user_menu_view_hidden));
        menu.add(viewId, 2, 0, res.getString(R.string.user_menu_view_found));
        menu.add(viewId, 3, 0, res.getString(R.string.user_menu_open_browser));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == 1) {
            cgeocaches.startActivityCacheOwner(this, contextMenuUser);
            return true;
        } else if (id == 2) {
            cgeocaches.startActivityCacheUser(this, contextMenuUser);
            return true;
        } else if (id == 3) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/profile/?u=" + URLEncoder.encode(contextMenuUser))));

            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, res.getString(R.string.trackable_log_touch)).setIcon(android.R.drawable.ic_menu_agenda); // log touch

        menu.add(0, 2, 0, res.getString(R.string.trackable_browser_open)).setIcon(android.R.drawable.ic_menu_info_details); // browser
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                logTouch();
                return true;
            case 2:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/track/details.aspx?tracker=" + trackable.getGeocode())));
                return true;
        }

        return false;
    }

    private class loadTrackable extends Thread {

        private Handler handler = null;
        private String geocode = null;
        private String guid = null;
        private String id = null;

        public loadTrackable(Handler handlerIn, String geocodeIn, String guidIn, String idIn) {
            handler = handlerIn;
            geocode = geocodeIn;
            guid = guidIn;
            id = idIn;

            if (geocode == null && guid == null && id == null) {
                showToast(res.getString(R.string.err_tb_forgot));

                stop();
                finish();
                return;
            }
        }

        @Override
        public void run() {
            trackable = base.searchTrackable(geocode, guid, id);
            handler.sendMessage(new Message());
        }
    }

    private void displayLogs() {
        // trackable logs
        LinearLayout listView = (LinearLayout) findViewById(R.id.log_list);
        listView.removeAllViews();

        RelativeLayout rowView;

        if (trackable != null && trackable.getLogs() != null) {
            for (cgLog log : trackable.getLogs()) {
                rowView = (RelativeLayout) inflater.inflate(R.layout.trackable_logitem, null);

                if (log.date > 0) {
                    ((TextView) rowView.findViewById(R.id.added)).setText(base.formatShortDate(log.date));
                }

                if (cgBase.logTypes1.containsKey(log.type)) {
                    ((TextView) rowView.findViewById(R.id.type)).setText(cgBase.logTypes1.get(log.type));
                } else {
                    ((TextView) rowView.findViewById(R.id.type)).setText(cgBase.logTypes1.get(4)); // note if type is unknown
                }
                ((TextView) rowView.findViewById(R.id.author)).setText(Html.fromHtml(log.author), TextView.BufferType.SPANNABLE);

                if (StringUtils.isBlank(log.cacheName)) {
                    ((TextView) rowView.findViewById(R.id.location)).setVisibility(View.GONE);
                } else {
                    ((TextView) rowView.findViewById(R.id.location)).setText(Html.fromHtml(log.cacheName));
                    final String cacheGuid = log.cacheGuid;
                    final String cacheName = log.cacheName;
                    ((TextView) rowView.findViewById(R.id.location)).setOnClickListener(new View.OnClickListener() {
                        public void onClick(View arg0) {
                            Intent cacheIntent = new Intent(cgeotrackable.this, cgeodetail.class);
                            cacheIntent.putExtra("guid", cacheGuid);
                            cacheIntent.putExtra("name", Html.fromHtml(cacheName).toString());
                            startActivity(cacheIntent);
                        }
                    });
                }

                ((TextView) rowView.findViewById(R.id.log)).setText(Html.fromHtml(log.log, new cgHtmlImg(cgeotrackable.this, null, false, 0, false), null), TextView.BufferType.SPANNABLE);

                ((TextView) rowView.findViewById(R.id.author)).setOnClickListener(new userActions());
                listView.addView(rowView);
            }

            if (trackable.getLogs().size() > 0) {
                ((LinearLayout) findViewById(R.id.log_box)).setVisibility(View.VISIBLE);
            }
        }
    }

    private class userActions implements View.OnClickListener {

        public void onClick(View view) {
            if (view == null) {
                return;
            }

            try {
                registerForContextMenu(view);
                openContextMenu(view);
            } catch (Exception e) {
                // nothing
            }
        }
    }

    private void logTouch() {
        Intent logTouchIntent = new Intent(this, cgeotouch.class);
        logTouchIntent.putExtra("geocode", trackable.getGeocode().toUpperCase());
        logTouchIntent.putExtra("guid", trackable.getGuid());
        startActivity(logTouchIntent);
    }

    private class tbIconThread extends Thread {
        String url = null;
        Handler handler = null;

        public tbIconThread(String urlIn, Handler handlerIn) {
            url = urlIn;
            handler = handlerIn;
        }

        @Override
        public void run() {
            if (url == null || handler == null) {
                return;
            }

            BitmapDrawable image = null;
            try {
                cgHtmlImg imgGetter = new cgHtmlImg(cgeotrackable.this, trackable.getGeocode(), false, 0, false);

                image = imgGetter.getDrawable(url);
                Message message = handler.obtainMessage(0, image);
                handler.sendMessage(message);
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeotrackable.tbIconThread.run: " + e.toString());
            }
        }
    }

    private static class tbIconHandler extends Handler {
        TextView view = null;

        public tbIconHandler(TextView viewIn) {
            view = viewIn;
        }

        @Override
        public void handleMessage(Message message) {
            BitmapDrawable image = (BitmapDrawable) message.obj;
            if (image != null && view != null) {
                view.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
            }
        }
    }

    public static void startActivity(final AbstractActivity fromContext,
            final String guid, final String geocode, final String name) {
        Intent trackableIntent = new Intent(fromContext, cgeotrackable.class);
        trackableIntent.putExtra("guid", guid);
        trackableIntent.putExtra("geocode", geocode);
        trackableIntent.putExtra("name", name);
        fromContext.startActivity(trackableIntent);
    }
}
