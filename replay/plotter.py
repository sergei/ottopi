import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from dateutil import tz


class Plotter:
    def __init__(self, use_local_tz=True):
        if use_local_tz:
            self.tz = tz.tzlocal()
        else:
            self.tz = tz.tzutc()

        self.utc = []
        self.twa = []
        self.tws = []
        self.awa = []
        self.aws = []
        self.hdg = []
        self.boat_vmg = []
        self.target_vmg = []
        self.target_sow = []
        self.bs = []
        self.target_twa = []
        self.wind_shift_utc = []
        self.wind_shift_text = []

    def show(self):
        print('Preparing the plots')
        fmt = mdates.DateFormatter('%H:%M')
        fmt.set_tzinfo(self.tz)

        ax1 = plt.subplot(4, 1, 1)
        twa = np.array(self.twa, dtype=np.float)
        awa = np.array(self.awa, dtype=np.float)
        ax1.plot(self.utc, twa, '.')
        ax1.plot(self.utc, awa, '.')
        ax1.xaxis.set_major_formatter(fmt)
        ax1.legend(['TWA', 'AWA'])
        plt.ylabel('Degrees')

        ax2 = plt.subplot(4, 1, 2, sharex=ax1)
        hdg = self.unwrap_deg(np.array(self.hdg, dtype=np.float))
        twd = self.unwrap_deg(hdg + twa)
        ax2.plot(self.utc, hdg, '.')
        ax2.plot(self.utc, twd, '.')
        for i in range(len(self.wind_shift_utc)):
            ax2.text(self.wind_shift_utc[i], -50, self.wind_shift_text[i], rotation=45)

        ax2.xaxis.set_major_formatter(fmt)
        ax2.legend(['HDG', 'TWD'])
        plt.ylabel('Degrees')

        ax3 = plt.subplot(4, 1, 3, sharex=ax1)
        tws = np.array(self.tws, dtype=np.float)
        aws = np.array(self.aws, dtype=np.float)
        ax3.plot(self.utc, tws, '.')
        ax3.plot(self.utc, aws, '.')
        ax3.xaxis.set_major_formatter(fmt)
        ax3.legend(['TWS', 'AWS'])
        plt.ylabel('KTS')

        ax4 = plt.subplot(4, 1, 4, sharex=ax1)
        ax4.plot(self.utc, np.array(self.bs, dtype=np.float), '.')
        ax4.plot(self.utc, np.array(self.target_sow, dtype=np.float), '.')
        ax4.plot(self.utc, np.array(self.boat_vmg, dtype=np.float), '.')
        ax4.plot(self.utc, np.array(self.target_vmg, dtype=np.float), '.')
        ax4.xaxis.set_major_formatter(fmt)
        ax4.legend(['Boat speed', 'Target speed', 'Boat VMG', 'Target VMG'])
        plt.ylabel('KTS')

        print('Plots are shown')
        plt.show()

    @staticmethod
    def unwrap_deg(deg):
        rad = np.deg2rad(np.mod(deg, 360.))
        good_idx = ~np.isnan(rad)
        rad[good_idx] = np.unwrap(rad[good_idx])
        deg = np.rad2deg(rad)
        return deg

    def on_instr_data(self, instr_data):
        self.utc.append(instr_data.utc)
        self.twa.append(instr_data.twa)
        self.awa.append(instr_data.awa)
        self.tws.append(instr_data.tws)
        self.aws.append(instr_data.aws)
        self.hdg.append(instr_data.hdg)

    def on_targets(self, targets):
        self.boat_vmg.append(targets.boat_vmg)
        self.target_vmg.append(targets.target_vmg)
        self.target_sow.append(targets.target_sow)
        self.bs.append(targets.bs)
        self.target_twa.append(targets.target_twa)

    def on_wind_shift(self, wind_shift):
        self.wind_shift_utc.append(wind_shift.utc)
        # angle_direction = 'Lift' if wind_shift.is_lift else 'Header'
        wind_direction = 'Veer' if wind_shift.shift_deg > 0 else 'Back'
        phrase = '{} {:.0f}'.format(wind_direction, abs(wind_shift.shift_deg))
        self.wind_shift_text.append(phrase)
