package com.santacruzinstruments.ottopi.navengine.polars;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.LinkedList;

import com.santacruzinstruments.ottopi.navengine.Targets;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

public class PolarTable {

	private static class Entry{
		double tws;
		double upwindTwa;
		double upwindBsp;
		double downWindTwa;
		double downWindBsp;
	}

	public enum PointOfSail {UPWIND, DOWNWIND}
	
	final private LinkedList<Entry> entries = new LinkedList<>();
	final private LinkedList<LinkedList<Double>> twaTable = new LinkedList<>();
	final private LinkedList<LinkedList<Double>>  boatSpeedTable = new LinkedList<>();
	
	
	public PolarTable (InputStream is) throws NumberFormatException, IOException{

		LineNumberReader lr = new LineNumberReader( new InputStreamReader(is));
		
		String l;
		
		while ( (l = lr.readLine()) != null ){
			String [] t = l.split("\\s+");
			Entry e = new Entry();
			e.tws = Double.parseDouble( t [0] );
			e.upwindTwa = Double.parseDouble( t [3] );
			e.upwindBsp = Double.parseDouble( t [4] );
			e.downWindTwa = Double.parseDouble( t [t.length - 4] );
			e.downWindBsp = Double.parseDouble( t [t.length - 3] );
			
			entries.add(e);

			LinkedList<Double> twaList = new LinkedList<>();
			for (int i = 1; i < t.length; i+=2){
				twaList.add( Double.parseDouble(t[i]));
			}
			twaTable.add(twaList);

			LinkedList<Double> bs = new LinkedList<>();
			for (int i = 2; i < t.length; i+=2){
				bs.add( Double.parseDouble(t[i]));
			}
			boatSpeedTable.add(bs);

		}
		
	}

	public Targets getTargets(Speed inputTws, PointOfSail pointOfSail){
		double tws = inputTws.getKnots();

		int[] indices = getTwsIndices(tws);

		int idxTwsLow = indices[0];
		int idxTwsHigh = indices[1];
		
		double twsLow = entries.get(idxTwsLow).tws;
		double twsHigh = entries.get(idxTwsHigh).tws;
		double twa0, twa1;
		double bsp0, bsp1;
		if ( pointOfSail == PointOfSail.UPWIND){
			twa0 = entries.get(idxTwsLow).upwindTwa;
			twa1 = entries.get(idxTwsHigh).upwindTwa;
			bsp0 = entries.get(idxTwsLow).upwindBsp;
			bsp1 = entries.get(idxTwsHigh).upwindBsp;
		} else {
			twa0 = entries.get(idxTwsLow).downWindTwa;
			twa1 = entries.get(idxTwsHigh).downWindTwa;
			bsp0 = entries.get(idxTwsLow).downWindBsp;
			bsp1 = entries.get(idxTwsHigh).downWindBsp;
		}

		double bsp = interpolate(tws, twsLow, twsHigh, bsp0, bsp1);
		double twa = interpolate(tws, twsLow, twsHigh, twa0, twa1);
		
		return new Targets(new Speed(bsp), new Angle (twa));
	}

	@NonNull
	private int[] getTwsIndices(double tws) {
		int i;
		for( i = 0; i < entries.size(); i++ ){
			Entry e = entries.get(i);
			if (  e.tws >= tws)
				break;
		}

		int idxTwsLow;
		int idxTwsHigh;
		if ( i == 0 ){
			idxTwsLow = 0;
			idxTwsHigh = 1;
		}else if ( i == entries.size() ){
			idxTwsLow = entries.size() - 2;
			idxTwsHigh = entries.size() - 1;
		}else{
			idxTwsLow = i - 1;
			idxTwsHigh = i;
		}

		return new int[] {idxTwsLow, idxTwsHigh};
	}

	public Speed getTargetSpeed(Speed inputTws, Angle inputTwa) {
		double tws = inputTws.getKnots();

		if ( tws < entries.getFirst().tws / 2.){
			return new Speed(0.);
		}

		tws = Math.min(tws, entries.getLast().tws);

		int[] twsIndices = getTwsIndices(tws);

		int idxTwsLow = twsIndices[0];
		int idxTwsHigh = twsIndices[1];

		double twsLow = entries.get(idxTwsLow).tws;
		double twsHigh = entries.get(idxTwsHigh).tws;

		// Interpolate along TWA axis
		double bspLowTws = interpBoatSpeed(inputTwa, idxTwsLow);
		double bspHighTws = interpBoatSpeed(inputTwa, idxTwsHigh);

		// Interpolate along TWS axis
		double bsp = interpolate(tws, twsLow, twsHigh, bspLowTws, bspHighTws);

		return new Speed(bsp);
	}

	double interpBoatSpeed(Angle inputTwa, int idxTws){
		LinkedList<Double> twaList = twaTable.get(idxTws);
		double twa = Math.min(Math.abs(inputTwa.toDegrees()), twaList.getLast());
		int i;
		for(i = 0; i < twaList.size() - 1 ; i++){
			if ( twaList.get(i) >= twa )
				break;
		}
		if ( i == twaList.size())
			i -- ;
		if( i == 0)
			i = 1;

		int idxTwaLow = i-1;
		int idxTwaHigh = i;
		double twaLow = twaList.get(idxTwaLow);
		double twaHigh = twaList.get(idxTwaHigh);

		// Interpolate along TWA axis
		return interpolate(twa, twaLow, twaHigh,
				boatSpeedTable.get(idxTws).get(idxTwaLow),
				boatSpeedTable.get(idxTws).get(idxTwaHigh));
	}

	private double interpolate(double x, double x1, double x2, double y1, double y2) {
		return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
	}

}
