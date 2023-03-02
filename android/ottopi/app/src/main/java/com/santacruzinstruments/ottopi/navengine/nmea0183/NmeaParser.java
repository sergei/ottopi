package com.santacruzinstruments.ottopi.navengine.nmea0183;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

/**
 * 
 */
public class NmeaParser implements NmeaReader.NmeaSentenceListener
{

	public interface NmeaMsgListener
	{
		void onVhw(NmeaParser.VHW vhw);
		void onVwr(NmeaParser.VWR vwr);
		void onMwv(NmeaParser.MWV mwv);
		void onHdg(NmeaParser.HDG hdg);
		void onRmc(NmeaParser.RMC rmc);
		void onPscirTST(NmeaParser.PscirTST obj);
		void onPmacrSEV(NmeaParser.PmacrSev obj);
		void PracrLIN(NmeaParser.PracrLIN obj);
		void PracrSTR(NmeaParser.PracrSTR obj);
		/**
		 * Called when received well formed but unknown message
		 * @param msg - NMEA string that was not recognized
		 */
		void onUnknownMessage(String msg);
	}

	int mFieldsNum = 0;
	String [] mFields;
	GregorianCalendar mcal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

	private final LinkedList<NmeaMsgListener> listeners = new LinkedList<>();
	public void addListener(NmeaMsgListener l)
	{
		listeners.add(l);
	}

	public void removeListener(NmeaMsgListener l)
	{
		listeners.remove(l);
	}


	@Override
	public void onValidMessage(String msg) {
		Object obj = Parse(msg);

		if ( obj instanceof NmeaParser.VHW){
			for( NmeaMsgListener l :  listeners )
				l.onVhw((NmeaParser.VHW)obj);
		}else if ( obj instanceof NmeaParser.VWR){
			for( NmeaMsgListener l :  listeners )
				l.onVwr((NmeaParser.VWR)obj);
		}else if ( obj instanceof NmeaParser.MWV){
			for( NmeaMsgListener l :  listeners )
				l.onMwv((NmeaParser.MWV)obj);
		}else if ( obj instanceof NmeaParser.HDG){
			for( NmeaMsgListener l :  listeners )
				l.onHdg((NmeaParser.HDG)obj);
		}else if ( obj instanceof NmeaParser.RMC){
			for( NmeaMsgListener l :  listeners )
				l.onRmc((NmeaParser.RMC)obj);
		}else if ( obj instanceof NmeaParser.PscirTST){
			for( NmeaMsgListener l :  listeners )
				l.onPscirTST((NmeaParser.PscirTST)obj);
		}else if ( obj instanceof NmeaParser.PmacrSev){
			for( NmeaMsgListener l :  listeners )
				l.onPmacrSEV((NmeaParser.PmacrSev)obj);
		}else if ( obj instanceof NmeaParser.PracrLIN){
			for( NmeaMsgListener l :  listeners )
				l.PracrLIN((NmeaParser.PracrLIN)obj);
		}else if ( obj instanceof NmeaParser.PracrSTR){
			for( NmeaMsgListener l :  listeners )
				l.PracrSTR((NmeaParser.PracrSTR)obj);
		}else if ( obj instanceof String ){
			for( NmeaMsgListener l :  listeners )
				l.onUnknownMessage((String) obj);
		}
	}

	private List<String> ignoreList = new LinkedList<>();
	public void setMsgToIgnore(List<String> ignoreList){
		this.ignoreList = ignoreList;
	}

