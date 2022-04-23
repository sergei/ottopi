package com.santacruzinstruments.ottopi.navengine.route;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutputListener;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Geodesy;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.LinkedList;



public class RouteManager implements NavComputerOutputListener {

	private static final double ARRIVAL_PROJ_HYSTEREZIS_FACTOR = 1.5;
	private static final float  START_POUINT_ARRIVAL_CIRCLE = 150;
	private static final float STANDARD_ARRIVAL_CIRCLE = 30;

	public enum ArrivalType {CIRCLE_ENTERED, PERPENDICULAR_PASSED }

	public interface RouteManagerListener{
		void onNewActivePoint(UtcTime utc, int rptIdx, ArrivalType arrivalType );
	}
	
	private final LinkedList<RouteManagerListener> listeners;
	private final LineSegment currentSegment;
	private double minDistance;
	private boolean currentSegmentValid;
	
	private Route route;
	private Geodesy mGeodesy;
	private boolean lastPointReported;
	
	public RouteManager(){
		listeners = new LinkedList<>();
		currentSegment = new LineSegment();
		currentSegmentValid = false;
		lastPointReported = false;
	}
	
	public void addRouteManagerListener( RouteManagerListener l ){
		listeners.add(l);
	}

	public void removeRouteManagerListener( RouteManagerListener l ){
		listeners.remove(l);
	}
	
	public void setRoute(Route route) {
		this.route = route; 
		this.currentSegmentValid = false;
	}

	public RoutePoint getActivePoint() {
		if ( route == null )
			return null;
		else
			return route.getActivePoint();
	}

	@Override
	public void onNavComputerOutput(NavComputerOutput nout) {
		if ( route == null ){
			return;
		}
		
		if ( ! nout.ii.loc.isValid() ){
			return;
		}
		
		mGeodesy = Geodesy.geodesyFactory(nout.ii.loc);
		
		RoutePoint activePoint = getActivePoint();

		if ( activePoint != null && ! lastPointReported ){

			if ( ! this.currentSegmentValid ){
				setCurrentSegment( nout.ii.loc, activePoint.loc );
				this.currentSegmentValid = true;
			}

			Distance dist = mGeodesy.dist(nout.ii.loc, activePoint.loc);


			float arrivalCircleRadius = STANDARD_ARRIVAL_CIRCLE;
			
			if ( activePoint.type == RoutePoint.Type.START_STBD)
				arrivalCircleRadius = START_POUINT_ARRIVAL_CIRCLE;
				
			if ( dist.toMeters() < arrivalCircleRadius ){ // Check for arrival by entering the radius 
				advanceActivePoint(nout.ii.utc, ArrivalType.CIRCLE_ENTERED);
			}else{ // Check for arrival by crossing the perpendicular 
				Coordinate p = mGeodesy.toCoordinate(nout.ii.loc);
				// The Projection Factor is the constant r by which the vector for this segment must be 
				// multiplied to equal the vector for the projection of p on the line defined by this segment.
				double r =  currentSegment.projectionFactor ( p );
				
				if ( r >= 1. ){
					advanceActivePoint(nout.ii.utc, ArrivalType.PERPENDICULAR_PASSED);
				}else if ( r > 0. ){  // Check for getting close enough and then sailing away
					
					// Compute how close we are on projected distance to wpt
					if ( dist.toMeters() < minDistance )
						minDistance = dist.toMeters(); 
					
					// If we moved back ( with some hysteresis ) let's toggle 
					if ( (minDistance < 200) && (dist.toMeters() > minDistance * ARRIVAL_PROJ_HYSTEREZIS_FACTOR) ){
						advanceActivePoint(nout.ii.utc, ArrivalType.PERPENDICULAR_PASSED);
					}
				}
			}
		}
	}

	private void advanceActivePoint(UtcTime utc, ArrivalType arrivalType) {

		if ( !route.lastPointIsActive() ){
			setCurrentSegment( route.getActivePoint().loc, route.getAfterActivePoint().loc );
			route.advanceActivePoint();
		}else{
			lastPointReported = true;
		}
		
		for ( RouteManagerListener l :listeners ){
			l.onNewActivePoint(utc, route.getActiveWptIdx(), arrivalType);
		}

	}

	private void setCurrentSegment(GeoLoc from, GeoLoc to) {
		Coordinate p0 = mGeodesy.toCoordinate(from);
		Coordinate p1 = mGeodesy.toCoordinate(to);
		currentSegment.setCoordinates(p0, p1);
		minDistance = currentSegment.getLength();
	}

}
