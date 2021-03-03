import math
from collections import deque


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

    def sum_halves(self):
        half_win = int(self.max_len / 2)
        sum_before = sum(list(self.q)[:half_win])
        sum_after = sum(list(self.q)[half_win:])
        return sum_before, sum_after

    def is_full(self):
        return len(self.q) == self.max_len


class NavWindow:
    STATE_UNKNOWN = 0
    STATE_STRAIGHT = 1  # Sailing on the same point of sail
    STATE_ROUNDED_TOP = 2  # Rounded upwind (top) mark
    STATE_ROUNDED_BOTTOM = 3  # Rounded downwind (bottom) mark
    STATE_TACKED = 4  # Tacked (or gybed)
    STATE_WIND_SHIFT = 5  # Wind shift detected

    WIN_LEN = 60  # Length of the sliding window
    SOG_THR = 2.  # If average SOG is below this threshold we throw the data out

    TURN_THR1 = WIN_LEN / 10  # Threshold to detect roundings and tacks
    TURN_THR2 = WIN_LEN / 4   # Threshold to detect roundings and tacks

    STRAIGHT_THR = WIN_LEN - TURN_THR1

    WIND_SHIFT_THR = 4

    """ This class implements the sliding window of nav data to perform averaging opeartions"""
    def __init__(self, on_wind_shift=None):
        self.sog = SlidingWindow(maxlen=self.WIN_LEN)
        self.up_down = SlidingWindow(maxlen=self.WIN_LEN)  # 1 - upwind, -1 - downwind, 0 - reach
        self.sb_pr = SlidingWindow(maxlen=self.WIN_LEN)  # 1 - starboard, -1 - port, 0 - head to wind or ddw
        self.twd = SlidingWindow(maxlen=self.WIN_LEN)
        self.ref_twd = None
        self.on_wind_shift = on_wind_shift

    def reset(self):
        self.sog.clear()
        self.up_down.clear()
        self.sb_pr.clear()
        self.twd.clear()
        self.ref_twd = None  # The instruments usually are not calibrated and we compare TWDs only on the same tack

    def update(self, instr_data):
        twa = instr_data.twa
        sog = instr_data.sog
        hdg = instr_data.hdg

        # Must have all data
        if twa is None or sog is None or hdg is None:
            self.reset()
            return self.STATE_UNKNOWN

        # Update the queues
        self.sog.append(sog)

        up_down = 1 if abs(twa) < 70 else -1 if abs(twa) > 110 else 0
        self.up_down.append(up_down)

        sb_pr = 1 if 5 < twa < 175 else -1 if -175 < twa < -5 else 0
        self.sb_pr.append(sb_pr)

        self.twd.append(twa + hdg)

        # Analyse the queues

        if self.sog.len() < self.WIN_LEN:
            return self.STATE_UNKNOWN

        if self.sog.get_avg() < self.SOG_THR:
            self.reset()
            return self.STATE_UNKNOWN

        if abs(self.up_down.get_sum()) < self.TURN_THR1:  # Suspected rounding either top or bottom mark
            sum_before, sum_after = self.up_down.sum_halves()
            if abs(sum_before) > self.TURN_THR1 and abs(sum_after) > self.TURN_THR2:
                if sum_after > 0:
                    self.reset()
                    return self.STATE_ROUNDED_BOTTOM
                else:
                    self.reset()
                    return self.STATE_ROUNDED_TOP

        if abs(self.sb_pr.get_sum()) < self.TURN_THR1:  # Suspected tacking or gybing
            sum_before, sum_after = self.sb_pr.sum_halves()
            if abs(sum_before) > self.TURN_THR1 and abs(sum_after) > self.TURN_THR2:
                self.reset()
                return self.STATE_TACKED

        if abs(self.up_down.get_sum()) > self.STRAIGHT_THR and abs(self.sb_pr.get_sum()) > self.STRAIGHT_THR \
                and self.twd.is_full():
            # Compute average TWD
            avg_twd = self.compute_avg_twd()
            self.twd.clear()
            if self.ref_twd is None:
                self.ref_twd = avg_twd

            wind_shift = avg_twd - self.ref_twd
            if abs(wind_shift) > self.WIND_SHIFT_THR:
                if self.on_wind_shift is not None:
                    self.on_wind_shift(wind_shift)
                self.ref_twd = avg_twd

            return self.STATE_STRAIGHT

        return self.STATE_UNKNOWN

    def compute_avg_twd(self):
        # To deal with wrapping around problem do it in cartesian system
        east_sum = 0
        north_sum = 0
        for twd in self.twd.q:
            east_sum += math.cos(math.radians(twd))
            north_sum += math.sin(math.radians(twd))

        avg_twd = math.degrees(math.atan2(north_sum, east_sum)) % 360.
        return avg_twd
