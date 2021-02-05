import math

from gpxpy import geo
from dest_info import DestInfo
import geomag

ARRIVAL_CIRCLE_M = 100 # Probably good enough given chart and GPS accuracy

METERS_IN_NM = 1852.


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
            self.mag_decl = None
            self.listeners = []
            self.route = None
            self.active_wpt_idx = None

    def add_listener(self, listener):
        self.listeners.append(listener)

    def remove_listener(self, listener):
        if listener in self.listeners:
            self.listeners.remove(listener)

    def update(self, raw_instr_data):
        if self.route is not None:
            if raw_instr_data.lat is not None and raw_instr_data.lon is not None:
                if self.mag_decl is None:
                    self.mag_decl = geomag.declination(raw_instr_data.lat, raw_instr_data.lon)

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

    def set_route(self, route, active_wpt_idx):
        self.active_wpt_idx = active_wpt_idx
        self.route = route
