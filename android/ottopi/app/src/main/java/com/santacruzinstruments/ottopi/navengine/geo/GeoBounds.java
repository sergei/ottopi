package com.santacruzinstruments.ottopi.navengine.geo;

public class GeoBounds {

	private boolean mIsValid;
	private GeoLoc  mNorthWest;
	private GeoLoc  mSouthEast;

	public GeoBounds() {
		mIsValid = false;
	}

	public GeoBounds(GeoLoc pt1, GeoLoc pt2) {
		double nwlat = pt1.lat > pt2.lat ? pt1.lat : pt2.lat;
		double nwlon = pt1.lon < pt2.lon ? pt1.lon : pt2.lon;
		double selat = pt1.lat < pt2.lat ? pt1.lat : pt2.lat;
		double selon = pt1.lon > pt2.lon ? pt1.lon : pt2.lon;

		mNorthWest = new GeoLoc(nwlat, nwlon);
		mSouthEast = new GeoLoc(selat, selon);
		mIsValid = true;
	}

	public GeoBounds(GeoBounds gb) {
		mNorthWest = gb.mNorthWest;
		mSouthEast = gb.mSouthEast;
		mIsValid = gb.mIsValid;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("GeoBounds[ NW:(%f, %f)  SE:(%f, %f)]", mNorthWest.lat, mNorthWest.lon, mSouthEast.lat, mSouthEast.lon);
	}


	public void setCorners(GeoLoc northWest, GeoLoc southEast) {
		mIsValid = true;
		if ( ! northWest.isValid() )
			throw new IllegalArgumentException("North West corner is invalid");
		if ( ! southEast.isValid() )
			throw new IllegalArgumentException("South East corner is invalid");
		if ( (northWest.lat <  southEast.lat)  )
			throw new IllegalArgumentException("North lattitude is less than south lattitude");
		if ( northWest.lon > southEast.lon )
			throw new IllegalArgumentException("West longitude is greater that east longitude");

		mNorthWest = northWest;
		mSouthEast = southEast;

	}

	public boolean isWithin(GeoLoc loc) {
		if ( ! loc.isValid() )
			throw new IllegalArgumentException("Location is invalid");

		return  loc.lat <= mNorthWest.lat && loc.lat >= mSouthEast.lat
		     && loc.lon >= mNorthWest.lon && loc.lon <= mSouthEast.lon;
	}

	public boolean isValid() {
		return mIsValid;
	}

	/**
	 *
	 * @return Lattitude span expressed in minuites
	 */
	public Distance getLatSpan() {
		return Geodesy.geodesyFactory(mNorthWest).
			dist(mNorthWest, new GeoLoc(mSouthEast.lat,mNorthWest.lon))  ;
	}

	public Distance getLonSpan() {
		return Geodesy.geodesyFactory(mNorthWest).
			dist(mNorthWest, new GeoLoc(mNorthWest.lat, mSouthEast.lon))  ;
	}

	public void copyScale(GeoBounds gb, double coeff) {

		double latHalfSpanScaled  = (gb.mNorthWest.lat - gb.mSouthEast.lat) / 2 * coeff;
		double latCenter    = (gb.mNorthWest.lat + gb.mSouthEast.lat) / 2;

		double lonHalfSpanScaled  = (gb.mSouthEast.lon - gb.mNorthWest.lon ) / 2 * coeff;
		double lonCenter    = (gb.mSouthEast.lon + gb.mNorthWest.lon ) / 2;


		mNorthWest = new GeoLoc(latCenter + latHalfSpanScaled , lonCenter - lonHalfSpanScaled );
		mSouthEast = new GeoLoc(latCenter - latHalfSpanScaled , lonCenter + lonHalfSpanScaled );
		mIsValid = true;
	}

	public GeoLoc getCenter() {
		double latCenter    = (mNorthWest.lat + mSouthEast.lat) / 2;
		double lonCenter    = (mSouthEast.lon + mNorthWest.lon ) / 2;
		return new GeoLoc(latCenter, lonCenter);
	}


	public boolean includes(GeoBounds gb1) {
		return isWithin( gb1.mNorthWest ) && isWithin( gb1.mSouthEast );
	}

	public void union(GeoLoc loc) {
		if ( loc.isValid() )
		{
			double nwlat = loc.lat > mNorthWest.lat ? loc.lat : mNorthWest.lat;
			double nwlon = loc.lon < mNorthWest.lon ? loc.lon : mNorthWest.lon;
			double selat = loc.lat < mSouthEast.lat ? loc.lat : mSouthEast.lat;
			double selon = loc.lon > mSouthEast.lon ? loc.lon : mSouthEast.lon;

			mNorthWest = new GeoLoc(nwlat, nwlon);
			mSouthEast = new GeoLoc(selat, selon);
		}
	}

	public void union(GeoBounds gb) {
		if ( gb.isValid() )
		{
			double nwlat = gb.mNorthWest.lat > mNorthWest.lat ? gb.mNorthWest.lat : mNorthWest.lat;
			double nwlon = gb.mNorthWest.lon < mNorthWest.lon ? gb.mNorthWest.lon : mNorthWest.lon;
			double selat = gb.mSouthEast.lat < mSouthEast.lat ? gb.mSouthEast.lat : mSouthEast.lat;
			double selon = gb.mSouthEast.lon > mSouthEast.lon ? gb.mSouthEast.lon : mSouthEast.lon;

			mNorthWest = new GeoLoc(nwlat, nwlon);
			mSouthEast = new GeoLoc(selat, selon);
		}

	}

	/**
	 * Changes current object to be centered around given point with a given side
	 * @param center
	 * @param sideDeg
	 */
	public void setAreaAround(GeoLoc center, double sideDeg ) {

		double d = sideDeg / 2;

		mNorthWest = new GeoLoc(center.lat + d, center.lon - d );
		mSouthEast = new GeoLoc(center.lat - d , center.lon + d);
		mIsValid = true;

	}


}
