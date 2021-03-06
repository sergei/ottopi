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
        self.gpx = None
        self.data_dir = None
        self.dest_info = None

    def set_data_dir(self, data_dir):
        self.data_dir = os.path.expanduser(data_dir)
        if not os.path.isdir(self.data_dir):
            os.makedirs(self.data_dir)

    def set_dest_info(self, dest_info):
        with self.lock:
            self.dest_info = dest_info

    def get_dest_info(self):
        with self.lock:
            dest_info = copy.copy(self.dest_info)
        return dest_info

    def set_raw_instr_data(self, raw_instr_data):
        with self.lock:
            self.raw_instr_data = raw_instr_data

    def get_raw_instr_data(self):
        with self.lock:
            raw_instr_data = copy.copy(self.raw_instr_data)
        return raw_instr_data

    def read_gpx_file(self, file_name):
        try:
            with open(file_name, 'r') as gpx_file:
                try:
                    self.gpx = gpxpy.parse(gpx_file)
                    for waypoint in self.gpx.waypoints:
                        print('waypoint {0} -> ({1},{2})'.format(waypoint.name, waypoint.latitude, waypoint.longitude))
                    for route in self.gpx.routes:
                        print('Route {0}'.format(route.name))
                    return True
                except Exception as e:
                    print(e)
        except IOError as error:
            print(error)
        return False

    def get_wpts(self):
        return self.gpx.waypoints if self.gpx is not None else []

    def get_routes(self):
        return self.gpx.routes if self.gpx is not None else []

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

    def clear_active_route(self):
        self.dest_info = None
        gpx_name = self.data_dir + os.sep + conf.GPX_CUR_ROUTE_NAME
        if os.path.isfile(gpx_name):
            os.unlink(gpx_name)
