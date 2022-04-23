package com.santacruzinstruments.ottopi.navengine;


import static com.santacruzinstruments.ottopi.navengine.Const.TWD_FILTER_CONST;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

import java.util.Arrays;
import java.util.LinkedList;

public class TrueWindComputer {

	private Speed mTws;
	private Angle mTwa;
	private Direction filteredTwd = Direction.INVALID;
	private Direction medianTwd = Direction.INVALID;
	private Angle twdIqr = Angle.INVALID;

	private double filteredTwdNorth;
	private double filteredTwdEast;

	private final int QUEUE_LEN = 300;
	private final LinkedList<Double> northWinds = new LinkedList<>();
	private final LinkedList<Double> eastWinds = new LinkedList<>();
	private final Double [] statsArray = new Double[QUEUE_LEN];

	public TrueWindComputer()
	{
		mTws = Speed.INVALID;
		mTwa = Angle.INVALID;
	}

	public void computeTrueWind(Speed sog, Speed aws, Angle awa, Direction mag) {
		mTws = Speed.INVALID;
		mTwa = Angle.INVALID;
		
		if ( ! sog.isValid() ) return;
		if ( ! aws.isValid() ) return;
		if ( ! awa.isValid() ) return;

		// Use information from http://en.wikipedia.org/wiki/Apparent_wind 

		// Inputs 
		double A = aws.getKnots();
		double V = sog.getKnots();
		double beta = awa.toDegrees() * Math.PI / 180;
		
		// True wind speed 
		double W = A * A + V * V - 2 * A * V * Math.cos( beta );
		
		// Do the sanity test. If true wind speed is less than one knot don't bother with the rest computations
		// and assume true wind angle equals to the apparent one 
		double alpha = beta;
		if ( W > 1. )
		{
			W = Math.sqrt( W ); // Safe now to take square root
			
			double r = ( A * Math.cos( beta ) - V ) / W;
			// Another check for data sanity 
			if ( r > 1. )
			{
				return;
			}
			
			// Safe to take arc cosine
			alpha = Math.acos( r );
			if ( beta < 0 )
				alpha = - alpha;
		}
		else if ( W < 0. ) // Nonsense input data
		{
			return;
		}
		
		// Since we made here all data makes sense now
		
		mTws = new Speed( W );
		mTwa = new Angle( alpha * 180 / Math.PI );

		// Now update the TWD
		if ( mag.isValid() ){

			Direction twd = mag.addAngle(mTwa);

			// Compute east component and west component
			double twdNorth = W * Math.cos( twd.toRadians() );
			double twdEast = W * Math.sin( twd.toRadians() );

			computeMedianWind(twdNorth, twdEast);

			filteredTwdNorth = filter(filteredTwdNorth, twdNorth);
			filteredTwdEast = filter(filteredTwdEast, twdEast);

			double filteredTwdKts = ( Math.sqrt( filteredTwdNorth*filteredTwdNorth + filteredTwdEast*filteredTwdEast ) );

			if ( filteredTwdKts > 0.1  )
			{
				double filteredTwdDegrees = Math.atan2(filteredTwdEast, filteredTwdNorth) * 180. / Math.PI;
				filteredTwd = new Direction(filteredTwdDegrees);
			}
		}
	}

	private void computeMedianWind(double twdNorth, double twdEast) {
		if ( northWinds.size() == QUEUE_LEN)
			northWinds.removeFirst();

		if ( eastWinds.size() == QUEUE_LEN)
			eastWinds.removeFirst();

		northWinds.addLast(twdNorth);
		eastWinds.addLast(twdEast);

		// Wait till we have enough data
		if (eastWinds.size() == QUEUE_LEN){
			double [] nq = computeQuartiles(northWinds);
			double [] eq = computeQuartiles(eastWinds);
			double twdQ1 = Math.atan2(eq[0], nq[0]) * 180. / Math.PI;
			double twdQ2 = Math.atan2(eq[1], nq[1]) * 180. / Math.PI;
			double twdQ3 = Math.atan2(eq[2], nq[2]) * 180. / Math.PI;

			medianTwd = new Direction(twdQ2);
			twdIqr = Direction.angleBetween(new Direction(twdQ1), new Direction(twdQ3));
		}
	}

	private double[] computeQuartiles(LinkedList<Double> list) {
		list.toArray(statsArray);
		Arrays.sort(statsArray);

		// Get quartiles
		double q1 = statsArray[QUEUE_LEN / 4];
		double q2 = statsArray[QUEUE_LEN / 2];
		double q3 = statsArray[QUEUE_LEN * 3 / 4];

		return new double[] {q1, q2, q3};
	}

	public Speed getTrueWindSpeed() {
		return mTws;
	}

	public Angle getTrueWindAngle() {
		return mTwa;
	}

	public Direction getFilteredTwd() {
		return filteredTwd;
	}

	private double filter( double f, double x )
	{
		return f * ( 1 - TWD_FILTER_CONST ) + x * TWD_FILTER_CONST;
	}

	public Direction getMedianTwd() {
		return medianTwd;
	}

	public Angle getTwdIqr() {
		return twdIqr;
	}
}
