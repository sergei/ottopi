package com.santacruzinstruments.ottopi.navengine.geo;

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

	@Override
	public String toString() {
		if (mIsValid) {
				return String.format("%2.1f", mDist);
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