	/**
	 * Parses the input NMEA string to the object.
	 * It assumes input strings to be valid NMEA sentences. 
	 * All validation must be done prior to passing string to the parser 
	 * @param nmea String containing valid NMEA sentence 
	 * @return Object built from this sentence or null
	 */
	public Object Parse(String nmea)
	{

		Object obj = null;
		mFields = nmea.split("[,*\n\r]");
		mFieldsNum = mFields.length;

		if ( mFieldsNum < 1)
			return null;
		
		String id = mFields[0];
		if ( id.length() < 3 )
			return null;
		
		// Ignore all RMCs but GPRMC or GNRMC
		String sender = id.substring(1, 3);
		
		String name;
		if ( id.length() < 6 ){
			name = id.substring(3);
		}else{
			// Cut GGA out of $GPGGA
			name = id.substring(3, 6);
		}

		if(ignoreList.contains(name)){
			return nmea;  // Unknown message
		}
		
		try { 
			if      (name.equals("VHW")) obj = ProcessVHW(); 
			else if (name.equals("VWR")) obj = ProcessVWR();
			else if (name.equals("MWV")) obj = ProcessMWV();
			else if (name.equals("HDG")) obj = ProcessHDG();
			else if (name.equals("RMC") && (sender.equals("GP") || sender.equals("GN")) ) obj = ProcessRMC();
			
			// Decode proprietary messages like $PRACR,STR,203942,110114
			else if (  (sender.equals("PR") || sender.equals("PS") || sender.equals("PM")) && mFields.length > 1 ){
				String subName =  mFields[1];
				switch (subName) {
					case "STR":
						obj = ProcessPracrSTR();
						break;
					case "LIN":
						obj = ProcessPracrLIN();
						break;
					case "TST":
						obj = ProcessPscirTST();
						break;
					case "SEV":
						obj = ProcessPmacrSev();
						break;
					default:
						obj = nmea; // Unknown proprietary message
						break;
				}
			}else{
				obj =  nmea; // Unknown message
			}
			
		}catch ( NumberFormatException ignore ){}
		
		return obj;
	}

	/** 
	 * Get the specified field in a NMEA string.
	 * @param idx Index of the field 0 - First field not counting name 
	 * @return null or this field  string value
	 */
	String GetField(int idx)
	{
		if ( (idx+1) >=  mFieldsNum) return null; 
	    String s = mFields[idx+1];
		if ( s.length() < 1 ) return null;
		return s;
	}

	private static class DoubleField
	{
		public double d;
		public boolean valid;
	}

	private static class IntegerField
	{
		public int i;
		public boolean valid;
	}

	private static class LongField
	{
		public long l;
		public boolean valid;
	}

	private static class DateField
	{
		public Date d;
		public boolean valid;
		int hrs;
		int min;
		int sec;
		int msec;
	}

	/**
	 * Returns true if given field equals the specified character
	 * @param idx Field offset
	 * @param c character to compare with 
	 * @return true if equals
	 */
	boolean FieldEquals(int idx, char c)
	{
		String f = GetField(idx);
		if ( f ==  null) return false;
		return f.charAt(0) == c;
	}
	
	/** 
	 * Get the double value from specified field in a NMEA string 
	 * @param idx  Index of the field 0 - First field not counting name
	 * @return This field value
	 */
	DoubleField GetDoubleField(int idx)
	{
		DoubleField df = new DoubleField();
		df.valid = false;
		df.d = 0;
		String f;
		if ( (f = GetField(idx)) != null)
		{
			df.d = Double.parseDouble(f);
			df.valid = true;
		}
		
		return df;
	}

	/** 
	 * Get the integer value from specified field in a NMEA string 
	 * @param idx  Index of the field 0 - First field not counting name
	 * @return This field value
	 */
	IntegerField GetIntegerField(int idx)
	{
		IntegerField fld = new IntegerField();
		fld.valid = false;
		fld.i = 0;
		String f;
		if ( (f = GetField(idx)) != null)
		{
			fld.i = Integer.parseInt(f);
			fld.valid = true;
		}
		
		return fld;
	}

	/** 
	 * Get the long value from specified field in a NMEA string 
	 * @param idx  Index of the field 0 - First field not counting name
	 * @return This field value
	 */
	LongField GetLongField(@SuppressWarnings("SameParameterValue") int idx)
	{
		LongField fld = new LongField();
		fld.valid = false;
		fld.l = 0;
		String f;
		if ( (f = GetField(idx)) != null)
		{
			fld.l = Long.parseLong(f);
			fld.valid = true;
		}
		
		return fld;
	}

