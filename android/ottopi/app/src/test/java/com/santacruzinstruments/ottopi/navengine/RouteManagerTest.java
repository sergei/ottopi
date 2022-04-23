package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaEpochAssembler;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaParser;
import com.santacruzinstruments.ottopi.navengine.nmea.NmeaReader;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RouteManager;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Objects;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;


import junit.framework.TestCase;

public class RouteManagerTest extends TestCase implements RouteManager.RouteManagerListener {

	static class MgrEvent{
		public int rptIdx;
		UtcTime utc;
		RouteManager.ArrivalType arrivalType;
	}
	
	LinkedList<MgrEvent> mgrEvents;
	protected void setUp() throws Exception {
		super.setUp();
		mgrEvents = new LinkedList<>();
		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testFixedLeg() throws IOException{
		
		InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("Duxbury.gpx");
		assertNotNull ( is );
		RouteCollection rc = new RouteCollection("Duxbury");
		rc.loadFromGpx ( is );
		assertEquals(3, rc.getRoutes().size());

		Route route = rc.getRoutes().get(1);
		assertEquals("bay", route.getName());
		
		RouteManager mgr = new RouteManager();
		
		mgr.setRoute( route );
		
		RoutePoint rtpt = mgr.getActivePoint(); 
		assertNotNull(rtpt);
		assertEquals("A", rtpt.name);
		
		NavComputer nc = new NavComputer();
		nc.addListener(mgr);
		NmeaEpochAssembler na = new NmeaEpochAssembler();
		na.addInstrumentInputListener(nc);

		NmeaParser nmeaParser = new NmeaParser();
		nmeaParser.addListener(na);

		NmeaReader nmeaReader = new NmeaReader();
		nmeaReader.addListener(nmeaParser);

		mgr.addRouteManagerListener( this );
		
		GZIPInputStream gzIs = new GZIPInputStream(getClass().getClassLoader().getResourceAsStream("duxbury.nmea.gz"));
		byte [] buffer = new byte[2048];
		int nRead;
		
		while ( (nRead = gzIs.read(buffer, 0, buffer.length)) != -1 ){
			nmeaReader.read(buffer, nRead);
		}
		
		assertEquals(-1, nRead);
		assertNotNull(mgrEvents);
		assertEquals(8, mgrEvents.size());

		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("PST"));
		cal.set(2014, Calendar.MAY, 10, 8, 33, 50);
		// Time: Sat, 10 May 2014 08:33:05 PDT(-0700)
		
		MgrEvent evt = mgrEvents.get(0);
		assertEquals("YRA16-OFF", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);
		assertEquals(cal.getTime().toString(), evt.utc.getDate().toString());

		evt = mgrEvents.get(1);
		assertEquals("YRA17", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);

		evt = mgrEvents.get(2);
		assertEquals("YRA 7", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);
		
		evt = mgrEvents.get(3);
		assertEquals("RED RK", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);

		evt = mgrEvents.get(4);
		assertEquals("YRA18", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);
		
		evt = mgrEvents.get(5);
		assertEquals("YRA16", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);

		evt = mgrEvents.get(6);
		assertEquals("A", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);

		evt = mgrEvents.get(7); // Reported one more time as a last point
		assertEquals("A", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.PERPENDICULAR_PASSED, evt.arrivalType);

	}

	@Override
	public void onNewActivePoint(UtcTime utc, int rptIdx, RouteManager.ArrivalType arrivalType ) {
		MgrEvent e = new MgrEvent();
		e.rptIdx = rptIdx;
		e.utc = utc;
		e.arrivalType = arrivalType;
		mgrEvents.add(e);
		
	}

}
