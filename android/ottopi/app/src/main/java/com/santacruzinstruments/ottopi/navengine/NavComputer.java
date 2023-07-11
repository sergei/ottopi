package com.santacruzinstruments.ottopi.navengine;

import static com.santacruzinstruments.ottopi.navengine.Const.BOAT_SPEED_CUTOFF_FREQUENCY_HZ;
import static com.santacruzinstruments.ottopi.navengine.Const.MAG_FILTER_CONST;
import static com.santacruzinstruments.ottopi.navengine.Const.MAG_HEADING_CUTOFF_FREQUENCY_HZ;
import static com.santacruzinstruments.ottopi.navengine.Const.WIND_CUTOFF_FREQUENCY_HZ;

import java.util.LinkedList;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.DirectionSmoother;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.LowPassDirectionFilter;
import com.santacruzinstruments.ottopi.navengine.geo.LowPassSpeedFilter;
import com.santacruzinstruments.ottopi.navengine.geo.LowPassVectorFilter;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import timber.log.Timber;

public class NavComputer implements InstrumentInputListener {

	private final LinkedList<NavComputerOutputListener> listeners = new LinkedList<>();
	private final TideComputer tideComputer = new TideComputer();
	private final TrueWindComputer trueWindComputer = new TrueWindComputer();
	private final LegComputer legComputer = new LegComputer();
	private final WindStats windStats = new WindStats();
	private final DirectionSmoother magSmoother = new DirectionSmoother(MAG_FILTER_CONST, 5);

	private final LowPassVectorFilter apparentWindFilter = new LowPassVectorFilter(WIND_CUTOFF_FREQUENCY_HZ);
	private final LowPassSpeedFilter boatSpeedFilter = new LowPassSpeedFilter(BOAT_SPEED_CUTOFF_FREQUENCY_HZ);
	private final LowPassDirectionFilter magFilter = new LowPassDirectionFilter(MAG_HEADING_CUTOFF_FREQUENCY_HZ);

	public void addListener(NavComputerOutputListener  l){
		listeners.add(l);
	}
	
	@Override
	public void onInstrumentInput(InstrumentInput ii) {
		Timber.d("onInstrumentInput: %s", ii);

		// Filter apparent wind
		apparentWindFilter.filter(ii.utc, ii.aws, ii.awa);
		// Filter boat speed
		Speed  filteredSow = boatSpeedFilter.filter(ii.utc, ii.sow);

		Direction filteredMag = magFilter.filter(ii.utc, ii.mag);

		InstrumentInput filtered = new InstrumentInput.Builder()
				.utc(ii.utc)
				// Don't filter GPS
				.loc(ii.loc)
				.cog(ii.cog)
				.sog(ii.sog)
				// Filter apparent wind
				.aws(apparentWindFilter.getFilteredSpeed())
				.awa(apparentWindFilter.getFilteredAngle())
				// Don't put the TWS, we will compute it here from the filtered AWS
				// Filter magnetic heading
				.mag(filteredMag)
				// Filter water speed
				.sow(filteredSow)
				.build();

		tideComputer.update(ii.sog, ii.cog, ii.sow, ii.mag);

		trueWindComputer.computeTrueWind(filtered.sow, filtered.aws, filtered.awa, filtered.mag);
		
		Direction twd = trueWindComputer.getFilteredTwd();
		Direction smoothHdg = magSmoother.update(ii.cog);

		legComputer.update(ii.loc, smoothHdg, twd);
		windStats.update(trueWindComputer.getTrueWindAngle());

		NavComputerOutput nout = new NavComputerOutput.Builder(filtered)
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

	// Compute the location of the rabbit boat, assuming it will sail for a minute on a port tack
	public static GeoLoc computeRabbitLoc(GeoLoc pinLoc, Direction twd) {
		Angle rabbitTwa = new Angle(45 );
		Direction rabbitDir = twd.addAngle(rabbitTwa);
		Distance startLineLen = new Distance(100. /1852.);  // 100 meters
		GeoLoc rabbitLoc =  pinLoc.project(startLineLen, rabbitDir);
		Timber.d("Pin %s, twd %s, rabbitLoc %s", pinLoc, twd, rabbitLoc);
		return rabbitLoc;
	}


}
