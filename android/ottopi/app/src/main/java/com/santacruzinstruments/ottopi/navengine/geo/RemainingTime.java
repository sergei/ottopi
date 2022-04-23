package com.santacruzinstruments.ottopi.navengine.geo;

public final class RemainingTime extends Quantity {
	private final int mSeconds;
	private final static String INVALID_VALUE = "--:--";
	public static final RemainingTime INVALID = new RemainingTime();

	public RemainingTime() {
		super(false);
		mSeconds = 0;
	}

	public RemainingTime(int seconds) {
		super(seconds >= 0);
		mSeconds = seconds;
	}

	@Override
	public final String toString() {
		if ( mIsValid )
		{
			if ( mSeconds < 3600 )
			{
				return String.format("%02d:%02d", 
						mSeconds / 60 , mSeconds % 60);
			}
			else
			{
				return String.format("%02d:%02d:%02d", 
						mSeconds / 3600,  
						(mSeconds % 3600)  / 60 , 
						mSeconds % 60);
			}
		}
		else
		{
			return INVALID_VALUE;
		}
	}


	public int toSeconds() {
		return mSeconds;
	}
}
