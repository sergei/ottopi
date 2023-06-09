package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.MagDecl;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import junit.framework.TestCase;

public class LegComputerTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testDirections() {

        LegComputer legComputer = new LegComputer();
        assertFalse(legComputer.atm.isValid());
        assertFalse(legComputer.dtm.isValid());
        assertFalse(legComputer.nextLegTwa.isValid());

        RoutePoint dest = new RoutePoint.Builder().loc(new GeoLoc( 37.811229, -122.466103)).name("dest")
            .type(RoutePoint.Type.ROUNDING).leaveTo(RoutePoint.LeaveTo.PORT).build();

        RoutePoint next = new RoutePoint.Builder().loc(new GeoLoc( 37.818154, -122.443944)).name("next")
            .type(RoutePoint.Type.ROUNDING).leaveTo(RoutePoint.LeaveTo.PORT).build();

        GeoLoc boat = new GeoLoc( 37.810517, -122.450527);
        Direction mag = new Direction(240);
        Direction twd = new Direction(270);

        legComputer.setDestinations(dest, next);
        legComputer.update(boat, mag, twd);

        assertTrue(legComputer.atm.isValid());
        assertEquals("dest", legComputer.destName);
        assertEquals(20., legComputer.atm.toDegrees(), 1);

        assertTrue(legComputer.dtm.isValid());
        assertEquals(1370, legComputer.dtm.toMeters(), 1);

        assertTrue(legComputer.nextLegTwa.isValid());
        assertEquals("next", legComputer.nextDestName);
        assertEquals(-144, legComputer.nextLegTwa.toDegrees(), 2);
    }

    public final void testTacking() {
        LegComputer legComputer = new LegComputer();

        RoutePoint dest = new RoutePoint.Builder().loc(new GeoLoc( 37, -122)).name("dest")
                .type(RoutePoint.Type.ROUNDING).leaveTo(RoutePoint.LeaveTo.PORT).build();

        Direction mag = Direction.INVALID;
        legComputer.setDestinations(dest, RoutePoint.INVALID);

        Direction twd = new Direction(MagDecl.getInstance().fromTrueToMag(0));

        // Place boat DDW of the mark, so mwa will be zero
        GeoLoc boat = new GeoLoc( 36, -122);
        legComputer.update(boat, mag, twd);

        // Now place boat, so mark is on port beam reach
        boat = new GeoLoc( 37, -123);
        legComputer.update(boat, mag, twd);

        // Now place boat, so mark is on starboard beam reach
        boat = new GeoLoc( 37, -121);
        legComputer.update(boat, mag, twd);

        // Now place boat, so mark is DDW
        boat = new GeoLoc( 38, -122);
        legComputer.update(boat, mag, twd);

    }

}
