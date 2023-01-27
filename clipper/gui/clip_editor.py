from datetime import datetime, timedelta
from tkinter import ttk, W, StringVar

from gui.clip_event import ClipEvent
from gui.slider import Slider
import pytz
UTC_TZ = pytz.UTC


class ClipEditor:

    def __init__(self, top, on_in_out_change, on_save_clip, on_remove_clip, on_create_clip):
        self.on_in_out_change = on_in_out_change
        self.on_save_clip = on_save_clip
        self.on_remove_clip = on_remove_clip
        self.on_create_clip = on_create_clip
        self.is_hidden = True
        self.event = None
        self.utc_to = None
        self.utc_from = None
        self.slider = None
        self.to_timestamp = None
        self.from_timestamp = None
        self.sv_utc_from_time = StringVar()
        self.sv_utc_to_time = StringVar()
        self.sv_clip_name = StringVar()

        self.edit_frame = ttk.Frame(top)
        self.create_frame = ttk.Frame(top)

        self.slider_frame = ttk.Frame(self.edit_frame, padding="3 3 12 12")
        self.slider_frame.grid(column=0, row=0, sticky='nwes')

        info_frame = ttk.Frame(self.edit_frame, padding="3 3 12 12")
        info_frame.grid(column=0, row=1, sticky='nwes')
        ttk.Label(info_frame, textvariable=self.sv_utc_from_time).grid(column=0, row=0, sticky=W)
        ttk.Label(info_frame, textvariable=self.sv_utc_to_time).grid(column=1, row=0, sticky=W)
        ttk.Entry(info_frame, textvariable=self.sv_clip_name).grid(column=2, row=0, sticky=W)
        ttk.Button(info_frame, text="Save", command=self.save_clip).grid(column=3, row=0, sticky=W)
        ttk.Button(info_frame, text="Delete clip", command=self.delete_clip).grid(column=4, row=0, sticky=W)

        ttk.Button(self.create_frame, text="Create clip", command=self.create_clip).grid(column=0, row=0, sticky=W)

    def show(self, event: ClipEvent):
        self.event = event
        self.sv_clip_name.set(event.name)
        min_val = (event.utc_from - timedelta(seconds=60)).timestamp()
        max_val = (event.utc_to + timedelta(seconds=60)).timestamp()
        self.utc_from = event.utc_from
        self.utc_to = event.utc_to
        self.from_timestamp = event.utc_from.timestamp()
        self.to_timestamp = event.utc_to.timestamp()
        self.sv_utc_from_time.set(event.utc_from.strftime("%H:%M:%S"))
        self.sv_utc_to_time.set(event.utc_to.strftime("%H:%M:%S"))
        self.slider = Slider(
            self.slider_frame,
            width=600,
            height=30,
            min_val=min_val,
            max_val=max_val,
            init_lis=[self.from_timestamp, self.to_timestamp],
            show_value=False,
            removable=False,
            addable=False,
        )
        self.slider.grid(column=0, row=0, sticky=W)
        self.slider.setValueChageCallback(lambda vals: self.on_time_change(vals))
        self.on_in_out_change(event.utc_from, event.utc_to)
        self.edit_frame.grid(column=0, row=0, sticky='nwes')
        self.create_frame.grid_forget()

        self.is_hidden = False

    def hide(self):
        if not self.is_hidden:
            self.is_hidden = True
            self.edit_frame.grid_forget()
            self.create_frame.grid(column=0, row=0, sticky='nwes')

    def save_clip(self):
        self.event.name = self.sv_clip_name.get()
        self.event.utc_from = self.utc_from
        self.event.utc_to = self.utc_to
        self.on_save_clip(self.event)

    def delete_clip(self):
        self.on_remove_clip(self.event)

    def create_clip(self):
        self.on_create_clip()

    def on_time_change(self, vals):
        ts_from = vals[0]
        ts_to = vals[1]
        self.utc_from = UTC_TZ.localize(datetime.utcfromtimestamp(ts_from))
        self.utc_to = UTC_TZ.localize(datetime.utcfromtimestamp(ts_to))

        self.sv_utc_from_time.set(self.utc_from.strftime("%H:%M:%S"))
        self.sv_utc_to_time.set(self.utc_to.strftime("%H:%M:%S"))

        self.on_in_out_change(self.utc_from, self.utc_to)
