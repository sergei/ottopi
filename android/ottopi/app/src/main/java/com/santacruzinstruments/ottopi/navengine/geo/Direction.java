package com.santacruzinstruments.ottopi.navengine.geo;

/** Represents direction value in degrees in range [0 ; +360] 
 * This class is supposed to be  thread-safe immutable, since it has final methods only 
 * */
public class Direction extends Quantity{
	private final double mDirection;
	private final static String INVALID_VALUE = "---";
	public static final Direction INVALID = new Direction();
	public Direction()
	{
		super(false);
		mDirection = 0;
	}
	public Direction(double dir)
	{
		super(true);
		mDirection = normalize(dir);
	}
	public final Direction addAngleDeg( double deg )
	{
		return mIsValid ? new Direction( normalize(mDirection + deg)) : INVALID;
	}
	
	public Direction addAngle(Angle twa) {
		return twa.isValid() ?  addAngleDeg(twa.toDegrees()) : INVALID;
	}

	public static Angle angleBetween(Direction from, Direction to)
	{
		if ( from.mIsValid && to.mIsValid)
		{
			return new Angle(to.mDirection - from.mDirection);
		}
		else
		{
			return  Angle.INVALID;
		}
	}
	private final double normalize(double dir)
	{
		for (int i=0; i < 10 && dir >= 360. ; i++ )
			dir -= 360;
		for (int i=0; i < 10 && dir < 0. ; i++ )
			dir += 360;
		return dir;
	}

	@Override
	public String toString() {
		if ( mIsValid )
		{
			return String.format("%03.0f", mDirection);
		}
		else
		{
			return INVALID_VALUE;
		}
	}
	public double toRadians() {
		if( mIsValid )
		{
			return mDirection * Math.PI / 180;
		}
		else
		{
			return 0;
		}
	}
	public double toDegrees() {
		return mDirection;
	}

}
