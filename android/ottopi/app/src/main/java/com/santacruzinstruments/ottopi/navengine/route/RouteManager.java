package com.santacruzinstruments.ottopi.navengine.route;
import com.santacruzinstruments.ottopi.navengine.MarkDetector;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutput;
import com.santacruzinstruments.ottopi.navengine.NavComputerOutputListener;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Geodesy;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public class RouteManager implements NavComputerOutputListener {

	private static final double ARRIVAL_PROJ_HYSTEREZIS_FACTOR = 1.5;
	private static final float  START_POUINT_ARRIVAL_CIRCLE = 150;
	private static final float STANDARD_ARRIVAL_CIRCLE = 30;
	private static final int MIN_SEGMENT_LENGTH_M = 1000;

	public enum ArrivalType {CIRCLE_ENTERED, PERPENDICULAR_PASSED, AUTO_MARK_DETECTED }

	public interface RouteManagerListener{
		void onNewActivePoint(UtcTime utc, int rptIdx, ArrivalType arrivalType );
		void onMarkLocationDetermined(List<RoutePoint> updatedRpts);
	}
	
	private final LinkedList<RouteManagerListener> listeners;
	private final LineSegment currentSegment;
	private double minDistance;
	private boolean currentSegmentValid;
	
	private Route route;
	private boolean lastPointReported;
	private final MarkDetector markDetector;
	private UtcTime lastUtc = UtcTime.INVALID;
	
	public RouteManager() {
		listeners = new LinkedList<>();
		currentSegment = new LineSegment();
		currentSegmentValid = false;
		lastPointReported = false;
		markDetector = new MarkDetector(loc -> { // New mark has been detected

			final RoutePoint activePoint = route.getActivePoint();
			if ( !activePoint.loc.isValid() ) {
				LinkedList<RoutePoint> updatedPts = new LinkedList<>();

				for( int i = 0; i < route.getRptsNum(); i++) {
					RoutePoint p = route.getRpt(i);
					if (Objects.equals(p.name, activePoint.name)) {
						RoutePoint updatedPt = new RoutePoint.Builder()
								.copy(p)
								.loc(loc)
								.time(lastUtc)
								.build();
						route.replaceRpt(i, updatedPt);
						updatedPts.add(updatedPt);
					}
				}

				for ( RouteManagerListener l :listeners ){
					l.onMarkLocationDetermined(updatedPts);
				}

				advanceActivePoint(lastUtc, loc, ArrivalType.AUTO_MARK_DETECTED);
			}

		});
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
		this.markDetector.setStartLine(route);
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

		lastUtc = nout.ii.utc;

		markDetector.onNavData(nout);

		RoutePoint activePoint = getActivePoint();
		if ( activePoint == null || !activePoint.loc.isValid() )
			return;

		if ( ! lastPointReported ){

			Distance dist = nout.ii.loc.distTo(activePoint.loc);

			if ( ! this.currentSegmentValid && dist.toMeters() > MIN_SEGMENT_LENGTH_M){
				setCurrentSegment( nout.ii.loc, activePoint.loc );
				this.currentSegmentValid = true;
			}

			float arrivalCircleRadius = STANDARD_ARRIVAL_CIRCLE;
			
			if ( activePoint.type == RoutePoint.Type.START)
				arrivalCircleRadius = START_POUINT_ARRIVAL_CIRCLE;
				
			if ( dist.toMeters() < arrivalCircleRadius ){ // Check for arrival by entering the radius 
				advanceActivePoint(nout.ii.utc, activePoint.loc, ArrivalType.CIRCLE_ENTERED);
			}else if ( this.currentSegmentValid ) { // Check for arrival by crossing the perpendicular
				Coordinate p = nout.ii.loc.toCoordinate();
				// The Projection Factor is the constant r by which the vector for this segment must be 
				// multiplied to equal the vector for the projection of p on the line defined by this segment.
				double r =  currentSegment.projectionFactor ( p );
				
				if ( r >= 1. ){
					advanceActivePoint(nout.ii.utc, activePoint.loc, ArrivalType.PERPENDICULAR_PASSED);
				}else if ( r > 0. ){  // Check for getting close enough and then sailing away
					
					// Compute how close we are on projected distance to wpt
					if ( dist.toMeters() < minDistance )
						minDistance = dist.toMeters(); 
					
					// If we moved back ( with some hysteresis ) let's toggle 
					if ( (minDistance < 200) && (dist.toMeters() > minDistance * ARRIVAL_PROJ_HYSTEREZIS_FACTOR) ){
						advanceActivePoint(nout.ii.utc, activePoint.loc, ArrivalType.PERPENDICULAR_PASSED);
					}
				}
			}
		}
	}

	public void startMarkDetection(){
		markDetector.start();
	}

	public void stopMarkDetection(){
		markDetector.stop();
	}

	private void advanceActivePoint(UtcTime utc, GeoLoc currentDestLoc, ArrivalType arrivalType) {

		if ( !route.lastPointIsActive() ){
			if (currentDestLoc.isValid() && route.getAfterActivePoint().loc.isValid()){
				setCurrentSegment( currentDestLoc, route.getAfterActivePoint().loc );
			}
			route.advanceActivePoint();
			for ( RouteManagerListener l :listeners ){
				l.onNewActivePoint(utc, route.getActiveWptIdx(), arrivalType);
			}
		}else{
			lastPointReported = true;
		}
	}

	private void setCurrentSegment(GeoLoc from, GeoLoc to) {

		Coordinate p0 = from.toCoordinate();
		Coordinate p1 = to.toCoordinate();
		currentSegment.setCoordinates(p0, p1);
		minDistance = currentSegment.getLength();
	}

}