	/**
	 * Get the signed double value from specified field in a NMEA string
	 * @param idx_value index of field containing value
	 * @param idx_sign  index of field containing sign character
	 * @param neg character designating value as negative
	 * @return value of a field with the sign applied to it
	 */
	private DoubleField GetDoubleFieldSigned(int idx_value, int idx_sign, char neg, char pos)
	{
		DoubleField df = GetDoubleField(idx_value);
		String f;
		if ( (f = GetField(idx_sign)) != null)
		{
			if ( f.charAt(0) == neg)
			{
				df.d = - df.d;
			}
			else //noinspection StatementWithEmptyBody
				if ( f.charAt(0) == pos)
			{
				; // Do nothing
			}
			else
			{
				df.valid = false;
			}
		}
		else
		{
			df.valid = false;
		}
		return df;
	}
	
	/** 
	 * Returns latitude or longitude encoded as dddmm.mmmmm,[NS] 
	 * @param idx Index of the field 0 - First field not counting name
	 * @return value (negative for south)
	 */
	DoubleField GetLatLonField(int idx)
	{
		DoubleField df = GetDoubleField(idx);
		if ( df.valid ) 
		{
			double int_deg = Math.floor(df.d/100); 
			double min = df.d - int_deg * 100; 
			double deg = int_deg + min/60;

			char sign = '-';
			String f = GetField(idx+1);
			if ( f !=  null) sign  = f.charAt(0);
			
			if ( sign == 'N' || sign ==  'E' )
				df.d = deg;
			else if ( sign ==  'S' || sign ==  'W' ) 
				df.d = - deg;
			else // In case if hemisphere is nonsensical
				df.valid = false;
		}
		return df;
	}
	
	/** 
	 * Returns time encoded as HHMMSS.SS 
	 * @param idx Index of the field 0 - First field not counting name
	 * @return Date
	 */
	DateField GetTimeField(int idx)
	{
		DoubleField df = GetDoubleField(idx);
		DateField  datefield = new DateField();
		datefield.valid = false;
		if ( df.valid ) 
		{
			datefield.msec = (int)Math.floor((df.d % 1. )* 1000); 
			datefield.sec  = (int)Math.floor(df.d        % 100.); 
			datefield.min  = (int)Math.floor(df.d/100.   % 100.); 
			datefield.hrs  = (int)Math.floor(df.d/10000. % 100.);
			
			mcal.set(2000, Calendar.JANUARY, 1, datefield.hrs, datefield.min, datefield.sec);
			mcal.set(Calendar.MILLISECOND, datefield.msec);
			datefield.d = mcal.getTime(); 
			datefield.valid = true;
		}
		return datefield;
	}

	/** 
	 * Returns time encoded as DDMMYY
	 * @param idx Index of the field 0 - First field not counting name
	 * @param t    Valid values are only HH:MM:SS.SSS
	 * @return Date
	 */
	DateField GetDateField(int idx, DateField t)
	{
		DoubleField df = GetDoubleField(idx);
		DateField  datefield = new DateField();
		datefield.valid = false;
		if ( df.valid && t.valid) 
		{
			int year   = (int)Math.floor(df.d        % 100.) + 2000; 
			int month  = (int)Math.floor(df.d/100.   % 100.); 
			int day    = (int)Math.floor(df.d/10000. % 100.);
			
			mcal.set(year, month - 1, day, t.hrs, t.min, t.sec);
			mcal.set(Calendar.MILLISECOND, t.msec);
			datefield.d = mcal.getTime(); 
			datefield.valid = true;
		}
		return datefield;
	}
	
	/* *************************************************************************************************
	VHW - Water speed and heading.
	The compass heading to which the vessel points and the speed of the vessel relative to the
	water.

	$--VHW, x.x, T, x.x, M, x.x, N, x.x, K*hh<CR><LF>
	        +-+--+  +-+--+  +-+--+  +-+--+  
	          |       |       |       +---- Speed, km/h
	          |       |       +------------ Speed, knots
	          |       +-------------------- Heading, degrees magnetic
	          +---------------------------- Heading, degrees true
	***************************************************************************************************/
	public static class VHW 
	{
		public boolean bHdgTrueValid;
		public double  dHdgTrueDeg;
		
		public boolean bHdgMagValid;
		public double  dHdgMagDeg;
		
		public boolean bSpeedKtsValid;
		public double  dSpeedKts;
	}
	
