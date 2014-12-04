#CROSS_INC=/usr/local

CROSS_INC=../../cross/usr

CC=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-g++
CXX=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-g++
STRIP=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-strip

INCS = -I$(CROSS_INC)/include -I$(CROSS_INC)/local/include -I$(CROSS_INC)/local/include/sphinxbase -I$(CROSS_INC)/local/include/pocketsphinx 
LIBS = -L$(CROSS_INC)/local/lib -lpocketsphinx -lsphinxbase -lsphinxad -lasound -lpthread -lm 

CFLAGS =	-O2 -Wall $(INCS)
CXXFLAGS =	-O2 -Wall $(INCS)

OBJS =	voice2nmea.o sendnmea.o

TARGET = voice2nmea

$(TARGET):	$(OBJS) 
	$(CXX) -o $(TARGET) $(OBJS)  $(LIBS)
		
all:	$(TARGET)

clean:
	rm -f $(OBJS) $(TARGET)
