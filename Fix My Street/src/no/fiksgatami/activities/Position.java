package no.fiksgatami.activities;

import android.os.Bundle;
import android.util.Log;
import no.fiksgatami.R;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;

/**
 *
 * TODO andlin: Document required params in extras (lat/long).
 */
public class Position extends Base {

    private static final String LOG_TAG = "Position";

    private Bundle extras;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.position);

        extras = getIntent().getExtras();

        MapView map = (MapView) findViewById(R.id.position_osm_map);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        MapController mapController = map.getController();
        // TODO andlin: Set zoom factor based on network/gps accuracy?
        mapController.setZoom(23);
        GeoPoint gPt;
        if (havePosition()) {
            gPt = new GeoPoint(latitude(), longitude());
        } else {
            Log.w(LOG_TAG, "Activity opened without position");
            gPt = new GeoPoint(51500000, -150000);
        }
        mapController.setCenter(gPt);
    }

    private boolean havePosition() {
        return latitude()!= null && longitude() != null;
    }

    private Double longitude() {
        return (Double) extras.get("long");
    }

    private Double latitude() {
        return (Double) extras.get("lat");
    }
}
