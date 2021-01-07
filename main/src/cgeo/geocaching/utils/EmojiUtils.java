package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.extension.EmojiLRU;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EmojiUtils {

    // list of emojis supported by the EmojiPopup
    // should be supported by the Android API level we have set as minimum (currently API 21 = Android 5)
    // for a list of supported Unicode standards by API level see https://developer.android.com/guide/topics/resources/internationalization
    // for characters by Unicode version see https://unicode.org/emoji/charts-5.0/full-emoji-list.html (v5.0)

    private static final EmojiSet[] symbols = {
        // category symbols
        new EmojiSet(0x2764, new int[]{
            /* hearts */        0x2764, 0x1f499, 0x1f49a, 0x1f49b, 0x1f49c, 0x1f9e1, 0x1f49c, 0x1f5a4,
            /* events */        0x1f383, 0x1f380,
            /* office */        0x1f4c6,
            /* warning */       0x26d4, 0x1f6d1, 0x2622,
            /* av-symbol */     0x1f506,
            /* other-symbol */  0x2b55, 0x2714, 0x2716, 0x274c, 0x203c, 0x2049, 0x2753, 0x2757,
            /* geometric */     0x1f536, 0x1f537, 0x26aa, 0x26ab, 0x1f534, 0x1f535,
            /* flags */         0x1f3c1, 0x1f6a9, 0x1f3f4, 0x1f3f3
        }),
        // category places
        new EmojiSet(0x1f5fa, new int[]{
            /* globe */         0x1f30d, 0x1f30e, 0x1f30f,
            /* geographic */    0x1f3d4, 0x1f3d6, 0x1f3dc, 0x1f3dd, 0x1f3de,
            /* buildings */     0x1f3e0, 0x1f3e2, 0x1f3da, 0x1f3e5, 0x1f3f0, 0x16cf,
            /* other */         0x26f2, 0x2668, 0x1f3ad, 0x1f3a8,
            /* plants */        0x1f332, 0x1f333, 0x1f334, 0x1f335, 0x1f340,
            /* transport */     0x1f682, 0x1f68d, 0x1f695, 0x1f6b2, 0x1f697, 0x2693, 0x26f5, 0x2708, 0x1f680,
            /* transp.-sign */  0x267f, 0x1f6bb
        }),
        // category food
        new EmojiSet(0x1f968, new int[]{
            /* fruits */        0x1f34a, 0x1f34b, 0x1f34d, 0x1f34e, 0x1f34f, 0x1f95d, 0x1f336, 0x1f344,
            /* other */         0x1f968, 0x1f354, 0x1f355,
            /* drink */         0x1f964, 0x2615, 0x1f37a
        }),
        // category activity
        new EmojiSet(0x1f3c3, new int[]{
            /* person-sport */  0x26f7, 0x1f3c4, 0x1f6a3, 0x1f3ca, 0x1f6b4

        }),
        // category people
        new EmojiSet(0x1f600, new int[]{
            /* smileys */       0x1f600, 0x1f60d, 0x1f641, 0x1f621, 0x1f47b,
            /* people */        0x1f466, 0x1f467, 0x1f468, 0x1f469, 0x1f474, 0x1f475
        }),

    };

    private static class EmojiSet {
        public int tabSymbol;
        public int[] symbols;

        EmojiSet(final int tabSymbol, final int[] symbols) {
            this.tabSymbol = tabSymbol;
            this.symbols = symbols;
        }
    }

    private EmojiUtils() {
        // utility class
    }

    public static void selectEmojiPopup(final Activity activity, final int currentValue, @DrawableRes final int defaultRes, final Action1<Integer> setNewCacheIcon) {

        final EmojiViewAdapter groupsAdapter;
        final EmojiViewAdapter gridAdapter;
        final EmojiViewAdapter lruAdapter;

        // calc sizes
        final Pair<Integer, Integer> markerDimensions = DisplayUtils.getDrawableDimensions(activity.getResources(), R.drawable.ic_menu_filter);
        final int markerAvailable = (int) (markerDimensions.second * 0.6);
        final int markerFontsize = DisplayUtils.calculateMaxFontsize(35, 10, 150, markerAvailable);

        final View dialogView = activity.getLayoutInflater().inflate(R.layout.emojiselector, null);
        final View customTitle = activity.getLayoutInflater().inflate(R.layout.dialog_title_button_button, null);
        final AlertDialog dialog = Dialogs.newBuilder(activity)
            .setView(dialogView)
            .setCustomTitle(customTitle)
            .create();

        final int maxCols = DisplayUtils.calculateNoOfColumns(activity, 60);
        final RecyclerView emojiGridView = dialogView.findViewById(R.id.emoji_grid);
        emojiGridView.setLayoutManager(new GridLayoutManager(activity, maxCols));
        gridAdapter = new EmojiViewAdapter(activity, symbols[0].symbols, currentValue, false, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        emojiGridView.setAdapter(gridAdapter);

        final RecyclerView emojiGroupView = dialogView.findViewById(R.id.emoji_groups);
        emojiGroupView.setLayoutManager(new GridLayoutManager(activity, symbols.length));
        final int[] emojiGroups = new int[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            emojiGroups[i] = symbols[i].tabSymbol;
        }
        groupsAdapter = new EmojiViewAdapter(activity, emojiGroups, symbols[0].tabSymbol, true, newgroup -> {
            for (EmojiSet symbol : symbols) {
                if (symbol.tabSymbol == newgroup) {
                    gridAdapter.setData(symbol.symbols);
                }
            }
        });
        emojiGroupView.setAdapter(groupsAdapter);

        final RecyclerView emojiLruView = dialogView.findViewById(R.id.emoji_lru);
        emojiLruView.setLayoutManager(new GridLayoutManager(activity, maxCols));
        final int[] lru = EmojiLRU.getLRU();
        lruAdapter = new EmojiViewAdapter(activity, lru, 0, false, newCacheIcon -> onItemSelected(dialog, setNewCacheIcon, newCacheIcon));
        emojiLruView.setAdapter(lruAdapter);

        ((TextView) customTitle.findViewById(R.id.dialog_title_title)).setText(R.string.cache_menu_set_cache_icon);

        // right button displays current value; clicking simply closes the dialog
        final ImageButton button2 = customTitle.findViewById(R.id.dialog_button2);
        button2.setVisibility(View.VISIBLE);
        if (currentValue == -1) {
            button2.setImageResource(R.drawable.ic_menu_mark);
        } else if (currentValue != 0) {
            button2.setImageDrawable(getEmojiDrawable(activity.getResources(), markerDimensions, markerAvailable, markerFontsize, currentValue));
        } else if (defaultRes != 0) {
            button2.setImageResource(defaultRes);
        }
        button2.setOnClickListener(v -> dialog.dismiss());

        // left button displays default value (if different from current value)
        if (currentValue != 0 && defaultRes != 0) {
            final ImageButton button1 = customTitle.findViewById(R.id.dialog_button1);
            button1.setVisibility(View.VISIBLE);
            button1.setImageResource(defaultRes);
            button1.setOnClickListener(v -> {
                setNewCacheIcon.call(0);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private static void onItemSelected(final AlertDialog dialog, final Action1<Integer> callback, final int selectedValue) {
        dialog.dismiss();
        EmojiLRU.add(selectedValue);
        callback.call(selectedValue);
    }

    private static class EmojiViewAdapter extends RecyclerView.Adapter<EmojiViewAdapter.ViewHolder> {

        private int[] data;
        private final LayoutInflater inflater;
        private final Action1<Integer> callback;
        private int currentValue = 0;
        private final boolean highlightCurrent;

        EmojiViewAdapter(final Context context, final int[] data, final int currentValue, final boolean hightlightCurrent, final Action1<Integer> callback) {
            this.inflater = LayoutInflater.from(context);
            this.setData(data);
            this.currentValue = currentValue;
            this.highlightCurrent = hightlightCurrent;
            this.callback = callback;
        }

        public void setData(final int[] data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        @NonNull
        public EmojiViewAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = inflater.inflate(R.layout.emojiselector_item, parent, false);
            return new EmojiViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final EmojiViewAdapter.ViewHolder holder, final int position) {
            holder.tv.setText(new String(Character.toChars(data[position])));
            if (highlightCurrent) {
                holder.sep.setVisibility(currentValue == data[position] ? View.VISIBLE : View.INVISIBLE);
            }
            holder.itemView.setOnClickListener(v -> {
                currentValue = data[position];
                callback.call(currentValue);
                if (highlightCurrent) {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            protected TextView tv;
            protected View sep;

            ViewHolder(final View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.info_text);
                sep = itemView.findViewById(R.id.separator);
            }
        }

    }

    /**
     * builds a drawable the size of a marker with a given text
     * @param res - the resources to use
     * @param bitmapDimensions - actual size (width/height) of the bitmap to place the text in
     * @param availableSize - available size
     * @param fontsize - fontsize to use
     * @param emoji codepoint of the emoji to display
     * @return drawable bitmap with text on it
     */
    public static BitmapDrawable getEmojiDrawable(final Resources res, final Pair<Integer, Integer> bitmapDimensions, final int availableSize, final int fontsize, final int emoji) {
        final String text = new String(Character.toChars(emoji));
        final TextPaint tPaint = new TextPaint();
        tPaint.setTextSize(fontsize);
        final Bitmap bm = Bitmap.createBitmap(bitmapDimensions.first, bitmapDimensions.second, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bm);
        final StaticLayout lsLayout = new StaticLayout(text, tPaint, availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        final int deltaTopLeft = (int) (0.4 * (bitmapDimensions.first - availableSize));
        canvas.translate(deltaTopLeft, deltaTopLeft + (int) ((availableSize - lsLayout.getHeight()) / 2));
        lsLayout.draw(canvas);
        canvas.save();
        canvas.restore();
        return new BitmapDrawable(res, bm);
    }
}
