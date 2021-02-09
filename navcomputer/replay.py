import glob
import os
import simplekml
from NavigatorListener import NavigationListener


class Replay(NavigationListener):
    def __init__(self, replay_dir, log_dir, nmea_parser):
        super().__init__()
        self.replay_dir = os.path.expanduser(replay_dir)
        self.log_dir = os.path.expanduser(log_dir)
        self.nmea_parser = nmea_parser
        self.last_dest_wpt = None  # Last WPT received from RMB message
        self.kml = simplekml.Kml()

        self.trk_pt_style = simplekml.Style()
        self.trk_pt_style.labelstyle.color = simplekml.Color.red  # Make the text red
        self.trk_pt_style.labelstyle.scale = 0.1  # Make the text twice as big
        self.trk_pt_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png'

        self.mark_style = simplekml.Style()
        self.mark_style.labelstyle.color = simplekml.Color.red  # Make the text red
        self.mark_style.labelstyle.scale = 1  # Make the text twice as big
        self.mark_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/paddle/ylw-blank.png'

    def run(self):
        log_list = sorted(glob.glob(self.replay_dir + os.sep + 'log-*.nmea'))
        log_list += sorted(glob.glob(self.replay_dir + os.sep + '*.log'))

        for log in log_list:
            with open(log, 'r') as f:
                for nmea in f:
                    self.nmea_parser.set_nmea_sentence(nmea)
        kml_file = self.log_dir + os.sep + "replay.kml"
        print('Saving {}'.format(kml_file))
        self.kml.save(kml_file)

    def on_dest_info(self, raw_instr_data, dest_info):
        point = self.kml.newpoint(name="", coords=[(raw_instr_data.lon, raw_instr_data.lat)])
        point.style = self.trk_pt_style

        description = "==INSTR==\n"
        for k in raw_instr_data.__dict__:
            description += '{}={}\n'.format(k, raw_instr_data.__dict__[k])

        description += "==DEST==\n"
        for k in dest_info.__dict__:
            description += '{}={}\n'.format(k, dest_info.__dict__[k])

        point.description = description

        if dest_info.wpt is not None:
            new_dest = self.last_dest_wpt is None or self.last_dest_wpt.name != dest_info.wpt.name
            if new_dest:
                self.last_dest_wpt = dest_info.wpt
                point = self.kml.newpoint(name=dest_info.wpt.name,
                                          coords=[(dest_info.wpt.longitude, dest_info.wpt.latitude)])
                point.style = self.mark_style





    def on_speech(self, s):
        print(s)
