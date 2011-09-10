package cgeo.geocaching;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import cgeo.geocaching.geopoint.Geopoint;

public class cgDistanceView extends TextView {
	private cgBase base = null;
	private Geopoint cacheCoords = null;

	public cgDistanceView(Context context) {
		super(context);
	}

	public cgDistanceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public cgDistanceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setContent(cgBase baseIn, final Geopoint cacheCoordsIn) {
		base = baseIn;
		cacheCoords = cacheCoordsIn;
	}

	public void update(final Geopoint coords) {
		if (cacheCoords == null) return;
		if (coords == null) return;
		if (base == null) return;

		setText(base.getHumanDistance(cgBase.getDistance(coords, cacheCoords)));
	}

	public void setDistance(Double distance) {
		setText("~" + base.getHumanDistance(distance));
	}

	public void clear() {
		setText(null);
	}
}