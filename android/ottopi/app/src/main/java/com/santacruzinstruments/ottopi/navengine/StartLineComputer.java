package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;


public class StartLineComputer {

    private LineSegment startLine;
    private Direction startLineNormal = new Direction();
    private final StartLineInfo startLineInfo = new StartLineInfo();

    public StartLineInfo getStartLineInfo(){
        return startLineInfo;
    }

    public StartLineInfo setRoute(Route route){
        startLineInfo.pin = new GeoLoc();
        startLineInfo.rcb = new GeoLoc();
        startLineInfo.distToLine = new Distance();
        startLineInfo.pinFavoredBy = new Angle();
        startLineNormal = new Direction();
        for(RoutePoint rpt : route) {
            if( rpt.type == RoutePoint.Type.START && rpt.leaveTo == RoutePoint.LeaveTo.PORT)
                startLineInfo.pin = rpt.loc;
            if( rpt.type == RoutePoint.Type.START && rpt.leaveTo == RoutePoint.LeaveTo.STARBOARD)
                startLineInfo.rcb = rpt.loc;
        }

        if( startLineInfo.pin.isValid() && startLineInfo.rcb.isValid()) {
            startLine = new LineSegment(startLineInfo.pin.toCoordinate(),
                    startLineInfo.rcb.toCoordinate());
            Direction startLineDir = startLineInfo.rcb.bearingTo(startLineInfo.pin);
            startLineNormal = startLineDir.addAngleDeg( 90.);
        }

        return startLineInfo;
    }

    public StartLineInfo updateStartLineInfo(GeoLoc loc, Direction twd, boolean computeFavoredEnd){

        if( loc.isValid() && startLineInfo.pin.isValid() && startLineInfo.rcb.isValid() ){
            Coordinate boat = loc.toCoordinate();
            Coordinate pin = startLineInfo.pin.toCoordinate();
            Coordinate rcb = startLineInfo.rcb.toCoordinate();
            double distMeters = org.locationtech.jts.algorithm.Distance.pointToLinePerpendicular(boat, pin, rcb);
            startLineInfo.distToLine = new Distance(distMeters / 1852.);
        }else{
            startLineInfo.distToLine = Distance.INVALID;
        }

        if ( twd.isValid() ) {

            if ( computeFavoredEnd && startLineNormal.isValid() ){
                startLineInfo.pinFavoredBy = Direction.angleBetween(twd, startLineNormal);
            } else {
                startLineInfo.pinFavoredBy = Angle.INVALID;
            }

            // Check if we are OCS
            if(loc.isValid()){
                Coordinate boat = loc.toCoordinate();
                // Find projec tion of the boat to the start line
                Coordinate proj = startLine.project(boat);
                // Segment between the projection and the boat
                LineSegment norm = new LineSegment(proj, boat);
                // Find angle from the projection to the boat
                double normAngle = norm.angle();
                double windAngle = Math.PI/2 - twd.toRadians();
                double angleToWind = normAngle - windAngle;
                if( angleToWind > Math.PI)
                    angleToWind -= 2*Math.PI;
                else if (angleToWind < -Math.PI)
                    angleToWind += 2*Math.PI;
                // We are OCS if the boat is upwind of the start line
                startLineInfo.isOcs = (angleToWind >= -Math.PI / 2) && (angleToWind <= Math.PI / 2);
            }else{
                startLineInfo.isOcs = false;
            }
        }else{
            startLineInfo.pinFavoredBy = Angle.INVALID;
            startLineInfo.isOcs = false;
        }

        return startLineInfo;
    }

}
