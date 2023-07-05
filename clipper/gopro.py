import argparse
import csv
import json
from datetime import datetime, timedelta, date
import os
import subprocess
from functools import reduce
from json import JSONEncoder
from gpxpy.geo import Location

import pytz

from raw_instr_data import RawInstrData

GOPRO_GPMF_BIN = '../gopro_gpmf/cmake-build-debug/gopro_gpmf'

UTC_TZ = pytz.timezone("UTC")


def from_timestamp(time_stamp):
    return UTC_TZ.localize(datetime.utcfromtimestamp(time_stamp)) if time_stamp is not None else None


def to_timestamp(start_utc):
    return start_utc.timestamp() if start_utc is not None else None


class GoProCacheEncoder(JSONEncoder):
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
        raise TypeError("Type %s not serializable" % type(obj))


def decode_gopro_cache(d):
    for k in d:
        if k == 'start_utc' or k == 'stop_utc':
            if d[k] is None:
                d[k] = None
        elif 'utc' in k:
                d[k] = datetime.fromisoformat(d[k])
        elif 'instr_data' in k:
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

    return d


class GoPro:
    def __init__(self, sd_card_dir, work_dir):
        cache_dir = work_dir + os.sep + 'gopro'
        os.makedirs(cache_dir, exist_ok=True)
        self.instr_data = []

        # Build the list of clips
        clips = []

        print(f'Looking for GOPRO clips in {sd_card_dir} ...')
        for root, dirs_list, files_list in os.walk(sd_card_dir):
            for file_name in files_list:
                if os.path.splitext(file_name)[-1] == ".MP4":
                    file_name_path = os.path.join(root, file_name)
                    if file_name[0] != '.':
                        clips.append({'name': file_name_path})

        # Time stamp the clips
        self.clips = []  # Only clips with valid UTC
        for clip in clips:
            clip_name = clip['name']
            clip_cache_name = cache_dir + os.sep + os.path.basename(clip_name) + '.json'
            clip_nmea_name = cache_dir + os.sep + os.path.basename(clip_name) + '.nmea'
            clip['clip_nmea_name'] = clip_nmea_name
            if os.path.isfile(clip_cache_name):
                print(f'Reading GOPRO clip info from cache {clip_cache_name}')
                with open(clip_cache_name, 'r') as f:
                    cache = json.load(f, object_hook=decode_gopro_cache)
                    start_utc = from_timestamp(cache['start_utc'])
                    stop_utc = from_timestamp(cache['stop_utc'])
                    instr_data = cache['instr_data']
            else:
                print(f'Scanning {clip_name}')
                [start_utc, stop_utc, instr_data] = self.extract_sensor_data(clip_name, clip_nmea_name)
                self.instr_data.append(instr_data)
                cache = {
                    'start_utc': to_timestamp(start_utc),
                    'stop_utc': to_timestamp(stop_utc),
                    'instr_data': instr_data
                }
                with open(clip_cache_name, 'w') as f:
                    json.dump(cache, f, indent=4, cls=GoProCacheEncoder)

            if start_utc is not None:
                clip['start_utc'] = start_utc
                clip['stop_utc'] = stop_utc
                clip['instr_data'] = instr_data
                self.clips.append(clip)
                print(f'{clip["name"]} {start_utc} {stop_utc}')
            else:
                print(f'Warning: Clip {clip["name"]} contains no valid UTC')

        # Sort clips by UTC start time
        self.clips.sort(key=lambda x: x['start_utc'])

        # Determine overall start and finish times
        self.start_time_utc = None
        self.finish_time_utc = None
        if len(self.clips) > 0:
            self.start_time_utc = self.clips[0]['start_utc']
            self.finish_time_utc = self.clips[-1]['stop_utc']

        # Create one NMEA file once clips are sorted
        gopro_nmea_file = cache_dir + os.sep + 'gopro.nmea'
        print(f'Creating GOPRO NMEA file {gopro_nmea_file}')
        with open(gopro_nmea_file, 'w') as nmea_file:
            for clip in clips:
                self.instr_data += clip['instr_data']
                with open(clip['clip_nmea_name'], 'r') as clip_nmea:
                    for line in clip_nmea:
                        nmea_file.write(line)

        print(f'Done with GOPRO processing')

    @staticmethod
    def extract_sensor_data(mp4_name, clip_nmea_name):
        instr_data = []
        cmd = [GOPRO_GPMF_BIN, mp4_name]
        result = subprocess.run(cmd, stdout=subprocess.PIPE)
        start_utc = None
        stop_utc = None
        timezone = pytz.timezone("UTC")
        if result.returncode == 0:
            print(f'Creating NMEA file {clip_nmea_name}')
            with open(clip_nmea_name, 'w') as nmea_file:
                lines = result.stdout.decode('utf-8').split('\n')
                reader = csv.DictReader(lines)
                for row in reader:
                    utc = timezone.localize(datetime.fromisoformat(row['utc']))
                    if row['fix_valid'] == 'True':
                        signed_lat = float(row['lat'])
                        lat_sign = 'N' if signed_lat > 0 else 'S'
                        lat = abs(signed_lat)
                        lat_min = (lat - int(lat)) * 60
                        signed_lon = float(row['lon'])
                        lon_sign = 'E' if signed_lon > 0 else 'W'
                        lon = abs(signed_lon)
                        lon_min = (lon - int(lon)) * 60
                        sog = float(row['sog_ms']) * 3600. / 1852.
                        if 0 <= lat <= 90 and 0 <= lon <= 180:
                            rmc = f'$GPRMC,{utc.hour:02d}{utc.minute:02d}{utc.second:02d}.{int(utc.microsecond / 1000):03d},' \
                                  f'A,{int(lat):02d}{lat_min:08.5f},{lat_sign},'\
                                  f'{int(lon):03d}{lon_min:08.5f},{lon_sign},'\
                                  f'{sog:.1f},,{utc.day:02d}{utc.month:02d}{utc.year % 100:02d},'

                            ii = RawInstrData(0, utc, signed_lat, signed_lon, sog)
                            instr_data.append(ii)
                        else:
                            print('GPRO GPMF bug')

                        if start_utc is None:
                            t_ms = int(float(row['t']) * 1000)
                            start_utc = utc - timedelta(milliseconds=t_ms)
                        stop_utc = utc
                    else:
                        rmc = f'$GPRMC,{utc.hour:02d}{utc.minute:02d}{utc.second:02d}.{int(utc.microsecond / 1000):03d},' \
                              f'V,,,'\
                              f',,'\
                              f',,{utc.day:02d}{utc.month:02d}{utc.year % 100:02d},'

                    body = rmc[1:]  # string between $ and *
                    cc = reduce(lambda i, j: int(i) ^ int(j), [ord(x) for x in body])
                    nmea = f'{rmc}*{cc:02X}\r\n'
                    nmea_file.write(nmea)

        return start_utc, stop_utc, instr_data

    def get_clips_for_time_interval(self, start_utc, stop_utc):
        # Find the clip containing the start of interval
        clips = []
        for start_idx in range(len(self.clips)):
            clip = self.clips[start_idx]
            if clip['start_utc'] <= start_utc <= clip['stop_utc']:
                in_time = (start_utc - clip['start_utc']).seconds
                # Now find the clip containing the stop time
                for stop_idx in range(start_idx, len(self.clips)):
                    clip = self.clips[stop_idx]
                    if stop_utc <= clip['stop_utc']:  # Last clip found
                        # Check for the corner case when the stop_utc falls inbetween start_utc of the previous clip
                        # and start_utc of this one
                        if stop_utc < clip['start_utc']:
                            return clips
                        out_time = (stop_utc - clip['start_utc']).seconds
                        clips.append({
                            'name': clip['name'],
                            'in_time': in_time,
                            'out_time': out_time,
                        })
                        return clips
                    else:  # The time interval spans to subsequent clips
                        clips.append({
                            'name': clip['name'],
                            'in_time': in_time,
                            'out_time': None,  # Till the end of clip
                        })
                        in_time = None  # Start from the beginning of the next clip

        return clips


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--work-dir", help="Working directory", default='/tmp')
    parser.add_argument("--gopro-dir", help="GoPro SD card directory", default='/Volumes/GOPRO')

    args = parser.parse_args()
    gopro = GoPro(args.gopro_dir, args.work_dir)
    start = datetime.fromisoformat('2021-11-19T18:04:54.825').astimezone(pytz.utc)
    stop = datetime.fromisoformat('2021-11-19T19:23:06.190').astimezone(pytz.utc)
    print(gopro.get_clips_for_time_interval(start, stop))
