/*
 * sendnmea.c
 *
 * Produces
 * $POTOR,LSN,SPS,[RDY|STR|PRC] - Speech Processing states
 *    RDY - Waiting for the silence to end to start speech recognition
 *    STR - End of silence detected, processing the input sound
 *    PRC - End of sound capture, starting recognition
 *
 * $POTOR,LSN,TRN,[UP|DOWN|LEFT|RIGHT],<degrees> - Turn command
 *
 * $POTOR,LSN,TCK,[RDY|NOW] - Tack command
 *
 * $POTOR,LSN,JBE,[RDY|NOW] - Jibe command
 *
 * $POTOR,LSN,CTL,[ON|OFF]  - Help control command
 *     ON - Controlled by autopilot
 *     OFF - Controlled by human
 */

#include <stdio.h>
#include <string.h>

#include "sendnmea.h"

#define _DIM(x) (int)(sizeof(x)/sizeof(x[0]))
static FILE *__outFile;

static const char *__acNumbers[] = {"ZERO","ONE","TWO","THREE","FOUR","FIVE","SIX","SEVEN","EIGHT","NINE","TEN"};

static const char * text2number(const char * text)
{
	static char acNumber[80]="";

	int i = 0;
	for ( i = 0; i < _DIM(__acNumbers);  i++){
		if ( !strcasecmp(text,__acNumbers[i])){
			break;
		}
	}

	if ( i < _DIM(__acNumbers) ){
		sprintf(acNumber,"%d",i);
	}else{
		strcpy(acNumber,"-");
	}

	return acNumber;
}

void openNmea  ( const char *fname )
{
	__outFile = NULL;
	if ( fname != NULL )
	{
		__outFile = fopen(fname,"w");
	}
	else
	{
		__outFile = stdout;
	}
}

void sendNmea  ( const char *text  )
{
	char acBuff[256];
	strcpy(acBuff,"$POTOR,LSN,");
	if (!strcasecmp(text,"READY")){
		strcat(acBuff, "SPS,RDY");
	}else if (!strcasecmp(text,"STARTED TO LISTEN")){
		strcat(acBuff, "SPS,STR");
	}else if (!strcasecmp(text,"PROCESSING")){
		strcat(acBuff, "SPS,PRC");
	}else if ( !strncasecmp(text,"UP ",3) ){
		strcat(acBuff, "TRN,UP,");
		strcat(acBuff, text2number(text + 3));
	}else if ( !strncasecmp(text,"DOWN ",5) ){
		strcat(acBuff, "TRN,DOWN,");
		strcat(acBuff, text2number(text + 5));
	}else if ( !strncasecmp(text,"LEFT ",5) ){
		strcat(acBuff, "TRN,LEFT,");
		strcat(acBuff, text2number(text + 5));
	}else if ( !strncasecmp(text,"RIGHT ",6) ){
		strcat(acBuff, "TRN,RIGHT,");
		strcat(acBuff, text2number(text + 6));
	}else if (!strcasecmp(text,"READY ABOUT")){
		strcat(acBuff, "TCK,RDY");
	}else if (!strcasecmp(text,"HELM A LEE")){
		strcat(acBuff, "TCK,NOW");
	}else if (!strcasecmp(text,"STANDBY TO JIBE")){
		strcat(acBuff, "JBE,RDY");
	}else if (!strcasecmp(text,"JIBE HO")){
		strcat(acBuff, "JBE,NOW");
	}else if (!strcasecmp(text,"MY HELM")){
		strcat(acBuff, "CTL,OFF");
	}

	strcat(acBuff, "\r\n");
	fputs(acBuff,__outFile);
	fflush( __outFile );
}

void closeNmea ( void )
{
	fclose(__outFile);
}
