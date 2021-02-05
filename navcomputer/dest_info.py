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
