package com.santacruzinstruments.ottopi.navengine.nmea;

import java.util.LinkedList;


/**
 *  This class communicates with it's user through the messages posted to the handler 
 *  specified in constructor 
 *
 */
public class NmeaReader {

	 public interface NmeaSentenceListener
	 {
		 /**
		  * Called when received well formed but unknown message
		  * @param msg - NMEA string that was not recognized
		  */
		 void onValidMessage(String msg);
	 }

	// Buffer to assemble NMEA sentence 
	final static int MAX_NMEA_LEN = 128;
	char [] mNmeaBuff = new char [MAX_NMEA_LEN + 6 ]; // Accommodate for artificial checksum 
	byte mComputedCheckSum = 0;
	byte mReceivedCheckSum = 0;
	int  mRcvdCcIdx = 0;
	int mNmeaIdx = 0;
	
	final static int STATE_WAIT_4_START = 1;
	final static int STATE_WAIT_4_CC = 2;
	final static int STATE_WAIT_4_END = 3;
	int mParserState = STATE_WAIT_4_START;

	private final LinkedList<NmeaSentenceListener> listeners = new LinkedList<>();
	public void addListener(NmeaSentenceListener l)
	{
		listeners.add(l);
	}

	public void removeListener(NmeaSentenceListener l)
	{
		listeners.remove(l);
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean IsHex(byte b)
    {
    	if ( b >= 'A' && b <= 'F') return true;
    	if ( b >= '0' && b <= '9') return true;
    	return false;
    }

    private byte Hex2Byte(byte b)
    {
    	if ( b >= 'A' && b <= 'F') return (byte) (b - 'A' + 0x0A) ; 
    	if ( b >= '0' && b <= '9') return (byte) (b - '0') ;
    	return 0;
    }

    private char Byte2Hex(byte b)
    {
    	if ( b < 10) return (char) (b + '0'); 
    	else if ( b < 16 ) return (char) (b - 10 + 'A');
    	return '.';
    }
    
    @SuppressWarnings("RedundantIfStatement")
	private boolean IsValidNmeaByte(byte b)
    {
    	if ( b >= 'a' && b <= 'z') return true;
    	if ( b >= 'A' && b <= 'Z') return true; 
    	if ( b >= '0' && b <= '9') return true; 
    	if ( b == '.') return true; 
    	if ( b == ',') return true; 
    	if ( b == '-') return true; 
    	if ( b == ' ') return true; 
    
    	return false;
    }

    public final void read(byte[]  buff, int size)
    {
    	for( int i = 0; i < size; i++)
    	{
    		byte b = buff[i];


    		// If we got $ at any moment we should start over 
    		if ( b == '$')
    		{
    			mParserState = STATE_WAIT_4_START;
    		}
    		
        	switch ( mParserState )
        	{
        	case STATE_WAIT_4_START:
        		if ( b == '$')
        		{
        			mComputedCheckSum = 0;
        			mNmeaIdx = 0;
        			mNmeaBuff[mNmeaIdx++] = (char) b;
        			mParserState = STATE_WAIT_4_CC;
        		}
	        	break;	
        	case STATE_WAIT_4_CC:
        		if ( b == '*')
        		{
        			mParserState = STATE_WAIT_4_END;
        			mReceivedCheckSum = 0;
        			mRcvdCcIdx = 0;
        		}
        		else if ( b == '\n' || b == '\r' )
        		{
        			// It happened to be no checksum, assume message valid and let's add one
        			// to benefit the recipients of our  NMEA stream
        			mNmeaBuff[mNmeaIdx++] = '*';
        			mNmeaBuff[mNmeaIdx++] = Byte2Hex((byte) ((mComputedCheckSum >>4) & 0x0F) ); 
        			mNmeaBuff[mNmeaIdx++] = Byte2Hex((byte) ((mComputedCheckSum    ) & 0x0F) );
        			mNmeaBuff[mNmeaIdx++] = '\r';
        			mNmeaBuff[mNmeaIdx++] = '\n';
        			
					PostNmeaSentence(new String(mNmeaBuff,0, mNmeaIdx));
        			mParserState = STATE_WAIT_4_START;
        		}
        		else if ( (mNmeaIdx < MAX_NMEA_LEN) && IsValidNmeaByte(b))
        		{
        			mNmeaBuff[mNmeaIdx++] = (char) b;
        			mComputedCheckSum ^= b;
        		}
        		else // Something went wrong 
        		{
        			mParserState = STATE_WAIT_4_START;
        		}
	        	break;	
        	case STATE_WAIT_4_END:
        		if ( IsHex(b) )
        		{
        			if ( mRcvdCcIdx == 0 )
        			{
        				mReceivedCheckSum = (byte) ((Hex2Byte(b) << 4) & 0x00F0 ) ; 
        				mRcvdCcIdx ++;
        			}
        			else
        			{
        				int ccH = mReceivedCheckSum & 0x00F0;
        				int ccL = (Hex2Byte(b)      & 0x000F); 
        				mReceivedCheckSum = (byte) ( ccH | ccL); 
        				if ( mReceivedCheckSum == mComputedCheckSum )
        				{
                			mNmeaBuff[mNmeaIdx++] = '*';
                			mNmeaBuff[mNmeaIdx++] = Byte2Hex((byte) ((mComputedCheckSum >>4) & 0x0F) ); 
                			mNmeaBuff[mNmeaIdx++] = Byte2Hex((byte) ((mComputedCheckSum    ) & 0x0F) );
                			mNmeaBuff[mNmeaIdx++] = '\r';
                			mNmeaBuff[mNmeaIdx++] = '\n';
        					PostNmeaSentence(new String(mNmeaBuff,0, mNmeaIdx));
        				}
        				mParserState = STATE_WAIT_4_START;
        			}
        		}
        		else // Something went wrong 
        		{
        			mParserState = STATE_WAIT_4_START;
        		}
	        	break;	
        	}
    	}
    	    	
    }

	private void PostNmeaSentence(String string) {
		for( NmeaSentenceListener l : listeners)
			l.onValidMessage(string);
	}
}
