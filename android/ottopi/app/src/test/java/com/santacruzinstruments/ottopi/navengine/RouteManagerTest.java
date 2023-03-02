package com.santacruzinstruments.ottopi.navengine;

import androidx.annotation.NonNull;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaEpochAssembler;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaParser;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaReader;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RouteManager;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;


import junit.framework.TestCase;

public class RouteManagerTest extends TestCase {

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

		mgr.addRouteManagerListener(new RouteManager.RouteManagerListener() {
			@Override
			public void onNewActivePoint(UtcTime utc, int rptIdx, RouteManager.ArrivalType arrivalType) {
				final RoutePoint rpt = route.getRpt(rptIdx);
				System.out.printf("%s New active point: %s (id=%d) %s\n", utc, rpt.name, rpt.id, arrivalType);
				MgrEvent e = new MgrEvent();
				e.rptIdx = rptIdx;
				e.utc = utc;
				e.arrivalType = arrivalType;
				mgrEvents.add(e);
			}

			@Override
			public void onMarkLocationDetermined( List<RoutePoint> updatedRpts) {
			}
		});
		
		GZIPInputStream gzIs = new GZIPInputStream(getClass().getClassLoader().getResourceAsStream("duxbury.nmea.gz"));
		byte [] buffer = new byte[2048];
		int nRead;
		
		while ( (nRead = gzIs.read(buffer, 0, buffer.length)) != -1 ){
			nmeaReader.read(buffer, nRead);
		}
		
		assertEquals(-1, nRead);
		assertNotNull(mgrEvents);
		assertEquals(7, mgrEvents.size());

		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("PST"));
		cal.set(2014, Calendar.MAY, 10, 8, 33, 50);
		// Time: Sat, 10 May 2014 08:33:05 PDT(-0700)
		
		MgrEvent evt = mgrEvents.get(0);
		assertEquals("YRA16-OFF", route.getRpt(evt.rptIdx).name);
		assertEquals(RouteManager.ArrivalType.CIRCLE_ENTERED, evt.arrivalType);

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

	}

	public void testInflatableLeg() throws IOException{
		runInflatableTest("set1");
		runInflatableTest("set2");
		runInflatableTest("set3");
		runInflatableTest("set4");
		runInflatableTest("set5");
		runInflatableTest("set6");
	}

	private void runInflatableTest(String setName) throws IOException {
		MarkDetectorTest.KmzFile kmzFile = new MarkDetectorTest.KmzFile("mark-detection/" + setName + "/set.kmz");
		String nmeaFile = "mark-detection/" + setName + "/race.nmea.gz";

		Route knownRoute = readRoute(kmzFile);
		// From the known route create route with unknown location (except start line)
		Route inflatableRoute = new Route();
		for( RoutePoint pt : knownRoute){
			if ( pt.type == RoutePoint.Type.START){
				inflatableRoute.addRpt(pt);
			}else{
				String name = pt.name.substring(0, 2);  // Use only two first letters, so both WM1 and WM2 became WM
				RoutePoint ipt = new RoutePoint.Builder().copy(pt).name(name).loc(GeoLoc.INVALID).build();
				inflatableRoute.addRpt(ipt);
			}
		}
		RouteManager mgr = new RouteManager();
		inflatableRoute.makeActiveWpt(2);
		mgr.setRoute(inflatableRoute);

		final int[] foundMarksCount = {0};
		final int[] activeCount = {2};
		mgr.addRouteManagerListener(new RouteManager.RouteManagerListener() {
			@Override
			public void onNewActivePoint(UtcTime utc, int rptIdx, RouteManager.ArrivalType arrivalType) {
				activeCount[0]++;
				final RoutePoint rpt = inflatableRoute.getRpt(rptIdx);
				System.out.printf("%s: %s New active point: %s (id=%d) %s\n", setName, utc, rpt.name, rpt.id, arrivalType);
				assertEquals(activeCount[0], rptIdx);
			}

			@Override
			public void onMarkLocationDetermined(List<RoutePoint> updatedRpts) {
				foundMarksCount[0]++;
				System.out.printf("%s: Detected point: %s\n", setName, updatedRpts.get(0).name);

				for( RoutePoint rpt : updatedRpts){
					// Compare mark with the same id in the fixed route
					boolean matchFound = false;
					for( RoutePoint fixedMark: knownRoute){
						if ( fixedMark.id == rpt.id ){
							assertEquals(fixedMark.name.substring(0,2), rpt.name.substring(0,2));
							GeoLoc expectedLoc = fixedMark.loc;
							double dist = expectedLoc.distTo(rpt.loc).toMeters();
							assertTrue(
									String.format(Locale.getDefault(),"Failed detection #%d: distance to mark %s is %f",
											foundMarksCount[0], rpt.name, dist),
									dist < 80 );
							matchFound = true;
							break;
						}
					}
					assertTrue(String.format("%s: No match found for id %d", setName, rpt.id), matchFound);
				}
			}
		});

		mgr.startMarkDetection();
		feedNmea(mgr, nmeaFile);
		mgr.stopMarkDetection();

		assertEquals(2, foundMarksCount[0]);
	}

	private void feedNmea(RouteManager mgr, String nmeaFile) throws IOException {
		NavComputer nc = new NavComputer();
		nc.addListener(mgr);
		NmeaEpochAssembler na = new NmeaEpochAssembler();
		na.addInstrumentInputListener(nc);

		NmeaParser nmeaParser = new NmeaParser();
		nmeaParser.addListener(na);

		NmeaReader nmeaReader = new NmeaReader();
		nmeaReader.addListener(nmeaParser);

		GZIPInputStream gzIs = new GZIPInputStream(Objects.requireNonNull(getClass()
				.getClassLoader()).getResourceAsStream(nmeaFile));

		byte [] buffer = new byte[2048];
		int nRead;

		while ( (nRead = gzIs.read(buffer, 0, buffer.length)) != -1 ){
			nmeaReader.read(buffer, nRead);
		}
	}

	@NonNull
	static Route readRoute(MarkDetectorTest.KmzFile kmz) {
		Route route = new Route();
		for( RoutePoint pt : kmz.marks){
			route.addRpt(pt);
		}
		return route;
	}

}
