
OBJ_DIR=obj

INCS = -I $(CROSS_LIBS)/include -I $(CROSS_LIBS)/local/include  
LIBS = -L $(CROSS_LIBS)/lib/arm-linux-gnueabihf -lflite_cmu_us_kal -lflite_usenglish -lflite_cmulex -lflite -L$(CROSS_LIBS)/local/lib -lasound -lpthread -lm 

CFLAGS =	-O2 -Wall $(INCS)
CXXFLAGS =	-O2 -Wall $(INCS)

OBJ_DIR=obj
BIN_DIR=bin

SRCS = $(wildcard *.c)
OBJS = $(addprefix $(OBJ_DIR)/, $(notdir $(SRCS:.c=.o)))

BIN  = $(BIN_DIR)/text2sound

$(OBJ_DIR)/%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@
 
$(OBJ_DIR):
	mkdir $(OBJ_DIR)
	  
$(BIN_DIR):
	mkdir $(BIN_DIR)  

$(BIN):	$(BIN_DIR) $(OBJ_DIR) $(OBJS) 
	$(CC) -o $(BIN) $(OBJS)  $(LIBS)
		
all:	$(BIN)   

clean:
	rm -rf $(OBJ_DIR) -rf $(BIN_DIR)
