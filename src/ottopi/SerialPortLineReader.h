/*
 * SerialPortLineReader.h
 *
 *  Created on: Jan 29, 2013
 *      Author: spodshivalov
 */

#ifndef SERIALPORTLINEREADER_H_
#define SERIALPORTLINEREADER_H_

#include "IStream.h"
#include "NmeaLineReader.h"

class NmeaListener;

class SerialPortLineReader : public IStream {
public:
	SerialPortLineReader(const char *pcName, NmeaListener &listener);
	virtual ~SerialPortLineReader();
	// Returns either valid file descriptor or -1
	virtual int getFileDesciptor() {return fileDescriptor; };
	virtual const char *getName()  {return pcPortName; }
	virtual void onTimerTick();
	virtual void onDataReceived(char *pcBuff, int size);
	virtual void onError();
	virtual void enableDebug(){debug = true;}
	virtual bool isDebugEnabled(){return debug;}

	virtual void transmit(char *pcBuff, int size);
	virtual bool hasDataToSend(){return m_xmitOffs != 0; };
        virtual int  getDataToSend(char *pcBuff, int size);

private:
	void tryToOpenPort();
	void closePort();
	// Prevent from copying, since it's tied to the actual HW
	SerialPortLineReader(SerialPortLineReader &reader);
private:
	const char *pcPortName;
	NmeaListener &lineListener;
	NmeaLineReader nmeaLineReader;
	int fileDescriptor;
	int  m_xmitOffs;
	bool debug;
	char acTransmitBuffer[1024];
};

#endif /* SERIALPORTLINEREADER_H_ */
