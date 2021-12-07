import argparse
import datetime
import gzip
import os
import re

import boto3 as boto3
import botocore
import pytz
import yaml
from kml_maker import make_kml
from race_events_recorder import RaceEventsRecorder
from video_maker import make_video
from navigator import Navigator
from nmeaparser import NmeaParser
from gopro import GoPro

SCENARIO_FMT = """# Autogenerated scenario file 
# Edit this file to adjust movie duration and or split movie to the several ones 
movies:
# First movie 
  - name: {movie_name}
    start: {start_utc}
    finish: {finish_utc}
## Events to remove
#    remove_events:
#     - {start_utc}
#     - {start_utc}
##  Manually added events
#    add_events:
#    - name: Event 1
#      in: {start_utc}
#      out: {start_utc}
#    - name: Event 2
#      in: {start_utc}
#      out: {start_utc}
#
## Second movie
#  - name: Second movie
#    start: {start_utc}
#    finish: {start_utc}
"""


def s3_sk_nmea_logs(start_time_utc, finish_time_utc, bucket, uuid, profile):
    # Build object prefix
    prefix = uuid + '/' + 'skserver-raw_'
    first_file = prefix + start_time_utc.strftime("%Y-%m-%dT%H.log.gz")

    if profile is not None:
        boto3.setup_default_session(profile_name=profile)
    client = boto3.client('s3')

    # Get first file
    lines = read_nmea_file(bucket, client, first_file)
    for line in lines:
        nmea = extract_nmea(line)
        if nmea is not None:
            yield nmea

    response = client.list_objects_v2(Bucket=bucket, Prefix=prefix, StartAfter=first_file)
    keys = [x['Key'] for x in response['Contents']]
    for key in keys:
        # Check if the log file belongs to the time range
        log_name = key.split('/')[1]
        d = datetime.datetime.strptime(log_name, 'skserver-raw_%Y-%m-%dT%H.log.gz')
        d = pytz.utc.localize(d)
        if start_time_utc <= d <= finish_time_utc:
            lines = read_nmea_file(bucket, client, key)
            for line in lines:
                nmea = extract_nmea(line)
                if nmea is not None:
                    yield nmea

        else:
            break


def extract_nmea(line):

    try:
        asc = line.decode('ascii')
        t = asc.split(';')
        if len(t) == 3:
            return t[2]
        else:
            return None
    except UnicodeDecodeError:
        return None


def read_nmea_file(bucket, client, file_name):
    lines = []
    # noinspection PyUnresolvedReferences
    try:
        print(f'Reading s3://{bucket}/{file_name} ')
        response = client.get_object(Bucket=bucket, Key=file_name)
        with gzip.open(response['Body']) as f:
            lines = f.readlines()
    except botocore.exceptions.ClientError as error:
        print(f'Failed to fetch log file {file_name} due to the following error:')
        print(f'  {error}')
    return lines


