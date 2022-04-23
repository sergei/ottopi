package com.santacruzinstruments.ottopi.navengine;

import com.santacruzinstruments.ottopi.navengine.geo.Angle;
import com.santacruzinstruments.ottopi.navengine.geo.Speed;
import com.santacruzinstruments.ottopi.navengine.polars.PolarTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


import junit.framework.TestCase;

public class PolarTableTest extends TestCase {
	
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Paste the output of this test to android/ottopi/app/src/test/resources/polarsplot.xlsx
	 * @throws IOException
	 */
	public final void testInterpolationCsv() throws IOException {
		InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("J105.txt");
		assertNotNull(is);

		PolarTable pt = new PolarTable(is);

		boolean hasHeader = false;
		StringBuilder header = new StringBuilder();
		StringBuilder table = new StringBuilder();
		for( double tws = 4; tws < 22.1; tws += 1){
			StringBuilder csvLine = new StringBuilder();
			csvLine.append(String.format("%.1f", tws));
			for(double twa = 0; twa < 180.1; twa +=1){
				if( !hasHeader ){
					header.append(',');
					header.append(String.format("%.1f", twa));
				}
				Speed bs = pt.getTargetSpeed(new Speed(tws), new Angle(twa));
				csvLine.append(',');
				csvLine.append(String.format("%.1f", bs.getKnots()));
			}
			table.append(csvLine);
			table.append("\n");
			hasHeader = true;
		}
		header.append("\n");
		header.append(table);
		System.out.println(header);
	}

	public final void testTargets() throws IOException
	{
		InputStream is = Objects.requireNonNull(getClass().getClassLoader()).getResourceAsStream("J105.txt");
		assertNotNull ( is );
		
		PolarTable pt = new PolarTable ( is );
		
		// Exact 
		// Up wind
		Targets t = pt.getTargets(new Speed(10.), PolarTable.PointOfSail.UPWIND);
		assertEquals(40.6, t.twa.toDegrees(), 0.01);
		assertEquals(6.00, t.bsp.getKnots(), 0.01);
		// Down wind1

		t = pt.getTargets(new Speed(10.), PolarTable.PointOfSail.DOWNWIND);
		assertEquals(145.2, t.twa.toDegrees(), 0.01);
		assertEquals(6.4, t.bsp.getKnots(), 0.01);
		
		// Zero speed
		// Up wind
		t = pt.getTargets(new Speed(0.), PolarTable.PointOfSail.UPWIND);
		assertEquals(53.3, t.twa.toDegrees(), 0.01);
		assertEquals(2.0, t.bsp.getKnots(), 0.01);
		// Down wind
		t = pt.getTargets(new Speed(0.), PolarTable.PointOfSail.DOWNWIND);
		assertEquals(127.6, t.twa.toDegrees(), 0.01);
		assertEquals(1.8, t.bsp.getKnots(), 0.01);
		
		// Extrapolation beyond polars
		t = pt.getTargets(new Speed(22.), PolarTable.PointOfSail.UPWIND);
		// Up wind
		assertEquals(39.9, t.twa.toDegrees(), 0.01);
		assertEquals(6.82, t.bsp.getKnots(), 0.01);
		// Down wind
		t = pt.getTargets(new Speed(22.), PolarTable.PointOfSail.DOWNWIND);
		assertEquals(179.25, t.twa.toDegrees(), 0.01);
		assertEquals(8.59, t.bsp.getKnots(), 0.01);
		
	}
	
	
	
}
