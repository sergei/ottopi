/*
 * NmeaLineReader.h
 *
 *  Created on: Feb 11, 2014
 *      Author: spodshivalov
 */

#ifndef NMEALINEREADER_H_
#define NMEALINEREADER_H_

class NmeaListener;
class NmeaLineReader
{
public:
  NmeaLineReader(NmeaListener &listener);
  void onDataReceived(char *pcBuff, int size);
  virtual
  ~NmeaLineReader();
private:
  NmeaListener &lineListener;
  void parseNmeaString( const char *pcBuff);
  bool waitForStart;
  char acNmeaBuff[256];
  int  nmeaIdx;

};

#endif /* NMEALINEREADER_H_ */