	private Object ProcessVHW() {
		VHW vhw = new VHW();
		
		DoubleField df = GetDoubleField(0);
		vhw.bHdgTrueValid = df.valid;
		vhw.dHdgTrueDeg = df.d;

		df = GetDoubleField(2);
		vhw.bHdgMagValid = df.valid;
		vhw.dHdgMagDeg = df.d;

		df = GetDoubleField(4);
		vhw.bSpeedKtsValid = df.valid;
		vhw.dSpeedKts = df.d;

		return vhw;
	}
	
	/* *************************************************************************************************
    VWR - Relative wind direction and speed
    VWR,148.,L,02.4,N,01.2,M,04.4,K
       148.,L       Wind from 148 deg Left of bow
       02.4,N       Speed 2.4 Knots
       01.2,M       1.2 Metres/Sec
       04.4,K       Speed 4.4 Kilometers/Hr
	$IIVWR,025,R,09.09,N,04.68,M,, 
	***************************************************************************************************/
	public static class VWR 
	{
		public boolean bRelWindAngleDegValid;
		public double  dRelWindAngleDeg;
		
		public boolean bRelWindSpeedKtsValid;
		public double  dRelWindSpeedKts;
	}
	
	private Object ProcessVWR() {
		VWR vwr = new VWR();
		
		DoubleField df = GetDoubleFieldSigned(0,1,'L','R');
		vwr.bRelWindAngleDegValid = df.valid;
		vwr.dRelWindAngleDeg = df.d;

		df = GetDoubleField(2);
		vwr.bRelWindSpeedKtsValid = df.valid;
		vwr.dRelWindSpeedKts = df.d;

		return vwr;
	}

	/* *************************************************************************************************
	When the reference field is set to R (Relative), data is provided giving the wind angle in relation
	to the vessel's bow/centreline and the wind speed, both relative to the (moving) vessel. Also
	called apparent wind, this is the wind speed as felt when standing on the (moving) ship.

	When the reference field is set to T (Theoretical, calculated actual wind), data is provided
	giving the wind angle in relation to the vessel's bow/centreline and the wind speed as if the
	vessel was stationary. On a moving ship these data can be calculated by combining the
	measured relative wind with the vessel's own speed.

	Example 1: If the vessel is heading west at 7 knots and the wind is from the east at 10 knots
	the relative wind is 3 knots at 180 degrees. In this same example the theoretical wind is
	10 knots at 180 degrees (if the boat suddenly stops, the wind will be at the full 10 knots and
	come from the stern of the vessel 180 degrees from the bow).

	Example 2: If the vessel is heading west at 5 knots and the wind is from the southeast at 7,07
	knots, the relative wind is 5 knots at 270 degrees. In this same example the theoretical wind is
	7,07 knots at 225 degrees (if the boat suddenly stops, the wind will be at the full 7,07 knots and
	come from the port-quarter of the vessel 225 degrees from the bow).

	$--MWV, x.x, a, x.x, a, A *hh<CR><LF>
	        +++  +  +++  +  +  
	         |   |   |   |  +----     Status, A = data valid V= data invalid
	         |   |   |   +------------ Wind speed units, K = km/h, M = m/s, N = knots
	         |   |   +---------------- Wind speed
	         |   +-------------------- Reference, R = relative, T = true
	         +------------------------ Wind angle, 0 to 359
	***************************************************************************************************/
	public static class MWV
	{
		public boolean bIsRelative;

		public boolean bWindAngleDegValid;
		public double  dWindAngleDeg;

		public boolean bWindSpeedKtsValid;
		public double  dWindSpeedKts;
	}

