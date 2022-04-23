package com.santacruzinstruments.ottopi.navengine;

import java.util.LinkedList;

import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

public class NavComputer implements InstrumentInputListener {

	private final LinkedList<NavComputerOutputListener> listeners = new LinkedList<>();
	private final TideComputer tideComputer = new TideComputer();
	private final TrueWindComputer trueWindComputer = new TrueWindComputer();
	private final LegComputer legComputer = new LegComputer();
	private final WindStats windStats = new WindStats();

	public void addListener(NavComputerOutputListener  l){
		listeners.add(l);
	}
	
	@Override
	public void onInstrumentInput(InstrumentInput ii) {
		tideComputer.update(ii.sog, ii.cog, ii.sow, ii.mag);
		trueWindComputer.computeTrueWind(ii.sow, ii.aws, ii.awa, ii.mag);
		
		Direction twd = trueWindComputer.getFilteredTwd();

		legComputer.update(ii.loc, ii.mag, twd);
		windStats.update(trueWindComputer.getTrueWindAngle());

		NavComputerOutput nout = new NavComputerOutput.Builder(ii)
				.twd(twd)

				.tws(trueWindComputer.getTrueWindSpeed())
				.twa(trueWindComputer.getTrueWindAngle())

				.sot(tideComputer.getSpeedOfTide())
				.dot(tideComputer.getDirectionOfTide())

				.destName(legComputer.destName)
				.atm(legComputer.atm)
				.dtm(legComputer.dtm)
				.nextDestName(legComputer.nextDestName)
				.nextLegTwa(legComputer.nextLegTwa)
				.watm(legComputer.watm)

				.medianPortTwa(windStats.getMedianPortTwa())
				.portTwaIqr(windStats.getPortTwaIqr())
				.medianStbdTwa(windStats.getMedianStbdTwa())
				.stbdTwaIqr(windStats.getStbdTwaIqr())

				.build();
		
		for( NavComputerOutputListener l : listeners){
			l.onNavComputerOutput(nout);
		}
	}

	public void setDestinations(RoutePoint dest, RoutePoint nextDest){
		legComputer.setDestinations(dest, nextDest);
	}

}
