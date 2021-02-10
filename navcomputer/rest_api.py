import os
import connexion
import flask
import gpxpy
from gpxpy.gpx import GPXWaypoint, GPXRoutePoint

from data_registry import DataRegistry
import conf
from logger import Logger
from navigator import Navigator


def get_raw_instr():
    data_registry = DataRegistry.get_instance()
    return data_registry.get_raw_instr_data_dict()


def goto_wpt(body=None):
    wpt = body
    data_registry = DataRegistry.get_instance()

    dest_wpt = GPXRoutePoint(name=wpt['name'], latitude=wpt['lat'], longitude=wpt['lon'])
    gpx_route = gpxpy.gpx.GPXRoute(name="WEB")
    gpx_route.points.append(dest_wpt)

    data_registry.set_active_route(gpx_route)
    return {'status': 200}


def get_wpts():
    data_registry = DataRegistry.get_instance()
    gpx_wpts = data_registry.get_wpts()
    dest_wpt = data_registry.get_dest_wpt()
    wpts = []
    for wpt in gpx_wpts:
        wpt_dict = {
            'name': wpt.name,
            'lat': wpt.latitude,
            'lon': wpt.longitude,
            'active': dest_wpt is not None and dest_wpt.name == wpt.name
        }
        wpts.append(wpt_dict)
    return wpts


def root_page():
    name = 'index.html'
    return static_page(name)


def static_page(name):
    root = conf.WEB_APP_DIR
    path = root + os.path.sep + name
    print('Serving {}'.format(path))
    if os.path.isfile(path):
        return flask.send_file(path)
    else:
        return '{} Not Found'.format(path), 404


def polars_upload():
    data_registry = DataRegistry.get_instance()
    uploaded_file = connexion.request.files['fileName']
    file_name = data_registry.data_dir + os.sep + conf.POLAR_NAME
    print('Storing polars to {}'.format(file_name))
    uploaded_file.save(file_name)
    return {'status': 200}


def gpx_upload():
    data_registry = DataRegistry.get_instance()
    uploaded_file = connexion.request.files['fileName']
    file_name = data_registry.data_dir + os.sep + conf.GPX_ARCHIVE_NAME
    print('Storing GPX to {}'.format(file_name))
    uploaded_file.save(file_name)
    data_registry.read_gpx_file()
    return {'status': 200}


def get_logs():
    return Logger.get_logs()


def get_log(name):
    content = Logger.get_log(name)
    if content is None:
        return {'Log not found': 200}
    else:
        return content


def zip_all_logs():
    content = Logger.zip_all_logs()
    if content is None:
        return {'Log not found': 200}
    else:
        return content


def tack():
    print('Tacking')
    if Navigator.get_instance().tack():
        return {'status': 200}
    else:
        return 'Not connected', 420


def steer(degrees):
    degrees = int(degrees)
    print('Steering {} degrees'.format(degrees))
    if Navigator.get_instance().steer(degrees):
        return {'status': 200}
    else:
        return 'Not connected', 420
