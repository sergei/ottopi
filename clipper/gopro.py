import csv
from datetime import datetime, timedelta
import os
import subprocess

import pytz

GOPRO_GPMF_BIN = '../gopro_gpmf/cmake-build-debug/gopro_gpmf'


class GoPro:
    def __init__(self, sd_card_dir='/Volumes/GOPRO'):
        # Build the list of clips
        clips = []
        for root, dirs_list, files_list in os.walk(sd_card_dir):
            for file_name in files_list:
                if os.path.splitext(file_name)[-1] == ".MP4":
                    file_name_path = os.path.join(root, file_name)
                    if file_name[0] != '.':
                        clips.append({'name': file_name_path})

        # Time stamp the clips
        self.clips = []  # Only clips with valid UTC
        for clip in clips:
            [start_utc, stop_utc] = self.time_stamp_mp4(clip['name'])
            if start_utc is not None:
                clip['start_utc'] = start_utc
                clip['stop_utc'] = stop_utc
                self.clips.append(clip)
            else:
                print(f'Warning: Clip {clip["name"]} contains no valid UTC')

        # Sort clips by UTC start time
        self.clips.sort(key=lambda x: x['start_utc'])

    @staticmethod
    def time_stamp_mp4(mp4_name):
        cmd = [GOPRO_GPMF_BIN, mp4_name]
        result = subprocess.run(cmd, stdout=subprocess.PIPE)
        start_utc = None
        stop_utc = None
        if result.returncode == 0:
            lines = result.stdout.decode('utf-8').split('\n')
            reader = csv.DictReader(lines)
            for row in reader:
                if row['fix_valid'] == 'True':
                    utc = datetime.fromisoformat(row['utc']).astimezone(pytz.utc)
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
                    if stop_utc <= clip['stop_utc']: # Last clip found
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
    gopro = GoPro()
    start = datetime.fromisoformat('2021-11-19T18:04:54.825').astimezone(pytz.utc)
    stop = datetime.fromisoformat('2021-11-19T19:23:06.190').astimezone(pytz.utc)
    print(gopro.get_clips_for_time_interval(start, stop))
