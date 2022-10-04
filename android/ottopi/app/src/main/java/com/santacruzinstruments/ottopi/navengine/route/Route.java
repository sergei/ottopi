package com.santacruzinstruments.ottopi.navengine.route;

import androidx.annotation.NonNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Route implements Iterable<RoutePoint>{

	private String name = "----";
	final private List<RoutePoint> pts;
	private RoutePoint activeRpt = RoutePoint.INVALID;
	private RoutePoint nextActiveRpt = RoutePoint.INVALID;
	public Route(){
		pts = new LinkedList<>();
	}

	public Route(List<RoutePoint> pts){
		this.pts = pts;
		activeRpt = RoutePoint.INVALID;
		nextActiveRpt = RoutePoint.INVALID;
		for( int i = 0; i < pts.size(); i++ ){
			if ( pts.get(i).isActive ){
				activeRpt = pts.get(i);
				nextActiveRpt = pts.get((i + 1) % pts.size());
			}
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addRpt(RoutePoint rpt){
		if ( pts.isEmpty() ){
			activeRpt = rpt.changeActiveStatus(true);
			pts.add(activeRpt);
		}else if( nextActiveRpt == RoutePoint.INVALID){
			nextActiveRpt = rpt;
			pts.add(nextActiveRpt);
		}else{
			pts.add(rpt);
		}
	}

	public int getRptsNum() {
		return pts.size();
	}

	public boolean isEmpty(){
		return pts.isEmpty();
	}

	public RoutePoint getRpt(int idx) {
		return pts.get(idx);
	}

	public void replaceRpt(int idx, RoutePoint newRpt) {
		// Forget not replace the active points
		RoutePoint oldRpt = pts.get(idx);
		if( activeRpt.equals(oldRpt))
			activeRpt = newRpt;
		if( nextActiveRpt.equals(oldRpt))
			nextActiveRpt = newRpt;
		// Now replace the point itself
		pts.set(idx, newRpt);
	}

	public RoutePoint getActivePoint() {
		return activeRpt;
	}

	public RoutePoint getAfterActivePoint() {
		return nextActiveRpt;
	}

	public String getName() {
		return name;
	}

    public void makeActiveWpt(int idx) {
		for ( int i = 0; i < pts.size(); i++){
			RoutePoint rpt = pts.get(i);

			if ( i == idx ){
				activeRpt = rpt.changeActiveStatus(true);
				pts.set(i, activeRpt);
				nextActiveRpt = pts.get((i + 1) % pts.size());
			}else if (pts.get(i).isActive){
				RoutePoint inactiveRpt = rpt.changeActiveStatus(false);
				pts.set(i, inactiveRpt);
			}
		}
    }

	public boolean hasStartLine(){
		return pts.size() > 1
				&& (pts.get(0).type == RoutePoint.Type.START && pts.get(0).leaveTo == RoutePoint.LeaveTo.PORT)
				&& (pts.get(1).type == RoutePoint.Type.START && pts.get(1).leaveTo == RoutePoint.LeaveTo.STARBOARD);
	}

	public boolean hasFinishLine(){
		int lastIdx = pts.size() -1;
		return pts.size() > 1
				&& pts.get(lastIdx-1).type == RoutePoint.Type.FINISH && pts.get(lastIdx-1).leaveTo == RoutePoint.LeaveTo.PORT
				&& pts.get(lastIdx).type == RoutePoint.Type.FINISH && pts.get(lastIdx).leaveTo == RoutePoint.LeaveTo.STARBOARD;
	}

	public int getActiveWptIdx() {
		return pts.indexOf(activeRpt);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean lastPointIsActive() {
		return !pts.isEmpty() &&  activeRpt == pts.get(pts.size() - 1);
	}

	public void advanceActivePoint() {
		if ( !lastPointIsActive() )
			makeActiveWpt(getActiveWptIdx() + 1);
	}

	@NonNull
	@Override
	public Iterator<RoutePoint> iterator() {
		return pts.listIterator();
	}

}
