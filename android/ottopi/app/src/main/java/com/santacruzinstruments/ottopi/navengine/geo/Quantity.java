package com.santacruzinstruments.ottopi.navengine.geo;

/** 
 * This class is supposed to be  thread-safe immutable, since it has final methods only  
 * */
public abstract class Quantity {
	protected final boolean mIsValid;

	protected Quantity(boolean isValid)
	{
		mIsValid = isValid;
	}
	public boolean isValid() {return mIsValid;}
	
	public abstract String toString();
}
