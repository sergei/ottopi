import math
from collections import deque

from gpxpy.gpx import GPXRoutePoint

METERS_IN_NM = 1852.
TURN_DURATION = 20  # Analyze that many points for the turn duration


class LegSummary:
    def __init__(self, utc=None, orig=None, dest=None, avg_boat_twa=None, avg_hdg=None, delta_dist_wind_m=None,
                 avg_delta_twa=None, delta_boat_speed_perc=None):
        self.utc = utc
        self.orig = orig
        self.dest = dest
        self.avg_boat_twa = avg_boat_twa
        self.avg_hdg = avg_hdg
        self.delta_dist_wind_m = delta_dist_wind_m
        self.avg_delta_twa = avg_delta_twa
        self.delta_boat_speed_perc = delta_boat_speed_perc


class WindShift:
    def __init__(self, detected, shift_deg, is_lift):
        self.detected = detected
        self.shift_deg = shift_deg
        self.is_lift = is_lift


class LegAnalyzer:
    HIST_LEN = 120
    STRAIGHT_THR_CNT = 110
    STRAIGHT_THR_ANGLE = 30
    TWDS_LEN = 40  # Length of running window to analyze for shifts
    SHIFT_THR_DEG = 10  # Detect shifts greater than that

    def __init__(self):
        self.hist = []
        self.summaries = []
        self.twds = deque()

    def update(self, instr_data, targets):
        summary = None

        # See if any maneuver was done
        if self.check_for_turns():
            self.hist.clear()
            self.twds.clear()

        self.hist.append((instr_data, targets))

        # Check if we accumulated enough data in our history buffer
        if len(self.hist) == LegAnalyzer.HIST_LEN:
            summary = self.process_history()
            self.hist.clear()

        #  Check for wind shifts
        wind_shift = self.detect_wind_shift(instr_data)

        return summary, wind_shift

    def process_history(self):
        # Check if sailed more or less straight line by comparing
        twas = [d.twa for d, t in self.hist]
        avg_twa, twa_cnt = self.mean(twas)
        if twa_cnt < LegAnalyzer.STRAIGHT_THR_ANGLE:
            print('Too few TWAs {}'.format(twa_cnt))
            self.hist.clear()
            return None

        twa_deltas = sorted([abs(twa - avg_twa) if twa is not None else 10000 for twa in twas])
        # Check if predefined percentile is within the threshold
        straight = twa_deltas[LegAnalyzer.STRAIGHT_THR_CNT] < LegAnalyzer.STRAIGHT_THR_ANGLE
        if not straight:
            print("Didn't sail straight line")
            self.hist.clear()
            return None

        # Now see how good was sailing on straight line
        avg_boat_sow, avg_boat_sow_cnt = self.mean([d.sow for d, t in self.hist])
        avg_boat_twa, avg_boat_twa_cnt = self.mean([d.twa for d, t in self.hist])
        avg_hdg, avg_hdg_cnt = self.mean([d.hdg for d, t in self.hist])

        avg_boat_vmg, avg_boat_vmg_cnt = self.mean([t.boat_vmg for d, t in self.hist])
        avg_target_vmg, avg_target_vmg_cnt = self.mean([t.target_vmg for d, t in self.hist])
        avg_target_sow, avg_target_sow_cnt = self.mean([t.target_sow for d, t in self.hist])
        avg_target_twa, avg_target_twa_cnt = self.mean([t.target_twa for d, t in self.hist])

        if avg_boat_twa is not None and avg_target_twa is not None:
            if avg_boat_twa < 0:
                avg_target_twa = - avg_target_twa

        elapsed_time_hr = (self.hist[-1][0].utc - self.hist[0][0].utc).total_seconds() / 3600.

        # Compare distances sailed to wind
        if avg_target_vmg is not None and avg_boat_vmg is not None:
            target_wind_dist_m = avg_target_vmg * elapsed_time_hr * METERS_IN_NM
            boat_wind_dist_m = avg_boat_vmg * elapsed_time_hr * METERS_IN_NM
            delta_dist_wind_m = boat_wind_dist_m - target_wind_dist_m
        else:
            delta_dist_wind_m = None

        # See if sailed higher or lower on average
        if avg_target_twa is not None and avg_boat_twa is not None:
            avg_delta_twa = avg_target_twa - avg_boat_twa
        else:
            avg_delta_twa = None

        # See if sailed faster or slower than target
        delta_boat_speed_perc = None
        if avg_boat_sow is not None and avg_target_sow is not None:
            if avg_boat_sow > 0.1 and avg_target_sow > 0.1:
                delta_boat_speed_perc = (avg_boat_sow/avg_target_sow - 1) * 100

        orig_wpt = GPXRoutePoint(name='', latitude=self.hist[0][0].lat, longitude=self.hist[0][0].lon,
                                 time=self.hist[0][0].utc)
        dest_wpt = GPXRoutePoint(name='', latitude=self.hist[-1][0].lat, longitude=self.hist[-1][0].lon,
                                 time=self.hist[-1][0].utc)

        utc = self.hist[-1][0].utc
        summary = LegSummary(utc=utc, orig=orig_wpt, dest=dest_wpt, avg_boat_twa=avg_boat_twa, avg_hdg=avg_hdg,
                             delta_dist_wind_m=delta_dist_wind_m, avg_delta_twa=avg_delta_twa,
                             delta_boat_speed_perc=delta_boat_speed_perc)

        self.summaries.append(summary)

        return summary

    @staticmethod
    def mean(xx):
        acc = sum([x if x is not None else 0 for x in xx])
        cnt = len(xx) - xx.count(None)
        return None if cnt == 0 else acc / cnt, cnt

    def check_for_turns(self):
        if len(self.hist) < TURN_DURATION:
            return False

        # TODO:
        # Exclude near DDW TWA is range [-170 +170] from consideration
        # Don't compute mean values over sign change
        # Consider [-170 +170] as separate DDW state

        # Get last N points
        twas = [d.twa for d, t in self.hist[-TURN_DURATION:]]
        staright_cnt = int(TURN_DURATION / 4)
        mean_twa_after, after_cnt = self.mean(twas[-staright_cnt:])
        mean_twa_before, before_cnt = self.mean(twas[:staright_cnt])

        # Check if we trust these mean values
        if before_cnt < staright_cnt:
            print('Too few before AWSs')
            return False

        if after_cnt < staright_cnt:
            print('Too few after AWSs')
            return False

        is_tack_or_gybe = math.copysign(1, mean_twa_before) * math.copysign(1, mean_twa_after) < 0
        is_upwind_before = abs(mean_twa_before) < 90
        is_upwind_after = abs(mean_twa_after) < 90

        if is_upwind_before and is_upwind_after and is_tack_or_gybe:
            print('Tack detected')
            return True
        elif (not is_upwind_before and not is_upwind_after) and is_tack_or_gybe:
            print('Gybe detected')
            return True

        if is_upwind_before and not is_upwind_after:
            print('Rounded windward mark')
            return True

        if not is_upwind_before and is_upwind_after:
            print('Rounded leeward mark')
            return True

        return False

    def detect_wind_shift(self, instr_data):
        if instr_data.twa is not None and instr_data.hdg is not None:
            if len(self.twds) == LegAnalyzer.TWDS_LEN:
                self.twds.popleft()
            twd = instr_data.hdg + instr_data.twa
            if twd < 0:
                twd += 360
            elif twd >= 360:
                twd -= 360

            self.twds.append(twd)

            half_win = int(LegAnalyzer.TWDS_LEN/2)
            if len(self.twds) == LegAnalyzer.TWDS_LEN:
                before_twd, _ = self.mean(list(self.twds)[0:half_win])
                after_twd, _ = self.mean(list(self.twds)[half_win:])
                shift_deg = after_twd - before_twd
                # Make sign negative for baccking and positive for veering
                if shift_deg > 180:  # eg, 355 - 10 = 345 = -15   backed by 15 degrees
                    shift_deg -= 360
                elif shift_deg < -180:  # Eg 10 - 355 = -345 = 15 veered by 15 degrees
                    shift_deg += 360

                wind_shift_detected = abs(shift_deg) > LegAnalyzer.SHIFT_THR_DEG
                if wind_shift_detected:
                    # Veer on a starboard tack is a lift
                    # Backing on a port tack is a lift as well
                    is_lift = shift_deg * instr_data.twa > 0
                    wind_shift = WindShift(wind_shift_detected, shift_deg, is_lift)
                    self.twds.clear()
                    # print(wind_shift.__dict__, instr_data.twa)
                    return wind_shift
                else:
                    return None

        return None
