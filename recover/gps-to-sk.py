import argparse
from nmeaparser import NmeaParser


class NmeaListener:
    def __init__(self):
        self.nmea_map = {}
        self.epoch = []

    def set_raw_instr_data(self, raw_instr_data):
        utc = raw_instr_data.utc
        ts = int(utc.timestamp())
        self.nmea_map[ts] = self.epoch.copy()
        self.epoch = []
        pass

    def set_sentences(self, msg):
        self.epoch.append(msg)


def gps_to_sk(args):
    nmea_listener = NmeaListener()
    nmea_parser = NmeaParser(nmea_listener, strict_cc=True)
    with open(args.nmea_file, 'r') as nmea_file:
        for line in nmea_file:
            nmea_listener.set_sentences(line)
            nmea_parser.set_nmea_sentence(line)

    with open(args.sk_file, 'r') as sk_file, open(args.merged_file, 'w') as merged_file:
        for sk_line in sk_file:
            merged_file.write(sk_line)
            t = sk_line.split(';')
            if len(t) > 2 and t[1] == 'N' and t[0].isnumeric():
                ts = int(t[0]) // 1000
                if ts in nmea_listener.nmea_map:
                    epoch = nmea_listener.nmea_map[ts]
                    nmea_listener.nmea_map.pop(ts)
                    for nmea_line in epoch:
                        merged_file.write(t[0] + ';N;' + nmea_line)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--nmea-file", help="NMEA file", required=True)
    parser.add_argument("--sk-file", help="SignalK file", required=True)
    parser.add_argument("--merged-file", help="Merged file file", required=True)

    gps_to_sk(parser.parse_args())
