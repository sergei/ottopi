/*
 * ottopi.cpp
 *
 */
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <stdio.h>

#include "log.h"

#include "Controller.h"
#include "SerialPortLineReader.h"
#include "Poller.h"

void  startPolling (char* pcInstrNmeaFileName, char* pcVoiceNmeaFileName , char* pcSpeechOutputFileName, char* pcAutopPilotControllerFileName )
{
	Controller controller;

	SerialPortLineReader instrumentsReader( pcInstrNmeaFileName, controller );

	Poller poller;
	poller.addStream(instrumentsReader);

	poller.startPolling();
}

int main(int argc, char **argv) {
	char* pcInstrNmeaFileName = NULL;
	char* pcVoiceNmeaFileName = NULL;
	char* pcSpeechOutputFileName = NULL;
	char* pcAutopPilotControllerFileName = NULL;
	bool  bRunAsDaemon = false;
	char *pcLogPriority = NULL ;



	for ( int i = 0; i < argc; i++ )
	{
		if( !strcmp(argv[i],"--instr" ) )
		{
			if ( (i+1) < argc )
			{
				pcInstrNmeaFileName = argv[i+1];
			}
		}

		if( !strcmp(argv[i],"--voice" ) )
		{
			if ( (i+1) < argc )
			{
				pcVoiceNmeaFileName = argv[i+1];
			}
		}

		if( !strcmp(argv[i],"--speech" ) )
		{
			if ( (i+1) < argc )
			{
				pcSpeechOutputFileName = argv[i+1];
			}
		}

		if( !strcmp(argv[i],"--acp" ) )
		{
			if ( (i+1) < argc )
			{
				pcAutopPilotControllerFileName = argv[i+1];
			}
		}

		if( !strcmp(argv[i],"--daemon" ) )
		{
			bRunAsDaemon = true;
		}

	}

	if ( pcInstrNmeaFileName == NULL ){
		ottolog(LOG_EMERG,"Please specify --instr /dev/ttyXXXXX\n");
		return EXIT_FAILURE;
	}

	if ( pcVoiceNmeaFileName == NULL ){
		ottolog(LOG_EMERG,"Please specify --voice voicefifo\n");
		return EXIT_FAILURE;
	}

	if ( pcSpeechOutputFileName == NULL ){
		ottolog(LOG_EMERG,"Please specify --speech speechfifo\n");
		return EXIT_FAILURE;
	}

	if ( pcAutopPilotControllerFileName == NULL ){
		ottolog(LOG_EMERG,"Please specify --acp acpfifo\n");
		return EXIT_FAILURE;
	}

	if (  bRunAsDaemon )
	{
		useSyslog( pcLogPriority == NULL ? "info" : pcLogPriority );
	}

	if (  bRunAsDaemon )
	{
		 daemon(0, 0);
	}

	startPolling (pcInstrNmeaFileName, pcVoiceNmeaFileName, pcSpeechOutputFileName, pcAutopPilotControllerFileName );

	return 0;
}
