package com.santacruzinstruments.ottopi.navengine.geo;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Distance extends Quantity {
	private final double mDist;
	private final static String INVALID_VALUE = "--.-";

	public static final Distance INVALID = new Distance();

	/**
	 * Constructs invalid distance object
	 */
	public Distance() {
		super(false);
		mDist = 0;
	}

	/**
	 * Constructs valid Distance object initialized to the value expressed in
	 * nautical miles
	 * 
	 * @param distNm
	 *            - Nautical miles
	 */
	public Distance(double distNm) {
		super(true);
		mDist = distNm;
	}

	@NonNull
	@Override
	public String toString() {
		if (mIsValid) {
			if ( mDist > 0.5 )
				return String.format(Locale.getDefault(), "%2.1f", mDist);
			else
				return String.format(Locale.getDefault(), "%.0f m", toMeters());
		} else {
			return INVALID_VALUE;
		}
	}

	public double toNauticalMiles() {
		return mDist;
	}

	public float toMeters() {
		return (float) (mDist * 1852.) ;
	}
}
