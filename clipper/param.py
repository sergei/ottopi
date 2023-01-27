import json
import os
from datetime import date, datetime
from json import JSONEncoder
from gopro import to_timestamp, from_timestamp

PORT = 'port'
STBD = 'stbd'
GOPRO = 'gopro'
NMEA = 'nmea'


class DateTimeEncoder(JSONEncoder):
    # Override the default method
    def default(self, obj):
        if isinstance(obj, (date, datetime)):
            return to_timestamp(obj)


def decode_date_time(d):
    for k in d:
        if 'utc' in k:
            d[k] = from_timestamp(d[k])
    return d


class Params:
    def __init__(self, json_file):
        self.json_file = os.path.expanduser(json_file)
        self.params = {
            'port': {'dir': os.path.expanduser('/tmp'), 'img_idx': 0},
            'stbd': {'dir': os.path.expanduser('/tmp'), 'img_idx': 0},
            'gopro': {'dir': os.path.expanduser('/tmp'), 'utc': None},
            'nmea': {'dir': os.path.expanduser('/tmp'), 'utc': None},
        }
        self.read()

    def set(self, section, key, val):
        self.params[section][key] = val
        json.dump(self.params, open(self.json_file, 'wt'), indent=4, cls=DateTimeEncoder)

    def get(self, section, key):
        return self.params[section][key]

    def read(self):
        try:
            self.params = json.load(open(self.json_file, 'rt'), object_hook=decode_date_time)
        except (FileNotFoundError, json.decoder.JSONDecodeError):
            pass
