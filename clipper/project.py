import json
from datetime import date, datetime
from json import JSONEncoder
from gpxpy.geo import Location

from gui.clip_event import ClipEvent
from gui.race_info import RaceInfo
from raw_instr_data import RawInstrData

COMMON = 'common'
GOPRO = 'gopro'
NMEA = 'nmea'


class ProjectEncoder(JSONEncoder):
    # Override the default method
    def default(self, obj):
        if isinstance(obj, (date, datetime)):
            return obj.isoformat()
        elif isinstance(obj, Location):
            if obj.elevation is None:
                return {'lat': obj.latitude, 'lon': obj.longitude}
            else:
                return {'lat': obj.latitude, 'lon': obj.longitude, 'alt': obj.elevation}
        elif isinstance(obj, RawInstrData):
            return obj.to_dict()
        elif isinstance(obj, ClipEvent):
            return obj.to_dict()
        elif isinstance(obj, RaceInfo):
            return obj.to_dict()

        raise TypeError("Type %s not serializable" % type(obj))


def decode_project(d):
    for k in d:
        if 'utc' in k:
            if d[k] is None:
                d[k] = None
            else:
                d[k] = datetime.fromisoformat(d[k])
        elif 'history' in k or 'instr_data' in k:
            ii_list = []
            for h in d[k]:
                ii = RawInstrData()
                ii.utc = h['utc']
                ii.lat = h['lat']
                ii.lon = h['lon']
                ii.sog = h['sog']
                ii.cog = h['cog']
                ii.awa = h['awa']
                ii.aws = h['aws']
                ii.twa = h['twa']
                ii.tws = h['tws']
                ii.sow = h['sow']
                ii.hdg = h['hdg']
                ii.n2k_epoch = h['n2k_epoch']
                ii_list.append(ii)
            d[k] = ii_list
        elif 'events' in k:
            event_list = []
            for e in d[k]:
                event = ClipEvent.from_dict(e)
                event_list.append(event)
            d[k] = event_list
        elif 'races' in k:
            race_list = []
            for r in d[k]:
                race = RaceInfo.from_dict(r, r['events'])
                race_list.append(race)
            d[k] = race_list

    return d


class Project:
    def __init__(self):
        self.json_file = None
        self._is_dirty = False
        self.params = {
            COMMON: {'polar_file': "", 'events': [], 'instr_data': []},
            GOPRO: {'dir': "", 'utc': None},
            NMEA: {'dir': "", 'utc': None},
        }

    def set(self, section, key, val):
        self.params[section][key] = val
        self._is_dirty = True

    def get(self, section, key):
        return self.params[section].get(key)

    def open(self, json_file):
        try:
            self.params = json.load(open(json_file, 'rt'), object_hook=decode_project)
            self.json_file = json_file
            return True
        except (FileNotFoundError, json.decoder.JSONDecodeError):
            print(f'Failed to parse {json_file}')
            return False

    def save(self, json_file=None):
        if json_file is None:
            json_file = self.json_file
        else:
            self.json_file = json_file
        json.dump(self.params, open(json_file, 'wt'), indent=4, cls=ProjectEncoder)
        self._is_dirty = False

    def is_dirty(self):
        return self._is_dirty
