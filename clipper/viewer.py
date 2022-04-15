import csv
import glob
import json
import os.path
from datetime import timedelta, datetime, date
from json import JSONEncoder
from tkinter import *
from tkinter import ttk
from tkinter import filedialog
from PIL import ImageTk, Image
import cv2

from gopro import GoPro, to_timestamp, from_timestamp

TIME_FORMAT = "%Y-%m-%dT%H:%M:%SZ"

PORT = 'port'
STBD = 'stbd'
GOPRO = 'gopro'


class DateTimeEncoder(JSONEncoder):
    # Override the default method
    def default(self, obj):
        if isinstance(obj, (date, datetime)):
            return to_timestamp(obj)


def decode_date_time(d):
    for k in d:
        if 'utc' in k:
            d[k] = from_timestamp(d[k])
    return d


class Params:
    def __init__(self, json_file):
        self.json_file = os.path.expanduser(json_file)
        self.params = {
            'port': {'dir': os.path.expanduser('/tmp'), 'img_idx': 0},
            'stbd': {'dir': os.path.expanduser('/tmp'), 'img_idx': 0},
            'gopro': {'dir': os.path.expanduser('/tmp'), 'utc': None},
        }
        self.read()

    def set(self, section, key, val):
        self.params[section][key] = val
        json.dump(self.params, open(self.json_file, 'wt'), indent=4, cls=DateTimeEncoder)

    def get(self, section, key):
        return self.params[section][key]

    def read(self):
        try:
            self.params = json.load(open(self.json_file, 'rt'), object_hook=decode_date_time)
        except (FileNotFoundError, json.decoder.JSONDecodeError):
            pass


class GoProView:
    def __init__(self, params, root, work_dir='/tmp'):
        self.params = params
        self.work_dir = work_dir
        self.current_clip_idx = 0
        self.current_utc = self.params.get(GOPRO, 'utc')
        self.vid_cap = None
        self.img = None
        self.gopro = None

        self.img_width = 192*3
        self.img_height = 108*3
        self.canvas = Canvas(root, width=self.img_width, height=self.img_height, background='gray75')
        self.canvas.grid(column=0, row=0, sticky='w')

        controls_frame = ttk.Frame(root, padding='3 3 12 12')
        controls_frame.grid(column=0, row=1, sticky='we')

        self.clip_label = ttk.Label(controls_frame, text='YYYY-MM-DD HH:MM:SS')
        self.clip_label.grid(column=0, row=0, sticky=W)

        ttk.Button(controls_frame, text='<<', command=lambda: self.move_frame(-10000.)).grid(column=0, row=1, sticky=W)
        ttk.Button(controls_frame, text='<', command=lambda: self.move_frame(-1000.)).grid(column=1, row=1, sticky=W)

        self.scale_val = DoubleVar()
        self.scale = ttk.Scale(controls_frame, from_=0, to=100, variable=self.scale_val, command=self.scroll_frame,
                               orient='horizontal')
        self.scale.grid(column=2, row=1, sticky='nesw')

        ttk.Button(controls_frame, text='>', command=lambda: self.move_frame(1000)).grid(column=3, row=1, sticky=E)
        ttk.Button(controls_frame, text='>>', command=lambda: self.move_frame(10000)).grid(column=4, row=1, sticky=E)

        ttk.Button(controls_frame, text="Open GoPro Folder",
                   command=self.read_clips).grid(column=0, row=2, sticky=W)

        clips_dir = self.params.get(GOPRO, 'dir')

        if clips_dir is not None and os.path.isdir(clips_dir):
            self.open_clips_dir(clips_dir)

    def read_clips(self):
        initial_dir = self.params.get(GOPRO, 'dir')

        dir_name = filedialog.askdirectory(initialdir=initial_dir)
        if len(dir_name) == 0:
            return

        self.params.set(GOPRO, 'dir', dir_name)

    def open_clips_dir(self, dir_name):
        self.gopro = GoPro(dir_name, self.work_dir)
        if len(self.gopro.clips) > 0:
            self.current_clip_idx = 0
            self.current_utc = self.gopro.clips[0]['start_utc']
            duration_sec = (self.gopro.clips[-1]['stop_utc'] - self.current_utc).total_seconds()
            self.vid_cap = cv2.VideoCapture(self.gopro.clips[0]['name'])
            self.scale.configure(to=duration_sec)
            self.show_frame()

    def show_frame(self):
        if not (self.gopro.clips[self.current_clip_idx]['start_utc'] <= self.current_utc
                <= self.gopro.clips[self.current_clip_idx]['stop_utc']):
            # Try to find a clip containing current UTC time
            current_clip_idx = -1
            for idx in range(len(self.gopro.clips)):
                clip = self.gopro.clips[idx]
                if clip['start_utc'] <= self.current_utc <= clip['stop_utc']:
                    current_clip_idx = idx
                    break

            if current_clip_idx == -1:
                print(f'{self.current_utc.strftime(TIME_FORMAT)} is not in GOPRO clips')
                return

            self.current_clip_idx = current_clip_idx

        current_clip = self.gopro.clips[self.current_clip_idx]
        self.vid_cap = cv2.VideoCapture(current_clip['name'])

        local_time_ms = (self.current_utc - self.gopro.clips[self.current_clip_idx]['start_utc']).total_seconds() * 1000
        self.vid_cap.set(cv2.CAP_PROP_POS_MSEC, local_time_ms)
        ret, frame = self.vid_cap.read()
        if ret:
            file_name = self.gopro.clips[self.current_clip_idx]['name']
            short_name = os.sep.join(file_name.split(os.sep)[-2:])
            self.clip_label.configure(text=f'{short_name} {self.current_utc.strftime(TIME_FORMAT)}')
            cv2image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGBA)
            img_resized = Image.fromarray(cv2image).resize((self.img_width, self.img_height))
            self.img = ImageTk.PhotoImage(img_resized)
            self.canvas.create_image(0, 0, image=self.img, anchor='nw')
            self.params.set(GOPRO, 'utc', self.current_utc)

    def set_frame(self, current_utc):
        self.current_utc = current_utc
        self.show_frame()

    def move_frame(self, delta_ms):
        self.current_utc += timedelta(milliseconds=delta_ms)
        self.show_frame()

    # noinspection PyUnusedLocal
    def scroll_frame(self, *args):
        self.current_utc = self.gopro.clips[0]['start_utc'] + timedelta(seconds=self.scale_val.get())
        self.show_frame()


