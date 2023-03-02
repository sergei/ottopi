package com.santacruzinstruments.ottopi.navengine.geo;

/** 
 * This class is supposed to be  thread-safe immutable, since it has final methods only  
 * */
public class Speed extends Quantity{
	final private double mSpeedKts;
	private final static String INVALID_VALUE = "--.-";
	public static final Speed INVALID = new Speed();

	public static double Ms2Kts(double meterPerSec){
		return meterPerSec / 1852. * 3600.;
	}
	public Speed()
	{
		super(false);
		mSpeedKts = 0;
	}
	public Speed(double kts)
	{
		super(true);
		mSpeedKts = kts;
	}
	@Override
	public String toString() {
		if ( mIsValid )
		{
			return String.format("%.1f", mSpeedKts);
		}
		else
		{
			return INVALID_VALUE;
		}
	}
	public double getKnots() {
		return mSpeedKts;
	}
}