	private Object ProcessMWV() {
		MWV mwv = new MWV();

		if ( FieldEquals(4, 'A') )
		{
			DoubleField df = GetDoubleField(0);
			mwv.bWindAngleDegValid = df.valid;
			mwv.dWindAngleDeg = df.d;

			mwv.bIsRelative = FieldEquals(1, 'R');
			
			df = GetDoubleField(2);
			mwv.bWindSpeedKtsValid = df.valid;
			mwv.dWindSpeedKts = df.d;
		}
		else
		{
			mwv.bWindSpeedKtsValid = false;
			mwv.bWindAngleDegValid = false;
		}

		return mwv;
	}
	/* *************************************************************************************************
     Heading, deviation and variation
      IMO Resolution A.382 (X). Heading (magnetic sensor reading), which if corrected for deviation
      will produce magnetic heading, which if offset by variation will provide true heading.
     $--HDG, x.x, x.x, a, x.x, a*hh<CR><LF>
             +++  +-+--+  +-+--+  
              |     |       +---- Magnetic variation,degrees E/W (see notes 2 and 3)
              |     +------------ Magnetic deviation, degrees E/W (see notes 1 and 3)
              +------------------ Magnetic sensor heading, degrees
        NOTE 1 To obtain magnetic heading: add easterly deviation (E) to magnetic sensor reading;
               subtract westerly deviation (W) from magnetic sensor reading.
        NOTE 2 To obtain true heading: add easterly variation (E) to magnetic heading;
               subtract westerly variation (W) from magnetic heading.
        NOTE 3 Variation and deviation fields will be null fields if unknown.
	***************************************************************************************************/
	public static class HDG
	{
		public boolean bMagSensHeadDegValid;
		public double  dMagSensHeadDeg ;
	}
	private Object ProcessHDG() 
	{
		HDG hdg = new HDG();

		DoubleField df = GetDoubleField(0);
		hdg.bMagSensHeadDegValid = df.valid;
		hdg.dMagSensHeadDeg = df.d;

		return hdg;
	}
	
	/* *************************************************************************************************
	$GPRMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,ddmmyy,x.x,a*hh
	1    = UTC of position fix
	2    = Data status (V=navigation receiver warning)
	3    = Latitude of fix
	4    = N or S
	5    = Longitude of fix
	6    = E or W
	7    = Speed over ground in knots
	8    = Track made good in degrees True
	9    = UTC date
	10   = Magnetic variation degrees (Easterly var. subtracts from true course)
	11   = E or W
	12   = Checksum 
	***************************************************************************************************/
	/**
	 * RMC message fields
	 */
	public static class RMC
	{
		/**  Is time stamp valid  */
		public boolean bTimestampValid;
		public Date    dtTimestamp;
		
		public boolean  bPosValid;			
		/**  current latitude */
		public double   dLat;				
		/** current longitude */
		public double   dLon;				

		public boolean  bSpeedValid;			
		public boolean  bCourseValid;			
		public double   dSpeedOverGround;  // speed over ground, knots
		public double   dCourseOverGround;	// course over ground, degrees true

		public boolean bMagVarValid;
		public double  dMagVarDeg;	        // magnetic variation, degrees East(+)/West(-)
	}
	private Object ProcessRMC() 
	{
		RMC rmc = new RMC();

		DateField time = GetTimeField(0);
		rmc.bTimestampValid = time.valid;
		rmc.dtTimestamp = time.d;
		if ( rmc.bTimestampValid )
		{
			DateField date = GetDateField(8, time);
			if ( date.valid )  
				rmc.dtTimestamp = date.d;
		}
		
		if ( FieldEquals(1, 'A') )
		{
			DoubleField df = GetLatLonField(2);
			rmc.bPosValid = df.valid;
			rmc.dLat = df.d;
	
			df = GetLatLonField(4);
			rmc.bPosValid = rmc.bPosValid && df.valid;
			rmc.dLon = df.d;
		}
		else
		{
			rmc.bPosValid = false;
		}
		
		DoubleField df = GetDoubleField(6);
		rmc.bSpeedValid = df.valid;
		rmc.dSpeedOverGround = df.d;
		
		df = GetDoubleField(7);
		rmc.bCourseValid =  df.valid;
		rmc.dCourseOverGround = df.d;

		df = GetDoubleFieldSigned(9,10,'W','E');
		rmc.bMagVarValid = df.valid;
		rmc.dMagVarDeg = df.d;
		
		return rmc;
	}

	// $PRACR,STR,203942,110114
	
