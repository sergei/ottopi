package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.DirectionSmoother;

import junit.framework.TestCase;

import java.util.Random;

public class DirectionSmootherTest extends TestCase {

    private DirectionSmoother ds;
    private Random generator;

    protected void setUp() throws Exception {
        ds = new DirectionSmoother(0.1, 5);
        generator = new Random(12345);
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testConstant() {
        Direction dir = new Direction(270);
        double sigma = 4;
        for( int i = 0; i < 100; i++) {
            dir = ds.update(new Direction(generator.nextGaussian() * sigma)) ;
        }

        double smoothedDir = dir.toDegrees();
        if (smoothedDir > 355){
            assertEquals(360, smoothedDir, 1);
        }else{
            assertEquals(0, smoothedDir, 1);
        }
    }

    public final void testInit() {
        Direction dir = ds.update(new Direction(270));
        assertEquals(270, dir.toDegrees(), 0.1);
    }

    public final void testTurn() {
        ds.update(new Direction(0));
        ds.update(new Direction(10));
        ds.update(new Direction(20));

        for( double d = 30; d < 360; d += 10 ){
            Direction dir = ds.update(new Direction(d));
            assertEquals(d, dir.toDegrees(), 0.1);
        }
    }

}
