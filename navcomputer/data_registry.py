import copy
import threading
import gpxpy
import gpxpy.gpx
from raw_instr_data import RawInstrData


class DataRegistry:
    __instance = None

    @staticmethod
    def get_instance():
        """ Static access method """
        if DataRegistry.__instance is None:
            DataRegistry()
        return DataRegistry.__instance

    def __init__(self):
        """ Virtually private constructor.  """
        if DataRegistry.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            DataRegistry.__instance = self
            self.raw_instr_data = RawInstrData()
            self.lock = threading.Lock()
            self.gpx = None

    def set_raw_instr_data(self, raw_instr_data):
        with self.lock:
            self.raw_instr_data = raw_instr_data

    def get_raw_instr_data_dict(self):
        with self.lock:
            raw_instr_data = copy.copy(self.raw_instr_data)

        if raw_instr_data is not None:
            return raw_instr_data.__dict__
        else:
            return {}

    def read_gpx_file(self, file_name):
        with open(file_name, 'r') as gpx_file:
            try:
                self.gpx = gpxpy.parse(gpx_file)
                for waypoint in self.gpx.waypoints:
                    print('waypoint {0} -> ({1},{2})'.format(waypoint.name, waypoint.latitude, waypoint.longitude))
            except Exception as e:
                print(e)
