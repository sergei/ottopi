# OttoPi

## Introduction

OttoPi is the Raspberry PI based box mounted on a sailboat. It's an interface between boat electronics and the sailor.
Sailor can communicate with OttoPi by the following means:
* Bluetooth remote control
* Using third party app like [OpenCPN](https://opencpn.org/) when connected to OttoPi over Wi-Fi
* Web browser by opening http://otto.pi/ottopi URL when connected to OttoPi over Wi-Fi
* Custom application controlling OttoPI using Rest APIs

OttoPi is connected to the following boat electronic interfaces 
* NMEA 0183 input (wind data from B&G Instruments)
* NMEA 0183 output (to VHF GPS input, B&G autopilot NMEA input)
* B&G autopilot controller (via remote control interface)

# User facing features

* Connect an external app to NMEA TCP socket to receive/send NMEA data 
* HTTP server running Web App
  * Upload GPX (WPTs, Routes)
  * Download ZIP (NMEA logs)
  * Upload Polars 
  * Current nav status
    * SOG, SOW, COG, HDG, Bearing to WPT, AWA, AWS, TWD, TWS, VMG, Target VMG
  * Historic plots
    * HDG
  * Race Timer control (start/sync/stop) (Reverse handicap start)
  * Race timer countdown 
  * Race timer elapsed handicap time
* Audio output 
  * Angle to the mark
  * Gain/Loss vs Target
  * Race Timer 
* Bluetooth remote control 
  * Route selection 
    * Announce a current destination   ( Play/Stop button )
    * Next Route/PrevRoute  ( +, - button )
    * Next mark/Prev mark  ( >|, |< buttons )
  * Autopilot control
    * Tack/gybe ( Play/Stop button )
    * Up/Down 10 degrees  ( >|, |< buttons )
    * Up/Down 1 degree  ( +, - button )
  * Race control 
    * Start/Reset Timer (Play/Stop button)
    * Sync timer        (+ button)
    * Set committee boat ( >| button)
    * Set pin            ( |< button)
  

# OttoPi box Hardware I/O

* GND
* NMEA IN
* 12 V IN
* NMEA OUT
* Audio out
* B&G Autopilot remote cable
  * 1 deg up
  * 10 deg up 
  * 1 deg down 
  * 10 deg down 
  * Auto/Resume
  * Off
    
# OttoPI REST APIs

The formal [OpenAPI](https://swagger.io/specification) definition is located 
at [navcomputer/openapi/ottopi.yaml](navcomputer/openapi/ottopi.yaml) 

Here is the high level functionality

* WPTs CRUD
  * Upload the GPX file with WPTs and Routes
* Select active WPT
* Routes CRUD
  * List Routes
* Select active route
* Show current nav status 
* Show leg/legs navigation history 
* Race control 
  * Start, Sync, Stop
  * Reverse handicap start
  * Countdown Timer status
* Log files management 
  * Download NMEA files
* Autopilot NAV functions
  * Navigate to WPT
  * Select Route
* Autopilot Vane functions
  * change course N deg 
  * tack/gybe 


# Software components 
* navcomputer/nmea_sim.py - simulator to replay NMEA log useful for development
* [navcomputer](navcomputer/main.py)
  * Listens for NMEA data from internal GPS and external instruments
  * Forwards the received NMEA data to connected TCP clients
  * Runs HTTP server to process REST API calls
  * Serves builtin [Web application](web/README.md) 
  * Computes necessary nav data
* [Bluetooth remote controller](bt_remote/README.md) 
  * Listens for input from remote BT controllers
  * Once the input is detected communicates with navcomputer over REST APIs

# Raspberry PI installation 
* Make access point https://www.raspberrypi.org/documentation/configuration/wireless/access-point-routed.md
* Install pip3 sudo apt-get install python3-pip
* install TTS sudo apt-get install espeak
* See https://www.raspberrypi.org/forums/viewtopic.php?t=136974 to rid of espeak errors
* See UART configuration at https://www.raspberrypi.org/documentation/configuration/uart.md
  * https://github.com/raspberrypi/firmware/blob/master/boot/overlays/README


