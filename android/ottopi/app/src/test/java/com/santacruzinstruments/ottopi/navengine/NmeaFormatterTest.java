package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaFormatter;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaParser;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

public class NmeaFormatterTest extends TestCase {

	NmeaParser mParser;

	protected void setUp() throws Exception {
		super.setUp();
		mParser = new NmeaParser();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void test_formatInstrumentInput() {

		UtcTime utc = new UtcTime(new Date(1680299682525L));
		final double lat = 37.123456;
		final double lon = -122.123456;
		final double sog = 5.1;
		final double cog = 255.1;
		final double mag = 271.3;
		final double sow = 12.1;
		final double awa = -37.1;
		final double aws = 14.1;
		InstrumentInput ii = new InstrumentInput.Builder()
				.utc(utc)
				.loc(new GeoLoc(lat, lon))
				.sog(new Speed(sog))
				.cog(new Direction(cog))
				.mag(new Direction(mag))
				.sow(new Speed(sow))
				.awa(new Angle(awa))
				.aws(new Speed(aws))
				.build();

		LinkedList<String> msgs = new LinkedList<>();
		NmeaFormatter.formatInstrumentInput(ii, msgs);

		assertEquals(3, msgs.size());
		for(String nmea : msgs){
			if ( nmea.contains("RMC")){
				Object obj = mParser.Parse(nmea);
				assertNotNull(obj);
				assertTrue(obj instanceof NmeaParser.RMC);
				NmeaParser.RMC rmc = (NmeaParser.RMC)obj;
				assertTrue(rmc.bTimestampValid);
				assertEquals(utc.getDate().getTime()/100, rmc.dtTimestamp.getTime()/100);
				assertTrue(rmc.bPosValid);
				assertEquals(lat, rmc.dLat, 0.00001);
				assertEquals(lon, rmc.dLon, 0.00001);
				assertTrue(rmc.bSpeedValid);
				assertEquals(sog, rmc.dSpeedOverGround);
				assertTrue(rmc.bCourseValid);
				assertEquals(cog, rmc.dCourseOverGround);
			}
			else if( nmea.contains("VHW")){
				Object obj = mParser.Parse(nmea);
				assertNotNull(obj);
				assertTrue(obj instanceof NmeaParser.VHW);
				NmeaParser.VHW vhw = (NmeaParser.VHW)obj;
				assertTrue(vhw.bHdgMagValid);
				assertEquals(mag, vhw.dHdgMagDeg, 0.1);
				assertTrue(vhw.bSpeedKtsValid);
				assertEquals(sow, vhw.dSpeedKts, 0.1);
			}
			else if( nmea.contains("VWR")){
				Object obj = mParser.Parse(nmea);
				assertNotNull(obj);
				assertTrue(obj instanceof NmeaParser.VWR);
				NmeaParser.VWR vwr = (NmeaParser.VWR)obj;
				assertTrue(vwr.bRelWindAngleDegValid);
				assertEquals(awa, vwr.dRelWindAngleDeg, 0.1);
				assertTrue(vwr.bRelWindSpeedKtsValid);
				assertEquals(aws, vwr.dRelWindSpeedKts, 0.1);
			}
		}

	}

	public final void test_formatInstrumentEmpty() {
		InstrumentInput ii = new InstrumentInput.Builder()
				.build();

		LinkedList<String> msgs = new LinkedList<>();
		NmeaFormatter.formatInstrumentInput(ii, msgs);

		assertEquals(3, msgs.size());
		for(String nmea : msgs){
			if ( nmea.contains("RMC")){
				Object obj = mParser.Parse(nmea);
				assertNotNull(obj);
				assertTrue(obj instanceof NmeaParser.RMC);
				NmeaParser.RMC rmc = (NmeaParser.RMC)obj;
				assertFalse(rmc.bPosValid);
				assertFalse(rmc.bSpeedValid);
				assertFalse(rmc.bCourseValid);
			}
			else if( nmea.contains("VHW")){
				Object obj = mParser.Parse(nmea);
				assertNotNull(obj);
				assertTrue(obj instanceof NmeaParser.VHW);
				NmeaParser.VHW vhw = (NmeaParser.VHW)obj;
				assertFalse(vhw.bHdgMagValid);
				assertFalse(vhw.bSpeedKtsValid);
			}
			else if( nmea.contains("VWR")){
				Object obj = mParser.Parse(nmea);
				assertNotNull(obj);
				assertTrue(obj instanceof NmeaParser.VWR);
				NmeaParser.VWR vwr = (NmeaParser.VWR)obj;
				assertFalse(vwr.bRelWindAngleDegValid);
				assertFalse(vwr.bRelWindSpeedKtsValid);
			}
		}
	}

	public final void testParse_VHW() {
		String nmea = "$IIVHW,,,125,M,00.0,N,,*62\r\n";

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.VHW);
		NmeaParser.VHW o = (NmeaParser.VHW)obj;

		assertFalse(o.bHdgTrueValid);

		assertTrue(o.bHdgMagValid);
		assertEquals(125, o.dHdgMagDeg, 0.5);

		assertTrue(o.bSpeedKtsValid);
		assertEquals(0, o.dSpeedKts, 0.5);
		
		// String from B&G log 
		nmea = "$IIVHW,,T,353,M,00.00,N,00.00,K*60";
		obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.VHW);
		o = (NmeaParser.VHW)obj;

