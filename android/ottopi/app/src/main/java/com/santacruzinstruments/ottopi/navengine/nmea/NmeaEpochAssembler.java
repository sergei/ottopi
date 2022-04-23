package com.santacruzinstruments.ottopi.navengine.nmea;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

import com.santacruzinstruments.ottopi.navengine.InstrumentInput;
import com.santacruzinstruments.ottopi.navengine.InstrumentInputListener;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.MagDecl;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

public class NmeaEpochAssembler implements NmeaParser.NmeaMsgListener {

	private static class Value
	{
		public double value;
		boolean isValid;
		long receivedAtMs;

		public Value()
		{
			isValid = false;
		}
		public void set(double value, long nowMs)
		{
			this.value = value;
			receivedAtMs = nowMs;
			isValid = true;
		}

		public void checkExpiration(long nowMs)
		{
			if ( nowMs - receivedAtMs > VALUE_EXPIRATION_TOUT_MS )
				isValid = false;
		}
	}

	private static final double MIN_GPS_SPEED_FOR_VALID_COURSE = 0.5;

	private static final long VALUE_EXPIRATION_TOUT_MS = 10 * 1000; // Declare value invalid after this time 
	private static final long TIMEOUT_WITHOUT_RMC_MS = 3 * 1000;    // Don't expect to see RMC after this time

	final private MagDecl mMagDecl;
	private NmeaParser.RMC mLastRmc;

	boolean mGotRmc;
	private boolean mGotNoRmcTimeout;
	
	long mLastRmcUtcMs;
	boolean mIslastRmcUtcValid;

	private UtcTime mLastPlatformUtc;
	private UtcTime mLastPublishedPlatformUtc;

	private final Value mag;
	private final Value awa;
	private final Value aws;
	private final Value twa;
	private final Value tws;
	private final Value sow;

	final private ArrayList<Value> mValues;

	LinkedList<InstrumentInputListener> mInstrumentInputListeners;

	GregorianCalendar mcal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
	
	public NmeaEpochAssembler()
	{
		mMagDecl = MagDecl.getInstance();

		awa = new Value();
		aws = new Value();
		twa = new Value();
		tws = new Value();
		mag = new Value();
		sow = new Value();

		mValues = new ArrayList<>();
		mValues.add(awa);
		mValues.add(aws);
		mValues.add(twa);
		mValues.add(tws);
		mValues.add(mag);
		mValues.add(sow);

		mGotRmc = false;
		mGotNoRmcTimeout = false;
		mIslastRmcUtcValid = false;
		
		mLastPlatformUtc = UtcTime.INVALID;
		mLastPublishedPlatformUtc = UtcTime.INVALID;
		
		mInstrumentInputListeners = new LinkedList<>();
	}

	public void addInstrumentInputListener( InstrumentInputListener l){
		mInstrumentInputListeners.add( l );
	}
	
	private void checkExpiration( long nowMs ){
		for(Value v: mValues )
			v.checkExpiration(nowMs);
	}

	private void postInstrumentInput()
	{
		InstrumentInput ii;

		UtcTime utc = UtcTime.INVALID;
		Direction cog = Direction.INVALID;
		Speed sog = Speed.INVALID;
		Angle awa = Angle.INVALID;
		Speed aws = Speed.INVALID;
		Angle twa = Angle.INVALID;
		Speed tws = Speed.INVALID;
		Direction mag = Direction.INVALID;
		Speed sow = Speed.INVALID;
		GeoLoc loc = GeoLoc.INVALID;

		if ( mGotRmc || mGotNoRmcTimeout )
		{
			if ( mGotRmc )
			{
				if ( mLastRmc.bTimestampValid)
				{
					utc = new UtcTime(mLastRmc.dtTimestamp);
				}
	
				if ( mLastRmc.bPosValid)
				{
					loc = new GeoLoc(mLastRmc.dLat, mLastRmc.dLon);
					mMagDecl.Update(utc, loc);
				}
	
				if ( mLastRmc.bSpeedValid)
				{
					sog = new Speed(mLastRmc.dSpeedOverGround);
	
					if ( mLastRmc.bCourseValid && mLastRmc.dSpeedOverGround > MIN_GPS_SPEED_FOR_VALID_COURSE)
					{
						cog = new Direction(mMagDecl.fromTrueToMag(mLastRmc.dCourseOverGround));
					}
				}
			}
			else
			{
				utc = mLastPlatformUtc;
			}

			if ( this.awa.isValid )
			{
				awa = new Angle(this.awa.value);
			}

			if (this.aws.isValid)
			{
				aws = new Speed(this.aws.value);
			}

			if ( this.twa.isValid )
			{
				twa = new Angle(this.twa.value);
			}

			if( this.tws.isValid )
			{
				tws = new Speed(this.tws.value);
			}

			if ( this.mag.isValid )
			{
				mag = new Direction(this.mag.value);
			}

			if( this.sow.isValid)
			{
				sow = new Speed(this.sow.value);
			}
			
			ii = new InstrumentInput(utc, loc,
									cog, sog,
									aws, awa,
									tws, twa,
									mag, sow
									);
			

			for ( InstrumentInputListener l : mInstrumentInputListeners )
				l.onInstrumentInput(ii);

			resetNoRmcTimeout();

		}

		
	}

