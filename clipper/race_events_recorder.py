import datetime
import json

from gpxpy.geo import Location

from nav_stats import NavStats
from navigator_listener import NavigationListener
from raw_instr_data import RawInstrData


def json_serial(obj):
    """JSON serializer for objects not serializable by default json code"""

    if isinstance(obj, datetime.datetime):
        return obj.isoformat()
    elif isinstance(obj, Location):
        if obj.elevation is None:
            return {'lat': obj.latitude, 'lon': obj.longitude }
        else:
            return {'lat': obj.latitude, 'lon': obj.longitude, 'alt': obj.elevation}
    elif isinstance(obj, RawInstrData):
        return {'utc': obj.utc.isoformat(), 'lat': obj.lat, 'lon': obj.lon, 'sog': obj.sog, 'cog': obj.cog,
                'awa': obj.awa, 'aws': obj.aws, 'twa': obj.twa, 'tws': obj.tws, 'sow': obj.sow, 'hdg': obj.hdg, }

    raise TypeError("Type %s not serializable" % type(obj))


class RaceEventsRecorder(NavigationListener):
    def __init__(self, work_dir, start_time_utc, finish_time_utc):
        super().__init__()
        self.work_dir = work_dir
        self.start_time_utc = start_time_utc
        self.finish_time_utc = finish_time_utc
        self.events = []
        self.instr_data = []

    def on_instr_data(self, instr_data: RawInstrData):
        self.instr_data.append(instr_data)

    def on_mark_rounding(self, utc, loc, is_windward):
        if self.start_time_utc <= utc <= self.finish_time_utc:
            self.events.append({
                'name': 'Windward Mark' if is_windward else 'Leeward Mark',
                'utc': utc,
                'location': loc,
                'history': self.instr_data.copy()[-NavStats.WIN_LEN:]
            })
            self.instr_data = []

    def on_tack(self, utc, loc, is_tack, distance_loss_m):
        if self.start_time_utc <= utc <= self.finish_time_utc:
            self.events.append({
                'name': 'Tack' if is_tack else 'Gybe',
                'utc': utc,
                'location': loc,
                'history': self.instr_data.copy()[-NavStats.WIN_LEN:]
            })
            self.instr_data = []

    def to_json(self):
        return json.dumps(self.events, indent=2, default=json_serial)
