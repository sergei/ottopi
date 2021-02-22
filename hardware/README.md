# Components
- Enclosure
- Raspberry PI
- GybeTime raspberry Pi HAT
- Relay board

## GybeTime raspberry Pi HAT

- [Schematic](gybetime_schem.pdf) 
- [PCB overview](gybetime_pcb.png) 
- [PCB gerber files](gybetime-gerber)

This board has two functions:

- Convert 12V boat power to 5V Raspberry Pi
- Convert +- 12V Boat NMEA to CMOS level Raspberry Pi RS-232 

## Relay board 
The B&G ACP remote controller input is connected to the relays 
mounted on this board. Each relay is controlled by RPI GPIO.   
We use Normally Open (N.O.) relay connectors. 
To act as a B&G remote controller we close these contacts for few hundreds of milliseconds  

### B&G ACP connections

- 1 - green - N.O. SW1  - D0 - RPI GPIOx
- 2 - blue  - N.O. SW2  - D1  - RPI GPIOx
- 3 - red  - N.O. SW3  - D2  - RPI GPIOx
- 4 - yellow  - N.O. SW4 - D3  - RPI GPIOx
- 5 - white  - N.O. SW5 - D4  - RPI GPIOx
- 6 - orange  - N.O. SW6 - D5  - RPI GPIOx
- 7 - brown  - N.O. SW7 - D6  - RPI GPIOx
- 8 - Black (common) 

# B&G ACP wiring

Here is the [post found on the internet](https://www.cruisersforum.com/forums/f116/b-and-g-h1000-pilot-handheld-wires-meaning-78713.html)
describing wiring of the ACP remote controller    

```  
  On the ACP the connenctions are color coded, if you have the proper B&G cable, the wire color are the same.
  The following color combinations are the ones connected when a (momentary) switch is pressed.
  
  AP ON: black-white
  AP OFF: black-brown
  
  +10: black-yellow
  -10: black-blue
  
  +1: black-violet
  -1: black-green
  
  that leaves a red wire unused, most likely for the LED, which I didn't bother to hook up.
  
  Dirk
```
