import copy
import os
import threading
import gpxpy
import gpxpy.gpx

import conf
from raw_instr_data import RawInstrData


class DataRegistry:

    def __init__(self):
        self.raw_instr_data = RawInstrData()
        self.lock = threading.Lock()
        self.wpts_gpx = None
        self.data_dir = None

    def set_data_dir(self, data_dir):
        self.data_dir = os.path.expanduser(data_dir)
        if not os.path.isdir(self.data_dir):
            os.makedirs(self.data_dir)

    def set_raw_instr_data(self, raw_instr_data):
        with self.lock:
            self.raw_instr_data = raw_instr_data

    def get_raw_instr_data(self):
        with self.lock:
            raw_instr_data = copy.copy(self.raw_instr_data)
        return raw_instr_data

    def get_raw_instr_data_dict(self):
        with self.lock:
            raw_instr_data = copy.copy(self.raw_instr_data)

        if raw_instr_data is not None:
            return raw_instr_data.__dict__
        else:
            return {}

    def read_gpx_file(self):
        file_name = os.path.expanduser(self.data_dir + os.sep + conf.GPX_ARCHIVE_NAME)
        try:
            with open(file_name, 'r') as gpx_file:
                try:
                    self.wpts_gpx = gpxpy.parse(gpx_file)
                    for waypoint in self.wpts_gpx.waypoints:
                        print('waypoint {0} -> ({1},{2})'.format(waypoint.name, waypoint.latitude, waypoint.longitude))
                except Exception as e:
                    print(e)
        except IOError as error:
            print(error)

    def get_wpts(self):
        return self.wpts_gpx.waypoints if self.wpts_gpx is not None else []

    def store_active_route(self, route):
        gpx = gpxpy.gpx.GPX()
        gpx.routes.append(route)
        gpx_name = self.data_dir + os.sep + conf.GPX_CUR_ROUTE_NAME
        with open(gpx_name, 'wt') as f:
            f.write(gpx.to_xml())
            print('Route stored to {}'.format(gpx_name))

    def restore_active_route(self):
        gpx_name = self.data_dir + os.sep + conf.GPX_CUR_ROUTE_NAME
        if os.path.isfile(gpx_name):
            with open(gpx_name, 'rt') as f:
                try:
                    gpx = gpxpy.parse(f)
                    if len(gpx.routes) > 0:
                        active_route = gpx.routes[0]
                        print('Restored active route {}'.format(active_route))
                        return active_route
                except Exception as e:
                    print(e)
        return None
