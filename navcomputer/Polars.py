import os


class Polars:
    def __init__(self):
        self.polars = {}
        self.valid = False

    def is_valid(self):
        return self.valid

    def read_table(self, file_name):
        file_name = os.path.expanduser(file_name)
        if os.path.isfile(file_name):
            try:
                with open(file_name, 'r') as f:
                    polars = {}
                    for line in f:
                        t = line.split()
                        tws = int(t[0])
                        wind_amp = dict()
                        polars[tws] = wind_amp
                        for i in range(1, len(t), 2):
                            twa = float(t[i])
                            bs = float(t[i+1])
                            wind_amp[twa] = bs
                    self.polars = polars
                    self.valid = True
            except Exception as e:
                print(e)

    def get_targets(self, tws, twa):
        """
        Computes boat target speed and optimal angle to sail
        :param tws: Current true wind speed
        :param twa: Current true wind angle (used to determine if we are sailing upwind or downwind)
        :return:  target_speed, target_twa
        """
        # Find the speeds above and below current wind speed
        speeds = list(self.polars.keys())
        speed_below = None
        speed_above = None
        for i, table_tws in enumerate(speeds):
            if tws <= table_tws:
                if i == 0:  # Will extrapolate below lowest wind in the table
                    speed_below = speeds[i]
                    speed_above = speeds[i+1]
                else:
                    speed_below = speeds[i-1]
                    speed_above = speeds[i]
                break

        if speed_below is None:  # Will extrapolate above highest wind speed in the table
            speed_below = speeds[-2]
            speed_above = speeds[-1]

        twa = abs(twa)
        targets_below = self.polars[speed_below]
        targets_above = self.polars[speed_above]
        if twa < 90:  # Upwind
            target_twa_below = list(targets_below.keys())[1]
            target_twa_above = list(targets_above.keys())[1]
        else:  # Downwind
            target_twa_below = list(targets_below.keys())[-2]
            target_twa_above = list(targets_above.keys())[-2]

        target_speed_below = targets_below[target_twa_below]
        target_speed_above = targets_above[target_twa_above]

        target_speed = self.interp(tws, speed_above, speed_below, target_speed_above, target_speed_below)
        target_twa = self.interp(tws, speed_above, speed_below, target_twa_above, target_twa_below)

        return target_speed, target_twa

    @staticmethod
    def interp(x, xa, xb, ya, yb):
        m = (ya - yb) / (xa - xb)
        yc = (x - xb) * m + yb
        return yc
