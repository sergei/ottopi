package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Geodesy;
import com.santacruzinstruments.ottopi.navengine.route.Route;

/**
 * Mark detector. Detects marks on "sausage" course by selecting most distant points from
 * start or the previous mark.
 */
public class MarkDetector {

    public interface MarkDetectorListener {
        void omMarkDetected(int markIdx, GeoLoc loc);
    }

    private static final int MIN_MAX_DIST_METERS = 1000;  // The leg must be at least that long
    private static final double DIST_FACTOR = 0.3;        // Should get back 70% of distance before mark will be detected

    private final MarkDetectorListener markDetectorListener;
    private Route route;
    private GeoLoc mostRecentValidLoc = GeoLoc.INVALID;
    private GeoLoc startLoc = GeoLoc.INVALID;
    private GeoLoc refPoint = GeoLoc.INVALID;
    private GeoLoc maxPoint = GeoLoc.INVALID;
    private double maxDist = 0;
    private Geodesy mGeodesy;

    public MarkDetector(MarkDetectorListener markDetectorListener){
        this.markDetectorListener = markDetectorListener;
    }

    public void setRoute(Route route){
        this.route = route;
        if ( this.route.hasStartLine()){
            GeoLoc pin = this.route.getRpt(0).loc;
            GeoLoc rcb = this.route.getRpt(1).loc;
            startLoc = new GeoLoc((pin.lat + rcb.lat)/2, (pin.lon + rcb.lon)/2);
        }
    }

    public void start() {
        if ( ! startLoc.isValid() )
            startLoc = mostRecentValidLoc;
        maxDist = 0;
        refPoint = startLoc;
        mGeodesy = Geodesy.geodesyFactory(refPoint);
    }

    public void onNavData(NavComputerOutput out){
        if ( out.ii.loc.isValid()) {
            mostRecentValidLoc = out.ii.loc;

            // Compute distance from reference point to the current point
            double currDist = mGeodesy.dist(refPoint, out.ii.loc).toMeters();

            // Update maximum distance from the reference point
            if ( currDist  > maxDist){
                maxDist = currDist;
                maxPoint = out.ii.loc;
            }

            if ( maxDist > MIN_MAX_DIST_METERS){
                // If the distance to the maximum pint is X times bigger than distance to the current point
                // the point where the maximum distance was achieved is the mark
                if ( currDist < maxDist * DIST_FACTOR){
                    markDetectorListener.omMarkDetected(route.getActiveWptIdx(), maxPoint);
                    refPoint = maxPoint;
                    maxDist = 0;
                }
            }

        }
    }

}
