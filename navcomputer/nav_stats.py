import datetime
import math
from collections import deque

from gpxpy.geo import Location

from const import METERS_IN_NM


class NavStatsEventsListener:
    def on_tack(self, utc: datetime, loc: Location, is_tack: bool, distance_loss_m: float):
        pass

    def on_mark_rounding(self, utc: datetime, loc: Location, is_windward: bool):
        pass

    def on_wind_shift(self, utc: datetime, loc: Location, shift_deg: float, new_twd: float, is_lift: bool):
        pass

    def on_history_update(self, utc: datetime, loc_from: Location, loc_to: Location, avg_hdg: float, avg_twa: float):
        pass

    def on_target_update(self, utc: datetime, loc: Location, distance_delta_m: float, speed_delta: float,
                         twa_angle_delta: float):
        pass


class SlidingWindow:
    def __init__(self, maxlen):
        self.max_len = maxlen
        self.q = deque(maxlen=maxlen)
        self.sum = 0.

    def clear(self):
        self.q.clear()
        self.sum = 0.

    def append(self, v):
        if len(self.q) < self.max_len:
            old_v = 0
        else:
            old_v = self.q.popleft()

        self.sum -= old_v
        self.q.append(v)
        self.sum += v

    def len(self):
        return len(self.q)

    def get_avg(self):
        return self.sum / len(self.q)

    def get_sum(self):
        return self.sum

    def sum_halves(self, split_point=None):
        split_point = int(self.max_len / 2) if split_point is None else int(split_point)
        sum_before = sum(list(self.q)[:split_point])
        sum_after = sum(list(self.q)[split_point:])
        return sum_before, sum_after

    def is_full(self):
        return len(self.q) == self.max_len


