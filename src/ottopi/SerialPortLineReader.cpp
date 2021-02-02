/*
 * SerialPortLineReader.cpp
 *
 */

#include <errno.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>

#include "log.h"
#include "SerialPortLineReader.h"
#include "NmeaListener.h"

SerialPortLineReader::SerialPortLineReader(const char *name, NmeaListener &listener)
:pcPortName(name)
,lineListener(listener)
,nmeaLineReader(listener)
,fileDescriptor(-1)
,m_xmitOffs(0)
,debug(false)
{
	tryToOpenPort();
}

void SerialPortLineReader::closePort() {
	ottolog(LOG_INFO, "Closing %s\n", pcPortName);
	if (fileDescriptor >= 0) {
		close(fileDescriptor);
		fileDescriptor = -1;
	}
}

SerialPortLineReader::~SerialPortLineReader() {
	closePort();
}

void SerialPortLineReader::onTimerTick()
{
  lineListener.onTimerTick();
}

void SerialPortLineReader::onError()
{
}

void SerialPortLineReader::onDataReceived(char *pcBuff, int size)
{
	if(debug) ottolog(LOG_DEBUG, "r->%s(%d)\n",pcPortName, size );
	nmeaLineReader.onDataReceived(pcBuff, size);
}

void SerialPortLineReader::tryToOpenPort()
{
	int speed = B4800;
	int parity = 0;
	ottolog(LOG_INFO, "Opening %s ... \n", pcPortName );
	fileDescriptor = open (pcPortName, O_RDWR | O_NOCTTY | O_SYNC );
	if (fileDescriptor >= 0)
	{
        struct termios tty;
        memset (&tty, 0, sizeof tty);
        if (tcgetattr (fileDescriptor, &tty) != 0)
        {
                ottolog(LOG_ERR, "error %d from tcgetattr for %d\n", errno, fileDescriptor);
        		close( fileDescriptor );
                fileDescriptor = -1;
                return;
        }

        cfsetospeed (&tty, speed);
        cfsetispeed (&tty, speed);

        tty.c_cflag = (tty.c_cflag & ~CSIZE) | CS8;     // 8-bit chars
        // disable IGNBRK for mismatched speed tests; otherwise receive break
        // as \000 chars
        tty.c_iflag &= ~IGNBRK;         // ignore break signal
        tty.c_lflag = 0;                // no signaling chars, no echo,
                                        // no canonical processing
        tty.c_oflag = 0;                // no remapping, no delays
        tty.c_cc[VMIN]  = 1;            // read doesn't block
        tty.c_cc[VTIME] = 50;            // 0.5 seconds read timeout

        tty.c_iflag &= ~(IXON | IXOFF | IXANY); // shut off xon/xoff ctrl

        /* CR/LF handling, Do no translation:
         *  no NL -> CR/NL mapping on output, and
         *  no CR -> NL mapping on input.
         */
        tty.c_oflag &= ~ONLCR;
        tty.c_iflag &= ~ICRNL;

        tty.c_cflag |= (CLOCAL | CREAD);// ignore modem controls,
                                        // enable reading
        tty.c_cflag &= ~(PARENB | PARODD);      // shut off parity
        tty.c_cflag |= parity;
        tty.c_cflag &= ~CSTOPB;
        tty.c_cflag &= ~CRTSCTS;

        if (tcsetattr (fileDescriptor, TCSANOW, &tty) != 0)
        {
        		ottolog(LOG_ERR, "error %d from tcsetattr\n", errno);
        		close( fileDescriptor );
                fileDescriptor = -1;
                return;
        }

        // read settings back
        if (tcgetattr (fileDescriptor, &tty) != 0)
        {
        		ottolog(LOG_ERR, "error %d from tcgetattr for %d\n", errno, fileDescriptor);
        		close( fileDescriptor );
                fileDescriptor = -1;
                return;
        }

        ottolog(LOG_INFO, "%s opened OK, fd=%d \n", pcPortName, fileDescriptor );
	}
	else
	{
        ottolog (LOG_ERR, "error %d opening %s: %s\n", errno, pcPortName, strerror (errno));
	}

}

void SerialPortLineReader::transmit(char *pcBuff, int size){

  if ( (m_xmitOffs  + size) < ( (int)sizeof( acTransmitBuffer ) - 1) )
  {
          memcpy(acTransmitBuffer + m_xmitOffs , pcBuff, size );
          m_xmitOffs += size;
  }
  else
  {
          ottolog(LOG_DEBUG, "Resetting offset form %d to 0...\n", m_xmitOffs);
          m_xmitOffs = 0; // Some nonsense is keep coming
  }
}

int  SerialPortLineReader::getDataToSend(char *pcBuff, int size){
  int sizeToCopy = size > m_xmitOffs ? m_xmitOffs : size;
  memcpy(pcBuff,acTransmitBuffer, sizeToCopy );
  m_xmitOffs = 0;
  return sizeToCopy;
}






