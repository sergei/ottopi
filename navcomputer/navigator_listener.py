from raw_instr_data import RawInstrData


class DestInfo:
    def __init__(self):
        self.wpt = None  # Destination waypoint
        self.org_wpt = None  # Origin waypoint
        self.dtw = None  # Distance to waypoint (Nautical miles)
        self.btw = None  # Bearing to waypoint (magnetic degrees)
        self.btw_true = None  # Bearing to waypoint (true degrees)
        self.atw = None  # Angle to waypoint [-180;+180] (negative mark is to the left, positive mark is to the right)
        self.atw_up = None  # If mark is up or down relative to the wind direction (True - Up, False  - Down)
        self.xte = None  # Cross track positive, steer left, negative steer right
        self.bod = None  # Bearing origin to destination
        self.stw = None  # Velocity towards waypoint
        self.is_in_circle = False  # Arrival Circle Entered


class HistoryItem:
    def __init__(self, utc=None, orig=None, dest=None, avg_boat_twa=None, avg_hdg=None):
        self.utc = utc
        self.orig = orig
        self.dest = dest
        self.avg_boat_twa = avg_boat_twa
        self.avg_hdg = avg_hdg


class WindShift:
    def __init__(self, utc, shift_deg, is_lift):
        self.utc = utc
        self.shift_deg = shift_deg
        self.is_lift = is_lift


class NavigationListener:
    def __init__(self):
        pass

    def on_nmea(self, nmea):
        pass

    def on_speech(self, speech):
        pass

    def on_instr_data(self, instr_data: RawInstrData):
        pass

    def on_targets(self, targets):
        pass

    def on_dest_info(self, instr_data: RawInstrData, dest_info: DestInfo):
        pass

    def on_history_item(self, leg_summary: HistoryItem):
        pass

    def on_wind_shift(self, wind_shift: WindShift):
        pass
