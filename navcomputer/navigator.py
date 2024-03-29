from __future__ import annotations
import datetime
import math

import gpxpy
from gpxpy import geo
from gpxpy.geo import Location
from gpxpy.gpx import GPXRoutePoint
from typing import List

from const import METERS_IN_NM
from nav_stats import NavStatsEventsListener, NavStats
from navigator_listener import DestInfo, WindShift, HistoryItem, NavigationListener
import geomag

from logger import Logger
from bang_control import BangControl
from phrf_table import PhrfTable
from polars import Polars
from data_registry import DataRegistry
from nmea_encoder import encode_apb, encode_rmb, encode_bwr
from raw_instr_data import RawInstrData
from speech_moderator import SpeechEntryType, SpeechEntry, SpeechModerator
from timer_talker import TimerTalker


BROKEN_SOW_SPD_THR = 4  # SOG must be greater than that while SOW is zero for SOW to be invalidated
BROKEN_SOW_CNT_THR = 60  # The test above must pass that many times for SOW to be invalidated

ARRIVAL_CIRCLE_M = 100  # Probably good enough given chart and GPS accuracy


class Targets:
    def __init__(self, polars=None, tws=None, twa=None, sow=None, sog=None):
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


class StatsEventsListener(NavStatsEventsListener):
    avg_twd: float
    speech_moderator: SpeechModerator
    listeners: List[NavigationListener]

    def __init__(self, listeners: List[NavigationListener], speech_moderator: SpeechModerator,
                 nav_history: List[HistoryItem]):
        self.listeners = listeners
        self.speech_moderator = speech_moderator
        self.nav_history = nav_history
        # noinspection PyTypeChecker
        self.avg_twd = None

    def on_tack(self, utc, loc, is_tack, distance_loss_m):
        maneuver = 'tack' if is_tack else 'gybe'
        direction = 'lost' if distance_loss_m > 0 else 'gained'

        phrase = 'You {} {:.0f} meters on this {}'.format(direction, abs(distance_loss_m), maneuver)
        self.speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_EVENT, utc, phrase))
        for listener in self.listeners:
            listener.on_tack(utc, loc, is_tack, distance_loss_m)

    def on_mark_rounding(self, utc, loc, is_windward):
        for listener in self.listeners:
            listener.on_mark_rounding(utc, loc, is_windward)

    def on_wind_shift(self, utc, loc, shift_deg, new_twd, is_lift):
        angle_direction = 'lifted' if is_lift else 'headed'
        wind_direction = 'veered' if shift_deg > 0 else 'backed'
        phrase = 'Wind {} by {:.0f} degrees. You got {} '.format(wind_direction, abs(shift_deg),
                                                                 angle_direction)

        self.speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_EVENT, utc, phrase))

        wind_shift = WindShift(utc, shift_deg, is_lift)
        for listener in self.listeners:
            listener.on_wind_shift(wind_shift)

    def on_history_update(self, utc, loc_from, loc, avg_hdg, avg_twa):
        orig_wpt = GPXRoutePoint(name='', latitude=loc_from.latitude, longitude=loc_from.longitude)
        dest_wpt = GPXRoutePoint(name='', latitude=loc.latitude, longitude=loc.longitude)
        history_item = HistoryItem(utc=utc, orig=orig_wpt, dest=dest_wpt, avg_boat_twa=avg_twa, avg_hdg=avg_hdg)
        self.avg_twd = (avg_hdg + avg_twa) % 360

        self.nav_history.append(history_item)
        for listener in self.listeners:
            listener.on_history_item(history_item)

    def on_target_update(self, utc: datetime, loc: Location,
                         distance_delta_m: float, speed_delta: float, twa_angle_delta: float):
        phrase = ''
        direction = 'gained' if distance_delta_m > 0 else 'lost'
        phrase += 'You {} {:.0f} meters to the target boat. '.format(direction, abs(distance_delta_m))

        direction = 'faster' if speed_delta > 0 else 'slower'
        phrase += 'You were {:.1f} knots {} than target. '.format(abs(speed_delta), direction)

        direction = 'higher' if twa_angle_delta < 0 else 'lower'
        phrase += 'You were sailing {:.0f} degrees {} than target. '.format(abs(twa_angle_delta), direction)

        self.speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_UPDATE, utc, phrase))
        for listener in self.listeners:
            listener.on_target_update(utc, loc, distance_delta_m, speed_delta, twa_angle_delta)

    def on_backup_alarm(self, utc, loc):
        phrase = 'You are moving backwards'
        self.speech_moderator.add_entry(SpeechEntry(SpeechEntryType.NAV_ALARM, utc, phrase))
        for listener in self.listeners:
            listener.on_backup_alarm(utc, loc)


