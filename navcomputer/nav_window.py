from collections import deque


class NavWindow:
    STATE_UNKNOWN = 0
    STATE_STRAIGHT = 1  # Sailing on the same point of sail
    STATE_ROUNDED_TOP = 2  # Rounded upwind (top) mark
    STATE_ROUNDED_BOTTOM = 3  # Rounded downwind (bottom) mark
    STATE_TACKED = 4  # Tacked (or gybed)

    WIN_LEN = 60  # Length of the sliding window
    SOG_THR = 2.  # If average SOG is below this threshold we throw the data out

    TURN_THR1 = WIN_LEN / 10  # Threshold to detect roundings and tacks
    TURN_THR2 = WIN_LEN / 4   # Threshold to detect roundings and tacks

    """ This class implements the sliding window of nav data to perform averaging opeartions"""
    def __init__(self):
        self.sog = deque(maxlen=self.WIN_LEN)
        self.sog_sum = 0.
        self.up_down = deque(maxlen=self.WIN_LEN)  # 1 - upwind, -1 - downwind, 0 - reach
        self.up_down_sum = 0

    def reset(self):
        self.sog.clear()
        self.sog_sum = 0.
        self.up_down.clear()
        self.up_down_sum = 0.

    def update(self, instr_data):
        if len(self.sog) < self.WIN_LEN:
            oldest_sog = 0
            oldest_up_down = 0
        else:
            oldest_sog = self.sog.popleft()
            oldest_up_down = self.up_down.popleft()

        self.sog_sum -= oldest_sog
        self.sog.append(instr_data.sog)
        self.sog_sum += instr_data.sog

        up_down = 1 if abs(instr_data.awa) < 70 else -1 if abs(instr_data.awa) > 110 else 0
        self.up_down_sum -= oldest_up_down
        self.up_down.append(up_down)
        self.up_down_sum += up_down

        if len(self.sog) < self.WIN_LEN:
            return self.STATE_UNKNOWN

        avg_sog = self.sog_sum / self.WIN_LEN
        if avg_sog < self.SOG_THR:
            self.reset()
            return self.STATE_UNKNOWN

        if abs(self.up_down_sum) < self.TURN_THR1:  # Suspected rounding either to or bottom mark
            half_win = int(self.WIN_LEN / 2)
            sum_before = sum(list(self.up_down)[:half_win])
            sum_after = sum(list(self.up_down)[half_win:])
            # Check if went straight on both sides of the window
            if abs(sum_before) > self.TURN_THR1 and abs(sum_after) > self.TURN_THR2:
                if sum_after > 0:
                    self.reset()
                    return self.STATE_ROUNDED_BOTTOM
                else:
                    self.reset()
                    return self.STATE_ROUNDED_TOP

        return self.STATE_UNKNOWN