	private boolean haveTimeStampToUse() 
	{
		return mIslastRmcUtcValid || mLastPlatformUtc.isValid() ;
	}

	private long getTimeStampToUse()
	{
		return mIslastRmcUtcValid ? mLastRmcUtcMs : mLastPlatformUtc.toMiliSec();
	}

	
	private void resetNoRmcTimeout()
	{
		mGotNoRmcTimeout = false;
		if ( mLastPlatformUtc.isValid() )
		{
			mLastPublishedPlatformUtc = mLastPlatformUtc;
		}
	}
	
	private boolean isTimeoutWithoutRmcExpired() {
		if ( mLastPlatformUtc.isValid() )
		{
			if ( !mLastPublishedPlatformUtc.isValid() )
				mLastPublishedPlatformUtc = mLastPlatformUtc;
			
			long diffTimeMs = mLastPlatformUtc.toMiliSec() - mLastPublishedPlatformUtc.toMiliSec();
			if ( diffTimeMs > TIMEOUT_WITHOUT_RMC_MS )
			{
				mGotNoRmcTimeout = true;
				return true;
			}
		}
		return false;
	}

	public void setPlatformUtcTime(UtcTime utc) {
		if ( utc.isValid() )
			mLastPlatformUtc = utc;
	}

	@Override
	public void onVhw(NmeaParser.VHW vhw) {
		if ( haveTimeStampToUse() ){
			if( vhw.bSpeedKtsValid)
			{
				this.sow.set(vhw.dSpeedKts, getTimeStampToUse() );
			}
			if ( vhw.bHdgMagValid)
			{
				this.mag.set(vhw.dHdgMagDeg, getTimeStampToUse() );
			}
		}else if ( isTimeoutWithoutRmcExpired() )
		{
			checkExpiration( mLastPlatformUtc.toMiliSec() );
		}
	}

	@Override
	public void onVwr(NmeaParser.VWR vwr) {
		if ( haveTimeStampToUse() ){
			if ( vwr.bRelWindAngleDegValid)
			{
				this.awa.set(vwr.dRelWindAngleDeg, getTimeStampToUse() );
			}
			if ( vwr.bRelWindSpeedKtsValid)
			{
				this.aws.set(vwr.dRelWindSpeedKts, getTimeStampToUse() );
			}
		}else if ( isTimeoutWithoutRmcExpired() )
		{
			checkExpiration( mLastPlatformUtc.toMiliSec() );
		}
	}

	@Override
	public void onMwv(NmeaParser.MWV mwv) {
		if ( haveTimeStampToUse() ){
			if ( mwv.bIsRelative )
			{
				if ( mwv.bWindAngleDegValid)
				{
					this.awa.set(mwv.dWindAngleDeg, getTimeStampToUse() );
				}
				if ( mwv.bWindSpeedKtsValid)
				{
					this.aws.set(mwv.dWindSpeedKts, getTimeStampToUse() );
				}
			}
			else
			{
				if ( mwv.bWindAngleDegValid)
				{
					this.twa.set(mwv.dWindAngleDeg, getTimeStampToUse() );
				}
				if ( mwv.bWindSpeedKtsValid)
				{
					this.tws.set(mwv.dWindSpeedKts, getTimeStampToUse() );
				}
			}
			
		}else if ( isTimeoutWithoutRmcExpired() )
		{
			checkExpiration( mLastPlatformUtc.toMiliSec() );
		}
	}

	@Override
	public void onHdg(NmeaParser.HDG hdg) {
		if ( haveTimeStampToUse() ){
			if ( hdg.bMagSensHeadDegValid){
				this.mag.set(hdg.dMagSensHeadDeg, getTimeStampToUse() );
			}
		}else if ( isTimeoutWithoutRmcExpired() )
		{
			checkExpiration( mLastPlatformUtc.toMiliSec() );
		}
	}

	@Override
	public void onRmc(NmeaParser.RMC rmc) {
		
		// We want to ignore RMC with time 00:00:00, due to SiRF bug when it can be day off 
		if ( rmc.bTimestampValid ){
			mcal.setTime(rmc.dtTimestamp);
			if ( (mcal.get(Calendar.HOUR_OF_DAY) == 0) && (mcal.get(Calendar.MINUTE) == 0) && (mcal.get(Calendar.SECOND) == 0) ){
				return;
			}
		}
		
		mLastRmc = rmc;
		mGotRmc = true;
		if ( mLastRmc.bTimestampValid )
		{
			mIslastRmcUtcValid = true;
			mLastRmcUtcMs = mLastRmc.dtTimestamp.getTime();
			checkExpiration(mLastRmcUtcMs);
		}
		postInstrumentInput();

	}

	@Override
	public void onUnknownMessage(String msg) {
	}

	@Override
	public void onPscirTST(NmeaParser.PscirTST obj) {
	}

	@Override
	public void onPmacrSEV(NmeaParser.PmacrSev obj) {
		
	}

	@Override
	public void PracrLIN(NmeaParser.PracrLIN obj) {

	}

	@Override
	public void PracrSTR(NmeaParser.PracrSTR obj) {

	}
}
