import math

from gpxpy import geo
from dest_info import DestInfo
import geomag

from Logger import Logger
from bang_control import BangControl
from Polars import Polars
from leg_analyzer import LegAnalyzer
from nmea_encoder import encode_apb, encode_rmb, encode_bwr

ARRIVAL_CIRCLE_M = 100  # Probably good enough given chart and GPS accuracy

METERS_IN_NM = 1852.


class Targets:
    def __init__(self, polars, tws, twa, sow):
        if sow is None or twa is None:
            self.boat_vmg = None
        else:
            self.boat_vmg = sow * math.cos(math.radians(twa))

        if tws is None or twa is None:
            self.target_sow = None
            self.target_vmg = None
            self.target_twa = None
        else:
            self.target_sow, self.target_twa = polars.get_targets(tws, twa)
            self.target_vmg = self.target_sow * math.cos(math.radians(self.target_twa))


class Navigator:
    __instance = None

    @staticmethod
    def get_instance():
        """ Static access method """
        if Navigator.__instance is None:
            Navigator()
        return Navigator.__instance

    def __init__(self):
        """ Virtually private constructor.  """
        if Navigator.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            Navigator.__instance = self
            self.bang_control = BangControl()
            self.mag_decl = None
            self.listeners = []
            self.route = None
            self.active_wpt_idx = None
            self.last_dest_announced_at = None
            self.polars = Polars()
            self.leg_analyzer = LegAnalyzer()

    def read_polars(self, file_name):
        self.polars.read_table(file_name)

    def add_listener(self, listener):
        self.listeners.append(listener)

    def remove_listener(self, listener):
        if listener in self.listeners:
            self.listeners.remove(listener)

    def update(self, raw_instr_data):
        Logger.set_utc(raw_instr_data.utc)

        targets = Targets(self.polars, raw_instr_data.tws, raw_instr_data.twa, raw_instr_data.sow)
        leg_summary = self.leg_analyzer.update(raw_instr_data, targets)
        if leg_summary is not None:
            for listener in self.listeners:
                listener.on_leg_summary(leg_summary)
            self.say_leg_summary(leg_summary)

        if raw_instr_data.lat is not None and raw_instr_data.lon is not None:
            if self.mag_decl is None:
                self.mag_decl = geomag.declination(raw_instr_data.lat, raw_instr_data.lon)

            if self.route is not None:
                dest_wpt = self.route.points[self.active_wpt_idx]
                dist_m = geo.distance(raw_instr_data.lat, raw_instr_data.lon, 0,
                                      dest_wpt.latitude, dest_wpt.longitude, 0, False)

                course_true = geo.get_course(raw_instr_data.lat, raw_instr_data.lon,
                                             dest_wpt.latitude, dest_wpt.longitude)
                dest_info = DestInfo()
                dest_info.wpt = dest_wpt
                dest_info.dtw = dist_m / METERS_IN_NM
                dest_info.xis_in_circle = dist_m < ARRIVAL_CIRCLE_M

                dest_info.btw_true = course_true
                dest_info.btw = course_true - self.mag_decl

                # Get angle to waypoint (left or right)
                if raw_instr_data.hdg is not None:
                    dest_info.atw = dest_info.btw - raw_instr_data.hdg  # Both angles are magnetic
                else:
                    dest_info.atw = course_true - raw_instr_data.cog  # Both angles are true

                dest_info.stw = raw_instr_data.sog * math.cos(math.radians(dest_info.atw))

                # Get angle to waypoint (up or down relative to the wind)
                if raw_instr_data.awa is not None:
                    # If wind angle and angle to waypoint have the same sign, then waypoint is upwind
                    dest_info.atw_up = dest_info.atw * raw_instr_data.awa > 0

                if self.active_wpt_idx > 0:
                    orig_wpt = self.route.points[self.active_wpt_idx-1]
                    bod_true = geo.get_course(orig_wpt.latitude, orig_wpt.longitude,
                                              dest_wpt.latitude, dest_wpt.longitude)
                    dest_info.bod = bod_true - self.mag_decl

                    loc = geo.Location(latitude=raw_instr_data.lat, longitude=raw_instr_data.lon)
                    dest_info.xte = geo.distance_from_line(loc, orig_wpt, dest_wpt) / METERS_IN_NM

                for listener in self.listeners:
                    listener.on_dest_info(raw_instr_data, dest_info)

                Logger.log('< ' + encode_apb(dest_info))
                Logger.log('< ' + encode_rmb(dest_info))
                Logger.log('< ' + encode_bwr(raw_instr_data, dest_info))

                self.say_dest_info(dest_info, raw_instr_data)

    def set_route(self, route, active_wpt_idx):
        self.active_wpt_idx = active_wpt_idx
        self.route = route

    def tack(self):
        for listener in self.listeners:
            listener.on_speech('Tacking')
        if self.bang_control.is_connected():
            return self.bang_control.tack()
        return False

    def steer(self, degrees):
        for listener in self.listeners:
            listener.on_speech('Tacking')

        if self.bang_control.is_connected():
            return self.bang_control.steer(degrees)
        return False

    def say_dest_info(self, dest_info, raw_instr_data):
        if self.last_dest_announced_at is not None:
            since_last_speech = (raw_instr_data.utc - self.last_dest_announced_at).total_seconds()
        else:
            since_last_speech = 3600
        if since_last_speech >= 60:
            self.last_dest_announced_at = raw_instr_data.utc
            if dest_info.atw_up is not None:
                s = 'Mark {} is {:.0f} degrees {}'.format(dest_info.wpt.name,
                                                          abs(dest_info.atw),
                                                          'up' if dest_info.atw_up else 'down')
            else:
                s = 'Mark {} is {:.0f} degrees to the {}'.format(dest_info.wpt.name,
                                                                 abs(dest_info.atw),
                                                                 'right' if dest_info.atw > 0 else 'left')
            for listener in self.listeners:
                listener.on_speech(s)

    def say_leg_summary(self, leg_summary):
        phrase = ''
        if leg_summary.delta_dist_wind_m is not None:
            direction = 'gained' if leg_summary.delta_dist_wind_m > 0 else 'lost'
            phrase += 'You {} {:.0f} meters to the target boat. '.format(direction,
                                                                         abs(leg_summary.delta_dist_wind_m))

        if leg_summary.delta_boat_speed_perc is not None:
            direction = 'faster' if leg_summary.delta_boat_speed_perc > 0 else 'slower'
            phrase += 'You were {:.0f} percent {} than target. '.format(abs(leg_summary.delta_boat_speed_perc),
                                                                        direction)

        if leg_summary.avg_delta_twa is not None:
            direction = 'higher' if leg_summary.avg_delta_twa > 0 else 'lower'
            phrase += 'You were sailing {:.0f} degrees {} than target. '.format(abs(leg_summary.avg_delta_twa),
                                                                                direction)

        if len(phrase) > 0:
            for listener in self.listeners:
                listener.on_speech(phrase)
