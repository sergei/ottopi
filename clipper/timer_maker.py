from datetime import datetime
import os

from PIL import ImageFont, ImageDraw
from PIL.Image import new

from overlay_maker import VALUE_FONT
from colors import FULLY_TRANSPARENT_COLOR, TIMER_FONT_COLOR


class TimerMaker:
    def __init__(self, work_dir, base_name, height, ignore_cache):
        self.ignore_cache = ignore_cache
        self.image_dir = work_dir + os.sep + base_name + os.sep + 'timer'
        os.makedirs(self.image_dir, exist_ok=True)
        self.height = height
        self.width = height
        font_size = 10
        for fs in range(10, height*2):
            font_size = fs
            font = ImageFont.truetype(VALUE_FONT, font_size)
            (w, h) = font.getsize('00:00')
            if h > height:
                font_size = fs - 1
                self.width = w
                break

        self.main_font = ImageFont.truetype(VALUE_FONT, font_size)

    def add_epoch(self, file_name, gun_utc, epoch):
        full_file_name = self.image_dir + os.sep + file_name

        if os.path.isfile(full_file_name) and not self.ignore_cache:
            print(f'{full_file_name} exists, skipped.')
            return full_file_name

        time_to_start = datetime.fromisoformat(gun_utc) - datetime.fromisoformat(epoch['utc'])
        total_seconds = abs(time_to_start.total_seconds())
        minutes, seconds = divmod(total_seconds, 60)
        s = f'{int(minutes):02d}:{int(seconds):02d}'

        image = new('RGBA', (self.width, self.height), FULLY_TRANSPARENT_COLOR)
        draw = ImageDraw.Draw(image)
        draw.text((0, 0), s, font=self.main_font,
                  fill=TIMER_FONT_COLOR)

        image.save(full_file_name)
        print(f'{full_file_name} created')
        return full_file_name


if __name__ == '__main__':
    timer_maker = TimerMaker('/tmp', 'test', 200, True)
    e = {'utc': '2022-01-08T20:15:00+00:00', 'lat': 37.858873833333334, 'lon': -122.37676716666667}
    g = '2022-01-08T20:17:00+00:00'
    filename = 'timer_test.png'
    timer_maker.add_epoch(filename, g, e)
