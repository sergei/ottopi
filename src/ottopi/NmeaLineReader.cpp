/*
 * NmeaLineReader.cpp
 *
 *  Created on: Feb 11, 2014
 *      Author: spodshivalov
 */

#include "log.h"

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

}

NmeaLineReader::~NmeaLineReader()
{
}

