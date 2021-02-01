class RawInstrData:
    def __init__(self, t=0, utc=None, lat=None, lon=None, sog=None, cog=None, awa=None, aws=None, twa=None, tws=None,
                 sow=None, hdg=None):
        self.t = t
        # GPS stuff
        self.utc = utc
        self.lat = lat
        self.lon = lon
        self.sog = sog
        self.cog = cog
        # Sensors
        self.awa = awa  # Apparent wind angle degrees
        self.aws = aws  # Apparent wind speed KTS
        self.twa = twa  # True wind angle degrees
        self.tws = tws  # True wind speed KTS
        self.sow = sow  # Speed over water KTS
        self.hdg = hdg  # Boat heading (magnetic degrees)


