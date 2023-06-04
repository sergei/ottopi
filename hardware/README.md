# Components
- Enclosure
- Raspberry PI
- GybeTime raspberry Pi HAT
- Relay board

## Enclosure 

- Hammond [1555HF42](1555HF42GY.pdf) The large one for Otto Pi with B&G Relay board 
  - Steel panel 1555HFPL - The RPI is mounted on this panel
- Hammond [1555NF42GY](1555NF42.pdf) for the regular one
  - Steel panel 1555NFPL - The RPI is mounted on this panel

## Sensor board 
- [ICM20948 9DoF Motion Sensor Breakout](https://shop.pimoroni.com/products/icm20948)
  - Sensor chipset [ICM-20948](DS-000189-ICM-20948-v1.3.pdf)
  - [Python library](https://github.com/pimoroni/icm20948-python)    

## RS-232 Board
  [SparkFun Transceiver Breakout - MAX3232](https://www.sparkfun.com/products/11189)
  T1IN (TTL IN)    - GPIO 14, RPI PIN 8  (UART0 TXD)
  R1OUT(TTL OUT)   - GPIO 15, RPI PIN 10 (UART0 RXD)
  R1IN (RS232 IN)  - NMEA IN
  T1OUT(RS232 OUT) - NMEA OUT
  GND              - RPI PIN 6 
  3.3V - 5.5V      - RPI PIN 4

## Audio jack
- A - Tip (Right audio) - US-CAB-73 Red 
- B - Ring (Left audio) - US-CAB-73 White
- C - Ground            - US-CAB-73 Green
- D - Microphone        - US-CAB-73 Black

## Relay board (For Otto PI with B&G ACP connection) 
The B&G ACP remote controller input is connected to the relays 
mounted on this board. Each relay is controlled by RPI GPIO.   
We use Normally Open (N.O.) relay connectors. 
To act as a B&G remote controller we close these contacts for few hundreds of milliseconds  

### B&G ACP connections

- 1 - green - N.O. SW1  - D0 - RPI PIN 32
- 2 - blue  - N.O. SW2  - D1  - RPI PIN 36
- 3 - red  - N.O. SW3  - D2  - RPI PIN 38
- 4 - yellow  - N.O. SW4 - D3  - RPI PIN 40
- 5 - white  - N.O. SW5 - D4  - RPI PIN 35  
- 6 - orange  - N.O. SW6 - D5  - RPI PIN 37 - color substitute for violet
- 7 - brown  - N.O. SW7 - D6  - RPI PIN 33
- 8 - Black (common) 

### B&G ACP wiring

Here is the [post found on the internet](https://www.cruisersforum.com/forums/f116/b-and-g-h1000-pilot-handheld-wires-meaning-78713.html)
describing wiring of the ACP remote controller    

```  
  On the ACP the connenctions are color coded, if you have the proper B&G cable, the wire color are the same.
  The following color combinations are the ones connected when a (momentary) switch is pressed.
  
  AP ON: black-white
  AP OFF: black-brown
  
  +10: black-yellow
  -10: black-blue
  
  +1: black-violet  (orange in Javelin cable)
  -1: black-green
  
  that leaves a red wire unused, most likely for the LED, which I didn't bother to hook up.
  
  Dirk
```

