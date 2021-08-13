import datetime
import glob
import gzip
import os
import re

import boto3 as boto3
from gpxpy.geo import Location

from kml_writer import KmlWriter
from navigator_listener import NavigationListener
from plotter import Plotter


def fetch_log_list(client, bucket, uuid, replay_start, replay_days):
    replay_finish = replay_start + datetime.timedelta(days=replay_days)
    log_list = []
    cont_token = None
    print('Retrieving list of files from {} for {}'.format(bucket, uuid))
    while True:
        params = dict(
            Bucket=bucket,
            Prefix=uuid,
        )

        if cont_token is not None:
            params['ContinuationToken'] = cont_token

        response = client.list_objects_v2(**params)
        for item in response['Contents']:
            key = item['Key']
            t = re.split(r'[/_.T]', key)
            if len(t) == 6:
                date = datetime.date.fromisoformat(t[2])
                if replay_start <= date <= replay_finish:
                    log_list.append(key)

        # Check if all keys were processed
        if response['IsTruncated']:
            cont_token = response['NextContinuationToken']
        else:
            break

    return log_list


class Replay(NavigationListener):
    def __init__(self, replay_dir, log_dir, nmea_parser):
        super().__init__()
        if replay_dir.startswith('s3://'):
            self.bucket = replay_dir.split('/')[2]
            self.uuid = replay_dir.split('/')[3]
        else:
            self.replay_dir = os.path.expanduser(replay_dir)
            self.bucket = None
        self.log_dir = os.path.expanduser(log_dir)
        self.nmea_parser = nmea_parser
        self.kml_writer = KmlWriter()
        self.plotter = Plotter()

    def run(self, with_prefix, signalk, replay_start, replay_days):
        if self.bucket is None:
            log_list = sorted(glob.glob(self.replay_dir + os.sep + '*.nmea'))
            log_list += sorted(glob.glob(self.replay_dir + os.sep + '*.log'))
            log_list += sorted(glob.glob(self.replay_dir + os.sep + '*.log.gz'))

            for log in log_list:
                if log.endswith('.gz'):
                    f = gzip.open(log, 'rt')
                else:
                    f = open(log, 'r')
                print('Replaying {}'.format(log))
                for nmea in f:
                    if with_prefix:
                        if nmea.startswith('>'):
                            self.nmea_parser.set_nmea_sentence(nmea[2:])
                    elif signalk:
                        t = nmea.split(';')
                        if len(t) == 3:
                            self.nmea_parser.set_nmea_sentence(t[2])
                    else:
                        self.nmea_parser.set_nmea_sentence(nmea)
                f.close()
        else:
            boto3.setup_default_session(profile_name='signalk-down')
            client = boto3.client('s3')
            log_list = fetch_log_list(client, self.bucket, self.uuid, replay_start, replay_days)
            for log_file in log_list:
                print('Processing {}'.format(log_file))
                response = client.get_object(Bucket=self.bucket, Key=log_file)
                f = gzip.open(response['Body'], 'rt')
                for nmea in f:
                    t = nmea.split(';')
                    if len(t) == 3:
                        self.nmea_parser.set_nmea_sentence(t[2])

        kml_file = self.log_dir + os.sep + "replay.kml"
        print('Saving {}'.format(kml_file))
        self.kml_writer.save(kml_file)
        self.plotter.show()

    def on_instr_data(self, instr_data):
        self.plotter.on_instr_data(instr_data)

    def on_targets(self, targets):
        self.plotter.on_targets(targets)

    def on_dest_info(self, raw_instr_data, dest_info):
        self.kml_writer.add_route_point(raw_instr_data, dest_info)

    def on_history_item(self, leg_summary):
        self.kml_writer.add_leg_summary(leg_summary)

    def on_wind_shift(self, leg_summary):
        self.plotter.on_wind_shift(leg_summary)

    def on_speech(self, s):
        print(s)
        self.kml_writer.add_speech(s)

    def on_target_update(self, utc: datetime, loc: Location,
                         distance_delta_m: float, speed_delta: float, twa_angle_delta: float):
        self.plotter.on_target_update(utc, loc, distance_delta_m, speed_delta, twa_angle_delta)
