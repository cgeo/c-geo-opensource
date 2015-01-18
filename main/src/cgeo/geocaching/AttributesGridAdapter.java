package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheAttribute;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

public class AttributesGridAdapter extends BaseAdapter {
    private final Context context;
    private final Resources resources;
    private final List<String> attributes;
    private final LayoutInflater inflater;

    public AttributesGridAdapter(final Context context, final Geocache cache) {
        this.context = context;
        resources = context.getResources();
        attributes = cache.getAttributes();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return attributes.size();
    }

    @Override
    public Object getItem(final int position) {
        return attributes.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return 0;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final FrameLayout attributeLayout;
        if (convertView == null) {
            attributeLayout = (FrameLayout) inflater.inflate(R.layout.attribute_image, parent, false);
        } else {
            attributeLayout = (FrameLayout) convertView;
        }

        drawAttribute(attributeLayout, attributes.get(position));
        return attributeLayout;
    }

    private void drawAttribute(final FrameLayout attributeLayout, final String attributeName) {
        final ImageView imageView = (ImageView) attributeLayout.getChildAt(0);

        final boolean strikeThrough = !CacheAttribute.isEnabled(attributeName);
        final CacheAttribute attrib = CacheAttribute.getByRawName(CacheAttribute.trimAttributeName(attributeName));
        if (attrib != null) {
            Drawable drawable = resources.getDrawable(attrib.drawableId);
            imageView.setImageDrawable(drawable);
            if (strikeThrough) {
                // generate strike through image with same properties as attribute image
                final ImageView strikeThroughImage = new ImageView(context);
                strikeThroughImage.setLayoutParams(imageView.getLayoutParams());
                drawable = resources.getDrawable(R.drawable.attribute__strikethru);
                strikeThroughImage.setImageDrawable(drawable);
                attributeLayout.addView(strikeThroughImage);
            }
        } else {
            imageView.setImageDrawable(resources.getDrawable(R.drawable.attribute_unknown));
        }
    }

}