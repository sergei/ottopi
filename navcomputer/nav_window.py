import math
from collections import deque

from gpxpy.geo import Location

from const import METERS_IN_NM


class NavWndEventsListener:
    def on_tack(self, utc, loc, is_tack, distance_loss_m):
        pass

    def on_mark_rounding(self, utc, loc, is_windward):
        pass

    def on_wind_shift(self, utc, loc, shift_deg, new_twd):
        pass

    def on_target_update(self, utc, loc, distance_delta, twa_angle_delta):
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


class NavWindow:

    WIN_LEN = 60  # Length of the sliding window
    HALF_WIN = int(WIN_LEN / 2)
    SOG_THR = 2.  # If average SOG is below this threshold we throw the data out

    TURN_THR1 = WIN_LEN / 10  # Threshold to detect roundings and tacks
    TURN_THR2 = WIN_LEN / 4   # Threshold to detect roundings and tacks

    STRAIGHT_THR = WIN_LEN - TURN_THR1

    WIND_SHIFT_THR = 4

    """ This class implements the sliding window of nav data to perform averaging opeartions"""
    def __init__(self, event_callbacks=None):
        self.event_callbacks = event_callbacks

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
        # Target performance analysis
        self.stats_vmg_diff = deque(maxlen=self.WIN_LEN)
        self.stats_speed_diff = deque(maxlen=self.WIN_LEN)
        self.stats_point_diff = deque(maxlen=self.WIN_LEN)

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
        self.stats_vmg_diff.clear()
        self.stats_speed_diff.clear()
        self.stats_point_diff.clear()

    def update(self, instr_data):
        twa = instr_data.twa
        sog = instr_data.sog
        hdg = instr_data.hdg
        utc = instr_data.utc

        # Must have all data
        if twa is None or sog is None or hdg is None or instr_data.lat is None or utc is None:
            self.reset()
            return

        up_down = 1 if abs(twa) < 70 else -1 if abs(twa) > 110 else 0
        sb_pr = 1 if 5 < twa < 175 else -1 if -175 < twa < -5 else 0
        twd = twa + hdg
        loc = Location(instr_data.lat, instr_data.lon)

        # Update the queues
        self.turns_sog.append(sog)
        self.turns_up_down.append(up_down)
        self.turns_sb_pr.append(sb_pr)
        self.stats_twd.append(twd)
        self.stats_utc.append(utc)
        self.stats_loc.append(loc)
        self.turns_utc.append(utc)
        self.turns_loc.append(loc)

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
                if self.event_callbacks is not None:
                    self.event_callbacks.on_mark_rounding(utc, loc, is_windward)
                self.reset()

        if abs(self.turns_sb_pr.get_sum()) < self.TURN_THR1:  # Suspected tacking or gybing
            sum_before, sum_after = self.turns_sb_pr.sum_halves()
            if abs(sum_before) > self.TURN_THR1 and abs(sum_after) > self.TURN_THR2:
                tack_idx = self.HALF_WIN
                utc = self.turns_utc[tack_idx]
                loc = self.turns_loc[tack_idx]
                is_tack = abs(twa) < 90
                distance_loss_m = self.compute_tack_efficiency(tack_idx)
                if self.event_callbacks is not None:
                    self.event_callbacks.on_tack(utc, loc, is_tack, distance_loss_m)
                self.reset()

        if abs(self.turns_up_down.get_sum()) > self.STRAIGHT_THR and abs(self.turns_sb_pr.get_sum()) > self.STRAIGHT_THR \
                and self.stats_twd.is_full():
            # Compute average TWD
            avg_twd = self.compute_avg_twd()
            if self.ref_twd is None:
                self.ref_twd = avg_twd

            wind_shift = avg_twd - self.ref_twd
            if abs(wind_shift) > self.WIND_SHIFT_THR:
                utc = self.stats_utc[-1]
                loc = self.stats_loc[-1]
                if self.event_callbacks is not None:
                    self.event_callbacks.on_wind_shift(utc, loc, wind_shift, avg_twd)
                self.ref_twd = avg_twd

            # Reset stats windows
            self.clear_stats_queues()
            return

    def compute_avg_twd(self):
        # To deal with wrapping around problem do it in cartesian system
        east_sum = 0
        north_sum = 0
        for twd in self.stats_twd.q:
            east_sum += math.cos(math.radians(twd))
            north_sum += math.sin(math.radians(twd))

        avg_twd = math.degrees(math.atan2(north_sum, east_sum)) % 360.
        return avg_twd

    def compute_tack_efficiency(self, tack_idx):
        before_tack_idx = tack_idx - self.TURN_THR1
        sum_before, sum_after = self.turns_sog.sum_halves(before_tack_idx)
        avg_sog_before = sum_before / before_tack_idx
        avg_sog_after = sum_after / (self.turns_sog.len() - before_tack_idx)

        duration_sec = self.turns_sog.len()
        distance_loss_m = (avg_sog_before - avg_sog_after) * METERS_IN_NM / 3600. * duration_sec
        return distance_loss_m
