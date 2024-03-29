package com.santacruzinstruments.ottopi.navengine.geo;

import androidx.annotation.NonNull;

import org.locationtech.jts.geom.Coordinate;

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

	public Distance distTo(GeoLoc to){
		if (isValid() && to.isValid()){
			return Geodesy.geodesyFactory(this).dist(this, to);
		}else{
			return Distance.INVALID;
		}
	}

	public Direction bearingTo(GeoLoc to){
		if (isValid() && to.isValid()){
			return Geodesy.geodesyFactory(this).bearing(this, to);
		}else{
			return Direction.INVALID;
		}
	}

	public final Coordinate toCoordinate()
	{
		if (isValid() ){
			return Geodesy.geodesyFactory(this).toCoordinate(this);
		}else{
			return new Coordinate();
		}
	}

	public final GeoLoc project(Distance dist, Direction bearing)
	{
		if (isValid() && dist.isValid(true) && bearing.isValid(true))
		{
			Coordinate c1 = toCoordinate();
			double distMeters = dist.toMeters();
			double trueBearingDeg = MagDecl.getInstance().fromMagToTrue(bearing.toDegrees());
			double trueBearingRad = Math.toRadians(trueBearingDeg);
			double angle = Math.PI/2 - trueBearingRad;  // Align zero angle with east ( X axis )
			Coordinate c2 = org.locationtech.jts.algorithm.Angle.project(c1, angle, distMeters);
			return Geodesy.geodesyFactory(this).toGeoLoc(c2);
		}
		else
		{
			return INVALID;
		}
	}

	public boolean isValid() {return mIsValid;}
	
	@NonNull
	@Override
	public final String toString()
	{
		return String.format(Locale.US, "%.5f,%.5f", lat, lon);
	}

}
