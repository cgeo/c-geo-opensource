package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.settings.Settings;

import java.net.MalformedURLException;
import java.net.URL;


import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

/**
 * Like {@link org.mapsforge.map.layer.download.tilesource.OpenCycleMap} but loading tiles from thunderforest servers with apikey.
 */
public class ThunderforestMap extends AbstractTileSource {

    private final String apiKey;

    public ThunderforestMap(final String[] hostNames, final int port) {
        super(hostNames, port);
        apiKey = Settings.getThunderForestApiKey();
    }

    public ThunderforestMap() {
        this(new String[]{"a.tile.thunderforest.com", "b.tile.thunderforest.com", "c.tile.thunderforest.com"}, 443);
    }

    @Override
    public int getParallelRequestsLimit() {
        return 8;
    }

    @Override
    public URL getTileUrl(final Tile tile) throws MalformedURLException {
        final StringBuilder builder = new StringBuilder("/cycle/");
        builder.append(tile.zoomLevel).append('/').append(tile.tileX).append('/').append(tile.tileY).append(".png");
        if (StringUtils.isNotBlank(apiKey)) {
            builder.append("?apikey=").append(apiKey);
        }
        return new URL("https", this.getHostName(), this.port, builder.toString());
    }

    @Override
    public byte getZoomLevelMax() {
        return 22;
    }

    @Override
    public byte getZoomLevelMin() {
        return 0;
    }

    @Override
    public boolean hasAlpha() {
        return false;
    }
}
