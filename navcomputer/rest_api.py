"""
This file contains the entry points for the REST API calls received form HTTP server
"""
import gzip
import os
import pwd
import grp
import tarfile

import connexion
import flask
import gpxpy
from gpxpy.gpx import GPXRoutePoint

import conf
from logger import Logger
from navigator import Navigator
from polars import Polars


def get_raw_instr():
    navigator = Navigator.get_instance()
    data = navigator.get_raw_instr_data()
    return {
        'utc': data.utc,
        'lat': data.lat,
        'lon': data.lon,
        'awa': data.awa,
        'aws': data.aws,
        'twa': data.twa,
        'tws': data.tws,
        'sow': data.sow,
        'sog': data.sog,
        'hdg': data.hdg,
        'cog': data.cog,
    }


def get_dest():
    navigator = Navigator.get_instance()
    dest_info = navigator.get_dest_info()
    if dest_info is None:
        return {}
    else:
        return {
            'name': dest_info.wpt.name,
            'dtw': dest_info.dtw,
            'btw': dest_info.btw,
            'atw': dest_info.atw,
            'atw_up': dest_info.atw_up,
        }


def get_history():
    navigator = Navigator.get_instance()
    history = []
    for item in navigator.get_history():
        history.append({
            'utc': item.utc,
            'hdg': item.avg_hdg,
            'twa': item.avg_boat_twa
        })
    return history


def get_wpts():
    navigator = Navigator.get_instance()
    gpx_wpts = navigator.get_wpts()
    wpts = []
    for wpt in gpx_wpts:
        wpt_dict = {
            'name': wpt.name,
            'lat': wpt.latitude,
            'lon': wpt.longitude,
        }
        wpts.append(wpt_dict)
    return wpts


def set_active_route(body=None):
    route = body
    print(route)
    active_wpt_idx = int(route['active_wpt_idx']) % len(route['wpts'])
    gpx_route = gpxpy.gpx.GPXRoute(name=route['name'], number=active_wpt_idx)
    for wpt in route['wpts']:
        gpx_route.points.append(GPXRoutePoint(name=wpt['name'],
                                              latitude=float(wpt['lat']), longitude=float(wpt['lon'])))

    Navigator.get_instance().set_route(gpx_route, active_wpt_idx)

    return {'status': 200}


def clear_active_route():
    print('Clear current destination')
    Navigator.get_instance().clear_dest()


def announce_current_route():
    Navigator.get_instance().announce_current_route()


def get_routes():
    navigator = Navigator.get_instance()
    active_route, active_wpt_idx = navigator.get_active_route()

    active_route_name = active_route.name if active_route is not None else ''

    gpx_routes = navigator.get_routes()
    routes = []
    for gpx_route in gpx_routes:
        route_is_active = active_route_name == gpx_route.name
        wpts = []
        for wpt_idx, wpt in enumerate(gpx_route.points):
            wpts.append({
                'name': wpt.name,
                'lat': wpt.latitude,
                'lon': wpt.longitude,
            })
        route = {
            'name': gpx_route.name,
            'wpts': wpts,
            'active': route_is_active,
            'active_wpt_idx': active_wpt_idx if active_route is not None else None
        }
        routes.append(route)
    return routes


def get_active_route():
    navigator = Navigator.get_instance()
    gpx_route, active_wpt_idx = navigator.get_active_route()

    if gpx_route is None:
        return {
            'name': '',
            'wpts': [],
            'active': False,
            'active_wpt_idx': -1
        }

    wpts = []
    for wpt_idx, wpt in enumerate(gpx_route.points):
        wpts.append({
            'name': wpt.name,
            'lat': wpt.latitude,
            'lon': wpt.longitude,
        })

    route = {
        'name': gpx_route.name if gpx_route.name is not None else '',
        'wpts': wpts,
        'active': True,
        'active_wpt_idx': active_wpt_idx
    }

    return route


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
    navigator = Navigator.get_instance()
    uploaded_file = connexion.request.files['fileName']
    polars_file_name = navigator.get_data_dir() + os.sep + conf.POLAR_NAME
    tmp_file_name = polars_file_name + '.tmp'
    print('Storing uploaded polars to {}'.format(tmp_file_name))
    uploaded_file.save(tmp_file_name)
    # Now verify if it's valid file
    polars = Polars()
    polars.read_table(tmp_file_name)
    if polars.is_valid():
        print('Polars validated OK, renaming to {}'.format(polars_file_name))
        os.rename(tmp_file_name, polars_file_name)
        navigator.set_polars(polars)
        return {'status': 200}
    else:
        print('Polars validation failed')
        return 'Invalid polars format', 420


