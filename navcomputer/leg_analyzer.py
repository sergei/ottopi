from gpxpy.gpx import GPXRoutePoint

METERS_IN_NM = 1852.


class LegSummary:
    def __init__(self, orig=None, dest=None, avg_boat_twa=None, avg_hdg=None, delta_dist_wind_m=None,
                 avg_delta_twa=None, delta_boat_speed_perc=None):
        self.orig = orig
        self.dest = dest
        self.avg_boat_twa = avg_boat_twa
        self.avg_hdg = avg_hdg
        self.delta_dist_wind_m = delta_dist_wind_m
        self.avg_delta_twa = avg_delta_twa
        self.delta_boat_speed_perc = delta_boat_speed_perc


class LegAnalyzer:
    HIST_LEN = 120
    STRAIGHT_THR_CNT = 110
    STRAIGHT_THR_ANGLE = 20

    def __init__(self):
        self.hist = []
        self.summaries = []

    def update(self, instr_data, targets):
        summary = None
        # Check if we accumulated enough data in our history buffer
        if len(self.hist) == LegAnalyzer.HIST_LEN:
            summary = self.process_history()
            self.hist.clear()

        self.hist.append((instr_data, targets))
        return summary

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
        if avg_boat_twa < 0:
            avg_target_twa = - avg_target_twa

        elapsed_time_hr = (self.hist[-1][0].utc - self.hist[0][0].utc).total_seconds() / 3600.

        # Compare distances sailed to wind
        target_wind_dist_m = avg_target_vmg * elapsed_time_hr * METERS_IN_NM
        boat_wind_dist_m = avg_boat_vmg * elapsed_time_hr * METERS_IN_NM
        delta_dist_wind_m = boat_wind_dist_m - target_wind_dist_m

        # See if sailed higher or lower on average
        avg_delta_twa = avg_target_twa - avg_boat_twa

        # See if sailed faster or slower than target
        if avg_boat_sow > 0.1 and avg_target_sow > 0.1:
            delta_boat_speed_perc = (avg_boat_sow/avg_target_sow - 1) * 100
        else:
            delta_boat_speed_perc = None

        orig_wpt = GPXRoutePoint(name='', latitude=self.hist[0][0].lat, longitude=self.hist[0][0].lon,
                                 time=self.hist[0][0].utc)
        dest_wpt = GPXRoutePoint(name='', latitude=self.hist[-1][0].lat, longitude=self.hist[-1][0].lon,
                                 time=self.hist[-1][0].utc)

        summary = LegSummary(orig=orig_wpt, dest=dest_wpt, avg_boat_twa=avg_boat_twa, avg_hdg=avg_hdg,
                             delta_dist_wind_m=delta_dist_wind_m, avg_delta_twa=avg_delta_twa,
                             delta_boat_speed_perc=delta_boat_speed_perc)

        self.summaries.append(summary)

        return summary

    @staticmethod
    def mean(xx):
        acc = sum([x if x is not None else 0 for x in xx])
        cnt = len(xx) - xx.count(None)
        return None if cnt == 0 else acc / cnt, cnt
