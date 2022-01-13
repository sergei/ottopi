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
            return {'lat': obj.latitude, 'lon': obj.longitude}
        else:
            return {'lat': obj.latitude, 'lon': obj.longitude, 'alt': obj.elevation}
    elif isinstance(obj, RawInstrData):
        return {'utc': obj.utc.isoformat(), 'lat': obj.lat, 'lon': obj.lon, 'sog': obj.sog, 'cog': obj.cog,
                'awa': obj.awa, 'aws': obj.aws, 'twa': obj.twa, 'tws': obj.tws, 'sow': obj.sow, 'hdg': obj.hdg, }

    raise TypeError("Type %s not serializable" % type(obj))


EVENTS_IN_OUT = {
    'Tack': {'in': 25, 'out': 35},
    'Gybe': {'in': 25, 'out': 35},
    'Windward Mark': {'in': 60, 'out': 60},
    'Leeward Mark': {'in': 60, 'out': 60},
}


class RaceEventsRecorder(NavigationListener):
    def __init__(self, work_dir, start_time_utc, finish_time_utc, ignore_events=None):
        super().__init__()
        self.work_dir = work_dir
        self.start_time_utc = start_time_utc
        self.finish_time_utc = finish_time_utc
        self.events = []
        self.instr_data = []
        self.ignore_events = [] if ignore_events is None else ignore_events

    def on_instr_data(self, instr_data: RawInstrData):
        self.instr_data.append(instr_data)

    def on_mark_rounding(self, utc, loc, is_windward):
        if utc in self.ignore_events:
            print(f'Ignoring event at {utc}')
            return

        if self.start_time_utc <= utc <= self.finish_time_utc:
            self.events.append({
                'name': 'Windward Mark' if is_windward else 'Leeward Mark',
                'utc': utc,
                'location': loc,
                'hist_idx': len(self.instr_data) - NavStats.HALF_WIN,
            })

    def on_tack(self, utc, loc, is_tack, distance_loss_m):
        if utc in self.ignore_events:
            print(f'Ignoring event at {utc}')
            return

        if self.start_time_utc <= utc <= self.finish_time_utc:
            self.events.append({
                'name': 'Tack' if is_tack else 'Gybe',
                'utc': utc,
                'location': loc,
                'hist_idx': len(self.instr_data) - NavStats.HALF_WIN,
            })

    def add_extra_events(self, events):
        for evt in events:
            duration = int((evt['out'] - evt['in']).total_seconds())
            half_span = duration // 2
            utc = evt['in'] + datetime.timedelta(seconds=half_span)
            for hist_idx, ii in enumerate(self.instr_data):
                if utc == ii.utc:
                    self.events.append({
                        'name': evt['name'],
                        'gun': evt.get('gun', None),
                        'in': half_span,
                        'out': half_span,
                        'utc': utc,
                        'location': Location(ii.lat, ii.lon),
                        'hist_idx': hist_idx
                    })

        # Sort events by UTC, since the newly added ones might come out of order
        self.events.sort(key=lambda x: x['utc'])

    def finalize(self):
        # There should be no events before the start
        start_evt_idx = None
        for idx, evt in enumerate(self.events):
            if evt['name'].lower() == 'start':
                start_evt_idx = idx
                break
        if start_evt_idx is not None:
            self.events = self.events[start_evt_idx:]

        # Add history to all events
        for evt in self.events:
            if 'in' in evt:
                in_idx = evt['hist_idx'] - evt['in']
            else:
                in_idx = evt['hist_idx'] - EVENTS_IN_OUT[evt['name']]['in']

            if 'out' in evt:
                out_idx = evt['hist_idx'] + evt['out']
            else:
                out_idx = evt['hist_idx'] + EVENTS_IN_OUT[evt['name']]['out']

            evt['history'] = self.instr_data[in_idx:out_idx]

    def to_json(self):
        return json.dumps(self.events, indent=2, default=json_serial)
