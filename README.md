# User facing features

* Connect an external app to NMEA TCP socket to receive/send NMEA data 
* HTTP server running Web App
  * Upload GPX (WPTs, Routes)
  * Download ZIP (GPX,NMEA,KMZ)
  * Upload Polars 
  * Upload PHRF CSV
  * Current nav status
    * SOG, SOW, COG, HDG, Bearing to WPT, AWA, AWS, TWD, TWS, VMG, Target VMG
  * Historic plots
    * HDG, SPD, VMG1
  * Race Timer control (start/sync/stop) (Reverse handicap start)
  * Race timer countdown 
  * Race timer elapsed handicap time
* Audio output 
  * Race Timer 
  * Gain/Loss vs Target
    
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
  * PHRF elapsed timers 
* Log files management 
  * List NMEA files
  * Download NMEA file
* Autopilot NAV functions
  * Navigate to WPT
  * Select Route
* Autopilot Vane functions
  * change course N deg 
  * tack/gybe 


# Software components 
* nmea_sim.py - simulator to replay NMEA log
* navcomputer.py
  * connects GPS serial port and NMEA serial port to TCP socket
  * reads NMEA from the NMEA bridge 
  * Reads user uploaded files 
  * Read user commands (select WPT, select route, start timer)
  * Computes nav data

# Raspberry PI installation 
* Make access point https://www.raspberrypi.org/documentation/configuration/wireless/access-point-routed.md
* Install pip3 sudo apt-get install python3-pip
* install TTS sudo apt-get install espeak
