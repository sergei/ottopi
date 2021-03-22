import argparse
import csv

from fusion import Fusion
from time_diff import time_diff


def fusion_test(args):
    with open(args.sensors_csv, 'r') as f:
        fusion = Fusion(timediff=time_diff)
        csv_reader = csv.DictReader(f, quoting=csv.QUOTE_NONE)
        for r in csv_reader:
            for k in r:
                r[k] = float(r[k])
            fusion.update((r['ax'], r['ay'], r['az']), (r['gx'], r['gy'], r['gz']), (r['mx'], r['my'], r['mz']), r['t'])
            print('heading {:.3f}, pitch {:.3f}, roll {:.3f}'.format(fusion.heading, fusion.pitch, fusion.roll))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--sensors-csv", help="File containing sensors log",  required=True)

    fusion_test(parser.parse_args())
