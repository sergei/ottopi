/*
 * IStream
 *
 *  Created on: Feb 7, 2014
 *      Author: spodshivalov
 */

#ifndef ISTREAM_
#define ISTREAM_

class IStream{
public:

  virtual int getFileDesciptor() = 0;
  virtual const char *getName()  = 0;
  virtual void onTimerTick()  = 0;
  virtual void onDataReceived(char *pcBuff, int size) = 0;
  virtual void transmit(char *pcBuff, int size) = 0;
  virtual void onError()  = 0;
  virtual bool hasDataToSend()  = 0;
  virtual int  getDataToSend(char *pcBuff, int size) = 0;
  virtual void enableDebug()  = 0;
  virtual bool isDebugEnabled()  = 0;

  virtual ~IStream(){}
};


#endif /* ISTREAM_ */