class NavStats:

    WIN_LEN = 60  # Length of the sliding window
    HALF_WIN = int(WIN_LEN / 2)
    SOG_THR = 2.  # If average SOG is below this threshold we throw the data out

    TURN_THR1 = WIN_LEN / 10  # Threshold to detect roundings and tacks
    TURN_THR2 = WIN_LEN / 4   # Threshold to detect roundings and tacks

    STRAIGHT_THR = WIN_LEN - TURN_THR1

    WIND_SHIFT_THR = 10

    """ This class implements the sliding window of nav data to perform averaging opeartions"""
    def __init__(self, stats_listener=None):
        self.stats_listener = stats_listener

        # Queues to analyze the turns
        self.turns_utc = deque(maxlen=self.WIN_LEN)
        self.turns_loc = deque(maxlen=self.WIN_LEN)
        self.turns_sog = SlidingWindow(maxlen=self.WIN_LEN)
        self.turns_up_down = SlidingWindow(maxlen=self.WIN_LEN)  # 1 - upwind, -1 - downwind, 0 - reach
        self.turns_sb_pr = SlidingWindow(maxlen=self.WIN_LEN)  # 1 - starboard, -1 - port, 0 - head to wind or ddw

        # Queues to analyze stats
        self.stats_utc = deque(maxlen=self.WIN_LEN)
        self.stats_loc = deque(maxlen=self.WIN_LEN)
        # Wind shift analysis
        self.ref_twd = None
        self.stats_twd = SlidingWindow(maxlen=self.WIN_LEN)
        self.stats_twa = SlidingWindow(maxlen=self.WIN_LEN)
        self.stats_hdg = SlidingWindow(maxlen=self.WIN_LEN)
        # Target performance analysis
        self.stats_vmg_diff = SlidingWindow(maxlen=self.WIN_LEN)
        self.stats_speed_diff = SlidingWindow(maxlen=self.WIN_LEN)
        self.stats_point_diff = SlidingWindow(maxlen=self.WIN_LEN)

    def reset(self):
        self.turns_sog.clear()
        self.turns_up_down.clear()
        self.turns_sb_pr.clear()
        self.turns_utc.clear()
        self.turns_loc.clear()

        # The instruments usually are not calibrated so we reset all stats information after every turn
        self.ref_twd = None
        self.clear_stats_queues()

    def clear_stats_queues(self):
        self.stats_utc.clear()
        self.stats_loc.clear()
        self.stats_twd.clear()
        self.stats_twa.clear()
        self.stats_hdg.clear()
        self.stats_vmg_diff.clear()
        self.stats_speed_diff.clear()
        self.stats_point_diff.clear()

    def update(self, instr_data, targets):
        twa = instr_data.twa
        sog = instr_data.sog
        hdg = instr_data.hdg
        utc = instr_data.utc

        if hdg is None:
            hdg = instr_data.cog

        # Must have all this data
        if twa is None or sog is None or hdg is None or instr_data.lat is None or utc is None:
            self.reset()
            return

        # The targets data is optional
        if targets.boat_vmg is not None and targets.target_vmg is not None:
            vmg_diff = targets.boat_vmg - targets.target_vmg
        else:
            vmg_diff = None
        if targets.bs is not None and targets.target_sow is not None:
            stats_speed_diff = targets.bs - targets.target_sow
        else:
            stats_speed_diff = None
        if twa is not None and targets.target_twa is not None:
            stats_point_diff = abs(twa) - abs(targets.target_twa)
        else:
            stats_point_diff = None

        up_down = 1 if abs(twa) < 70 else -1 if abs(twa) > 110 else 0
        sb_pr = 1 if 5 < twa < 175 else -1 if -175 < twa < -5 else 0
        twd = twa + hdg
        loc = Location(instr_data.lat, instr_data.lon)

        # Update the queues
        self.turns_utc.append(utc)
        self.turns_loc.append(loc)
        self.turns_sog.append(sog)
        self.turns_up_down.append(up_down)
        self.turns_sb_pr.append(sb_pr)

        self.stats_utc.append(utc)
        self.stats_loc.append(loc)
        self.stats_twd.append(twd)
        self.stats_twa.append(twa)
        self.stats_hdg.append(hdg)

        if vmg_diff is not None and stats_speed_diff is not None and stats_point_diff is not None:
            self.stats_vmg_diff.append(vmg_diff)
            self.stats_speed_diff.append(stats_speed_diff)
            self.stats_point_diff.append(stats_point_diff)

        # Analyse the queues

        if self.turns_sog.len() < self.WIN_LEN:
            return

        if self.turns_sog.get_avg() < self.SOG_THR:
            self.reset()
            return

        if abs(self.turns_up_down.get_sum()) < self.TURN_THR1:  # Suspected rounding either top or bottom mark
            sum_before, sum_after = self.turns_up_down.sum_halves()
            if abs(sum_before) > self.TURN_THR1 and abs(sum_after) > self.TURN_THR2:
                utc = self.turns_utc[self.HALF_WIN]
                loc = self.turns_loc[self.HALF_WIN]
                is_windward = sum_after < 0
                if self.stats_listener is not None:
                    self.stats_listener.on_mark_rounding(utc, loc, is_windward)
                self.reset()

        if abs(self.turns_sb_pr.get_sum()) < self.TURN_THR1:  # Suspected tacking or gybing
            sum_before, sum_after = self.turns_sb_pr.sum_halves()
            if abs(sum_before) > self.TURN_THR1 and abs(sum_after) > self.TURN_THR2:
                tack_idx = self.HALF_WIN
                utc = self.turns_utc[tack_idx]
                loc = self.turns_loc[tack_idx]
                is_tack = abs(twa) < 90
                distance_loss_m = self.compute_tack_efficiency(tack_idx)
                if self.stats_listener is not None:
                    self.stats_listener.on_tack(utc, loc, is_tack, distance_loss_m)
                self.reset()

        if abs(self.turns_up_down.get_sum()) > self.STRAIGHT_THR \
                and abs(self.turns_sb_pr.get_sum()) > self.STRAIGHT_THR \
                and self.stats_twd.is_full():

            utc = self.stats_utc[-1]
            loc_from = self.stats_loc[0]
            loc = self.stats_loc[-1]
            is_downwind = self.turns_up_down.get_sum() < 0

            # Update stats
            avg_hdg = self.compute_avg_angle(self.stats_hdg.q, unsigned=True)
            avg_twa = self.compute_avg_angle(self.stats_twa.q, unsigned=False)
            if self.stats_listener is not None:
                self.stats_listener.on_history_update(utc, loc_from, loc, avg_hdg, avg_twa)

            # Check for wind shift
            avg_twd = self.compute_avg_angle(self.stats_twd.q, unsigned=True)
            if self.ref_twd is None:
                self.ref_twd = avg_twd

            wind_shift = (avg_twd - self.ref_twd) % 360
            if wind_shift > 180:
                wind_shift -= 360

            is_lift = wind_shift * avg_twa > 0

            if abs(wind_shift) > self.WIND_SHIFT_THR:
                if self.stats_listener is not None:
                    self.stats_listener.on_wind_shift(utc, loc, wind_shift, avg_twd, is_lift)
                self.ref_twd = avg_twd

            # Compute target stats
            distance_delta_m, speed_delta, twa_angle_delta = self.compute_target_stats(is_downwind)
            if self.stats_listener is not None and distance_delta_m is not None:
                self.stats_listener.on_target_update(utc, loc, distance_delta_m, speed_delta, twa_angle_delta)

            # Reset stats windows
            self.clear_stats_queues()
            return

    @staticmethod
    def compute_avg_angle(angles, unsigned=True):
        # To deal with wrapping around problem do it in cartesian system
        east_sum = 0
        north_sum = 0
        for angle in angles:
            east_sum += math.cos(math.radians(angle))
            north_sum += math.sin(math.radians(angle))

        avg_angle = math.degrees(math.atan2(north_sum, east_sum)) % 360
        if not unsigned and avg_angle > 180:
            avg_angle = avg_angle - 360.

        return avg_angle

    def compute_tack_efficiency(self, tack_idx):
        before_tack_idx = tack_idx - self.TURN_THR1
        sum_before, sum_after = self.turns_sog.sum_halves(before_tack_idx)
        avg_sog_before = sum_before / before_tack_idx
        avg_sog_after = sum_after / (self.turns_sog.len() - before_tack_idx)

        duration_sec = self.turns_sog.len()
        distance_loss_m = (avg_sog_before - avg_sog_after) * METERS_IN_NM / 3600. * duration_sec
        return distance_loss_m

    def compute_target_stats(self, is_downwind):
        distance_delta_m = speed_delta = twa_angle_delta = None
        if self.stats_vmg_diff.len() > 0:
            duration_sec = self.stats_vmg_diff.len()
            speed_delta = self.stats_speed_diff.get_avg()

            distance_delta_m = self.stats_vmg_diff.get_avg() * METERS_IN_NM / 3600. * duration_sec
            twa_angle_delta = self.stats_point_diff.get_avg()
            if is_downwind:  # Since VMG is negative
                distance_delta_m = - distance_delta_m

        return distance_delta_m, speed_delta, twa_angle_delta
