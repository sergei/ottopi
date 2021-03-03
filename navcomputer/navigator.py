import datetime
import math

import gpxpy
from gpxpy import geo
from gpxpy.gpx import GPXRoutePoint

from const import METERS_IN_NM
from dest_info import DestInfo
import geomag

from logger import Logger
from bang_control import BangControl
from phrf_table import PhrfTable
from polars import Polars
from leg_analyzer import LegAnalyzer
from data_registry import DataRegistry
from nmea_encoder import encode_apb, encode_rmb, encode_bwr
from timer_talker import TimerTalker

BROKEN_SOW_SPD_THR = 4  # SOG must be greater than that while SOW is zero for SOW to be invalidated
BROKEN_SOW_CNT_THR = 60  # The test above must pass that many times for SOW to be invalidated

ARRIVAL_CIRCLE_M = 100  # Probably good enough given chart and GPS accuracy


class Targets:
    def __init__(self, polars, tws, twa, sow, sog):
        self.bs = sow if sow is not None else sog

        if self.bs is None or twa is None:
            self.boat_vmg = None
        else:
            # Compute VMG
            self.boat_vmg = self.bs * math.cos(math.radians(twa))

        if self.bs is None or twa is None:
            self.target_sow = None
            self.target_vmg = None
            self.target_twa = None
        else:
            # Compute target speed and angle
            self.target_sow, self.target_twa = polars.get_targets(tws, twa)
            if self.target_sow is not None and self.target_twa is not None:
                # Compute target vmg
                self.target_vmg = self.target_sow * math.cos(math.radians(self.target_twa))
            else:
                self.target_vmg = None


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
            self.data_registry = DataRegistry()
            self.bang_control = BangControl()
            self.polars = Polars()
            self.leg_analyzer = LegAnalyzer()
            self.mag_decl = None
            self.listeners = []
            self.active_route = None
            self.active_wpt_idx = None
            self.last_dest_announced_at = None
            self.race_starts_at = None
            self.phrf_table = PhrfTable()
            self.timer_talker = TimerTalker()
            self.sow_is_broken = False
            self.sow_broken_cnt = 0

    def get_data_dir(self):
        return self.data_registry.data_dir

    def set_data_dir(self, data_dir):
        self.data_registry.set_data_dir(data_dir)

    def read_polars(self, file_name):
        self.polars.read_table(file_name)

    def set_polars(self, polars):
        self.polars = polars

    def add_listener(self, listener):
        self.listeners.append(listener)

    def remove_listener(self, listener):
        if listener in self.listeners:
            self.listeners.remove(listener)

    def get_raw_instr_data(self):
        return self.data_registry.get_raw_instr_data()

    def get_dest_info(self):
        return self.data_registry.get_dest_info()

    def announce_current_route(self):
        route = self.active_route
        if route is not None:
            phrase = 'Route {}. Navigating to {}'.format(route.name, route.points[self.active_wpt_idx].name)
        else:
            phrase = 'No route is selected'

        for listener in self.listeners:
            listener.on_speech(phrase)

        dest_info = self.data_registry.get_dest_info()
        if dest_info is not None:
            raw_instr_data = self.data_registry.get_raw_instr_data()
            if raw_instr_data is not None:
                self.say_dest_info(dest_info, raw_instr_data, say_now=True)

    def set_raw_instr_data(self, raw_instr_data):
        Logger.set_utc(raw_instr_data.utc)

        # Validate input data
        self.validate_data(raw_instr_data)

        # We might need to compute true wind angle, since some instruments don't have it in NMEA stream
        if raw_instr_data.twa is None and raw_instr_data.tws is None:
            if raw_instr_data.awa is not None and raw_instr_data.aws is not None:
                bs = raw_instr_data.sow if raw_instr_data.sow is not None else raw_instr_data.sog
                if bs is not None:
                    raw_instr_data.tws, raw_instr_data.twa = Navigator.compute_tws_twa(aws=raw_instr_data.aws,
                                                                                       awa=raw_instr_data.awa, bs=bs)

        # Store validated instruments data
        self.data_registry.set_raw_instr_data(raw_instr_data)

        for listener in self.listeners:
            listener.on_instr_data(raw_instr_data)

        # Compute target values
        targets = Targets(self.polars, raw_instr_data.tws, raw_instr_data.twa, raw_instr_data.sow, raw_instr_data.sog)

        for listener in self.listeners:
            listener.on_targets(targets)

        # Update the leg stats
        leg_summary, wind_shift = self.leg_analyzer.update(raw_instr_data, targets)

        if leg_summary is not None:
            for listener in self.listeners:
                listener.on_leg_summary(leg_summary)
            self.say_leg_summary(leg_summary)

        if wind_shift is not None:
            self.say_wind_shift(wind_shift)
            for listener in self.listeners:
                listener.event_callbacks(wind_shift)

        if raw_instr_data.lat is not None and raw_instr_data.lon is not None:
            if self.mag_decl is None:
                self.mag_decl = geomag.declination(raw_instr_data.lat, raw_instr_data.lon)

            if self.active_route is not None:
                dest_wpt = self.active_route.points[self.active_wpt_idx]
                dist_m = geo.distance(raw_instr_data.lat, raw_instr_data.lon, 0,
                                      dest_wpt.latitude, dest_wpt.longitude, 0, False)

                course_true = geo.get_course(raw_instr_data.lat, raw_instr_data.lon,
                                             dest_wpt.latitude, dest_wpt.longitude)
                dest_info = DestInfo()
                dest_info.wpt = dest_wpt
                dest_info.dtw = dist_m / METERS_IN_NM
                dest_info.is_in_circle = dist_m < ARRIVAL_CIRCLE_M

                dest_info.btw_true = course_true
                dest_info.btw = course_true - self.mag_decl

                # Get angle to waypoint (left or right)
                if raw_instr_data.hdg is not None:
                    dest_info.atw = dest_info.btw - raw_instr_data.hdg  # Both angles are magnetic
                elif raw_instr_data.cog is not None:
                    dest_info.atw = course_true - raw_instr_data.cog  # Both angles are true

                if dest_info.atw is not None:
                    dest_info.stw = raw_instr_data.sog * math.cos(math.radians(dest_info.atw))

                # Get angle to waypoint (up or down relative to the wind)
                if raw_instr_data.awa is not None and dest_info.atw is not None:
                    # If wind angle and angle to waypoint have the same sign, then waypoint is upwind
                    dest_info.atw_up = dest_info.atw * raw_instr_data.awa > 0

                if self.active_wpt_idx > 0:
                    orig_wpt = self.active_route.points[self.active_wpt_idx - 1]
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

                # Consider switching to the next waypoint
                self.next_wpt(dest_info)

                self.data_registry.set_dest_info(dest_info)

    def clear_dest(self):
        self.data_registry.clear_active_route()
        self.active_route = None
        self.active_wpt_idx = None

    def goto_wpt(self, dest_wpt):
        gpx_route = gpxpy.gpx.GPXRoute(name="TO WPT")
        gpx_route.points.append(dest_wpt)

        instr_data = self.data_registry.get_raw_instr_data()
        if instr_data is not None:
            orig_wpt = GPXRoutePoint(name="HERE", latitude=instr_data.lat, longitude=instr_data.lon)
            gpx_route.points.insert(0, orig_wpt)

        self.set_route(gpx_route, len(gpx_route.points)-1)

    def set_route(self, route, active_wpt_idx):
        print('Set new active route {}'.format(route))
        self.active_wpt_idx = active_wpt_idx % route.get_points_no()
        self.active_route = route
        self.data_registry.store_active_route(route)

        phrase = 'Selected route {}. Next mark is {}'.format(route.name, route.points[self.active_wpt_idx].name)
        for listener in self.listeners:
            listener.on_speech(phrase)

    def get_dest_wpt(self):
        if self.active_route is not None:
            return self.active_route.points[-1]
        else:
            return None

    def read_gpx_file(self, file_name):
        return self.data_registry.read_gpx_file(file_name)

    def restore_active_route(self):
        route = self.data_registry.restore_active_route()
        if route is not None:
            self.active_route = route
            if route.number is not None:
                self.active_wpt_idx = route.number % self.active_route.get_points_no()
            else:
                self.active_wpt_idx = self.active_route.get_points_no() - 1

    def get_wpts(self):
        return self.data_registry.get_wpts()

    def get_routes(self):
        return self.data_registry.get_routes()

    def get_active_route(self):
        return self.active_route, self.active_wpt_idx

    def tack(self):
        for listener in self.listeners:
            listener.on_speech('Tacking')
        if self.bang_control.is_connected():
            return self.bang_control.tack()
        return False

    def steer(self, degrees):
        sign = 'plus' if degrees > 0 else 'minus'
        phrase = 'Steering {} {} degrees'.format(sign, abs(degrees))

        for listener in self.listeners:
            listener.on_speech(phrase)

        if self.bang_control.is_connected():
            return self.bang_control.steer(degrees)
        return False

    def say_dest_info(self, dest_info, raw_instr_data, say_now=False):
        if self.last_dest_announced_at is not None:
            since_last_speech = (raw_instr_data.utc - self.last_dest_announced_at).total_seconds()
        else:
            since_last_speech = 3600

        if since_last_speech >= 60 or say_now:
            self.last_dest_announced_at = raw_instr_data.utc
            if dest_info.atw_up is not None:
                s = 'Mark {} is {:.0f} degrees {}'.format(dest_info.wpt.name,
                                                          abs(dest_info.atw),
                                                          'up' if dest_info.atw_up else 'down')
            elif dest_info.atw is not None:
                s = 'Mark {} is {:.0f} degrees to the {}'.format(dest_info.wpt.name,
                                                                 abs(dest_info.atw),
                                                                 'right' if dest_info.atw > 0 else 'left')
            else:
                s = None

            if s is not None:
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

    def say_wind_shift(self, wind_shift):
        angle_direction = 'lifted' if wind_shift.is_lift else 'headed'
        wind_direction = 'veered' if wind_shift.shift_deg > 0 else 'backed'
        phrase = 'Wind {} by {:.0f} degrees. You got {} '.format(wind_direction, abs(wind_shift.shift_deg),
                                                                 angle_direction)
        for listener in self.listeners:
            listener.on_speech(phrase)

    def next_wpt(self, dest_info):
        if dest_info.is_in_circle:
            if self.active_wpt_idx >= len(self.active_route.points) - 1:
                return  # We are at last point, nowhere to move

            # Advance to the next one
            old_name = self.active_route.points[self.active_wpt_idx].name
            self.active_wpt_idx += 1
            new_name = self.active_route.points[self.active_wpt_idx].name
            phrase = 'Arrived to {} mark. Next mark is {}'.format(old_name, new_name)
            for listener in self.listeners:
                listener.on_speech(phrase)

    def get_history(self):
        return self.leg_analyzer.summaries

    def timer_start(self):
        now = datetime.datetime.now()
        secs_to_start = 5 * 60
        self.race_starts_at = now + datetime.timedelta(0, secs_to_start)
        return self.timer_talker.start_timer(secs_to_start)

    def timer_stop(self):
        self.race_starts_at = None
        return self.timer_talker.stop_timer()

    def timer_sync(self):
        if self.race_starts_at is not None:
            now = datetime.datetime.now()
            secs_to_start = (self.race_starts_at - now).total_seconds()
            if secs_to_start > 0:
                secs_to_start = int(round(secs_to_start/60.)) * 60
                self.race_starts_at = now + datetime.timedelta(0, secs_to_start)
                return self.timer_talker.update_timer(secs_to_start)
        return False

    def read_phrf_table(self, file_name):
        return self.phrf_table.read_file(file_name)

    def is_timer_active(self):
        return self.race_starts_at is not None

    def get_elapsed_time(self):
        now = datetime.datetime.now()
        elapsed_time = (now - self.race_starts_at).total_seconds()
        return elapsed_time

    def get_phrf_timers(self):
        if len(self.phrf_table.table) == 0:
            return []

        if self.active_route is None:
            return []

        dist = self.active_route.length() / METERS_IN_NM
        corrected_times = self.phrf_table.get_corrected_times(dist, self.get_elapsed_time())
        phrf_timers = []
        for entry in self.phrf_table.table:
            phrf_timers.append({
                'name': entry.name,
                'phrf_rating': entry.phrf,
                'corrected_time': corrected_times[entry.phrf],
            })

        return phrf_timers

    @staticmethod
    def compute_tws_twa(aws, awa, bs):
        """ Compute True wind angle and direction
        """
        # Compute true wind speed
        cos_awa = math.cos(math.radians(awa))
        tws = aws * aws + bs * bs - 2 * aws * bs * cos_awa

        # Do sanity test, if TWS is too small assume it's the same as apparent wind
        if tws < 1:
            return aws, awa

        tws = math.sqrt(tws)
        r = (aws * cos_awa - bs) / tws

        # Just a sanity test before doing cosine
        if r > 1:
            r = 1
        elif r < -1:
            r = -1

        twa_rad = math.acos(r)
        # Assign the same sign as awa
        if awa < 0:
            twa_rad = - twa_rad

        return tws, math.degrees(twa_rad)

    def validate_data(self, raw_instr_data):
        # Javelin has the water speed sensor that always reads zero. Invalidate it if see the discrepancy with GPS SOG
        if raw_instr_data.sow is not None:
            if raw_instr_data.sow == 0 and raw_instr_data.sog > BROKEN_SOW_SPD_THR:
                self.sow_broken_cnt += 1

            if raw_instr_data.sow > 0:
                self.sow_broken_cnt = 0

            if self.sow_broken_cnt > BROKEN_SOW_CNT_THR:
                self.sow_is_broken = True
            else:
                self.sow_is_broken = False

        if self.sow_is_broken:
            raw_instr_data.sow = None
