import os
from PIL import ImageFont, ImageDraw
from PIL.Image import new
from PIL import Image

from colors import SEMI_TRANSPARENT_BOX_COLOR_2, FULLY_TRANSPARENT_COLOR, SEMI_TRANSPARENT_FONT_COLOR, \
    NON_TRANSPARENT_WHITE_FONT_COLOR

VALUE_FONT = "/System/Library/Fonts/Courier.dfont"
LABEL_FONT = "/System/Library/Fonts/Courier.dfont"
TIMESTAMP_FONT = "/System/Library/Fonts/Courier.dfont"


class InfoCell:
    def __init__(self):
        self.main_font = None
        self.label_font = None
        self.height = None
        self.width = None

    def set_size(self, width):
        font_size = 10
        for fs in range(10, 100):
            font_size = fs
            font = ImageFont.truetype(VALUE_FONT, font_size)
            (w, h) = font.getsize('----')
            if w > width:
                font_size = fs - 1
                break

        self.main_font = ImageFont.truetype(VALUE_FONT, font_size)
        self.height = self.main_font.getmetrics()[0]
        self.label_font = ImageFont.truetype(LABEL_FONT, int(font_size * 2. / 4.))
        self.height += self.label_font.getmetrics()[0] + self.label_font.getmetrics()[1]
        self.width = width

    def draw(self, draw, x, y, name, value):
        (label_w, label_h) = self.label_font.getsize(name)
        label_x_offset = int((self.width - label_w) / 2)
        label_y_offset = 0
        draw.text((x + label_x_offset, y + label_y_offset), name, font=self.label_font,
                  fill=SEMI_TRANSPARENT_FONT_COLOR)

        (value_w, value_h) = self.main_font.getsize(value)
        value_x_offset = int((self.width - value_w) / 2)
        value_y_offset = label_h + label_y_offset
        draw.text((x + value_x_offset, y + value_y_offset), value, font=self.main_font,
                  fill=NON_TRANSPARENT_WHITE_FONT_COLOR)

    def draw_vc_label(self, draw, x, y, name):
        (label_w, label_h) = self.label_font.getsize(name)
        label_x_offset = x
        label_y_offset = (self.height - label_h) / 2
        draw.text((x + label_x_offset, y + label_y_offset), name, font=self.label_font,
                  fill=SEMI_TRANSPARENT_FONT_COLOR)
        return label_w + label_x_offset


class OverlayMaker:
    def __init__(self, work_dir, base_name, width, height, ignore_cache):
        self.ignore_cache = ignore_cache
        self.overlay_dir = work_dir + os.sep + base_name + os.sep + 'overlay'
        os.makedirs(self.overlay_dir, exist_ok=True)
        self.width = width
        self.height = height

        num_cells = 5
        self.rect_width = width
        cell_width = self.rect_width / num_cells
        self.cell_step = int(self.rect_width / num_cells)
        cell_width = int(cell_width * 3 / 4)  # Make it a bit smaller

        self.info_cell = InfoCell()
        self.info_cell.set_size(cell_width)

        self.time_stamp_font = None
        self.time_stamp_height = None
        self.choose_time_stamp_font(self.info_cell.height / 3)

        self.rect_height = self.info_cell.height
        self.rect_x_offset = 0
        self.rect_y_offset = height - self.rect_height

    def choose_time_stamp_font(self, time_stamp_height):
        font_size = 10
        for fs in range(10, 100):
            font_size = fs
            font = ImageFont.truetype(TIMESTAMP_FONT, font_size)
            (w, h) = font.getsize('----')
            if h > time_stamp_height:
                font_size = fs - 1
                break

        self.time_stamp_font = ImageFont.truetype(TIMESTAMP_FONT, font_size)
        self.time_stamp_height = self.time_stamp_font.getmetrics()[0]

    def add_epoch(self, file_name, epoch, thumb_png_name):
        full_file_name = self.overlay_dir + os.sep + file_name

        if os.path.isfile(full_file_name) and not self.ignore_cache:
            print(f'{full_file_name} exists, skipped.')
            return full_file_name

        image = new('RGBA', (self.width, self.height), FULLY_TRANSPARENT_COLOR)
        draw = ImageDraw.Draw(image)

        draw.rectangle([(0, 0), (self.rect_width, self.rect_height)], fill=SEMI_TRANSPARENT_BOX_COLOR_2)

        x = self.cell_step
        spd_txt = f"{epoch['sow']:.1f}" if epoch['sow'] is not None else "-.-"
        self.info_cell.draw(draw, x, self.rect_y_offset, "SPD", spd_txt)

        x += self.cell_step
        sog_txt = f"{epoch['sog']:.1f}" if epoch['sog'] is not None else "-.-"
        self.info_cell.draw(draw, x, self.rect_y_offset, "SOG", sog_txt)

        x += self.cell_step
        tws_txt = f"{epoch['tws']:.1f}" if epoch['tws'] is not None else "-.-"
        self.info_cell.draw(draw, x, self.rect_y_offset, "TWS", tws_txt)

        x += self.cell_step
        twa_txt = f"{epoch['twa']:.1f}" if epoch['twa'] is not None else "-.-"
        self.info_cell.draw(draw, x, self.rect_y_offset, "TWA", twa_txt)

        x = 8
        x = self.info_cell.draw_vc_label(draw, x, self.rect_y_offset, "VMG")
        x += 16

        # Draw the performance chart
        if thumb_png_name is not None:
            thumb_img = Image.open(thumb_png_name)
            image.paste(thumb_img, (x, 16), thumb_img)

        image.save(full_file_name)
        print(f'{full_file_name} created')
        return full_file_name
