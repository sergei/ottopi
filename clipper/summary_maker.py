import math
import os
from datetime import datetime

import matplotlib.pyplot as plt
import numpy as np
import pytz


class SummaryMaker:
    def __init__(self, work_dir, base_name, width, height, polars, ignore_cache):
        self.summary_dir = work_dir + os.sep + base_name + os.sep + 'summary'
        os.makedirs(self.summary_dir, exist_ok=True)
        self.width = width
        self.height = height
        self.polars = polars
        self.ignore_cache = ignore_cache
        self.tz = pytz.timezone("UTC")
        self.t0 = None
        self.t = None
        self.spd = None
        self.twa = None
        self.target_spd = None
        self.target_twa = None
        self.vmg = None
        self.target_vmg = None

    def make_chapter_png(self, evt, file_name, width, height):
        png_name = self.summary_dir + os.sep + file_name
        if os.path.isfile(png_name) and not self.ignore_cache:
            print(f'{png_name} exists, skipped.')
            return png_name

        dpi = 360
        plt.figure(figsize=(width / dpi, height / dpi), dpi=dpi)

        label_font = {'family': 'serif',
                      'color': 'darkred',
                      'weight': 'normal',
                      'size': 14,
                      }

        title_font = {'family': 'serif',
                      'color': 'darkblue',
                      'weight': 'normal',
                      'size': 22,
                      }

        plt.subplot(3, 1, 1)
        plt.title(evt['name'], fontdict=title_font)
        plt.axhline(y=self.target_twa, linestyle='--')
        plt.plot(self.t, np.abs(self.twa), color='darkred')
        plt.grid(visible=True, which='both')
        # make these tick labels invisible
        plt.tick_params('x', labelbottom=False)
        plt.ylabel('TWA', fontdict=label_font)
        max_angle = np.round(np.max(np.abs(self.twa) / 10) + 1) * 10
        plt.ylim(0, max_angle)

        plt.subplot(3, 1, 2)
        if not np.isnan(self.spd).any():
            plt.axhline(y=self.target_spd, linestyle='--')
            plt.plot(self.t, self.spd, color='darkred')
            plt.grid(visible=True, which='both')
            # make these tick labels invisible
            plt.tick_params('x', labelbottom=False)
            plt.ylim(0, np.round(np.max(np.abs(self.spd) + 1)))
            plt.ylabel('SPD', fontdict=label_font)

        plt.subplot(3, 1, 3)
        if not np.isnan(self.vmg).any():
            plt.axhline(y=self.target_vmg, linestyle='--')
            plt.plot(self.t, self.vmg, color='darkred')
            plt.grid(visible=True, which='both')
            plt.ylim(0, np.round(np.max(np.abs(self.vmg) + 1)))
            plt.ylabel('VMG', fontdict=label_font)

        plt.savefig(png_name, dpi=dpi)
        print(f'Created {png_name}')

        return png_name

    def make_thumbnail(self, file_name, epoch_idx, epoch, width, height):
        png_name = self.summary_dir + os.sep + file_name
        if os.path.isfile(png_name) and not self.ignore_cache:
            print(f'{png_name} exists, skipped.')
            return png_name

        dpi = 320
        plt.figure(figsize=(width / dpi, height / dpi), dpi=dpi)

        t = (datetime.fromisoformat(epoch['utc']) - self.t0).total_seconds()

        plt.subplot(1, 1, 1)
        if not np.isnan(self.vmg).any():
            plt.axvline(x=t)
            plt.axhline(y=self.target_vmg, linestyle=':', color='cyan')
            plt.plot(self.t, self.vmg, color='darkred')

            plt.axis('off')
            maxy = np.round(np.max(np.abs(self.vmg) + 1))
            plt.ylim(0, maxy)

            if t <= 0:
                horizontalalignment = 'left'
            else:
                horizontalalignment = 'right'

            plt.text(t, maxy / 2, f'{self.vmg[epoch_idx]:.1f}', color='white', horizontalalignment=horizontalalignment)
            plt.text(t, 0, f'{int(abs(t))}', color='black', horizontalalignment=horizontalalignment)

        plt.savefig(png_name, dpi=dpi, transparent=True, bbox_inches='tight', pad_inches=0)
        print(f'Created {png_name}')

        return png_name

    def prepare_data(self, evt):
        self.t0 = datetime.fromisoformat(evt['utc'])
        self.t = []
        self.spd = []
        self.twa = []
        tws = []
        for h in evt['history']:
            self.t.append((datetime.fromisoformat(h['utc']) - self.t0).total_seconds())
            self.spd.append(h['sow'])
            self.twa.append(h['twa'])
            tws.append(h['tws'])

        mean_tws = np.mean(tws)
        self.target_spd, self.target_twa = self.polars.get_targets(mean_tws, self.twa[0])
        self.target_vmg = abs(self.target_spd * math.radians(self.target_twa))
        self.spd = np.array(self.spd, dtype=float)
        self.twa = np.array(self.twa, dtype=float)
        self.vmg = np.abs(self.spd * np.cos(self.twa * np.pi / 180))




