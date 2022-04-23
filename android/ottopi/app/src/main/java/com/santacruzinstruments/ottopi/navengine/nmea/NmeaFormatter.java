package com.santacruzinstruments.ottopi.navengine.nmea;

import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.data.StartLineInfo;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
		int deg = (int)Math.abs(coord);
		double minutes = (Math.abs(coord) - deg) * 60;
		String sign;
		if( isLat )
			sign = coord > 0 ? "N" : "S";
		else
			sign = coord > 0 ? "E" : "W";
		return String.format(Locale.US, "%d.%.5f,%s", deg, minutes, sign);
	}
}
