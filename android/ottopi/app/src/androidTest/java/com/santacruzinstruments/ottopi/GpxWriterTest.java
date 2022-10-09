package com.santacruzinstruments.ottopi;

import com.santacruzinstruments.ottopi.navengine.geo.GeoLoc;
import com.santacruzinstruments.ottopi.navengine.geo.UtcTime;
import com.santacruzinstruments.ottopi.navengine.route.GpxBuilder;
import com.santacruzinstruments.ottopi.navengine.route.RouteCollection;
import com.santacruzinstruments.ottopi.navengine.route.RoutePoint;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class GpxWriterTest  extends TestCase {

    public void testWriteRoute() throws IOException {

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        GpxBuilder gpxWriter = new GpxBuilder();

        // Write route point
        RoutePoint startPt = new RoutePoint.Builder()
                .loc(new GeoLoc( 37.811229, -122.466103))
                .name("RCP")
                .type(RoutePoint.Type.START)
                .leaveTo(RoutePoint.LeaveTo.STARBOARD)
                .time(new UtcTime(new Date()))
                .build();
        gpxWriter.addPoint(startPt, writer);

        // GPS writer writes all marks added before, so we need to reset the output stream
        writer.flush();
        outputStream.reset();

        RoutePoint roundingPt = new RoutePoint.Builder()
                .loc(new GeoLoc( 37.811229, -122.466103))
                .name("pt1")
                .type(RoutePoint.Type.ROUNDING)
                .leaveTo(RoutePoint.LeaveTo.PORT)
                .time(new UtcTime(new Date()))
                .build();
        gpxWriter.addPoint(roundingPt, writer);

        String gpxString = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        System.out.printf("[%s]\n", gpxString);
        // Now read it back
        InputStream is = new ByteArrayInputStream(outputStream.toByteArray());

        RouteCollection rc = new RouteCollection("");
        rc.loadFromGpx ( is );
        assertEquals(1, rc.getRoutes().size());

        assertEquals(2, rc.getRoutes().get(0).getRptsNum());

        RoutePoint readPt = rc.getRoutes().get(0).getRpt(0);
        assertEquals(startPt.name, readPt.name);
        assertEquals(startPt.loc.lat, readPt.loc.lat, 0.0001);
        assertEquals(startPt.loc.lon, readPt.loc.lon, 0.0001);
        assertEquals(startPt.leaveTo, readPt.leaveTo);
        assertEquals(startPt.type, readPt.type);

        // Ignore millisceonds, since they are not stored in GPX
        assertEquals(roundingPt.time.toMiliSec()/1000, readPt.time.toMiliSec()/1000);
        readPt = rc.getRoutes().get(0).getRpt(1);
        assertEquals(roundingPt.name, readPt.name);
        assertEquals(roundingPt.loc.lat, readPt.loc.lat, 0.0001);
        assertEquals(roundingPt.loc.lon, readPt.loc.lon, 0.0001);
        assertEquals(roundingPt.leaveTo, readPt.leaveTo);
        assertEquals(roundingPt.type, readPt.type);
        // Ignore millisceonds, since they are not stored in GPX
        assertEquals(roundingPt.time.toMiliSec()/1000, readPt.time.toMiliSec()/1000);
    }


}
