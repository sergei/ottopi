package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.algorithm.distance.PointPairDistance;


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

    public StartLineInfo updateStartLineInfo(GeoLoc loc, Direction twd){

        if( loc.isValid() && startLineInfo.pin.isValid() && startLineInfo.rcb.isValid() ){
            Coordinate pt = loc.toCoordinate();
            PointPairDistance pointPairDistance = new PointPairDistance();
            DistanceToPoint.computeDistance(startLine, pt, pointPairDistance);
            startLineInfo.distToLine = new Distance(pointPairDistance.getDistance() / 1852.);
        }else{
            startLineInfo.distToLine = Distance.INVALID;
        }

        if ( twd.isValid() && startLineNormal.isValid()) {
            startLineInfo.pinFavoredBy = Direction.angleBetween(twd, startLineNormal);

            // Check if we are OCS
            if(loc.isValid()){
                Direction boatDir = loc.bearingTo(startLineInfo.rcb);
                Angle a = Direction.angleBetween(twd, boatDir);
                // If absolute angle less than 90, then we are downwind of RCB
                startLineInfo.isOcs = Math.abs(a.toDegrees()) > 90;
            }
        }else{
            startLineInfo.pinFavoredBy = Angle.INVALID;
        }

        return startLineInfo;
    }

}
