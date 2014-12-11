
OBJ_DIR=obj

INCS =  -I ../piutils -I minmea
LIBS = -lpthread -lm -lrt

CFLAGS =	-O2 -Wall $(INCS)
CXXFLAGS =	-O2 -Wall $(INCS)

OBJ_DIR=obj
BIN_DIR=bin

SRCS = $(wildcard *.cpp)
OBJS = $(addprefix $(OBJ_DIR)/, $(notdir $(SRCS:.cpp=.o))) ../piutils/log.o

BIN  = $(BIN_DIR)/ottopi

$(OBJ_DIR)/%.o: %.cpp
	$(CXX) $(CFLAGS) -c $< -o $@
 
$(OBJ_DIR):
	mkdir $(OBJ_DIR)
	  
$(BIN_DIR):
	mkdir $(BIN_DIR)  

$(BIN):	$(BIN_DIR) $(OBJ_DIR) $(OBJS) 
	$(CXX) -o $(BIN) $(OBJS) $(LIBS)
		
all:	$(BIN)   

clean:
	rm -rf $(OBJS) $(OBJ_DIR) -rf $(BIN_DIR) 
