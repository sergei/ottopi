package com.santacruzinstruments.ottopi.navengine.geo;


/**
 * This class is supposed to be  thread-safe immutable, since it has final methods only  
 * */
public abstract class Quantity {
	private static final int MAX_AGE_MS = 2000;
	protected final boolean mIsValid;

	private final long mCreatedAt = ClockProvider.getClock().millis();

	protected Quantity(boolean isValid)
	{
		mIsValid = isValid;
	}
	public boolean isValid() {return mIsValid && getAgeMs() < MAX_AGE_MS;}
	public boolean isValid(boolean ignoreAge) {return mIsValid && ((getAgeMs() < MAX_AGE_MS) || ignoreAge) ;}

	public long getAgeMs() { return ClockProvider.getClock().millis() - mCreatedAt; }
	
	public abstract String toString();
}
