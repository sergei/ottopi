import argparse
import csv
import datetime
import gpxpy
import gpxpy.gpx
import pyproj
from dateutil import parser


def jps2gpx(args):
    ecef = pyproj.Proj(proj='geocent', ellps='WGS84', datum='WGS84')
    lla = pyproj.Proj(proj='latlong', ellps='WGS84', datum='WGS84')

    gpx = gpxpy.gpx.GPX()

    # Create first track in our GPX:
    gpx_track = gpxpy.gpx.GPXTrack()
    gpx.tracks.append(gpx_track)

    # Create first segment in our GPX track:
    gpx_segment = gpxpy.gpx.GPXTrackSegment()
    gpx_track.segments.append(gpx_segment)

    with open(args.csv, newline='') as csvfile, open(args.gpx, 'w') as gpx_file:
        reader = csv.DictReader(csvfile, delimiter=',')
        for row in reader:
            sigma = float(row['sigma'])
            if sigma < 0.8:
                lon, lat, alt = pyproj.transform(ecef, lla, float(row['x']), float(row['y']), float(row['z']), radians=False)
                t = parser.parse(row['t'])
                utc = datetime.datetime(2021, 9, 23, t.time().hour, t.time().minute, t.time().second) - \
                      datetime.timedelta(seconds=17)
                gpx_segment.points.append(gpxpy.gpx.GPXTrackPoint(lat, lon, alt, utc))

        gpx_file.write(gpx.to_xml())
        print('Created GPX:', args.gpx)


if __name__ == '__main__':
    arg_parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    arg_parser.add_argument("--csv", help="CSV file derived form JPS file",  required=True)
    arg_parser.add_argument("--gpx", help="GPX file",  required=True)

    jps2gpx(arg_parser.parse_args())
