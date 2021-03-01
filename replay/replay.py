import glob
import os

from kml_writer import KmlWriter
from navigator_listener import NavigationListener


class Replay(NavigationListener):
    def __init__(self, replay_dir, log_dir, nmea_parser):
        super().__init__()
        self.replay_dir = os.path.expanduser(replay_dir)
        self.log_dir = os.path.expanduser(log_dir)
        self.nmea_parser = nmea_parser
        self.kml_writer = KmlWriter()

    def run(self, with_prefix):
        log_list = sorted(glob.glob(self.replay_dir + os.sep + 'log-*.nmea'))
        log_list += sorted(glob.glob(self.replay_dir + os.sep + '*.log'))

        for log in log_list:
            with open(log, 'r') as f:
                print('Replaying {}'.format(log))
                for nmea in f:
                    if with_prefix:
                        if nmea.startswith('>'):
                            self.nmea_parser.set_nmea_sentence(nmea[2:])
                    else:
                        self.nmea_parser.set_nmea_sentence(nmea)

        kml_file = self.log_dir + os.sep + "replay.kml"
        print('Saving {}'.format(kml_file))
        self.kml_writer.save(kml_file)

    def on_dest_info(self, raw_instr_data, dest_info):
        self.kml_writer.add_route_point(raw_instr_data, dest_info)

    def on_leg_summary(self, leg_summary):
        self.kml_writer.add_leg_summary(leg_summary)

    def on_speech(self, s):
        print(s)
        self.kml_writer.add_speech(s)
