import os
import connexion
import flask
from data_registry import DataRegistry


def get_raw_instr():
    data_registry = DataRegistry.get_instance()
    return data_registry.get_raw_instr_data_dict()


def root_page():
    name = 'index.html'
    return static_page(name)


def static_page(name):
    root = '../web/build'
    path = root + os.path.sep + name
    print('Serving {}'.format(path))
    if os.path.isfile(path):
        return flask.send_file(path)
    else:
        return '{} Not Found'.format(path), 404


def polars_upload():
    uploaded_file = connexion.request.files['fileName']
    print('Storing {}'.format(uploaded_file.filename))
    uploaded_file.save("/tmp/polars.txt")
    return {'status': 200}
