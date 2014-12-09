#CROSS_INC=/usr/local

CROSS_INC=../../cross/usr

CC=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-gcc
CXX=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-g++
STRIP=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-strip

INCS = -I$(CROSS_INC)/include -I$(CROSS_INC)/local/include -I$(CROSS_INC)/local/include/sphinxbase -I$(CROSS_INC)/local/include/pocketsphinx 
LIBS = -lpthread -lm 

SPHINX_LIBS = -L $(CROSS_INC)/local/lib -lpocketsphinx -lsphinxbase -lsphinxad -lasound  
FLITE_LIBS  = -L $(CROSS_INC)/lib/arm-linux-gnueabihf -lflite_cmu_us_kal -lflite_usenglish -lflite_cmulex -lflite -L$(CROSS_INC)/local/lib -lasound 

CFLAGS =	-O2 -Wall $(INCS)
CXXFLAGS =	-O2 -Wall $(INCS)

VOICE2NMEA_OBJS = voice2nmea.o sendnmea.o
VOICE2NMEA_BIN  = voice2nmea
 
TEXT2SOUND_OBJS = text2sound.o
TEXT2SOUND_BIN  = text2sound

OBJS =	$(VOICE2NMEA_OBJS) $(TEXT2SOUND_OBJS)


$(VOICE2NMEA_BIN):	$(VOICE2NMEA_OBJS) 
	$(CC) -o $(VOICE2NMEA_BIN) $(VOICE2NMEA_OBJS) $(SPHINX_LIBS) $(LIBS)

$(TEXT2SOUND_BIN):	$(TEXT2SOUND_OBJS) 
	$(CC) -o $(TEXT2SOUND_BIN) $(TEXT2SOUND_OBJS) $(FLITE_LIBS)  $(LIBS)
		
all:	$(VOICE2NMEA_BIN) $(TEXT2SOUND_BIN)  

clean:
	rm -f $(OBJS) $(TEXT2SOUND_BIN) $(VOICE2NMEA_BIN)
