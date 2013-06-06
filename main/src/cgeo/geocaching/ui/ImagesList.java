package cgeo.geocaching.ui;

import cgeo.geocaching.Image;
import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.IOUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ImagesList {

    private BitmapDrawable currentDrawable;
    private Image currentImage;

    public enum ImageType {
        LogImages(R.string.cache_log_images_title),
        SpoilerImages(R.string.cache_spoiler_images_title),
        AllImages(R.string.cache_images_title);

        private final int titleResId;

        ImageType(final int title) {
            this.titleResId = title;
        }

        public int getTitle() {
            return titleResId;
        }
    }

    private LayoutInflater inflater = null;
    private final Activity activity;
    // We could use a Set here, but we will insert no duplicates, so there is no need to check for uniqueness.
    private final Collection<Bitmap> bitmaps = new LinkedList<Bitmap>();
    /**
     * map image view id to image
     */
    private final SparseArray<Image> images = new SparseArray<Image>();
    private final String geocode;
    private LinearLayout imagesView;

    public ImagesList(final Activity activity, final String geocode) {
        this.activity = activity;
        this.geocode = geocode;
        inflater = activity.getLayoutInflater();
    }

    public void loadImages(final View parentView, final List<Image> images, final boolean offline) {

        imagesView = (LinearLayout) parentView.findViewById(R.id.spoiler_list);

        for (final Image img : images) {
            LinearLayout rowView = (LinearLayout) inflater.inflate(R.layout.cache_image_item, null);

            if (StringUtils.isNotBlank(img.getTitle())) {
                ((TextView) rowView.findViewById(R.id.title)).setText(Html.fromHtml(img.getTitle()));
                rowView.findViewById(R.id.titleLayout).setVisibility(View.VISIBLE);
            }

            if (StringUtils.isNotBlank(img.getDescription())) {
                final TextView descView = (TextView) rowView.findViewById(R.id.description);
                descView.setText(Html.fromHtml(img.getDescription()), TextView.BufferType.SPANNABLE);
                descView.setVisibility(View.VISIBLE);
            }

            new AsyncImgLoader(rowView, img, offline).execute();
            imagesView.addView(rowView);
        }
    }

    private class AsyncImgLoader extends AsyncTask<Void, Void, BitmapDrawable> {

        final private LinearLayout view;
        final private Image img;
        final boolean offline;

        public AsyncImgLoader(final LinearLayout view, final Image img, final boolean offline) {
            this.view = view;
            this.img = img;
            this.offline = offline;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params) {
            final HtmlImage imgGetter = new HtmlImage(geocode, true, offline ? StoredList.STANDARD_LIST_ID : StoredList.TEMPORARY_LIST_ID, false);
            return imgGetter.getDrawable(img.getUrl());
        }

        @Override
        protected void onPostExecute(final BitmapDrawable image) {
            if (image != null) {
                bitmaps.add(image.getBitmap());
                final ImageView imageView = (ImageView) inflater.inflate(R.layout.image_item, null);

                final Rect bounds = image.getBounds();

                imageView.setImageResource(R.drawable.image_not_loaded);
                imageView.setClickable(true);
                imageView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        viewImageInStandardApp(image);
                    }
                });
                activity.registerForContextMenu(imageView);
                imageView.setImageDrawable(image);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new LayoutParams(bounds.width(), bounds.height()));

                view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
                view.addView(imageView);

                imageView.setId(image.hashCode());
                images.put(imageView.getId(), img);
            }
        }
    }

    public void removeAllViews() {
        imagesView.removeAllViews();
        for (final Bitmap b : bitmaps) {
            b.recycle();
        }
        bitmaps.clear();
    }

    public void onCreateContextMenu(ContextMenu menu, View v) {
        activity.getMenuInflater().inflate(R.menu.images_list_context, menu);
        final Resources res = activity.getResources();
        menu.setHeaderTitle(res.getString(R.string.cache_image));
        final ImageView view = (ImageView) v;
        currentDrawable = (BitmapDrawable) view.getDrawable();
        currentImage = images.get(view.getId());
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.image_open_file:
                viewImageInStandardApp(currentDrawable);
                return true;
            case R.id.image_open_browser:
                if (currentImage != null) {
                    currentImage.openInBrowser(activity);
                }
                return true;
            default:
                return false;
        }
    }

    private void viewImageInStandardApp(final BitmapDrawable image) {
        final File file = LocalStorage.getStorageFile(null, "temp.jpg", false, true);
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(file));
            image.getBitmap().compress(CompressFormat.JPEG, 100, stream);
        } catch (Exception e) {
            Log.e("ImagesList.viewImageInStandardApp", e);
            return;
        } finally {
            IOUtils.closeQuietly(stream);
        }

        final Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
        activity.startActivity(intent);

        if (file.exists()) {
            file.deleteOnExit();
        }
    }

}
