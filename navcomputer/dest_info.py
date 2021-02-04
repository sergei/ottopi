class DestInfo:
    def __init__(self):
        self.wpt = None  # Destination waypoint
        self.dtw = None  # Distance to waypoint (Nautical miles)
        self.btw = None  # Bearing to waypoint (magnetic degrees)
        self.atw = None  # Angle to waypoint [-180;+180] (negative mark is to the left, positive mark is to the right)
        self.atw_up = None  # If mark is up or down relative to the wind direction (True - Up, False  - Down)