	/* *************************************************************************************************
	$PRACR,STR,hhmmss,ddmmyy*hh
	1    = UTC time of race start
	2    = UTC date of race start
	3   = Checksum 
	***************************************************************************************************/
	/**
	 * PRACR,STR message fields
	 */
	public static class PracrSTR
	{
		public boolean bTimestampValid;
		public Date    dtTimestamp;
	}
	private Object ProcessPracrSTR() {
		PracrSTR pracrSTR = new PracrSTR();
		pracrSTR.bTimestampValid = false;
		
		DateField time = GetTimeField(1);
		if ( time.valid ){ 
			DateField date = GetDateField(2, time);
			if ( date.valid ){
				pracrSTR.bTimestampValid = true;
				pracrSTR.dtTimestamp = date.d;
			}
		}
		return pracrSTR;
	}

    // $PRACR,LIN,PIN,3752.763200,N,12223.143100,W,CMTE,3752.812300,N,12223.347600,W
	
	/* *************************************************************************************************
	$PRACR,LIN,PIN,llll.ll,a,yyyyy.yy,a,CMTE,llll.ll,a,yyyyy.yy*hh
	1    = Latitude of start pin
	2    = N or S
	3    = Longitude of start pin
	4    = E or W
	5    = Latitude of committee boat
	6    = N or S
	7    = Longitude of committee boat
	8    = E or W
	9   = Checksum 
	***************************************************************************************************/
	/**
	 * RMC message fields
	 */
	public static class PracrLIN
	{
		public boolean  bPinValid;			
		/**  pin latitude */
		public double   dPinLat;				
		/** pin longitude */
		public double   dPinLon;				

		public boolean  bCmteValid;			
		/**  pin latitude */
		public double   dCmteLat;				
		/** pin longitude */
		public double   dCmteLon;				
	}
	private Object ProcessPracrLIN() {
		PracrLIN lin = new PracrLIN();
		
		DoubleField df = GetLatLonField(2);
		lin.bPinValid = df.valid;
		lin.dPinLat = df.d;

		df = GetLatLonField(4);
		lin.bPinValid = lin.bPinValid && df.valid;
		lin.dPinLon = df.d;
		
		df = GetLatLonField(7);
		lin.bCmteValid = df.valid;
		lin.dCmteLat = df.d;

		df = GetLatLonField(9);
		lin.bCmteValid = lin.bCmteValid && df.valid;
		lin.dCmteLon = df.d;

		return lin;
	}
	
	/* *************************************************************************************************
	$PSCIR,TST,HHMMSS,10*cc
	1    = UTC time stamp
	2    = Sequence counter
	3   = Checksum 
	***************************************************************************************************/
	/**
	 * PSCIR,TST message fields
	 */
	public static class PscirTST
	{
		public Date timeStamp;
		public boolean isTimeStampValid;
		public int  seqNo;
		public boolean isSeqNoValid;
	}
	
	private Object ProcessPscirTST() {
		PscirTST obj = new PscirTST();
		obj.isTimeStampValid = false;
		obj.isSeqNoValid = false;
		
		DateField time = GetTimeField(1);
		obj.isTimeStampValid = time.valid; 
		obj.timeStamp = time.d;
		
		IntegerField sequenceCount = GetIntegerField(2);
		obj.seqNo = sequenceCount.i;
		obj.isSeqNoValid = sequenceCount.valid;
		
		return obj;
	}
	
	
	public static class PmacrSev
	{
		public Date timeStamp;
		public String name;
		public int accuracy;
		public double x,y,z;
	}

	private Object ProcessPmacrSev()
	{
		PmacrSev obj = new PmacrSev();

		LongField time = GetLongField(1);
 
		if ( !time.valid )
			return null;
		
		obj.timeStamp = new Date ( time.l );
		obj.name = GetField(2);

		IntegerField acc = GetIntegerField(3);
		if ( !acc.valid )
			return null;
		obj.accuracy = acc.i;
		
		DoubleField df = GetDoubleField(4);
		if ( !df.valid )
			return null;
		obj.x = df.d;

		df = GetDoubleField(5);
		if ( !df.valid )
			return null;
		obj.y = df.d;

		df = GetDoubleField(6);
		if ( !df.valid )
			return null;
		obj.z = df.d;

		return obj;
	}


}
