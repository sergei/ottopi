import copy
import datetime
import math
import random

import geomag
from gpxpy.geo import LocationDelta

from const import METERS_IN_NM
from raw_instr_data import RawInstrData


class BoatModel:
    def __init__(self, twd=0, tws=10, cog=30, sog=5, speed_rms=0.5, angle_rms=1):
        self.t = [0]
        self.twd = [twd]
        self.tws = [tws]
        self.cog = [cog]
        self.sog = [sog]
        self.speed_rms = speed_rms
        self.angle_rms = angle_rms

    def update(self, t, twd=None, tws=None, cog=None, sog=None):
        twd = twd if twd is not None else self.twd[-1]
        tws = tws if tws is not None else self.tws[-1]
        cog = cog if cog is not None else self.cog[-1]
        sog = sog if sog is not None else self.sog[-1]

        self.t.append(t)
        self.twd.append(twd)
        self.tws.append(tws)
        self.cog.append(cog)
        self.sog.append(sog)

    def intsr_data(self, start_utc, start_loc, num, dt=1):
        idx = 0
        t = 0
        twd = self.t[0]
        tws = self.tws[0]
        cog = self.cog[0]
        sog = self.sog[0]
        loc = copy.deepcopy(start_loc)
        mag_decl = geomag.declination(loc.latitude, loc.longitude)

        for i in range(0, num):
            if idx < len(self.t) and t >= self.t[idx]:
                twd = self.twd[idx]
                tws = self.tws[idx]
                cog = self.cog[idx]
                sog = self.sog[idx]
                idx += 1

            utc = start_utc + datetime.timedelta(0, t)
            hdg = cog - mag_decl
            twa = (twd - hdg) % 360
            if twa > 180:
                twa -= 360
            cos_twa = math.cos(math.radians(twa))
            aws = math.sqrt(sog * sog + tws * tws + 2 * sog * tws * cos_twa)
            awa = math.degrees(math.acos((tws * cos_twa + sog) / aws))
            awa = awa if twa > 0 else -awa

            instr_data = RawInstrData(utc=utc, lat=loc.latitude, lon=loc.longitude,
                                      cog=self.noisy_dir(cog), sog=self.noisy_speed(sog),
                                      awa=self.noisy_angle(awa), aws=self.noisy_speed(aws),
                                      twa=self.noisy_angle(twa), tws=self.noisy_speed(tws),
                                      hdg=self.noisy_dir(hdg), sow=self.noisy_speed(sog))

            yield instr_data

            t += dt
            dist = (sog * METERS_IN_NM) * (dt / 3600.)
            loc.move(LocationDelta(dist, cog))

    def noisy_angle(self, angle):
        angle = random.gauss(angle, self.angle_rms)
        if angle > 180:
            return angle - 360
        elif angle < -180:
            return angle + 360
        else:
            return angle

    def noisy_dir(self, direction):
        return random.gauss(direction, self.angle_rms) % 360.

    def noisy_speed(self, speed):
        return abs(random.gauss(speed, self.speed_rms))
