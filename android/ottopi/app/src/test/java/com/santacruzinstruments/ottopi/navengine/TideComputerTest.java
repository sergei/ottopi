package com.santacruzinstruments.ottopi.navengine;


import com.santacruzinstruments.ottopi.navengine.geo.Direction;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;

import junit.framework.TestCase;

public class TideComputerTest extends TestCase {

	private static final int TIDE_TIME_CONSTANT = 60;

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testUsingComputeData()
	{
		TideComputer tc = new TideComputer();
		Speed sog, sow;
		Direction cog, hdg;
		
		// Going in the direction of tide 
		sog = new Speed( 10 );
		sow = new Speed( 8 );
		cog = new Direction( 45 );
		hdg = new Direction( 45 );
		
		// Iterate ten times for filter to converge in a minute
		for (int i = 0; i < TIDE_TIME_CONSTANT; i++)
			tc.update ( sog, cog, sow, hdg);
		
		Speed sot = tc.getSpeedOfTide();
		assertNotNull ( sot );
		assertTrue ( sot.isValid() );
		assertEquals( 2., sot.getKnots(), 0.05 );
		
		Direction dot = tc.getDirectionOfTide();
		assertNotNull ( dot );
		assertTrue ( dot.isValid() );
		assertEquals( 45., dot.toDegrees(), 0.05 );
	
		// Struggling against the tide
		tc = new TideComputer();
		sog = new Speed( 0 );
		sow = new Speed( 8 );
		cog = new Direction( 0 );
		hdg = new Direction( 130 );
		
		// Iterate ten times for filter to converge
		for ( int i = 0; i < TIDE_TIME_CONSTANT ; i++)
			tc.update ( sog, cog, sow, hdg);
		
		assertEquals( 8., tc.getSpeedOfTide().getKnots(), 0.05 );
		assertEquals( 310., tc.getDirectionOfTide().toDegrees(), 0.05 );
		
		// Crossing the tide. Read the same speed, but pointing more to north, the current must be going south
		tc = new TideComputer();
		sog = new Speed( 8 );
		sow = new Speed( 8 );
		cog = new Direction( 280 );
		hdg = new Direction( 290 );
		
		// Iterate ten times for filter to converge
		for ( int i = 0; i < TIDE_TIME_CONSTANT ; i++)
			tc.update ( sog, cog, sow, hdg);
		
		assertEquals( 1.4, tc.getSpeedOfTide().getKnots(), 0.05 );
		assertEquals( 195., tc.getDirectionOfTide().toDegrees(), 0.05 );
		
	}
	
}
