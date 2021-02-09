import datetime
import time
from functools import reduce
import geomag
import gpxpy
from gpxpy.gpx import GPXWaypoint, GPXRoutePoint

from Logger import Logger
from raw_instr_data import RawInstrData

SPEED_FACTOR = {
    'N': 1,            # Knots
    'M': 3600./1852.,  # Meters per second
    'K': 1000./1852.   # Kilometers per hour
}


class NmeaParser:
    def __init__(self, data_registry):
        self.data_registry = data_registry
        self.mag_decl = None
        self.last_dest_wpt = None  # Last WPT received from RMB message
        # Cache most recent instrument readings
        self.awa = None  # Apparent wind angle degrees
        self.awa_t = 0
        self.aws = None  # Apparent wind speed KTS
        self.aws_t = 0
        self.twa = None  # True wind angle degrees
        self.twa_t = 0
        self.tws = None  # True wind speed KTS
        self.tws_t = 0
        self.sow = None  # Speed over water KTS
        self.sow_t = 0
        self.hdg_true = None
        self.hdg_true_t = 0
        self.hdg_mag = None
        self.hdg_mag_t = 0
        self.utc = None
        self.utc_t = 0
        self.lat = None
        self.lat_t = 0
        self.lon = None
        self.lon_t = 0
        self.sog = None
        self.sog_t = 0
        self.cog_true = None
        self.cog_true_t = 0

    def set_nmea_sentence(self, nmea_sentence):
        Logger.log('> ' + nmea_sentence)
        # print('Got [{}]'.format(nmea_sentence))
        # Verify optional checksum
        nmea_sentence = nmea_sentence.rstrip()
        cc_idx = nmea_sentence.find('*')
        if cc_idx >= 0 and (len(nmea_sentence) - cc_idx - 1) == 2:
            body = nmea_sentence[1:cc_idx]  # string between $ and *
            received_cc = int(nmea_sentence[cc_idx+1:], 16)
            computed_crc = reduce(lambda i,j: int(i) ^ int(j), [ord(x) for x in body])
            if received_cc != computed_crc:
                return
            else:
                nmea_sentence = nmea_sentence[0:cc_idx]

        t = nmea_sentence.split(',')
        if t[0].endswith('MWV'):
            self.parse_mwv(t)
        elif t[0].endswith('VHW'):
            self.parse_vhw(t)
        elif t[0].endswith('HDG'):
            self.parse_hdg(t)
        elif t[0].endswith('RMB'):
            self.parse_rmb(t)
        elif t[0].endswith('RMC'):
            self.parse_rmc(t)

    def parse_mwv(self, t):
        """ https://gpsd.gitlab.io/gpsd/NMEA.html#_mwv_wind_speed_and_angle """
        if t[2] == 'R':
            if t[5] == 'A':
                self.awa = float(t[1])
                self.awa_t = time.time()
                self.aws = float(t[3]) * SPEED_FACTOR[t[4]]
                self.aws_t = time.time()
            else:
                self.awa = None
                self.awa_t = time.time()
                self.aws = None
                self.aws_t = time.time()
        elif t[2] == 'T':
            if t[5] == 'A':
                self.twa = float(t[1])
                self.twa_t = time.time()
                self.tws = float(t[3]) * SPEED_FACTOR[t[4]]
                self.tws_t = time.time()
            else:
                self.twa = None
                self.twa_t = time.time()
                self.tws = None
                self.tws_t = time.time()

    def parse_vhw(self, t):
        """ https://gpsd.gitlab.io/gpsd/NMEA.html#_vhw_water_speed_and_heading """
        self.hdg_true = float(t[1]) if len(t[1]) > 0 else None
        self.hdg_true_t = time.time()
        self.hdg_mag = float(t[3]) if len(t[3]) > 0 else None
        self.hdg_mag_t = time.time()
        self.sow = float(t[7])*SPEED_FACTOR['K'] if len(t[7]) > 0 else None
        self.sow = float(t[5]) if len(t[5]) > 0 else None
        self.sow_t = time.time()

    def parse_hdg(self, t):
        """ https://gpsd.gitlab.io/gpsd/NMEA.html#_hdg_heading_deviation_variation """
        self.hdg_mag = float(t[1]) if len(t[1]) > 0 else None
        self.hdg_mag_t = time.time()

    def parse_rmb(self, t):
        """ https://gpsd.gitlab.io/gpsd/NMEA.html#_rmb_recommended_minimum_navigation_information """
        if t[1] == 'A':
            name = t[5]
            lat = self.parse_coord(t[6], t[7])
            lon = self.parse_coord(t[8], t[9])
            if self.last_dest_wpt is not None:
                # Check if we got the same wpt once again, so we would ignore it
                same_name = name == self.last_dest_wpt.name
                same_lat = abs(lat - self.last_dest_wpt.latitude) < 1.e-5
                same_lon = abs(lon - self.last_dest_wpt.longitude) < 1.e-5
                if same_name and same_lat and same_lon:
                    return

            dest_wpt = GPXRoutePoint(name=name, latitude=lat, longitude=lon)
            self.last_dest_wpt = dest_wpt
            gpx_route = gpxpy.gpx.GPXRoute(name="RMB")
            gpx_route.points.append(dest_wpt)
            if self.lat is not None:
                orig_wpt = GPXRoutePoint(name="HERE", latitude=self.lat, longitude=self.lon)
                gpx_route.points.insert(0, orig_wpt)

            self.data_registry.set_active_route(gpx_route)

    def parse_rmc(self, t):
        """ https://gpsd.gitlab.io/gpsd/NMEA.html#_rmc_recommended_minimum_navigation_information """
        hour = int(t[1][0:2])
        minute = int(t[1][2:4])
        sec = int(t[1][4:6])
        day = int(t[9][0:2])
        month = int(t[9][2:4])
        year = int(t[9][4:6]) + 2000
        self.utc = datetime.datetime(year, month, day, hour, minute, sec, tzinfo=datetime.timezone.utc)
        self.utc_t = time.time()
        if t[2] == 'A':
            self.lat = self.parse_coord(t[3], t[4])
            self.lat_t = time.time()
            self.lon = self.parse_coord(t[5], t[6])
            self.lon_t = time.time()
        if len(t[7]) > 0:
            self.sog = float(t[7])
            self.sog_t = time.time()
        if len(t[8]) > 0:
            self.cog_true = float(t[8])
            self.cog_true_t = time.time()

        # Update instruments data when have valid GPS fix
        if t[2] == 'A':
            self.set_raw_instr_data()

    def parse_coord(self, p, hemi):
        dot_idx = p.find('.')
        deg_len = 2 if dot_idx == 4 else 3
        deg = float(p[0:deg_len])
        minutes = float(p[deg_len:])
        sign = 1 if hemi in 'NE' else -1
        return sign * (deg + minutes/60.)

    def set_raw_instr_data(self):
        if self.mag_decl is None and self.lat is not None and self.lon is not None:
            self.mag_decl = geomag.declination(self.lat, self.lon)
            print('Set magnetic declination to {} degrees'.format(self.mag_decl))

        now = int(time.time())
        exp_time = now - 10  # Ten seconds expiration time
        utc = None if self.utc_t < exp_time else self.utc
        lat = None if self.lat_t < exp_time else self.lat
        lon = None if self.lon_t < exp_time else self.lon
        sog = None if self.sog_t < exp_time else self.sog
        cog = None if self.cog_true_t < exp_time else self.cog_true - self.mag_decl
        # Sensors
        awa = None if self.awa_t < exp_time else self.awa
        aws = None if self.aws_t < exp_time else self.aws
        twa = None if self.twa_t < exp_time else self.twa
        tws = None if self.tws_t < exp_time else self.tws
        sow = None if self.sow_t < exp_time else self.sow
        hdg = None if self.hdg_mag_t < exp_time else self.hdg_mag

        self.data_registry.set_raw_instr_data(RawInstrData(t=now, utc=utc, lat=lat, lon=lon, sog=sog, cog=cog,
                                                           awa=awa, aws=aws, twa=twa, tws=tws, sow=sow, hdg=hdg))
