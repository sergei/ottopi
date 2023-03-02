package com.santacruzinstruments.ottopi.navengine;


import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaEpochAssembler;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaParser;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaReader;

import junit.framework.TestCase;

public class NmeaEpochAssemblerTest extends TestCase implements InstrumentInputListener {

	NmeaEpochAssembler mNmeaEpochAssembler;
	NmeaReader mNmeaReader;

	InstrumentInput mLastInstrumentInput;
	
	protected void setUp() throws Exception {
		super.setUp();
		mNmeaEpochAssembler = new NmeaEpochAssembler();
		mNmeaEpochAssembler.addInstrumentInputListener( this );
		mLastInstrumentInput = null;
		NmeaParser nmeaParser = new NmeaParser();
		mNmeaReader = new NmeaReader();

		mNmeaReader.addListener(nmeaParser);
		nmeaParser.addListener(mNmeaEpochAssembler);
	}
	
	@Override
	public void onInstrumentInput(InstrumentInput ii) {
		mLastInstrumentInput = ii;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testPostChunk_fullValidWithCC() {
		String validRMC = "$GPRMC,162259.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A*46\r\n";
		mNmeaReader.read(validRMC.getBytes(), validRMC.length());
		mNmeaReader.read(validRMC.getBytes(), validRMC.length());
		assertNotNull ( mLastInstrumentInput );
	}

	public final void testSkipMidnightRMC() {
		String validRMC = "$GPRMC,000000.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A\r\n";
		mNmeaReader.read(validRMC.getBytes(), validRMC.length());
		assertNull ( mLastInstrumentInput );
	}


}
