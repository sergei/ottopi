import argparse
import datetime
import gzip
import json
import os
import re
import zoneinfo

import boto3 as boto3
import botocore
import pytz
from pytz import UnknownTimeZoneError
from tzlocal import get_localzone

from kml_maker import make_kml
from race_events_recorder import RaceEventsRecorder
from video_maker import make_video
from navigator import Navigator
from nmeaparser import NmeaParser
from gopro import GoPro


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

    json_name = args.work_dir + os.sep + get_valid_filename(args.name) + '.json'

    navigator = Navigator.get_instance()
    navigator.read_polars(args.polar_file)

    gopro = GoPro(args.gopro_dir)

    if not args.cache_only:
        if args.race_date is None:
            finish_time_utc, start_time_utc = gopro.finish_time_utc, gopro.start_time_utc
        else:
            print(f'Using GOPRO clips time span to determine movie time span')
            finish_time_utc, start_time_utc = get_race_time_interval(args)

        nmea_parser = NmeaParser(navigator, strict_cc=True)

        events_recorder = RaceEventsRecorder(args.work_dir, start_time_utc, finish_time_utc)
        navigator.add_listener(events_recorder)

        data_dir = os.path.expanduser(args.work_dir)
        navigator.set_data_dir(data_dir)

        for nmea in s3_sk_nmea_logs(start_time_utc, finish_time_utc, args.bucket, args.uuid, args.profile):
            nmea_parser.set_nmea_sentence(nmea)

        # All NMEA is consumed finalize the events
        events_recorder.finalize()

        # Store race json file
        with open(json_name, 'wt') as f:
            f.write(events_recorder.to_json())
            print(f'Created {json_name}')
    else:
        print(f'Using cached {json_name}')

    # Read the cached race file
    with open(json_name, 'rt') as f:
        race_events = json.load(f)

    kml_file = args.work_dir + os.sep + get_valid_filename(args.name) + '.kml'
    make_kml(kml_file, race_events)

    ignore_cache = not args.cache_only
    make_video(args.work_dir, get_valid_filename(args.name), race_events, gopro, navigator.polars,
               ignore_cache)


def get_valid_filename(s):
    s = str(s).strip().replace(' ', '_')
    return re.sub(r'(?u)[^-\w.]', '', s)


def get_race_time_interval(args):
    race_date = datetime.datetime.strptime(args.race_date, '%Y-%m-%d')
    start_time = datetime.datetime.combine(race_date, datetime.datetime.strptime(args.start_time, '%H:%M').time())
    finish_time = datetime.datetime.combine(race_date, datetime.datetime.strptime(args.finish_time, '%H:%M').time())

    # Convert from local time zone to UTC

    if args.timezone is None:
        local_tz = get_localzone()
    else:
        try:
            local_tz = pytz.timezone(args.timezone)
        except UnknownTimeZoneError as e:
            print(f'Unknown Timezone {e}')
            print('Please use one of these:')
            print(zoneinfo.available_timezones())
            raise e

    start_time = local_tz.localize(start_time)
    finish_time = local_tz.localize(finish_time)
    start_time_utc = start_time.astimezone(pytz.utc)
    finish_time_utc = finish_time.astimezone(pytz.utc)

    return finish_time_utc, start_time_utc


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--work-dir", help="Working directory", default='/tmp')
    parser.add_argument("--name", help="Name of the clip")
    parser.add_argument("--race-date", help="Race date YYYY-MM-DD (local timezone)", required=False)
    parser.add_argument("--start-time", help="Start time HH:MM (local timezone)", required=False)
    parser.add_argument("--finish-time", help="Start time HH:MM (local timezone)", required=False)
    parser.add_argument("--timezone", help="Local timezone, e.g. PDT", required=False)
    parser.add_argument("--uuid", help="SK UUID", required=True)
    parser.add_argument("--bucket", help="S3 Bucket", required=True)
    parser.add_argument("--profile", help="S3 profile", required=True)
    parser.add_argument("--polar-file", help="Boat polar file", required=True)
    parser.add_argument("--cache-only", help="Use cached data only", default=False, action='store_true')
    parser.add_argument("--gopro-dir", help="GoPro SD card directory", default='/Volumes/GOPRO')

    main(parser.parse_args())
