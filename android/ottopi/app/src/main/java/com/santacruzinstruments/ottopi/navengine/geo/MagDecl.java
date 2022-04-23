package com.santacruzinstruments.ottopi.navengine.geo;

/**
 * This class is supposed to be  thread-safe immutable, since it has final methods only
 * */
public class MagDecl {
	  private static MagDecl instance = null;
	  private double mMagDecl;
	  final private GeoBounds mCachedRegion ;

	   protected MagDecl() {
		  mCachedRegion = new GeoBounds( );
		  mMagDecl = 14. + 2./60.;
	   }

	  final public static MagDecl getInstance() {
	      if(instance == null) {
	         instance = new MagDecl();
	      }
	      return instance;
	   }

	  final public static void reset() {
	      instance = null;
	   }


	  /**
	   *
	   * @param time
	   * @param loc
	   * @return  true if the new value was computed
	   */
	  final public boolean Update(UtcTime time, GeoLoc loc)
	  {
		  if ( !mCachedRegion.isValid() || ! mCachedRegion.isWithin( loc ) )
		  {
			  mCachedRegion.setAreaAround( loc, 1 );
			  GeomagneticField gm  = new GeomagneticField((float)loc.lat, (float)loc.lon, 0, time.toMiliSec() );
			  mMagDecl = gm.getDeclination();
			  return true;
		  }
		  else
		  {
			  return false;
		  }
	  }

	  final public double fromTrueToMag(double deg)
	  {
		  return deg - mMagDecl;
	  }

	  final public double fromMagToTrue(double deg)
	  {
		  return deg + mMagDecl;
	  }

}






