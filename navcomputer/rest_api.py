import os
import connexion
import flask
from data_registry import DataRegistry
import conf


def get_raw_instr():
    data_registry = DataRegistry.get_instance()
    return data_registry.get_raw_instr_data_dict()


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
    uploaded_file = connexion.request.files['fileName']
    file_name = conf.DATA_DIR + os.sep + conf.POLAR_NAME
    print('Storing polars to {}'.format(file_name))
    uploaded_file.save(file_name)
    return {'status': 200}


def gpx_upload():
    uploaded_file = connexion.request.files['fileName']
    file_name = conf.DATA_DIR + os.sep + conf.GPX_NAME
    print('Storing GPX to {}'.format(file_name))
    uploaded_file.save(file_name)
    data_registry = DataRegistry.get_instance()
    data_registry.read_gpx_file(file_name)
    return {'status': 200}
