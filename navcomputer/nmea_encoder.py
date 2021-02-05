from functools import reduce
# Make APB, BWR and RMB
"""
APB Autopilot Sentence "B"
                           13 15
 1 2 3 4 5 6 7 8 9 10 11 12| 14|
 | | | | | | | | | | | | | | |
$--APB,A,A,x.x,a,N,A,A,x.x,a,c--c,x.x,a,x.x,a*hh
     1) Status
         V = LORAN-C Blink or SNR warning
         A = general warning flag or other navigation systems when a reliable fix is not available
     2) Status
         V = Loran-C Cycle Lock warning flag
         A = OK or not used
     3) Cross Track Error Magnitude
     4) Direction to steer, L or R
     5) Cross Track Units, N = Nautical Miles
     6) Status A = Arrival Circle Entered
     7) Status A = Perpendicular passed at waypoint
     8) Bearing origin to destination
     9) M = Magnetic, T = True
    10) Destination Waypoint ID
    11) Bearing, present position to Destination
    12) M = Magnetic, T = True
    13) Heading to steer to destination waypoint
    14) M = Magnetic, T = True
    15) Checksum

Example: $GPAPB,A,A,0.10,R,N,V,V,011,M,DEST,011,M,011,M*82
"""


def append_checksum(nmea):
    cc = reduce(lambda i, j: int(i) ^ int(j), [ord(x) for x in nmea[1:]])  # Exclude $ sign
    return nmea + '*{:02X}'.format(cc)


def encode_apb(dest):
    nmea = '$OPAPB,'
    nmea += 'A,A,'  # 1,2  Set to valid
    if dest.xte is None:
        nmea += ',,,'  # 3,4,5 XTE is not valid
    else:
        dir_to_steer = 'R' if dest.xte > 0 else 'L'
        nmea += '{:.3f},{},N,'.format(dest.xte, dir_to_steer)
    nmea += 'V,V,'  # 6,7 Not entered, not crossed
    if dest.bod is None:
        nmea += ',M,'  # 8,9 Don't have origin to destination
    else:
        nmea += '{:.1f},M,'.format(dest.bod)
    nmea += dest.wpt.name + ',' if dest.wpt is not None else ','  # Waypoint name
    nmea += '{:.1f},M'.format(dest.btw) if dest.btw is not None else ',M'  # 11,12
    nmea += '{:.1f},M'.format(dest.btw) if dest.btw is not None else ',M'  # 13,14 Keep the same as 11,12
    nmea = append_checksum(nmea)
    nmea += '\r\n'

    return nmea
