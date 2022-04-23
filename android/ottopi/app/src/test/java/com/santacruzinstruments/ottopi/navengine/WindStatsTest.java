package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;

import junit.framework.TestCase;

import java.util.Random;

public class WindStatsTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testUsingComputeData() {

        Random generator = new Random(12345);

        WindStats windStats = new WindStats();

        assertFalse(windStats.getPortTwaIqr().isValid());
        assertFalse(windStats.getMedianPortTwa().isValid());

        assertFalse(windStats.getStbdTwaIqr().isValid());
        assertFalse(windStats.getMedianStbdTwa().isValid());

        // Sail on port
        for( int i = 0; i < 60; i++){
            double angle = generator.nextGaussian() * 10 - 45;
            windStats.update(new Angle(angle));
        }

        assertTrue(windStats.getMedianPortTwa().isValid());
        assertEquals(-45, windStats.getMedianPortTwa().toDegrees(), 2);
        assertTrue(windStats.getPortTwaIqr().isValid());
        assertEquals(15, windStats.getPortTwaIqr().toDegrees(), 2);

        assertFalse(windStats.getStbdTwaIqr().isValid());
        assertFalse(windStats.getMedianStbdTwa().isValid());

        // Now sail on starboard
        for( int i = 0; i < 60; i++){
            double angle = generator.nextGaussian() * 10 + 45;
            windStats.update(new Angle(angle));
        }

        assertTrue(windStats.getMedianPortTwa().isValid());
        assertEquals(-45, windStats.getMedianPortTwa().toDegrees(), 2);
        assertTrue(windStats.getPortTwaIqr().isValid());
        assertEquals(15, windStats.getPortTwaIqr().toDegrees(), 2);

        assertTrue(windStats.getMedianStbdTwa().isValid());
        assertEquals(45, windStats.getMedianStbdTwa().toDegrees(), 2);
        assertTrue(windStats.getStbdTwaIqr().isValid());
        assertEquals(15, windStats.getStbdTwaIqr().toDegrees(), 2);

    }

}