class TimeLapseView:
    def __init__(self, side, params, root, on_frame_change):
        self.side = side
        self.params = params
        self.on_frame_change = on_frame_change
        self.img_list = []
        self.current_idx = self.params.get(self.side, 'img_idx')
        self.current_utc = None
        self.img = None

        self.img_width = 216
        self.img_height = 384
        self.canvas = Canvas(root, width=self.img_width, height=self.img_height, background='gray75')
        self.canvas.grid(column=0, row=0, sticky='w')

        controls_frame = ttk.Frame(root, padding='3 3 12 12')
        controls_frame.grid(column=0, row=1, sticky='we')

        self.clip_label = ttk.Label(controls_frame, text='YYYY-MM-DD HH:MM:SS')
        self.clip_label.grid(column=0, row=0, sticky=W)

        ttk.Button(controls_frame, text='<', command=lambda: self.move_frame(-1)).grid(column=0, row=1, sticky=W)
        self.scale_val = DoubleVar()
        self.scale = ttk.Scale(controls_frame, from_=0, to=100, variable=self.scale_val, command=self.scroll_frame,
                               orient='horizontal')
        self.scale.grid(column=1, row=1, sticky='nesw')
        ttk.Button(controls_frame, text='>', command=lambda: self.move_frame(1)).grid(column=2, row=1, sticky=E)

        ttk.Button(controls_frame, text="Open Cam Folder",
                   command=self.read_files).grid(column=0, row=2, sticky=W)

        images_dir = self.params.get(self.side, 'dir')
        if images_dir is not None and os.path.isdir(images_dir):
            self.open_images_dir(images_dir)

    def read_files(self):
        print(f'{self.side}')
        initial_dir = self.params.get(self.side, 'dir')

        dir_name = filedialog.askdirectory(initialdir=initial_dir)
        if len(dir_name) == 0:
            return

        if dir_name != initial_dir:
            self.current_idx = 0

        self.open_images_dir(dir_name)
        self.params.set(self.side, 'dir', dir_name)

    def open_images_dir(self, dir_name):
        self.img_list = sorted(glob.glob(dir_name + os.sep + '/**/T*.JPG', recursive=True), reverse=False)
        if len(self.img_list) > 0:
            self.scale.configure(to=len(self.img_list))
            if self.current_idx >= len(self.img_list):
                self.current_idx = 0
            self.show_image()

    def move_frame(self, count):
        self.current_idx += count
        if self.current_idx < 0:
            self.current_idx = 0
        if self.current_idx > len(self.img_list) - 1:
            self.current_idx = len(self.img_list) - 1

        self.scale_val.set(self.current_idx)
        self.show_image()
        self.on_frame_change(self.current_idx, self.current_utc)

    # noinspection PyUnusedLocal
    def scroll_frame(self, *args):
        self.current_idx = int(self.scale_val.get())

        self.show_image()
        self.on_frame_change(self.current_idx, self.current_utc)

    def set_frame_idx(self, frame_idx):
        self.current_idx = frame_idx
        self.scale_val.set(self.current_idx)
        self.show_image()

    def show_image(self):
        if len(self.img_list) > 0:
            file_name = self.img_list[self.current_idx]
            short_name = os.sep.join(file_name.split(os.sep)[-2:])
            self.current_utc = datetime.utcfromtimestamp(os.path.getmtime(file_name))
            self.clip_label.configure(text=f'{short_name} {self.current_utc.strftime(TIME_FORMAT)}')
            image = Image.open(file_name)
            image.thumbnail((self.img_height, self.img_width), Image.ANTIALIAS)
            rotated_img = image.transpose(method=Image.ROTATE_270)
            self.img = ImageTk.PhotoImage(rotated_img)
            self.canvas.create_image(0, 0, image=self.img, anchor='nw')
            self.params.set(self.side, 'img_idx', self.current_idx)

    def create_csv(self, csv_out_dir, true_utc):
        csv_file_name = os.path.join(csv_out_dir, self.side + '.csv')
        name_field = self.side + '_image_name'
        img_name = self.img_list[self.current_idx]
        uncorrected_utc = from_timestamp(os.path.getmtime(img_name))
        utc_correction = true_utc - uncorrected_utc
        with open(csv_file_name, 'wt') as csv_file:
            writer = csv.DictWriter(csv_file, ['utc', name_field])
            writer.writeheader()
            for img_name in self.img_list:
                utc_nc = datetime.utcfromtimestamp(os.path.getmtime(img_name))
                img_utc = utc_nc + utc_correction
                writer.writerow({'utc': img_utc, name_field: img_name})

            print(f'Created {csv_file_name}')