		assertFalse(o.bHdgTrueValid);

		assertTrue(o.bHdgMagValid);
		assertEquals(353, o.dHdgMagDeg, 0.5);
	}

	public final void testParse_VWR_Left() {
		String left = "$IIVWR,060,L,15.7,N,,,,*7A\r\n";

		Object obj = mParser.Parse(left);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.VWR);
		NmeaParser.VWR o = (NmeaParser.VWR)obj;

		assertTrue(o.bRelWindAngleDegValid);
		assertEquals(-60, o.dRelWindAngleDeg, 0.5);

		assertTrue(o.bRelWindSpeedKtsValid);
		assertEquals(15.7, o.dRelWindSpeedKts, 0.1);
	}

	public final void testParse_VWR_Right() {
		String right = "$IIVWR,023,R,08.8,N,04.5,M,016.2,K*52\r\n";

		Object obj = mParser.Parse(right);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.VWR);
		NmeaParser.VWR o = (NmeaParser.VWR)obj;

		assertTrue(o.bRelWindAngleDegValid);
		assertEquals(23, o.dRelWindAngleDeg, 0.5);

		assertTrue(o.bRelWindSpeedKtsValid);
		assertEquals(8.8, o.dRelWindSpeedKts, 0.1);

	}
	public final void testParse_MWV_Relative() {

		String nmea = "$IIMWV,022,R,19.9,N,A*12\r\n";

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.MWV);
		NmeaParser.MWV o = (NmeaParser.MWV)obj;

		assertTrue(o.bIsRelative);

		assertTrue(o.bWindAngleDegValid);
		assertEquals(22, o.dWindAngleDeg, 0.5);

		assertTrue(o.bWindSpeedKtsValid);
		assertEquals(19.9, o.dWindSpeedKts, 0.1);
	}
	public final void testParse_MWV_True() {

		String nmea = "$IIMWV,016,T,15.6,N,A*10\r\n";

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.MWV);
		NmeaParser.MWV o = (NmeaParser.MWV)obj;

		assertFalse(o.bIsRelative);

		assertTrue(o.bWindAngleDegValid);
		assertEquals(16, o.dWindAngleDeg, 0.5);

		assertTrue(o.bWindSpeedKtsValid);
		assertEquals(15.6, o.dWindSpeedKts, 0.1);
	}

	public final void testParse_HDG() {

		String nmea = "$IIHDG,279,,,,*5B\r\n";

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.HDG);
		NmeaParser.HDG o = (NmeaParser.HDG)obj;

		assertTrue(o.bMagSensHeadDegValid);
		assertEquals(279, o.dMagSensHeadDeg, 0.5);

	}

	public final void testParse_RMC_Valid() {

		String nmea = "$GPRMC,013213.17,A,3657.272849,N,12159.878474,W,006.7,052.0,160409,,*22\r\n";
		GregorianCalendar mcal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		mcal.set(2009, Calendar.APRIL, 16, 1, 32, 13);
		mcal.set(Calendar.MILLISECOND, 170);
		Date expectedDate = mcal.getTime();

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.RMC);
		NmeaParser.RMC o = (NmeaParser.RMC)obj;

		assertTrue(o.bTimestampValid);
		assertEquals(expectedDate, o.dtTimestamp);

		assertTrue(o.bPosValid);
		assertEquals(36 + 57.272849/60., o.dLat,     0.00001);
		assertEquals(-(121 + 59.878474/60.), o.dLon, 0.00001);
		assertEquals(6.7,  o.dSpeedOverGround, 0.1);
		assertEquals(52.0, o.dCourseOverGround, 0.1);
		assertFalse(o.bMagVarValid);
	}

	
	public final void testParse_RACR_STR(){
		String nmea = "$PRACR,STR,203942,110114\r\n";
		GregorianCalendar mcal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		mcal.set(2014, Calendar.JANUARY, 11, 20, 39, 42);
		mcal.set(Calendar.MILLISECOND, 0);
		Date expectedDate = mcal.getTime();

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.PracrSTR);
		NmeaParser.PracrSTR o = (NmeaParser.PracrSTR)obj;
		assertTrue(o.bTimestampValid);
		assertEquals(expectedDate, o.dtTimestamp);
	}
	
	public final void testParse_RACR_LIN(){
		String nmea = "$PRACR,LIN,PIN,3752.763200,N,12223.143100,W,CMTE,3752.812300,N,12223.347600,W\r\n";

		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.PracrLIN);
		NmeaParser.PracrLIN o = (NmeaParser.PracrLIN)obj;

		assertTrue(o.bPinValid);
		assertTrue(o.bCmteValid);
		
		assertEquals(37 + 52.7632/60., o.dPinLat,     0.00001);
		assertEquals(-(122 + 23.1431/60.), o.dPinLon, 0.00001);

		assertEquals(37 + 52.8123/60., o.dCmteLat,     0.00001);
		assertEquals(-(122 + 23.3476/60.), o.dCmteLon, 0.00001);
		
	}

	
	public final void testParse_SCIR_TST(){
		String nmea = "$PSCIR,TST,141516,10*23\r\n";
		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.PscirTST);
		NmeaParser.PscirTST msg = (NmeaParser.PscirTST) obj;
		assertTrue(msg.isTimeStampValid);
		assertTrue(msg.isSeqNoValid);
		assertEquals(10, msg.seqNo);
		
	}	

	public final void testParse_PMACR_SEV(){
		String nmea = "$PMACR,SEV,1417978397135,ACCEL,3, -0.407,  0.124,  9.906\r\n";
		Object obj = mParser.Parse(nmea);
		assertNotNull(obj);
		assertTrue(obj instanceof NmeaParser.PmacrSev);
		NmeaParser.PmacrSev sev = (NmeaParser.PmacrSev) obj;
		
		GregorianCalendar mcal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		mcal.set(2014, Calendar.DECEMBER, 7, 18, 53, 17);
		mcal.set(Calendar.MILLISECOND, 135);
		Date expectedDate = mcal.getTime();
		assertEquals(expectedDate, sev.timeStamp);
		assertEquals("ACCEL",sev.name);
		assertEquals(3,sev.accuracy);
		
		assertEquals(-0.407, sev.x, 1e-4);
		assertEquals( 0.124, sev.y, 1e-4);
		assertEquals( 9.906, sev.z, 1e-4);
	}
	
}
