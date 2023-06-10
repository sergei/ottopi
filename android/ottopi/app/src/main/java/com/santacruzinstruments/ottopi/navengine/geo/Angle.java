package com.santacruzinstruments.ottopi.navengine.geo;

/** Represents angle value in degrees in range (-180 ; +180] 
 * This class is supposed to be  thread-safe immutable, since it has final methods only 
 * */
public class Angle extends Quantity{
	private final double mAngle;
	private final static String INVALID_VALUE = "---";
	public static final Angle INVALID = new Angle();
	public Angle()
	{
		super(false);
		mAngle = 0;
	}
	public Angle(double dAngle)
	{
		super(true);
		for (int i=0; i < 10 && dAngle > 180. ; i++ )
			dAngle -= 360;
		for (int i=0; i < 10 && dAngle <= -180. ; i++ )
			dAngle += 360;
		mAngle = dAngle;
	}
	public final double toDegrees()
	{
		return mAngle;
	}
	public final double toRadians()
	{
		return Math.toRadians(mAngle);
	}
	@Override
	public String toString() {
		if ( mIsValid )
		{
			return String.format("%03.0f", mAngle);
		}
		else
		{
			return INVALID_VALUE;
		}
	}
}
