package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import java.io.InputStream;
import java.util.Objects;

import junit.framework.TestCase;

public class RouteCollectionTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testLoadingGpx()
	{
		InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("SWIFT.gpx");
		assertNotNull ( is );
		
		RouteCollection rc = new RouteCollection("SWIFT");
		rc.loadFromGpx ( is );
		assertEquals(6, rc.getRoutes().size());
		
		Route route = rc.getRoutes().get(4);
		assertEquals("E", route.getName());
		assertEquals(7, route.getRptsNum());
		
		RoutePoint pt = route.getRpt(2);
		
		assertEquals("YRA18", pt.name);
		assertEquals(37.81833,   pt.loc.lat, 0.000001);
		assertEquals(-122.40333, pt.loc.lon, 0.000001);
		assertEquals(RoutePoint.LeaveTo.PORT, pt.leaveTo);
		assertEquals(RoutePoint.Type.ROUNDING, pt.type);

		route = rc.getRoutes().get(0);
		assertEquals("A", route.getName());
		assertEquals(5, route.getRptsNum());

		pt = route.getRpt(0);
		assertEquals("start-sw", pt.name);
		assertEquals(RoutePoint.LeaveTo.PORT, pt.leaveTo);
		assertEquals(RoutePoint.Type.START, pt.type);

	}

	public final void testLoadingLoosePts()
	{
		InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("wns-wpt-only.gpx");
		assertNotNull ( is );

		RouteCollection rc = new RouteCollection("WNS");
		rc.loadFromGpx ( is );
		assertEquals(1, rc.getRoutes().size());

		Route route = rc.getRoutes().get(0);
		assertEquals("Misc Points", route.getName());
		assertEquals(12, route.getRptsNum());

		RoutePoint pt = route.getRpt(0);

		assertEquals("GOV", pt.name);
		assertEquals(36.95800,   pt.loc.lat, 0.000001);
		assertEquals(-122.012166667, pt.loc.lon, 0.000001);
		assertEquals(RoutePoint.LeaveTo.PORT, pt.leaveTo);
		assertEquals(RoutePoint.Type.ROUNDING, pt.type);

	}
}