class Navigator:
    __instance: Navigator
    speech_moderator: SpeechModerator
    stats_listener: StatsEventsListener
    nav_stats: NavStats
    phrf_table: PhrfTable
    timer_talker: TimerTalker
    polars: Polars
    bang_control: BangControl
    data_registry: DataRegistry
    raw_instr_data: RawInstrData

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
            self.mag_decl = None
            self.listeners = []
            self.active_route = None
            self.active_wpt_idx = None
            self.race_starts_at = None
            self.phrf_table = PhrfTable()
            self.timer_talker = TimerTalker()
            self.sow_is_broken = False
            self.sow_broken_cnt = 0
            self.nav_history = []
            self.speech_moderator = SpeechModerator(self.listeners)
            self.stats_listener = StatsEventsListener(self.listeners, self.speech_moderator, self.nav_history)
            self.nav_stats = NavStats(self.stats_listener)
            self.raw_instr_data = RawInstrData()

    def get_data_dir(self):
        return self.data_registry.data_dir

    def set_data_dir(self, data_dir):
        self.data_registry.set_data_dir(data_dir)
        self.speech_moderator.read_settings(data_dir)

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
            phrase = 'No route is active'

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
        self.nav_stats.update(raw_instr_data, targets)

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
                else:
                    dest_info.bod = None
                    dest_info.xte = None

                    # Compute wind angle at what we would be sailing from current WPT to the next one
                if self.active_wpt_idx < self.active_route.get_points_no() - 1 \
                        and self.stats_listener.avg_twd is not None:
                    flw_wpt = self.active_route.points[self.active_wpt_idx+1]
                    next_hdg = geo.get_course(dest_wpt.latitude, dest_wpt.longitude,
                                              flw_wpt.latitude, flw_wpt.longitude) - self.mag_decl
                    flw_twa = (self.stats_listener.avg_twd - next_hdg) % 360
                    if flw_twa > 180:
                        flw_twa -= 360
                    dest_info.flw_twa = flw_twa
                    dest_info.flw_wpt = flw_wpt
                else:
                    dest_info.flw_twa = None
                    dest_info.flw_wpt = None

                for listener in self.listeners:
                    listener.on_dest_info(raw_instr_data, dest_info)

                Logger.log('< ' + encode_apb(dest_info))
                Logger.log('< ' + encode_rmb(dest_info))
                Logger.log('< ' + encode_bwr(raw_instr_data, dest_info))

                self.say_dest_info(dest_info, raw_instr_data)

                # Consider switching to the next waypoint
                self.next_wpt(dest_info)

                self.data_registry.set_dest_info(dest_info)

        self.speech_moderator.say_something(raw_instr_data.utc, self.in_pre_start())

    def on_update_from_sk(self, utc, value_list):
        gps_ready = False

        for v in value_list:
            path = v['path']
            value = v['value']

            # GPS
            if 'navigation.position' == path:
                self.raw_instr_data.utc = utc
                self.raw_instr_data.lat = value['latitude']
                self.raw_instr_data.lon = value['longitude']
                gps_ready = True
            elif 'navigation.speedOverGround' == path:
                self.raw_instr_data.sog = value
            elif 'navigation.courseOverGroundTrue' == path:
                self.raw_instr_data.cog = math.degrees(value)
            # Instruments
            elif 'environment.wind.angleApparent' == path:
                self.raw_instr_data.awa = math.degrees(value)
            elif 'environment.wind.speedApparent' == path:
                self.raw_instr_data.aws = value
            elif 'environment.wind.angleTrueWater' == path:
                self.raw_instr_data.twa = math.degrees(value)
            elif 'environment.wind.speedTrue' == path:
                self.raw_instr_data.tws = value
            elif 'navigation.speedThroughWater' == path:
                self.raw_instr_data.sow = value
            elif 'navigation.headingMagnetic' == path:
                self.raw_instr_data.hdg = math.degrees(value)
            elif 'navigation.magneticVariation' == path:
                self.mag_decl = math.degrees(value)

        if gps_ready:
            self.set_raw_instr_data(self.raw_instr_data)

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

    def enable_autopilot(self, enable):
        if enable:
            phrase = 'Autopilot enabled'
        else:
            phrase = 'Autopilot off'

        for listener in self.listeners:
            listener.on_speech(phrase)

        if self.bang_control.is_connected():
            return self.bang_control.enable(enable)
        return False

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

    def announce_autopilot_state(self):
        if self.bang_control.is_connected():
            phrase = 'Autopilot is connected'
        else:
            phrase = 'No autopilot is present'
        for listener in self.listeners:
            listener.on_speech(phrase)

    def say_dest_info(self, dest_info, raw_instr_data, say_now=False):
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

        if dest_info.flw_twa is not None and s is not None:
            if abs(dest_info.flw_twa) < 45:
                point_of_sail = 'beat'
            elif abs(dest_info.flw_twa) > 145:
                point_of_sail = 'run'
            else:
                point_of_sail = 'reach'

            if dest_info.flw_twa < 0:
                board = 'port'
            else:
                board = 'starboard'

            s += ' followed by {name} on {board} {point_of_sail}'.format(name=dest_info.flw_wpt.name, board=board,
                                                                         point_of_sail=point_of_sail)

        if s is not None:
            entry_type = SpeechEntryType.DEST_UPDATE if not say_now else SpeechEntryType.NAV_EVENT
            self.speech_moderator.add_entry(SpeechEntry(entry_type, raw_instr_data.utc, s))

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
        return self.nav_history

    def timer_start(self):
        now = datetime.datetime.now()
        secs_to_start = 5 * 60
        self.race_starts_at = now + datetime.timedelta(0, secs_to_start)
        return self.timer_talker.start_timer(secs_to_start)

    def timer_stop(self):
        phrase = 'Race timer has stopped'
        self.speech_moderator.say_now(phrase)
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

    def in_pre_start(self):
        return self.is_timer_active() and self.get_elapsed_time() < 0

    def announce_timer_state(self):
        if self.in_pre_start():
            phrase = TimerTalker.format_time(-self.get_elapsed_time())
            self.speech_moderator.say_now(phrase)
        elif self.is_timer_active():
            phrase = 'Race in progress'
            self.speech_moderator.say_now(phrase)
        else:
            phrase = 'Timer is stopped'
            self.speech_moderator.say_now(phrase)

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
        # Javelin has the water speed sensor that always reads zero. Invalidate it if the discrepancy with GPS SOG
        if raw_instr_data.sow is not None and raw_instr_data.sog is not None:
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
