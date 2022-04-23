package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import junit.framework.TestCase;

public class StartLineComputerTest extends TestCase {
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testStartLine() {
        StartLineComputer startLineComputer = new StartLineComputer();

        // No start line
        Route route = new Route();
        StartLineInfo  si = startLineComputer.setRoute(route);
        assertFalse(si.pin.isValid());
        assertFalse(si.rcb.isValid());
        assertFalse(si.distToLine.isValid());
        assertFalse(si.pinFavoredBy.isValid());

        // Create start line
        RoutePoint pin = new RoutePoint(new GeoLoc( 37.858000, -122.382092), "PIN",
                RoutePoint.Type.START_PORT, RoutePoint.LeaveTo.PORT, RoutePoint.Location.KNOWN);
        RoutePoint rcb = new RoutePoint(new GeoLoc(  37.861112, -122.377744), "RCB",
                RoutePoint.Type.START_STBD, RoutePoint.LeaveTo.STARBOARD, RoutePoint.Location.KNOWN);
        route.addRpt(pin);
        route.addRpt(rcb);

        si = startLineComputer.setRoute(route);
        assertTrue(si.pin.isValid());
        assertTrue(si.rcb.isValid());

        // Now set the boat position
        GeoLoc boat = new GeoLoc( 37.857947, -122.378670);
        Direction twd = new Direction();
        si = startLineComputer.updateStartLineInfo(boat, twd);
        assertTrue(si.distToLine.isValid());
        assertEquals(206., si.distToLine.toMeters(), 1.);
        assertFalse(si.pinFavoredBy.isValid());
        assertFalse(si.isOcs);

        // Now set the wind
        // The stat line direction ( from rcb to pin is 214 degrees )

        twd = new Direction(214);
        si = startLineComputer.updateStartLineInfo(boat, twd);
        assertTrue(si.pinFavoredBy.isValid());
        assertEquals(90, si.pinFavoredBy.toDegrees(), 1.);

        twd = new Direction(214 + 90);
        si = startLineComputer.updateStartLineInfo(boat, twd);
        assertTrue(si.pinFavoredBy.isValid());
        assertEquals(0, si.pinFavoredBy.toDegrees(), 1.);

        twd = new Direction(214 + 60);
        si = startLineComputer.updateStartLineInfo(boat, twd);
        assertTrue(si.pinFavoredBy.isValid());
        assertEquals(30, si.pinFavoredBy.toDegrees(), 1.);

        // Test OCS
        GeoLoc ocs = new GeoLoc(  37.862035, -122.381071);
        twd = new Direction(214 + 90);
        si = startLineComputer.updateStartLineInfo(ocs, twd);
        assertTrue(si.isOcs);

        // Now set the route again, with rcb deleted
        route = new Route();
        route.addRpt(pin);
        startLineComputer.setRoute(route);
        si = startLineComputer.updateStartLineInfo(ocs, twd);
        assertTrue(si.pin.isValid());
        assertFalse(si.rcb.isValid());
        assertFalse(si.pinFavoredBy.isValid());
        assertFalse(si.distToLine.isValid());
    }
}