def gpx_upload():
    navigator = Navigator.get_instance()
    uploaded_file = connexion.request.files['fileName']
    gpx_file_name = navigator.get_data_dir() + os.sep + conf.GPX_ARCHIVE_NAME
    tmp_file_name = gpx_file_name + '.tmp'
    print('Storing GPX to {}'.format(tmp_file_name))
    uploaded_file.save(tmp_file_name)
    if navigator.read_gpx_file(tmp_file_name):
        print('GPX validated OK, renaming to {}'.format(gpx_file_name))
        os.rename(tmp_file_name, gpx_file_name)
        return {'status': 200}
    else:
        print('GPX validation failed')
        return 'Invalid GPX format', 420


def sw_update():
    navigator = Navigator.get_instance()
    uploaded_file = connexion.request.files['fileName']
    package_file_name = navigator.get_data_dir() + os.sep + conf.UPDATE_PKG_NAME
    tmp_package_file_name = package_file_name + '.tmp'
    print('Storing update to {}'.format(tmp_package_file_name))
    uploaded_file.save(tmp_package_file_name)

    # Validate uploaded file
    # First check if it's valid gzip
    gz_is_valid = False
    error = ''
    try:
        with gzip.open(tmp_package_file_name, 'rb') as f:
            try:
                while f.read(10000000) != '':
                    pass
                gz_is_valid = True
            except Exception as e:
                error = e
                print(error)
    except Exception as e:
        error = e
        print(error)

    # Now check if's valid tar
    tgz_valid = False
    if gz_is_valid:
        try:
            tgz_valid = tarfile.is_tarfile(tmp_package_file_name)
        except Exception as e:
            error = e
            print(error)

    if tgz_valid:
        os.rename(tmp_package_file_name, package_file_name)
        # The update service is running as user pi
        uid = pwd.getpwnam("pi").pw_uid
        gid = grp.getgrnam("pi").gr_gid
        os.chown(package_file_name, uid, gid)
        return {'status': 200}
    else:
        return str(error), 420


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


def timer_start():
    if Navigator.get_instance().timer_start():
        return {'status': 200}
    else:
        return {'status': 420}


def timer_stop():
    if Navigator.get_instance().timer_stop():
        return {'status': 200}
    else:
        return {'status': 420}


def timer_sync():
    if Navigator.get_instance().timer_sync():
        return {'status': 200}
    else:
        return {'status': 420}


def timer_phrf_upload():
    navigator = Navigator.get_instance()
    uploaded_file = connexion.request.files['fileName']
    phrf_file_name = navigator.get_data_dir() + os.sep + conf.PHRF_TABLE
    tmp_file_name = phrf_file_name + '.tmp'
    print('Storing PHRF table to {}'.format(tmp_file_name))
    uploaded_file.save(tmp_file_name)
    if navigator.read_phrf_table(tmp_file_name):
        print('PHRF validated OK, renaming to {}'.format(phrf_file_name))
        os.rename(tmp_file_name, phrf_file_name)
        return {'status': 200}
    else:
        print('PHRF validation failed')
        return 'Invalid PHRF format', 420


def timer_get_data():
    navigator = Navigator.get_instance()
    if navigator.is_timer_active():
        return {
            'is_running': navigator.is_timer_active(),
            'elapsed_time': navigator.get_elapsed_time(),
            'phrf_timers': navigator.get_phrf_timers()
        }
    else:
        return {}


def timer_announce_timer_state():
    navigator = Navigator.get_instance()
    navigator.announce_timer_state()
    return {'status': 200}