class SailView:

    def __init__(self, root, work_dir='/tmp'):
        self.work_dir = work_dir
        self.params = Params('~/.sail_view.json')
        root.title('Sail View')

        mainfarme = ttk.Frame(root, padding='3 3 12 12')
        mainfarme.grid(column=0, row=0, sticky='nwes')

        root.columnconfigure(0, weight=1)
        root.rowconfigure(0, weight=1)

        top_frame = ttk.Frame(mainfarme, padding='3 3 12 12')
        top_frame.grid(column=0, row=0, sticky='nwes')

        port_frame = ttk.Frame(top_frame, padding='3 3 12 12')
        port_frame.grid(column=0, row=0, sticky='nwes')
        self.port_view = TimeLapseView(PORT, self.params, port_frame, lambda x, y: self.on_port_frame_change(x, y))

        self.sync_top_frames = BooleanVar(value=True)
        ttk.Checkbutton(top_frame, text='Sync', variable=self.sync_top_frames,
                        onvalue=True, offvalue=False).grid(column=1, row=0, sticky='we')

        stbd_frame = ttk.Frame(top_frame, padding='3 3 12 12')
        stbd_frame.grid(column=2, row=0, sticky='nwes')
        self.stbd_view = TimeLapseView(STBD, self.params, stbd_frame, lambda x, y: self.on_stbd_frame_change(x, y))

        bottom_frame = ttk.Frame(mainfarme, padding='3 3 12 12')
        bottom_frame.grid(column=0, row=1, sticky='nwes')

        ttk.Button(mainfarme, text='Save CSV ...', command=self.store_csv).grid(column=0, row=2, sticky=W)

        self.gopro_view = GoProView(self.params, bottom_frame, self.work_dir)

        self.port_frame_idx = self.port_view.current_idx
        self.stbd_frame_idx = self.stbd_view.current_idx

    def store_csv(self):

        csv_out_dir = filedialog.askdirectory(title='Directory to store CSV files')
        print(f'Saving to {csv_out_dir}')
        self.port_view.create_csv(csv_out_dir, self.gopro_view.current_utc)
        self.stbd_view.create_csv(csv_out_dir, self.gopro_view.current_utc)

    def on_port_frame_change(self, frame_idx, current_utc):
        if self.sync_top_frames.get():
            delta = frame_idx - self.port_frame_idx
            self.stbd_frame_idx += delta
            self.stbd_view.set_frame_idx(self.stbd_frame_idx)
        self.port_frame_idx = frame_idx

    def on_stbd_frame_change(self, frame_idx, current_utc):
        if self.sync_top_frames.get():
            delta = frame_idx - self.stbd_frame_idx
            self.port_frame_idx += delta
            self.port_view.set_frame_idx(self.port_frame_idx)
        self.stbd_frame_idx = frame_idx


def viewer():
    rt = Tk()
    SailView(rt)
    rt.mainloop()


if __name__ == '__main__':
    viewer()
