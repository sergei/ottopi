package com.santacruzinstruments.ottopi.navengine.nmea0183;

import static java.lang.Math.abs;

import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.navengine.InstrumentInput;
import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class NmeaFormatter {

	private static final GregorianCalendar sCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));


	// $PRACR,STR,203942,110114

	/* *************************************************************************************************
	$PRACR,STR,hhmmss,ddmmyy*hh
	1    = UTC time of race start
	2    = UTC date of race start
	3   = Checksum
	***************************************************************************************************/
	public static String fmtSTR(long startTime) {
		sCal.setTime(new Date(startTime));
		String s = String.format(Locale.US,"PRACR,STR,%02d%02d%02d,,%02d%02d%02d"
				,sCal.get(Calendar.HOUR_OF_DAY)
				,sCal.get(Calendar.MINUTE)
				,sCal.get(Calendar.SECOND)
				,sCal.get(Calendar.YEAR)
				,sCal.get(Calendar.MONTH)
				,sCal.get(Calendar.DAY_OF_MONTH)
		);
		return makeNmea(s);

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
	public static String fmtLin(StartLineInfo si) {

		String s = String.format(Locale.US,"PRACR,LIN,PIN,%s,%s,%s,%s"
			,si.pin.isValid() ? formatCoords(si.pin.lat, true) : ","
			,si.pin.isValid() ? formatCoords(si.pin.lon, false) : ","
			,si.rcb.isValid() ? formatCoords(si.rcb.lat, true) : ","
			,si.rcb.isValid() ? formatCoords(si.rcb.lon, false) : ","
		);

		return makeNmea(s);
	}

	@NonNull
	private static String makeNmea(String s) {
		byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
		byte cc = 0;
		for (byte b : bytes) {
			cc ^= b;
		}

		return String.format(Locale.US,"$%s*%02X\r\n", s, cc);
	}

	private static String formatCoords(double coord, boolean isLat){
		int deg = (int) abs(coord);
		double minutes = (abs(coord) - deg) * 60;
		String sign;
		if( isLat ) {
			sign = coord > 0 ? "N" : "S";
			return String.format(Locale.US, "%02d%08.5f,%s", deg, minutes, sign);
		}
		else {
			sign = coord > 0 ? "E" : "W";
			return String.format(Locale.US, "%03d%08.5f,%s", deg, minutes, sign);
		}
	}

	public static void formatInstrumentInput(final InstrumentInput ii, List<String> msgs){
		String vwr = formatVwr(ii.awa, ii.aws);
		msgs.add(vwr);
		String vhw = formatVhw(ii.sow, ii.mag);
		msgs.add(vhw);
		String rmc = formatRmc(ii.utc, ii.loc, ii.sog, ii.cog );
		msgs.add(rmc);
	}

	/*
		Recommended minimum specific GPS/Transit data

		eg4. $GPRMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,ddmmyy,x.x,a*hh
		1    = UTC of position fix
		2    = Data status (V=navigation receiver warning)
		3    = Latitude of fix
		4    = N or S
		5    = Longitude of fix
		6    = E or W
		7    = Speed over ground in knots
		8    = Track made good in degrees True
		9    = UT date
		10   = Magnetic variation degrees (Easterly var. subtracts from true course)
		11   = E or W
		12   = Checksum
	 */
	private static String formatRmc(UtcTime utc, GeoLoc loc, Speed sog, Direction cog) {
		sCal.setTime(utc.getDate());
		String s = String.format(Locale.US,"GPRMC,%02d%02d%02d.%02d,%s,%s,%s,%s,%s,%02d%02d%02d,,"
				,sCal.get(Calendar.HOUR_OF_DAY)
				,sCal.get(Calendar.MINUTE)
				,sCal.get(Calendar.SECOND)
				,sCal.get(Calendar.MILLISECOND) / 10
				,loc.isValid() ? "A": "V"
				,loc.isValid() ? formatCoords(loc.lat, true) : ","
				,loc.isValid() ? formatCoords(loc.lon, false) : ","
				,sog.isValid() ? String.format(Locale.US, "%03.1f", sog.getKnots()) : ""
				,cog.isValid() ? String.format(Locale.US, "%03.1f", cog.toDegrees()) : ""
				,sCal.get(Calendar.DAY_OF_MONTH)
				,sCal.get(Calendar.MONTH) + 1
				,sCal.get(Calendar.YEAR) % 100
		);
		return makeNmea(s);
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

	private static String formatVhw(Speed sow, Direction mag) {
		String s = String.format(Locale.US,"SCVHW,,T,%s,M,%s,N,,K"
			,mag.isValid() ? String.format(Locale.US, "%03.1f", mag.toDegrees()) : ""
			,sow.isValid() ? String.format(Locale.US, "%03.1f", sow.getKnots()) : ""
		);
		return makeNmea(s);
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
	private static String formatVwr(Angle awa, Speed aws) {
		String s = String.format(Locale.US,"SCVWR,%s,%s,%s,N,,M,,K"
				,awa.isValid() ? String.format(Locale.US, "%03.1f", abs(awa.toDegrees())) : ""
				,awa.toDegrees() < 0 ? "L" : "R"
				,aws.isValid() ? String.format(Locale.US, "%03.1f", aws.getKnots()) : ""
		);
		return makeNmea(s);
	}

}
