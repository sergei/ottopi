package com.santacruzinstruments.ottopi.navengine;


import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

import junit.framework.TestCase;

import java.util.Random;


public class TrueWindComputerTest extends TestCase {

	public final void testInvalidInputs()
	{
		TrueWindComputer twc = new TrueWindComputer();

		Speed sog = Speed.INVALID;
		Speed aws = new Speed( 10 );
		Angle awa = new  Angle ( 45 );

		twc.computeTrueWind ( sog, aws, awa ,Direction.INVALID);
		Speed tws = twc.getTrueWindSpeed();
		Angle twa = twc.getTrueWindAngle();

		assertFalse( tws.isValid() );
		assertFalse( twa.isValid() );
	}
	
	
	public final void testSailing()
	{
		TrueWindComputer twc = new TrueWindComputer();

		// Sailing case 
		Speed sog = new Speed( 5 );
		Speed aws = new Speed( 10 );
		Angle awa = new  Angle ( 45 );
		
		twc.computeTrueWind ( sog, aws, awa, Direction.INVALID );
		Speed tws = twc.getTrueWindSpeed();
		Angle twa = twc.getTrueWindAngle();
		
		assertTrue( tws.isValid() );
		assertEquals( 7.4, tws.getKnots(), 0.1 );

		assertTrue( twa.isValid() );
		assertEquals(  73, twa.toDegrees(), 1 );
		
		
		// Port tack 
		awa = new  Angle ( -45 );
		twc.computeTrueWind ( sog, aws, awa, Direction.INVALID);
		assertEquals(  -73, twc.getTrueWindAngle().toDegrees(), 1 );
		
	}

	public final void testMotoring()
	{
		TrueWindComputer twc = new TrueWindComputer();

		// Sailing case 
		Speed sog = new Speed( 10 );
		Speed aws = new Speed( 10 );
		Angle awa = new  Angle ( 0 );
		
		twc.computeTrueWind ( sog, aws, awa, Direction.INVALID);
		Speed tws = twc.getTrueWindSpeed();
		Angle twa = twc.getTrueWindAngle();
		
		assertTrue( tws.isValid() );
		assertEquals( 0, tws.getKnots(), 0.1 );

		assertTrue( twa.isValid() );
		assertEquals( 0, twa.toDegrees(), 1 );
	}

	public final void testDownwind()
	{
		TrueWindComputer twc = new TrueWindComputer();

		// Sailing case 
		Speed sog = new Speed( 10 );
		Speed aws = new Speed( 0 );
		Angle awa = new  Angle ( 0 );
		
		twc.computeTrueWind ( sog, aws, awa, Direction.INVALID );
		Speed tws = twc.getTrueWindSpeed();
		Angle twa = twc.getTrueWindAngle();
		
		assertTrue( tws.isValid() );
		assertEquals( 10, tws.getKnots(), 0.1 );

		assertTrue( twa.isValid() );
		assertEquals( 180, twa.toDegrees(), 1 );
	}

	public void testMedian(){
		TrueWindComputer twc = new TrueWindComputer();

		Speed sog = new Speed( 10 );
		Speed aws = new Speed( 20 );
		Angle awa = new  Angle ( 0 );
		Direction mag = new Direction(0);

		twc.computeTrueWind ( sog, aws, awa, mag );
		assertTrue(twc.getTrueWindAngle().isValid());
		assertFalse(twc.getMedianTwd().isValid());
		assertFalse(twc.getTwdIqr().isValid());

		Random generator = new Random(12345);

		for( int i = 0; i < 600; i ++){
			double angle = generator.nextGaussian() * 10 ;
			mag = new Direction(angle);
			twc.computeTrueWind ( sog, aws, awa, mag );
		}

		assertEquals(0., twc.getMedianTwd().toDegrees(), 1);
		assertEquals(14., twc.getTwdIqr().toDegrees(), 1);
	}

}
