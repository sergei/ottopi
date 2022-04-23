package com.santacruzinstruments.ottopi.navengine.geo;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;

/** 
 * This class is supposed to be  thread-safe immutable, since it has final methods only  
 * */
public class GeoLoc implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final GeoLoc INVALID = new GeoLoc();

	protected final boolean mIsValid;

	public final double lat;
	public final double lon;
	
	public GeoLoc(double lat_, double lon_) {
		lat = lat_;
		lon = lon_;
		mIsValid = true;
	}

	public GeoLoc() {
		lat = 0;
		lon = 0;
		mIsValid = false;
	}
	
	public boolean isValid() {return mIsValid;}
	
	@NonNull
	@Override
	public final String toString()
	{
		return String.format(Locale.US, "%.5f,%.5f", lat, lon);
	}

}
