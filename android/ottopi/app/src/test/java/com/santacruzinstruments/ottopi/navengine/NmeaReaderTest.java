package com.santacruzinstruments.ottopi.navengine;


import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaParser;
import com.santacruzinstruments.ottopi.navengine.nmea0183.NmeaReader;

import junit.framework.TestCase;

public class NmeaReaderTest extends TestCase {

	static class SentenceListener implements NmeaParser.NmeaMsgListener
	{
		boolean mGotValid;
		boolean mGotUnknown;
		NmeaParser.VHW vhw;
		NmeaParser.VWR vwr;
		NmeaParser.MWV mwv;
		NmeaParser.HDG hdg;
		NmeaParser.RMC rmc;

		void reset(){
			mGotValid = false;
		}
		@Override
		public void onVhw(NmeaParser.VHW vhw) {
			mGotValid = true;
			this.vhw = vhw;
		}

		@Override
		public void onVwr(NmeaParser.VWR vwr) {
			mGotValid = true;
			this.vwr = vwr;
		}

		@Override
		public void onMwv(NmeaParser.MWV mwv) {
			mGotValid = true;
			this.mwv = mwv;
		}

		@Override
		public void onHdg(NmeaParser.HDG hdg) {
			mGotValid = true;
			this.hdg = hdg;
		}

		@Override
		public void onRmc(NmeaParser.RMC rmc) {
			mGotValid = true;
			this.rmc = rmc;
		}

		@Override
		public void onUnknownMessage(String msg) {
			mGotUnknown = true;
		}

		@Override
		public void onPscirTST(NmeaParser.PscirTST obj) {
			mGotValid = true;
		}

		@Override
		public void onPmacrSEV(NmeaParser.PmacrSev obj) {
			mGotValid = true;
		}

		@Override
		public void PracrLIN(NmeaParser.PracrLIN obj) {

		}
		@Override
		public void PracrSTR(NmeaParser.PracrSTR obj) {

		}
	}

	NmeaReader mNmeaReader;
	SentenceListener mSentenceListener;
	
	protected void setUp() throws Exception {
		super.setUp();
		NmeaParser  nmeaParser = new NmeaParser();
		mNmeaReader = new NmeaReader();

		mNmeaReader.addListener(nmeaParser);
		mSentenceListener = new SentenceListener();
		nmeaParser.addListener(mSentenceListener);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testPostChunk_garbageOnly() {
		mNmeaReader.read("aaaaa".getBytes(), "aaaaa".length());
		assertFalse( mSentenceListener.mGotValid );
	}

	public final void testPostChunk_fullValidWithCC() {
		String validRMC = "$GPRMC,162259.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A*46\r\n";
		mNmeaReader.read(validRMC.getBytes(), validRMC.length());
		assertTrue( mSentenceListener.mGotValid );
		assertNotNull ( mSentenceListener.rmc );
	}

	public final void testPostChunk_fullValidNoCC() {
		String validRMC = "$GPRMC,162259.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A\r\n";
		mNmeaReader.read(validRMC.getBytes(), validRMC.length());
		assertTrue( mSentenceListener.mGotValid );
		assertNotNull ( mSentenceListener.rmc );
	}


	public final void testPostChunk_fullWrongCc() {
		String validRMC = "$GPRMC,162259.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A*77\r\n";
		mNmeaReader.read(validRMC.getBytes(), validRMC.length());
		assertFalse( mSentenceListener.mGotValid );
		assertNull ( mSentenceListener.rmc );
	}

	public final void testPostChunk_fullValidAfterPartial() {
		String validRMC = "$GPRMC,162259.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A*46\r\n";
		String nmea = "$GPGGA," + validRMC;
		mNmeaReader.read(nmea.getBytes(), nmea.length());
		assertTrue( mSentenceListener.mGotValid );
		assertNotNull ( mSentenceListener.rmc );
	}

	public final void testPostChunk_splitValid() {
		
		String validRMC = "$GPRMC,162259.89,A,3723.463875,N,12158.218664,W,000.0,000.0,100609,,,A*46\r\n";
		byte [] bytes = validRMC.getBytes(); 
		mNmeaReader.read(bytes, 10);
		
		int len =  bytes.length - 10;
		System.arraycopy(bytes, 10, bytes, 0, len);
		mNmeaReader.read(bytes, len);
		
		assertTrue( mSentenceListener.mGotValid );
		assertNotNull ( mSentenceListener.rmc );
	}
	
	/*
	$PSCI,HBT,1.3,244.0.g493d1b7,10.21.83.255,12345,2763,10.21.83.255,2222,2763,192.1
	68.42.255,12345,27630,192.168.42.255,2222,276$GPRMC,233625.974,V,,,,,,,271014,,,N*45
	*/
	public final void testRead_InvalidMsgCut() {
		String unkn = "$SCBWR,000000.00,,,,,,T,,M,,,N,*02\r\n";
		String part = "$PSCI,HBT,1.3,244.0.g493d1b7,10.21.83.255,12345,2763,10.21.83.255,2222,2763,192.168.42.255,12345,27630,192.168.42.255,2222,276";
		String valid = "$GPRMC,233625.974,V,,,,,,,271014,,,N*45\r\n";

		mSentenceListener.reset();
		mNmeaReader.read(unkn.getBytes(), unkn.getBytes().length);
		mNmeaReader.read(part.getBytes(), part.getBytes().length);
		mNmeaReader.read(valid.getBytes(), valid.getBytes().length);

		assertNotNull ( mSentenceListener.rmc );

	}

	/*
	$PSCI,HBT,1.3,244.0.g493d1b7,10.21.83.255,12345,2763,10.21.83.255,2222,2763,192.1
	68.42.255,12345,27630,192.168.42.255,2222,276<0><0><-80><-21>$GPRMC,233625.974,V,
	,,,,,,271014,,,N*45
	$GPVTG,,T,,M,,N,,K,N*2C
	*/
	public final void testRead_InvalidMsgWithJunk() {
		String unkn = "$SCBWR,000000.00,,,,,,T,,M,,,N,*02\r\n";
		String part = "$PSCI,HBT,1.3,244.0.g493d1b7,10.21.83.255,12345,2763,10.21.83.255,2222,2763,192.168.42.255,12345,27630,192.168.42.255,2222,276";
		byte [] junk = {0, -80, -21};
		String valid = "$GPRMC,233625.974,V,,,,,,,271014,,,N*45\r\n";
		
		
		mNmeaReader.read(unkn.getBytes(), unkn.getBytes().length);
		mNmeaReader.read(part.getBytes(), part.getBytes().length);
		mNmeaReader.read(junk,junk.length);
		assertFalse( mSentenceListener.mGotValid );
		mNmeaReader.read(valid.getBytes(), valid.getBytes().length);
		assertNotNull ( mSentenceListener.rmc );
		
	}

	public final void testReadFullOfSpaces() {
		String withSpaces = "$PMACR,SEV,1417968256011,ACCEL,3, -0.07,  0.08, 10.03\r\n";
		String withOutSpaces = "$PMACR,SEV,1417968256011,ACCEL,3,-0.07,0.08,10.03\r\n";
		mNmeaReader.read(withSpaces.getBytes(), withSpaces.length());
		assertTrue( mSentenceListener.mGotValid );
		mNmeaReader.read(withOutSpaces.getBytes(), withOutSpaces.length());
		assertTrue( mSentenceListener.mGotValid );
	}
	
}
