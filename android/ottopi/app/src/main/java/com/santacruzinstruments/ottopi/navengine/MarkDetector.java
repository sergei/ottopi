package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Geodesy;
import com.santacruzinstruments.ottopi.navengine.route.Route;

import timber.log.Timber;

/**
 * Mark detector. Detects marks on "sausage" course by selecting most distant points from
 * start or the previous mark.
 */
public class MarkDetector {

    public interface MarkDetectorListener {
        void omMarkDetected(GeoLoc loc);
    }

    private static final int MIN_MAX_DIST_METERS = 500;  // The leg must be at least that long
    private static final double DIST_FACTOR = 0.5;       // Should get back 50% of distance before mark will be detected

    private final MarkDetectorListener markDetectorListener;
    private GeoLoc mostRecentValidLoc = GeoLoc.INVALID;
    private GeoLoc startLoc = GeoLoc.INVALID;
    private GeoLoc refPoint = GeoLoc.INVALID;
    private GeoLoc maxPoint = GeoLoc.INVALID;
    private double maxDist = 0;
    private boolean isRunning = false;

    public MarkDetector(MarkDetectorListener markDetectorListener){
        this.markDetectorListener = markDetectorListener;
    }

    public void setStartLine(Route route){
        if ( route.hasStartLine()){
            GeoLoc pin = route.getRpt(0).loc;
            GeoLoc rcb = route.getRpt(1).loc;
            startLoc = new GeoLoc((pin.lat + rcb.lat)/2, (pin.lon + rcb.lon)/2);
            Timber.d("setStartLine: startLoc=%s", startLoc);
        }
    }

    public void start() {
        if ( ! startLoc.isValid() )
            startLoc = mostRecentValidLoc;
        maxDist = 0;
        refPoint = startLoc;
        isRunning = true;
        Timber.d("start: refPoint=%s", startLoc);
    }

    public void stop() {
        isRunning = false;
    }

    public void onNavData(NavComputerOutput out){
        if (isRunning && out.ii.loc.isValid() && refPoint.isValid() ) {
            mostRecentValidLoc = out.ii.loc;

            // Compute distance from reference point to the current point
            double currDist = refPoint.distTo(out.ii.loc).toMeters();

            // Update maximum distance from the reference point
            if ( currDist  > maxDist){
                maxDist = currDist;
                maxPoint = out.ii.loc;
            }

            Timber.d("onNavData: ref=%s -> cur=%s currDist = %f, maxDist = %f maxPoint=%s",
                    refPoint, out.ii.loc, currDist, maxDist, maxPoint);

            if ( maxDist > MIN_MAX_DIST_METERS){
                // If the distance to the maximum pint is X times bigger than distance to the current point
                // the point where the maximum distance was achieved is the mark
                if ( currDist < maxDist * DIST_FACTOR){
                    markDetectorListener.omMarkDetected(maxPoint);
                    refPoint = maxPoint;
                    maxDist = 0;
                    Timber.d("onNavData: omMarkDetected ref=%s", refPoint);
                }
            }
        }
    }

}
