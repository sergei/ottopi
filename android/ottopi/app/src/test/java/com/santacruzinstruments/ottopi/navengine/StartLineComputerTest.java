package com.santacruzinstruments.ottopi.navengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import junit.framework.TestCase;

import org.junit.BeforeClass;

import timber.log.Timber;

public class StartLineComputerTest extends TestCase {
    @BeforeClass
    public static void SetupTimber(){
        Timber.plant(new Timber.Tree() {
            @Override
            protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
                System.out.println(message);
            }
        });
    }

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
        RoutePoint pin = new RoutePoint.Builder().loc(new GeoLoc( 37.858000, -122.382092)).name("PIN")
            .type(RoutePoint.Type.START).leaveTo(RoutePoint.LeaveTo.PORT).build();
        RoutePoint rcb = new RoutePoint.Builder().loc(new GeoLoc( 37.861112, -122.377744)).name("RCB")
                .type(RoutePoint.Type.START).leaveTo(RoutePoint.LeaveTo.STARBOARD).build();
        route.addRpt(pin);
        route.addRpt(rcb);

        si = startLineComputer.setRoute(route);
        assertTrue(si.pin.isValid());
        assertTrue(si.rcb.isValid());

        // Now set the boat position
        GeoLoc boat = new GeoLoc( 37.857947, -122.378670);
        Direction twd = new Direction();
        si = startLineComputer.updateStartLineInfo(boat, twd, true);
        assertTrue(si.distToLine.isValid());
        assertEquals(206., si.distToLine.toMeters(), 1.);
        assertFalse(si.pinFavoredBy.isValid());
        assertFalse(si.isOcs);

        // Now set the wind
        // The stat line direction ( from rcb to pin is 214 degrees )

        twd = new Direction(214);
        si = startLineComputer.updateStartLineInfo(boat, twd, true);
        assertTrue(si.pinFavoredBy.isValid());
        assertEquals(90, si.pinFavoredBy.toDegrees(), 1.);

        twd = new Direction(214 + 90);
        si = startLineComputer.updateStartLineInfo(boat, twd, true);
        assertTrue(si.pinFavoredBy.isValid());
        assertEquals(0, si.pinFavoredBy.toDegrees(), 1.);

        twd = new Direction(214 + 60);
        si = startLineComputer.updateStartLineInfo(boat, twd, true);
        assertTrue(si.pinFavoredBy.isValid());
        assertEquals(30, si.pinFavoredBy.toDegrees(), 1.);

        // Test OCS
        GeoLoc ocs = new GeoLoc(  37.862035, -122.381071);
        twd = new Direction(214 + 90);
        si = startLineComputer.updateStartLineInfo(ocs, twd, true);
        assertTrue(si.isOcs);

        // Now set the route again, with rcb deleted
        route = new Route();
        route.addRpt(pin);
        startLineComputer.setRoute(route);
        si = startLineComputer.updateStartLineInfo(ocs, twd, true);
        assertTrue(si.pin.isValid());
        assertFalse(si.rcb.isValid());
        assertFalse(si.pinFavoredBy.isValid());
        assertFalse(si.distToLine.isValid());
    }

    public final void testRabbitStartLine(){
        GeoLoc pinLoc = new GeoLoc(  37.824690, -122.412750);
        Direction rabbitDir = new Direction(-14);  // True north
        Distance startLineLen = new Distance(100. /1852.);  // 100 meters
        GeoLoc rabbitLoc =  pinLoc.project(startLineLen, rabbitDir);
        Timber.d("Pin %s, rabbitDir %s, rabbitLoc %s", pinLoc, rabbitDir, rabbitLoc);

        Direction twd = new Direction(270 - 14);
        rabbitLoc = NavComputer.computeRabbitLoc(pinLoc, twd);
        Timber.d("Pin %s, rabbitDir %s, rabbitLoc %s", pinLoc, rabbitDir, rabbitLoc);
    }

}
