package com.santacruzinstruments.ottopi.navengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.santacruzinstruments.ottopi.data.StartLineInfo;
import com.santacruzinstruments.ottopi.navengine.geo.ClockProvider;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Distance;
import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.route.Route;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import junit.framework.TestCase;

import org.junit.BeforeClass;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

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

    public final void testDistanceToRabbitLine(){
        Instant now = Instant.ofEpochMilli(1624365735681L);
        ClockProvider.setsClock(Clock.fixed(now, ZoneId.of("UTC")));


        // 2023/06/22 12:22:15.681 MSG,onNavComputerOutput,NavComputerOutput,ii,utc,01:12:46,loc,36.95275,-121.99639,cog,311,sog,5.5,mag,289,sow,5.3,awa,-23,aws,16.6,tws,11.8,twa,-33,twd,252,sot,1.6,destName,Schuyler,atm,-67,dtm,1.2,nextDestName,HARBOR,nextLegTwa,-144,medianPortTwa,-35,portTwaIqr,008,medianStbdTwa,103,stbdTwaIqr,083
        GeoLoc pinLoc = new GeoLoc(  36.9509354, -121.99406429999999);
        GeoLoc rabbitLoc = new GeoLoc(  36.951480865478516, -121.99494934082031);
        // Now set the boat position
        GeoLoc boat = new GeoLoc( 36.95275, -121.99639);
        Direction twd = new Direction(252);

        StartLineComputer startLineComputer = new StartLineComputer();
        Route route = new Route();

        RoutePoint pin = new RoutePoint.Builder().loc(pinLoc).name("PIN")
                .type(RoutePoint.Type.START).leaveTo(RoutePoint.LeaveTo.PORT).build();
        RoutePoint rcb = new RoutePoint.Builder().loc(rabbitLoc).name("Rabbit")
                .type(RoutePoint.Type.START).leaveTo(RoutePoint.LeaveTo.STARBOARD).build();
        route.addRpt(pin);
        route.addRpt(rcb);

        StartLineInfo  si = startLineComputer.setRoute(route);
        assertTrue(si.pin.isValid());
        assertTrue(si.rcb.isValid());

        si = startLineComputer.updateStartLineInfo(boat, twd, true);
        assertTrue(si.distToLine.isValid());
        assertEquals(33., si.distToLine.toMeters(), 1.);
        assertTrue(si.pinFavoredBy.isValid());
        assertFalse(si.isOcs);


        GeoLoc ocsBoat = new GeoLoc(  36.951084, -121.994605);
        si = startLineComputer.updateStartLineInfo(ocsBoat, twd, true);
        assertTrue(si.isOcs);

    }

}
