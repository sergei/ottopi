#SPHINX_BASE=/usr/local

SPHINX_BASE=../../cross/usr/local

CC=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-g++
CXX=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-g++
STRIP=$(PI_TOOL_CHAIN)/bin/arm-unknown-linux-gnueabi-strip

INCS = -I$(SPHINX_BASE)/include -I$(SPHINX_BASE)/include/sphinxbase -I$(SPHINX_BASE)/include/pocketsphinx 
LIBS = -L$(SPHINX_BASE)/lib -lpocketsphinx -lsphinxbase -lsphinxad -lasound -lpthread -lm 

CFLAGS =	-O2 -Wall $(INCS)
CXXFLAGS =	-O2 -Wall $(INCS)

OBJS =	hello_ps.o


TARGET =	hello_ps

$(TARGET):	$(OBJS) 
	$(CXX) -o $(TARGET) $(OBJS)  $(LIBS)
		
all:	$(TARGET)

clean:
	rm -f $(OBJS) $(TARGET)
