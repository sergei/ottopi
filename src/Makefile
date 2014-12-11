CROSS_LIBS=../../cross/usr

CC=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-gcc
CXX=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-g++
STRIP=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-strip
		
all:
	@make -C ottopi/minmea all	
	@make -C voice2nmea CC=$(CC) CXX=$(CXX) CROSS_LIBS=$(CROSS_LIBS) CXX=$(CXX) STRIP=$(STRIP) all
	@make -C text2sound CC=$(CC) CXX=$(CXX) CROSS_LIBS=$(CROSS_LIBS) CXX=$(CXX) STRIP=$(STRIP) all
	@make -C ottopi     CC=$(CC) CXX=$(CXX) CROSS_LIBS=$(CROSS_LIBS) CXX=$(CXX) STRIP=$(STRIP) all

clean:
	@make -C voice2nmea clean
	@make -C text2sound clean
	@make -C ottopi     clean
	@make -C ottopi/minmea clean	
