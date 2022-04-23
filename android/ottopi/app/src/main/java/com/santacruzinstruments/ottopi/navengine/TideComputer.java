package com.santacruzinstruments.ottopi.navengine;


import static com.santacruzinstruments.ottopi.navengine.Const.TIDE_FILTER_CONST;

import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

public class TideComputer {

	private Direction mDot;
	private Speed mSot;
	
	double mFilteredTideNorth;
	double mFilteredTideEast;
	
	public TideComputer()
	{
		mDot = Direction.INVALID;
		mSot = Speed.INVALID;
		mFilteredTideNorth = 0;
		mFilteredTideEast = 0;
	}
	
	public void update(Speed sog, Direction cog, Speed sow, Direction hdg) {
		if ( sog.isValid() && cog.isValid() && sow.isValid() && hdg.isValid() )
		{
			double cogRad = cog.toRadians();
			double sogNorth = sog.getKnots() * Math.cos( cogRad );
			double sogEast = sog.getKnots() * Math.sin( cogRad );

			double hdgRad = hdg.toRadians();
			double sowNorth = sow.getKnots() * Math.cos( hdgRad );
			double sowEast = sow.getKnots() * Math.sin( hdgRad );

			double tideNorth = sogNorth -  sowNorth;
			double tideEast = ( sogEast - sowEast);
			
			mFilteredTideNorth = filter ( mFilteredTideNorth, tideNorth);
			mFilteredTideEast = filter( mFilteredTideEast, tideEast);
			
			double tideSpeed = ( Math.sqrt( mFilteredTideNorth*mFilteredTideNorth + mFilteredTideEast*mFilteredTideEast ) );
			double tideDirection = 0;
			
			if ( tideSpeed > 0.1  )
			{
				tideDirection = Math.atan2(mFilteredTideEast, mFilteredTideNorth) * 180. / Math.PI;
			}
			
			mSot = new Speed(tideSpeed);
			mDot = new Direction(tideDirection);
		}
	}

	double filter( double f, double x )
	{
		return f * ( 1 - TIDE_FILTER_CONST ) + x * TIDE_FILTER_CONST;
	}
	
	public Speed getSpeedOfTide() {
		return mSot;
	}

	public Direction getDirectionOfTide() {
		return mDot;
	}

}
