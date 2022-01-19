import simplekml

TIME_FORMAT = "%Y-%m-%dT%H:%M:%SZ"


class KmlWriter:
    def __init__(self):
        self.kml = simplekml.Kml()
        self.route_folder = self.kml.newfolder(name="Route")
        self.wpts_folder = self.kml.newfolder(name="Wpts")
        self.summary_folder = self.kml.newfolder(name="Sumary")
        self.speech_folder = self.kml.newfolder(name="Speech")

        # Icons are available at http://kml4earth.appspot.com/icons.html
        self.trk_pt_style = simplekml.Style()
        self.trk_pt_style.labelstyle.color = simplekml.Color.red  # Make the text red
        self.trk_pt_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png'
        self.trk_pt_style.iconstyle.scale = 0.5

        self.mark_style = simplekml.Style()
        self.mark_style.labelstyle.color = simplekml.Color.red  # Make the text red
        self.mark_style.labelstyle.scale = 1
        self.mark_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/paddle/ylw-blank.png'

        self.summary_style = simplekml.Style()
        self.summary_style.labelstyle.color = simplekml.Color.yellow  # Make the text red
        self.summary_style.labelstyle.scale = 2
        self.summary_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/yen.png'

        self.speech_style = simplekml.Style()
        self.speech_style.labelstyle.color = simplekml.Color.yellow  # Make the text red
        self.speech_style.labelstyle.scale = 2
        self.speech_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/electronics.png'

        self.alam_style = simplekml.Style()
        self.alam_style.labelstyle.color = simplekml.Color.yellow  # Make the text red
        self.alam_style.labelstyle.scale = 2
        self.alam_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/marina.png'

        self.last_dest_wpt = None  # Last WPT received from RMB message
        self.last_coords = None

    def save(self, kml_file):
        self.kml.save(kml_file)

    def add_route_point(self, raw_instr_data, dest_info):
        self.last_coords = (raw_instr_data.lon, raw_instr_data.lat)
        self.last_utc = raw_instr_data.utc.strftime(TIME_FORMAT)

        point = self.route_folder.newpoint(name="", coords=[self.last_coords])
        point.style = self.trk_pt_style
        point.timestamp.when = self.last_utc

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
                point = self.wpts_folder.newpoint(name=dest_info.wpt.name,
                                                  coords=[(dest_info.wpt.longitude, dest_info.wpt.latitude)])
                point.style = self.mark_style
                point.timestamp.when = self.last_utc

    def add_leg_summary(self, leg_summary):
        dest = self.summary_folder.newpoint(name="", coords=[(leg_summary.dest.longitude, leg_summary.dest.latitude)])
        dest.style = self.summary_style
        dest.timestamp.when = leg_summary.utc.strftime(TIME_FORMAT)
        description = ""
        for k in leg_summary.__dict__:
            description += '{}={}\n'.format(k, leg_summary.__dict__[k])
        dest.description = description

        ls = self.summary_folder.newlinestring(name="")
        ls.coords = [(leg_summary.orig.longitude, leg_summary.orig.latitude),
                     (leg_summary.dest.longitude, leg_summary.dest.latitude)]
        ls.extrude = 1
        ls.altitudemode = simplekml.AltitudeMode.relativetoground
        ls.timestamp.when = leg_summary.utc.strftime(TIME_FORMAT)

    def add_speech(self, s):
        if self.last_coords is not None:
            point = self.speech_folder.newpoint(name="", coords=[self.last_coords])
            point.style = self.speech_style
            point.description = s
            point.timestamp.when = self.last_utc

    def add_backup_alarm(self):
        if self.last_coords is not None:
            point = self.speech_folder.newpoint(name="Backup Alarm", coords=[self.last_coords])
            point.style = self.alam_style
            point.timestamp.when = self.last_utc
