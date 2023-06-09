package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

public class LegComputer {
    /** Destination name */
    public String destName = "";
    /** Angle to destination */
    public Angle atm = Angle.INVALID;
    /** Distance to destination */
    public Distance dtm = Distance.INVALID;
    /** Name of second mark */
    public String nextDestName = "";
    /**
     * TWA from current mark to the next mark
     */
    public Angle nextLegTwa = Angle.INVALID;

    RoutePoint dest = RoutePoint.INVALID;
    RoutePoint nextDest = RoutePoint.INVALID;

    public void setDestinations(RoutePoint dest, RoutePoint nextDest){
        this.dest = dest;
        this.nextDest  = nextDest;
    }

    void update(GeoLoc loc, Direction hdg, Direction twd){
        atm = Angle.INVALID;
        dtm = Distance.INVALID;
        nextLegTwa = Angle.INVALID;

        if ( loc.isValid() ) {
            if ( dest.loc.isValid()){

                destName = dest.name;
                Direction btm = loc.bearingTo(dest.loc);
                atm = Direction.angleBetween(hdg, btm);
                dtm = loc.distTo(dest.loc);

                if( nextDest.loc.isValid() && twd.isValid() ){
                    nextDestName = nextDest.name;
                    Direction legDir = dest.loc.bearingTo(nextDest.loc);
                    nextLegTwa = Direction.angleBetween(legDir, twd);
                }

            }
        }
    }

}
