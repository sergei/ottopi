/*
 * NmeaLineReader.cpp
 *
 *  Created on: Feb 11, 2014
 *      Author: spodshivalov
 */

#include "log.h"

#include "minmea.h"

#include "NmeaLineReader.h"
#include "NmeaListener.h"

NmeaLineReader::NmeaLineReader(NmeaListener &listener)
:lineListener(listener)
,waitForStart(true)
,nmeaIdx(0)
{
}

void NmeaLineReader::onDataReceived(char *pcBuff, int size)
{
      for ( int i = 0; i < size; i++ )
      {
              char ch = pcBuff[i];
              if ( waitForStart )
              {
                      if ( ch == '$' )
                      {
                              acNmeaBuff[nmeaIdx++] = ch;
                              waitForStart = false;
                      }
              }
              else
              {
                      if ( ch == 0x0A )
                      {
                              acNmeaBuff[nmeaIdx++] = ch;
                              acNmeaBuff[nmeaIdx++]= '\0';
                              waitForStart = true;
                              parseNmeaString( acNmeaBuff );
                              nmeaIdx = 0;
                      }
                      else if ( nmeaIdx >= (int)sizeof(acNmeaBuff) )
                      {
                              waitForStart = true;
                              nmeaIdx = 0;
                              ottolog(LOG_DEBUG, "Nmea line overflow, discarding ...\n");
                      }
                      else
                      {
                              acNmeaBuff[nmeaIdx++] = ch;
                      }

              }
      }
}

void NmeaLineReader::parseNmeaString( const char *pcBuff)
{
	 switch (minmea_sentence_id(pcBuff, false)) {
     case MINMEA_SENTENCE_VWR: {
         struct minmea_sentence_vwr frame;
         if ( minmea_parse_vwr(&frame, pcBuff) ) {
        	 lineListener.onVwr( frame );
         }
     } break;

     case MINMEA_SENTENCE_LSN: {
         struct minmea_sentence_lsn frame;
         if ( minmea_parse_lsn(&frame, pcBuff) ) {
        	 lineListener.onLsn( frame );
         }
     } break;

     default: {
     } break;

	 }
}

NmeaLineReader::~NmeaLineReader()
{
}

