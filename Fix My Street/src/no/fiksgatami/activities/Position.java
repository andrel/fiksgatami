package no.fiksgatami.activities;

import android.app.Activity;
import android.os.Bundle;
import no.fiksgatami.R;
import org.metalev.multitouch.controller.MultiTouchController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

/**
 *
 */
public class Position extends Base {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.position);

        MapView map = (MapView) findViewById(R.id.osm_map);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        MapController mapController = map.getController();
        mapController.setZoom(23);
        GeoPoint gPt = new GeoPoint(51500000, -150000);
        mapController.setCenter(gPt);
    }
}
