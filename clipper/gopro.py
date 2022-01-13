import argparse
import csv
import json
from datetime import datetime, timedelta
import os
import subprocess

import pytz

GOPRO_GPMF_BIN = '../gopro_gpmf/cmake-build-debug/gopro_gpmf'

UTC_TZ = pytz.timezone("UTC")


def from_timestamp(time_stamp):
    return UTC_TZ.localize(datetime.utcfromtimestamp(time_stamp)) if time_stamp is not None else None


def to_timestamp(start_utc):
    return start_utc.timestamp() if start_utc is not None else None


class GoPro:
    def __init__(self, args):
        sd_card_dir = args.gopro_dir
        cache_dir = args.work_dir + os.sep + 'gopro'
        os.makedirs(cache_dir, exist_ok=True)

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
            if os.path.isfile(clip_cache_name):
                print(f'Reading GOPRO clip info from cache {clip_cache_name}')
                with open(clip_cache_name, 'r') as f:
                    cache = json.load(f)
                    start_utc = from_timestamp(cache['start_utc'])
                    stop_utc = from_timestamp(cache['stop_utc'])
            else:
                print(f'Scanning {clip_name}')
                [start_utc, stop_utc] = self.time_stamp_mp4(clip_name)
                cache = {
                    'start_utc': to_timestamp(start_utc),
                    'stop_utc': to_timestamp(stop_utc)
                }
                with open(clip_cache_name, 'w') as f:
                    json.dump(cache, f)

            if start_utc is not None:
                clip['start_utc'] = start_utc
                clip['stop_utc'] = stop_utc
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

    @staticmethod
    def time_stamp_mp4(mp4_name):
        cmd = [GOPRO_GPMF_BIN, mp4_name]
        result = subprocess.run(cmd, stdout=subprocess.PIPE)
        start_utc = None
        stop_utc = None
        timezone = pytz.timezone("UTC")
        if result.returncode == 0:
            lines = result.stdout.decode('utf-8').split('\n')
            reader = csv.DictReader(lines)
            for row in reader:
                if row['fix_valid'] == 'True':
                    utc = timezone.localize(datetime.fromisoformat(row['utc']))
                    if start_utc is None:
                        t_ms = int(float(row['t']) * 1000)
                        start_utc = utc - timedelta(milliseconds=t_ms)
                    stop_utc = utc

        return start_utc, stop_utc

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

    gopro = GoPro(parser.parse_args())
    start = datetime.fromisoformat('2021-11-19T18:04:54.825').astimezone(pytz.utc)
    stop = datetime.fromisoformat('2021-11-19T19:23:06.190').astimezone(pytz.utc)
    print(gopro.get_clips_for_time_interval(start, stop))
