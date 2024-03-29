import math
import os

import numpy as np
from PIL import ImageDraw
from PIL.Image import new

from colors import rgba, POLAR_DOT_OUTLINE, POLAR_GRID_COLOR, POLAR_TARGET_COLOR, FULLY_TRANSPARENT_COLOR

CURRENT_DOT_WIDTH = 4
ARM_WIDTH = 3
TARGET_CROSS_WIDTH = 5
HISTORY_TRANSPARENCY_FACTOR = 1  # [0 : 1]


class PolarMaker:
    def __init__(self, work_dir, base_name, width, polars, ignore_cache):
        self.ignore_cache = ignore_cache
        self.image_dir = work_dir + os.sep + base_name + os.sep + 'polar'
        os.makedirs(self.image_dir, exist_ok=True)
        self.width = width
        self.height = None
        self.polars = polars
        self.history = []
        self.is_tack = False
        self.is_gybe = False
        self.target_x = None
        self.target = None
        self.max_speed = None
        self.min_speed = None
        self.speed_step = 2
        self.y_scale = None
        self.x_scale = None
        self.dot_radius = 15
        self.x_pad = self.dot_radius
        self.y_pad = self.dot_radius
        self.y0 = 0

    def to_screen(self, xy):
        x, y = xy
        screen_x = x * self.x_scale + self.width / 2
        screen_y = - y * self.y_scale + self.height - self.y_pad - self.y0
        return screen_x, screen_y

    def update_scale(self):
        self.x_scale = (self.width - self.x_pad * 2) / (2 * (self.max_speed - self.min_speed))
        self.y_scale = self.x_scale
        self.height = int(self.y_scale * (self.max_speed - self.min_speed)) + self.y_pad * 2

    @staticmethod
    def pol_to_cart(rho, theta):
        return rho * math.sin(math.radians(theta)), rho * math.cos(math.radians(theta))  # x, y

    def is_available(self):
        return self.is_tack or self.is_gybe

    def set_epoch(self, file_name, epoch_idx):
        full_file_name = self.image_dir + os.sep + file_name

        if os.path.isfile(full_file_name) and not self.ignore_cache:
            print(f'{full_file_name} exists, skipped.')
            return full_file_name

        # Draw grid
        image = new('RGBA', (self.width, self.height), FULLY_TRANSPARENT_COLOR)
        draw = ImageDraw.Draw(image)

        # Draw grid
        # Speed circles
        if self.is_tack:
            start_angle = -90
            end_angle = 100
            arc_start = 180
            arc_end = 0
        else:
            start_angle = 90
            end_angle = 280
            arc_start = 0
            arc_end = 180

        for speed in range(self.max_speed, self.min_speed, -self.speed_step):
            ulx, uly = self.to_screen((-speed, speed))
            lrx, lry = self.to_screen((speed, -speed))
            draw.arc((ulx, uly, lrx, lry), start=arc_start, end=arc_end, fill=POLAR_GRID_COLOR)

        # Angle lines

        for angle in range(start_angle, end_angle, 30):
            x1, y1 = self.to_screen(self.pol_to_cart(self.min_speed, 0))
            x2, y2 = self.to_screen(self.pol_to_cart(self.max_speed, angle))
            draw.line((x1, y1, x2, y2), fill=POLAR_GRID_COLOR)

        # Draw the history
        for idx in range(epoch_idx):
            xy = self.history[idx]
            x1, y1 = self.to_screen(self.pol_to_cart(self.min_speed, 0))
            x2, y2 = self.to_screen(xy)
            ulx = x2 - self.dot_radius
            uly = y2 - self.dot_radius
            lrx = x2 + self.dot_radius
            lry = y2 + self.dot_radius
            a = int(255 * (1 - HISTORY_TRANSPARENCY_FACTOR * (epoch_idx - idx) / epoch_idx))
            color = rgba(255, 0, 0, a)
            if idx == epoch_idx - 1:
                draw.line((x1, y1, x2, y2), fill=POLAR_DOT_OUTLINE, width=ARM_WIDTH)
                draw.ellipse((ulx, uly, lrx, lry), fill=color, outline=POLAR_DOT_OUTLINE, width=CURRENT_DOT_WIDTH)
            else:
                draw.ellipse((ulx, uly, lrx, lry), fill=color, outline=POLAR_DOT_OUTLINE)

        # Draw the target point
        x, y = self.to_screen(self.pol_to_cart(self.target[0], self.target[1]))
        draw.line((x, y-self.dot_radius, x, y+self.dot_radius),
                  fill=POLAR_TARGET_COLOR, width=TARGET_CROSS_WIDTH)
        draw.line((x-self.dot_radius, y, x+self.dot_radius, y),
                  fill=POLAR_TARGET_COLOR, width=TARGET_CROSS_WIDTH)
        x, y = self.to_screen(self.pol_to_cart(self.target[0], -self.target[1]))
        draw.line((x, y-self.dot_radius, x, y+self.dot_radius),
                  fill=POLAR_TARGET_COLOR, width=TARGET_CROSS_WIDTH)
        draw.line((x-self.dot_radius, y, x+self.dot_radius, y),
                  fill=POLAR_TARGET_COLOR, width=TARGET_CROSS_WIDTH)

        image.save(full_file_name)
        print(f'{full_file_name} created')
        return full_file_name

    def set_history(self, event_name, epochs):
        self.history = []
        self.is_tack = event_name.lower().startswith('tack')
        self.is_gybe = event_name.lower().startswith('gybe')

        if not (self.is_tack or self.is_gybe):
            return

        tws_sum = 0
        self.max_speed = 0
        self.min_speed = 99
        for epoch in epochs:
            twa = epoch['twa']
            sow = epoch['sow']
            if sow is not None and epoch['tws'] is not None:
                self.max_speed = int(max(sow, self.max_speed) + 0.5)
                self.min_speed = int(min(sow, self.min_speed))
                tws_sum += epoch['tws']
                self.history.append(self.pol_to_cart(sow, twa))
            else:
                self.history.append((np.nan, np.nan))

        mean_tws = tws_sum / len(epochs)
        if self.polars is not None:
            target_spd, target_twa = self.polars.get_targets(mean_tws, epochs[0]['twa'])
            self.max_speed = int(max(target_spd, self.max_speed) + 0.5)
            self.min_speed = int(min(target_spd, self.min_speed))
            self.target = (target_spd, target_twa)

        self.min_speed = 0
        self.max_speed += self.speed_step

        self.update_scale()

        if self.is_tack:
            self.y0 = 0
        else:
            self.y0 = self.height - 2 * self.y_pad


def fake_epochs(start_angle):
    epochs = []
    for i in range(0, 60):
        epochs.append({
            'twa': start_angle + i,
            'sow': 5 + i * 0.1,
            'tws': 10})
    return epochs


if __name__ == '__main__':
    polar_maker = PolarMaker('/tmp', 'polar_test', 400, None, True)

    polar_maker.set_history('Tack', fake_epochs(-30))
    polar_maker.target = (7, 35)
    polar_maker.set_epoch('polar-tack.png', 60)

    polar_maker.set_history('Gybe', fake_epochs(160))
    polar_maker.target = (7, 135)
    polar_maker.set_epoch('polar-gybe.png', 60)