def main(args):
    # Read configuration
    with open(args.cfg_file, 'r') as stream:
        try:
            config = yaml.safe_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
            return

    navigator = Navigator.get_instance()
    navigator.read_polars(config['polar_file'])

    gopro = GoPro(args.gopro_dir)

    if args.scenario is None:
        print(f'Using GOPRO clips time span to determine movie time span')
        finish_time_utc, start_time_utc = gopro.finish_time_utc, gopro.start_time_utc
        movie_name = 'movie_' + get_valid_filename(start_time_utc.strftime('%Y-%m-%d-%H-%M-%S'))
        events_json_name = create_events(args, config, movie_name, navigator, start_time_utc, finish_time_utc)

        # Create sample scenario file
        scenario_file = 'args' + os.sep + movie_name + '.yaml'
        with open(scenario_file, 'wt') as sf:
            sf.write(SCENARIO_FMT.format(movie_name=movie_name,
                                         start_utc=start_time_utc.strftime('%Y-%m-%d %H:%M:%S+00:00'),
                                         finish_utc=finish_time_utc.strftime('%Y-%m-%d %H:%M:%S+00:00')))
            print(f'Created {scenario_file}')

        if not args.force_video:
            print(f'To create the video')
            print(f' - Edit file {scenario_file}')
            print(f' - Run:')
            print(
                f'PYTHONPATH=$PYTHONPATH:../navcomputer caffeinate -i /usr/local/bin/python3.9 main.py {scenario_file}')

        else:
            # Make video
            make_video(args.work_dir, movie_name, events_json_name, gopro, navigator.polars, args.ignore_cache)

    else:
        # Read scenario
        with open(args.scenario, 'r') as stream:
            try:
                scenario = yaml.safe_load(stream)
            except yaml.YAMLError as exc:
                print(exc)
                return

        for movie in scenario['movies']:
            movie_name = movie['name']
            movie_file_name = get_valid_filename(movie_name)
            finish_time_utc, start_time_utc = movie['finish'], movie['start']
            add_events = movie['add_events'] if 'add_events' in movie else None
            remove_events = movie['remove_events'] if 'remove_events' in movie else None

            # Make events for this movie
            events_json_name = create_events(args, config, movie_file_name, navigator, start_time_utc, finish_time_utc,
                                             add_events, remove_events)

            # Make video
            make_video(args.work_dir, movie_file_name, events_json_name, gopro, navigator.polars, args.ignore_cache)


def create_events(args, config, movie_name, navigator, start_time_utc, finish_time_utc, extra_events=None,
                  ignore_events=None):

    events_json_name = args.work_dir + os.sep + movie_name + '.json'
    nmea_cache_name = args.work_dir + os.sep + movie_name + '.nmea'
    nmea_parser = NmeaParser(navigator, strict_cc=True)
    events_recorder = RaceEventsRecorder(args.work_dir, start_time_utc, finish_time_utc, ignore_events)
    navigator.add_listener(events_recorder)
    data_dir = os.path.expanduser(args.work_dir)
    navigator.set_data_dir(data_dir)

    # Create automatic events from the NMEA stream
    if os.path.isfile(nmea_cache_name):
        print(f'Using cached NMEA from {nmea_cache_name}')
        with open(nmea_cache_name, 'r') as f:
            for nmea in f:
                nmea_parser.set_nmea_sentence(nmea)
    else:
        with open(nmea_cache_name, 'w') as f:
            print(f'Will cache NMEA to {nmea_cache_name}')
            for nmea in s3_sk_nmea_logs(start_time_utc, finish_time_utc,
                                        config['bucket'], config['uuid'], config['profile']):
                nmea_parser.set_nmea_sentence(nmea)
                f.write(nmea)

    # Add manual events
    if extra_events is not None:
        events_recorder.add_extra_events(extra_events)

    # All events are created, finalize the events

    events_recorder.finalize()

    # Store race json file
    with open(events_json_name, 'wt') as f:
        f.write(events_recorder.to_json())
        print(f'Created {events_json_name}')

    # Create KML file that can be used to generate manual events
    kml_file = args.work_dir + os.sep + movie_name + '.kml'
    make_kml(kml_file, events_recorder.events, events_recorder.instr_data)

    return events_json_name


def get_valid_filename(s):
    s = str(s).strip().replace(' ', '_')
    return re.sub(r'(?u)[^-\w.]', '', s)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("scenario", help="YAML file describing movies to be produced", nargs='?', default=None)
    parser.add_argument("--cfg-file", help="Config YAML file", default='cfg/config.yaml')
    parser.add_argument("--work-dir", help="Working directory", default='/tmp')
    parser.add_argument("--ignore-cache", help="Use cached data only", default=False, action='store_true')
    parser.add_argument("--force-video", '-f', help="Make video even if there is no scenario", default=False,
                        action='store_true')
    parser.add_argument("--gopro-dir", help="GoPro SD card directory", default='/Volumes/GOPRO')

    main(parser.parse_args())
