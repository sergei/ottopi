import csv
import datetime
import json
import os

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
    'Top speed': {'in': 10, 'out': 10},
}


class RaceEventsRecorder(NavigationListener):
    def __init__(self, work_dir, start_time_utc, finish_time_utc, clips, ignore_events=None, exclude_ranges=None):
        super().__init__()
        self.ignore_evens_before_start = True
        self.work_dir = work_dir
        self.start_time_utc = start_time_utc
        self.finish_time_utc = finish_time_utc
        self.events = []
        self.instr_data = []
        self.targets = []
        self.target_summaries = []
        self.clips = clips
        self.ignore_events = [] if ignore_events is None else ignore_events
        self.exclude_ranges = [] if exclude_ranges is None else exclude_ranges
        self.max_sow_event = {'name': 'Top speed', 'sow': 0, 'utc': None, 'location': Location(37, -122),
                              'hist_idx': 0}

        self.targets_csv_file_name = os.path.join(self.work_dir, 'targets.csv')
        self.targets_csv_file = open(self.targets_csv_file_name, 'wt')
        self.targets_writer = csv.DictWriter(self.targets_csv_file, ['utc', 'tws', 'sow', 'target_sow',
                                                                     'twa', 'target_twa'])
        self.targets_writer.writeheader()

    def is_excluded(self, utc):
        for exclude_range in self.exclude_ranges:
            from_utc = exclude_range['from']
            to_utc = exclude_range['to']
            if from_utc < utc < to_utc:
                return True
        return False
    
    def on_instr_data(self, instr_data: RawInstrData):
        if instr_data.sow is not None and instr_data.sow > self.max_sow_event['sow']:
            self.max_sow_event['name'] = f'Top speed: {instr_data.sow:.1f} kts'
            self.max_sow_event['sow'] = instr_data.sow
            self.max_sow_event['utc'] = instr_data.utc
            self.max_sow_event['hist_idx'] = len(self.instr_data)
            self.max_sow_event['location'] = Location(instr_data.lat, instr_data.lon)
        self.instr_data.append(instr_data)

    def on_mark_rounding(self, utc, loc, is_windward):
        if utc in self.ignore_events:
            print(f'Ignoring event at {utc}')
            return

        if self.is_excluded(utc):
            print(f'Excluding event at {utc}')
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

    def on_targets(self, targets):
        self.targets.append(targets)

    def on_target_update(self, utc: datetime, loc: Location,
                         distance_delta_m: float, speed_delta: float, twa_angle_delta: float):

        start_idx = len(self.targets) - int(NavStats.STRAIGHT_THR)
        for i in range(start_idx, len(self.targets)):
            ii = self.instr_data[i]
            tg = self.targets[i]
            self.targets_writer.writerow({'utc': ii.utc, 'tws': ii.tws, 'sow': ii.sow, 'target_sow': tg.target_sow,
                                          'twa': ii.twa, 'target_twa': tg.target_twa})

    def evt_utc(self, s):
        """ Get event utc time """
        utc = None
        if isinstance(s, datetime.datetime):
            utc = s
        elif s.startswith('clip'):
            clip_name = s.split('/')[1]
            mm_ss = s.split('/')[2].split(':')
            clip_time = int(mm_ss[0]) * 60 + int(mm_ss[1])
            for clip in self.clips:
                if clip['name'].endswith(clip_name):
                    utc = clip['start_utc'] + datetime.timedelta(seconds=clip_time)
                    break
        else:
            utc = None

        return utc

    def add_extra_events(self, events):
        for evt in events:
            duration = int((self.evt_utc(evt['out']) - self.evt_utc(evt['in'])).total_seconds())
            half_span = duration // 2
            utc = self.evt_utc(evt['in']) + datetime.timedelta(seconds=half_span)
            for hist_idx, ii in enumerate(self.instr_data):
                if int(utc.timestamp()) == int(ii.utc.timestamp()):  # Ignore milliseconds
                    print(f"Adding {evt['name']}")
                    self.events.append({
                        'name': evt['name'],
                        'gun': evt.get('gun', None),
                        'in': half_span,
                        'out': half_span,
                        'utc': utc,
                        'location': Location(ii.lat, ii.lon),
                        'hist_idx': hist_idx
                    })

    def finalize(self):
        print(f'Created TARGETS CSV file{self.targets_csv_file_name}')
        self.targets_csv_file.close()

        # Add maximum speed event
        if self.max_sow_event['utc'] is not None:
            self.events.append(self.max_sow_event)

        # Sort events by UTC
        self.events.sort(key=lambda x: x['utc'])

        # There should be no events before the start
        if self.ignore_evens_before_start:
            start_evt_idx = None
            for idx, evt in enumerate(self.events):
                if evt['name'].lower() == 'start':
                    start_evt_idx = idx
                    break
            if start_evt_idx is not None:
                self.events = self.events[start_evt_idx:]

        # Add history to all events
        for evt in self.events:
            evt_name = evt['name']
            if ':' in evt_name:
                evt_name = evt_name.split(':')[0]

            if 'in' in evt:
                in_idx = evt['hist_idx'] - evt['in']
            else:
                in_idx = evt['hist_idx'] - EVENTS_IN_OUT[evt_name]['in']

            if 'out' in evt:
                out_idx = evt['hist_idx'] + evt['out']
            else:
                out_idx = evt['hist_idx'] + EVENTS_IN_OUT[evt_name]['out']

            evt['history'] = self.instr_data[in_idx:out_idx]

    def to_json(self):
        return json.dumps(self.events, indent=2, default=json_serial)
