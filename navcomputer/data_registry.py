import copy
import os
import threading
import gpxpy
import gpxpy.gpx

import conf
from navigator import Navigator
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
            self.wpts_gpx = None
            self.active_route = None

    def set_raw_instr_data(self, raw_instr_data):
        with self.lock:
            self.raw_instr_data = raw_instr_data
        Navigator.get_instance().update(self.raw_instr_data)

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
                self.wpts_gpx = gpxpy.parse(gpx_file)
                for waypoint in self.wpts_gpx.waypoints:
                    print('waypoint {0} -> ({1},{2})'.format(waypoint.name, waypoint.latitude, waypoint.longitude))
            except Exception as e:
                print(e)

    def get_wpts(self):
        return self.wpts_gpx.waypoints

    def set_active_route(self, route):
        self.active_route = route
        print('Set new active route {}'.format(self.active_route))
        Navigator.get_instance().set_route(route, route.get_points_no() - 1)
        # Store new route
        gpx = gpxpy.gpx.GPX()
        gpx.routes.append(route)
        gpx_name = conf.DATA_DIR + os.sep + conf.GPX_CUR_ROUTE_NAME
        with open(gpx_name, 'wt') as f:
            f.write(gpx.to_xml())
            print('Route stored to {}'.format(gpx_name))

    def restore_active_route(self):
        gpx_name = conf.DATA_DIR + os.sep + conf.GPX_CUR_ROUTE_NAME
        if os.path.isfile(gpx_name):
            with open(gpx_name, 'rt') as f:
                try:
                    gpx = gpxpy.parse(f)
                    if len(gpx.routes) > 0:
                        self.active_route = gpx.routes[0]
                        print('Restored active route {}'.format(self.active_route))
                        Navigator.get_instance().set_route(self.active_route, self.active_route.get_points_no() - 1)
                except Exception as e:
                    print(e)

    def get_dest_wpt(self):
        if self.active_route is not None:
            return self.active_route.points[-1]
        else:
            return None
