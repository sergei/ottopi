from functools import reduce
# Make APB, BWR and RMB


def encode_apb(dest):
    """ https://gpsd.gitlab.io/gpsd/NMEA.html#_apb_autopilot_sentence_b """
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


def encode_bwr(instr, dest):
    """ https://gpsd.gitlab.io/gpsd/NMEA.html#_bwr_bearing_and_distance_to_waypoint_rhumb_line """
    nmea = '$OPBWR,'
    nmea += '{:02d}{:02d}{:02d},'.format(instr.utc.hour, instr.utc.minute, instr.utc.second)  # 1 UTC
    nmea += encode_coord(dest.wpt.latitude, ['N', 'S'])  # 2,3 Waypoint Latitude
    nmea += encode_coord(dest.wpt.longitude, ['E', 'W'])  # 4,5 Waypoint Longitude
    nmea += ',T,'  # 6 Bearing, degrees True
    nmea += '{:.1f},M,'.format(dest.btw) if dest.btw is not None else ',M,'  # 8,9 Bearing, degrees Magnetic
    nmea += '{:.3f},N,'.format(dest.dtw) if dest.dtw is not None else ',N,'  # 10,11 Distance, Nautical Miles
    nmea += '{},'.format(dest.wpt.name)  # 12 Waypoint ID
    nmea += ','  # 13 FAA mode indicator (NMEA 2.3 and later, optional)
    nmea = append_checksum(nmea)
    nmea += '\r\n'

    return nmea


def encode_coord(coord, hemispheres):
    hemisphere = hemispheres[0] if coord > 0 else hemispheres[1]
    coord = abs(coord)
    degrees = int(coord)
    minutes = (coord - degrees) * 60.

    return '{}{:.5f},{},'.format(degrees, minutes, hemisphere)


def append_checksum(nmea):
    cc = reduce(lambda i, j: int(i) ^ int(j), [ord(x) for x in nmea[1:]])  # Exclude $ sign
    return nmea + '*{:02X}'.format(cc)
