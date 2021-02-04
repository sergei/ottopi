from gpxpy import geo
from dest_info import DestInfo
import geomag


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
            self.listeners = []
            self.dest_wpt = None
            self.mag_decl = None

    def add_listener(self, listener):
        self.listeners.append(listener)

    def remove_listener(self, listener):
        if listener in self.listeners:
            self.listeners.remove(listener)

    def update(self, raw_instr_data):
        if self.dest_wpt is not None:
            if raw_instr_data.lat is not None and raw_instr_data.lon is not None:
                if self.mag_decl is None:
                    self.mag_decl = geomag.declination(raw_instr_data.lat, raw_instr_data.lon)
                dist_m = geo.distance(raw_instr_data.lat, raw_instr_data.lon, 0,
                                      self.dest_wpt.latitude, self.dest_wpt.longitude, 0, False)
                course_true = geo.get_course(raw_instr_data.lat, raw_instr_data.lon,
                                             self.dest_wpt.latitude, self.dest_wpt.longitude)
                dest_info = DestInfo()
                dest_info.wpt = self.dest_wpt
                dest_info.dtw = dist_m / 1852.
                dest_info.btw = course_true - self.mag_decl

                # Get angle to waypoint (left or right)
                if raw_instr_data.hdg is not None:
                    dest_info.atw = dest_info.btw - raw_instr_data.hdg  # Both angles are magnetic
                else:
                    dest_info.atw = course_true - raw_instr_data.cog  # Both angles are true

                # Get angle to waypoint (up or down relative to the wind)
                if raw_instr_data.awa is not None:
                    # If wind angle and angle to waypoint have the same sign, then waypoint is upwind
                    dest_info.atw_up = dest_info.atw * raw_instr_data.awa > 0

                for listener in self.listeners:
                    listener.set_dest_info(dest_info)

    def set_destination(self, dest_wpt):
        self.dest_wpt = dest_wpt
