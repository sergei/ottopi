import copy
import os
import threading
import gpxpy
import gpxpy.gpx

from navcomputer import conf
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
            self.dest_wpt = None

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

    def get_wpts(self):
        return self.gpx.waypoints

    def set_destination(self, wpt):
        self.dest_wpt = wpt
        print('Set new destination {}'.format(self.dest_wpt))
        # Store new destination
        gpx = gpxpy.gpx.GPX()
        gpx.waypoints.append(wpt)
        gpx_name = conf.DATA_DIR + os.sep + conf.GPX_DESTINATION_NAME
        with open(gpx_name, 'wt') as f:
            f.write(gpx.to_xml())
            print('Destination stored to {}'.format(gpx_name))

    def restore_destination(self):
        gpx_name = conf.DATA_DIR + os.sep + conf.GPX_DESTINATION_NAME
        if os.path.isfile(gpx_name):
            with open(gpx_name, 'rt') as f:
                try:
                    gpx = gpxpy.parse(f)
                    if len(gpx.waypoints) > 0:
                        self.dest_wpt = gpx.waypoints[0]
                        print('Restored destination {}'.format(self.dest_wpt))
                except Exception as e:
                    print(e)

    def get_dest_wpt(self):
        return self.dest_wpt
