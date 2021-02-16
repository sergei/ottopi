from functools import reduce
# Make APB, BWR and RMB

VALID_NMEA_CHARS = [chr(x) for x in range(ord('A'), ord('Z'))]


def nmea_name(name):
    good_name = ''
    for c in name.upper():
        if c in VALID_NMEA_CHARS:
            good_name += c
        else:
            good_name += '_'

        if len(good_name) > 6:
            break

    return good_name


def encode_apb(dest):
    """ https://gpsd.gitlab.io/gpsd/NMEA.html#_apb_autopilot_sentence_b """
    nmea = '$OPAPB,'
    nmea += 'A,A,'  # 1,2  Set to valid
    if dest.xte is None:
        nmea += ',,,'  # 3,4,5 XTE is not valid
    else:
        dir_to_steer = 'R' if dest.xte > 0 else 'L'
        nmea += '{:.3f},{},N,'.format(dest.xte, dir_to_steer)
    nmea += 'A,' if dest.is_in_circle else 'V,'  # 6 Arrival Status, A = Arrival Circle Entered. V = not entered/passed
    nmea += 'V,'  # 7 Not crossed
    if dest.bod is None:
        nmea += ',M,'  # 8,9 Don't have origin to destination
    else:
        nmea += '{:.1f},M,'.format(dest.bod)
    nmea += nmea_name(dest.wpt.name) + ',' if dest.wpt is not None else ','  # Waypoint name
    nmea += '{:.1f},M,'.format(dest.btw) if dest.btw is not None else ',M'  # 11,12
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
    nmea += '{},'.format(nmea_name(dest.wpt.name))  # 12 Waypoint ID
    nmea += ''  # 13 FAA mode indicator (NMEA 2.3 and later, optional)
    nmea = append_checksum(nmea)
    nmea += '\r\n'

    return nmea


def encode_rmb(dest):
    """ https://gpsd.gitlab.io/gpsd/NMEA.html#_rmb_recommended_minimum_navigation_information """

    nmea = '$OPRMB,'
    nmea += 'A,'  # 1 Status, A = Active, V = Invalid
    if dest.xte is None:
        nmea += ',,'  # 2,3 XTE is not valid
    else:
        dir_to_steer = 'R' if dest.xte > 0 else 'L'
        nmea += '{:.3f},{},'.format(dest.xte, dir_to_steer)  # 2,3
    nmea += '{},'.format(nmea_name(dest.org_wpt.name)) if dest.org_wpt is not None else ","  # 4 Origin Waypoint ID
    nmea += '{},'.format(nmea_name(dest.wpt.name))  # 5 Destination Waypoint ID
    nmea += encode_coord(dest.wpt.latitude, ['N', 'S'])  # 6,7 Destination Waypoint Latitude
    nmea += encode_coord(dest.wpt.longitude, ['E', 'W'])  # 8,9 DestinationDestination Waypoint Longitude
    nmea += '{:.3f},'.format(dest.dtw) if dest.dtw is not None else ','  # 10 Range to destination in nautical miles
    nmea += '{:.1f},'.format(dest.btw_true) if dest.btw_true is not None else ','  # 11 Bearing to destination deg true
    nmea += '{:.1f},'.format(dest.stw) if dest.stw is not None else ','  # 12 Destination closing velocity in knots
    nmea += 'A,' if dest.is_in_circle else 'V,'  # 13 Arrival Status, A = Arrival Circle Entered. V = not entered/passed
    nmea += ''  # 14 FAA mode indicator (NMEA 2.3 and later, optional)

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
