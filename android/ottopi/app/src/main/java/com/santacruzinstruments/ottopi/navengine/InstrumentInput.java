package com.santacruzinstruments.ottopi.navengine;


import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

/** This class is supposed to be thread safe, since it has final members only  */
public class InstrumentInput {
	public static final InstrumentInput INVALID = new InstrumentInput(new UtcTime(), new GeoLoc(), new Direction(),
			                                                          new Speed(), new Speed(), new Angle(), new Speed(), new Angle(), new Direction(), new Speed());
	/** Most recent UTC time */
	final public UtcTime   utc;
	/** Most recent location */
	final public GeoLoc    loc;
	/** Course Over Ground obtained from GPS (magnetic)  */
	final public Direction cog;
	/** Speed  Over Ground obtained from GPS  */
	final public Speed     sog;
	/** Apparent wind speed */
	final public Speed  aws;
	/** Apparent wind angle */
	final public Angle awa;
	/** True wind speed as reported by external sensors */
	final public Speed tws;
	/** True wind angle as reported by external sensors */
	final public Angle twa;
	/** Magnetic bearing as reported by external magnetic compass (magnetic)*/
	final public Direction mag;
	/** Speed over water as reported by knot meter */
	final public Speed sow;

	public static class Builder
	{
		private UtcTime   utc = UtcTime.INVALID;
		private GeoLoc    loc = GeoLoc.INVALID;
		private Direction cog = Direction.INVALID;
		private Speed     sog = Speed.INVALID;
		private Speed     aws = Speed.INVALID ;
		private Angle     awa = Angle.INVALID;
		private Speed     tws = Speed.INVALID;
		private Angle     twa = Angle.INVALID;
		private Direction mag = Direction.INVALID;
		private Speed     sow = Speed.INVALID;
		public boolean    isDemo = false;

		public Builder utc( UtcTime val )   { utc = val; return this; }
		public Builder loc( GeoLoc val )    { loc = val; return this; }
		public Builder cog( Direction val ) { cog = val; return this; }
		public Builder sog( Speed val )     { sog = val; return this; }
		public Builder aws( Speed val )     { aws = val; return this; }
		public Builder awa( Angle val )     { awa = val; return this; }
		public Builder tws( Speed val )     { tws = val; return this; }
		public Builder twa( Angle val )     { twa = val; return this; }
		public Builder mag( Direction val ) { mag = val; return this; }
		public Builder sow( Speed val )     { sow = val; return this; }
		public Builder isDemo( boolean val )  { isDemo = val; return this; }

		public InstrumentInput build() { return new InstrumentInput( this ); }
	}

	public InstrumentInput(UtcTime theUtc,
			GeoLoc    theLoc,
			Direction theCog, Speed theSog,
			Speed theAws, Angle theAwa,
			Speed theTws, Angle theTwa,
			Direction theMag,
			Speed theSow)
	{
		utc = theUtc;
		loc = theLoc;
		cog = theCog;
		sog = theSog;
		aws = theAws;
		awa = theAwa;
		tws = theTws;
		twa = theTwa;
		mag = theMag;
		sow = theSow;
	}

	public InstrumentInput(Builder builder) {
		utc = builder.utc;
		loc = builder.loc;
		cog = builder.cog;
		sog = builder.sog;
		aws = builder.aws;
		awa = builder.awa;
		tws = builder.tws;
		twa = builder.twa;
		mag = builder.mag;
		sow = builder.sow;
	}

	@NonNull
	@Override
	public String toString()
	{
		return  "utc," + utc.toString() +
				",loc," + loc.toString() +
				",cog," + cog.toString() +
		        ",sog," + sog.toString() +
				",mag," + mag.toString() +
				",sow," + sow.toString() +
				",awa," + awa.toString() +
				",aws," + aws.toString()
		;
	}
}
