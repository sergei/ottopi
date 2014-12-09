#include <stdio.h>
#include <flite/flite.h>

extern  cst_voice *register_cmu_us_kal();

int main(int argc, char **argv)
{
	const char *pcVoiceName=NULL;
	FILE *pInFile = stdin;
	cst_voice * voice;
	char acBuff[256];

	flite_init();

	voice =  flite_voice_select(pcVoiceName);
	voice = register_cmu_us_kal();
	if (  voice == NULL ){
		fprintf(stderr,"Failed to select voice [%s]", pcVoiceName);
		return -1;
	}

	while ( 1 ){
		char *pcText = fgets( acBuff, sizeof(acBuff), pInFile );
		if ( pcText ){
			acBuff[sizeof(acBuff) - 1] = '\0'; // Just being on a paranoid side ...
			flite_text_to_speech(acBuff, voice, "play");
		}

	}

}
