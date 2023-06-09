package com.santacruzinstruments.ottopi.navengine;


import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

public class NavComputerOutput {

	/** Instrument input used to generate this output */
	final public InstrumentInput ii;
	
	/** True wind speed (computed by us, as opposed to InstrumentInput#tws where it directly read from external instruments ) */
	final public Speed tws;
	/** True wind angle (computed by us, as opposed to InstrumentInput#tws where it directly read from external instruments) */
	final public Angle twa;
	/** True Wind Direction */
	final public Direction twd;
	/** Speed of tide */
	final public Speed sot;
	/** Direction of tide */
	final public Direction dot;
	/** Destination name */
	final public String destName;
	/** Angle to destination */
	final public Angle atm;
	/** Distance to destination */
	final public Distance dtm;
	/** Name of second mark */
	final public String nextDestName;
	/** TWA from current mark to the next mark*/
	final public Angle nextLegTwa;

	/** Median true wind angle on port tack (either current or previous ) */
	final public Angle medianPortTwa;
	/** Interquartile range of true wind angle on port tack (either current or previous ) */
	final public Angle portTwaIqr;
	/** Median true wind angle on starboard tack (either current or previous ) */
	final public Angle medianStbdTwa;
	/** Interquartile range of true wind angle on starboard tack (either current or previous ) */
	final public Angle stbdTwaIqr;

	@NonNull
	@Override
	public String toString() {
		return "NavComputerOutput" +
				",ii," + ii +
				",tws," + tws +
				",twa," + twa +
				",twd," + twd +
				",sot," + sot +
				",destName," + destName +
				",atm," + atm +
				",dtm," + dtm +
				",nextDestName," + nextDestName +
				",nextLegTwa," + nextLegTwa +
				",medianPortTwa," + medianPortTwa +
				",portTwaIqr," + portTwaIqr +
				",medianStbdTwa," + medianStbdTwa +
				",stbdTwaIqr," + stbdTwaIqr;
	}

	private NavComputerOutput(Builder builder) {
		ii = builder.ii;
		tws = builder.tws;
		twa = builder.twa;
		twd = builder.twd;
		sot = builder.sot;
		dot = builder.dot;
		destName = builder.destName;
		atm = builder.atm;
		dtm = builder.dtm;
		nextDestName = builder.nextDestName;
		nextLegTwa = builder.nextLegTwa;
		medianPortTwa = builder.medianPortTwa;
		portTwaIqr = builder.portTwaIqr;
		medianStbdTwa = builder.medianStbdTwa;
		stbdTwaIqr = builder.stbdTwaIqr;
	}
	
	public static class Builder
	{
		// Required parameters
		final private InstrumentInput ii;
		// Optional parameters - initialized to default values
		public Speed tws = Speed.INVALID;
		public Angle twa = Angle.INVALID;
		public Direction twd = Direction.INVALID;
		public Speed sot = Speed.INVALID;
		public Direction dot = Direction.INVALID;
		public String destName = "";
		public Angle atm = Angle.INVALID;
		public Distance dtm = Distance.INVALID;
		public String nextDestName = "";
		public Angle nextLegTwa = Angle.INVALID;
		public Angle medianPortTwa = Angle.INVALID;
		public Angle portTwaIqr = Angle.INVALID;
		public Angle medianStbdTwa = Angle.INVALID;
		public Angle stbdTwaIqr = Angle.INVALID;

		public Builder(InstrumentInput ii)
		{
			this.ii = ii;
		}
		public NavComputerOutput build() {
			return new NavComputerOutput(this);
		}
		public Builder tws(Speed val) 
		  { tws = val; return this; }
		public Builder twa(Angle val) 
		  { twa = val; return this; }
		public Builder twd(Direction val)
		  { twd = val; return this; }
		public Builder sot(Speed val) 
		  { sot = val; return this; }
		public Builder dot(Direction val)
		  { dot = val; return this; }
		public Builder destName(String val)
		  { destName = val; return this; }
		public Builder atm(Angle val)
		  { atm = val; return this; }
		public Builder dtm(Distance val)
		  { dtm = val; return this; }
		public Builder nextDestName(String val)
		  { nextDestName = val; return this; }
		public Builder nextLegTwa(Angle val)
		  { nextLegTwa = val; return this; }
		public Builder medianPortTwa(Angle val)
		  { medianPortTwa = val; return this; }
		public Builder portTwaIqr(Angle val)
		  { portTwaIqr = val; return this; }
		public Builder medianStbdTwa(Angle val)
		  { medianStbdTwa = val; return this; }
		public Builder stbdTwaIqr(Angle val)
		  { stbdTwaIqr = val; return this; }
	}

}
