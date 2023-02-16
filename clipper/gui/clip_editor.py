from datetime import datetime, timedelta
from tkinter import ttk, W, StringVar

from gui.clip_event import ClipEvent
from gui.slider import Slider
import pytz

EVT_TYPE_NAME_NORMAL = "Maneuver"
EVT_TYPE_NAME_RACE_START = "Race Start"
UTC_TZ = pytz.UTC


class ClipEditor:

    def __init__(self, top, on_in_out_change, on_save_clip, on_remove_clip, on_create_clip):
        self.on_in_out_change = on_in_out_change
        self.on_save_clip = on_save_clip
        self.on_remove_clip = on_remove_clip
        self.on_create_clip = on_create_clip
        self.is_hidden = True
        self.utc_gun = None
        self.event = None
        self.utc_to = None
        self.utc_from = None
        self.slider = None
        self.to_timestamp = None
        self.from_timestamp = None
        self.sv_utc_from_time = StringVar()
        self.sv_utc_to_time = StringVar()
        self.sv_clip_name = StringVar()
        self.sv_utc_gun = StringVar(value='Not Set')

        self.edit_frame = ttk.Frame(top)

        self.slider_frame = ttk.Frame(self.edit_frame, padding="3 3 12 12")
        self.slider_frame.grid(column=0, row=0, sticky='nwes')

        info_frame = ttk.Frame(self.edit_frame, padding="3 3 12 12")
        info_frame.grid(column=0, row=1, sticky='nwes')
        ttk.Button(info_frame, text="Create new", command=self.create_clip).grid(column=0, row=0, sticky=W)
        ttk.Label(info_frame, textvariable=self.sv_utc_from_time).grid(column=1, row=0, sticky=W)
        ttk.Label(info_frame, textvariable=self.sv_utc_to_time).grid(column=2, row=0, sticky=W)
        ttk.Entry(info_frame, textvariable=self.sv_clip_name).grid(column=3, row=0, sticky=W)
        ttk.Button(info_frame, text="Save", command=self.save_clip).grid(column=4, row=0, sticky=W)
        ttk.Button(info_frame, text="Delete clip", command=self.delete_clip).grid(column=5, row=0, sticky=W)

        self.event_type_combo = ttk.Combobox(info_frame, values=[EVT_TYPE_NAME_NORMAL, EVT_TYPE_NAME_RACE_START],
                                             state="readonly")
        self.event_type_combo.set(EVT_TYPE_NAME_NORMAL)
        self.event_type_combo.grid(column=6, row=0, sticky=W)
        self.event_type_combo.bind("<<ComboboxSelected>>", self.event_type_changed)

        self.gun_label = ttk.Label(info_frame, textvariable=self.sv_utc_gun)
        self.set_gun_button = ttk.Button(info_frame, text="Set gun", command=self.set_gun)

    def set_gun(self):
        self.event.utc_gun = self.utc_gun

    # noinspection PyUnusedLocal
    def event_type_changed(self, event):
        selection = self.event_type_combo.get()
        is_race_start = selection == EVT_TYPE_NAME_RACE_START
        self.show_hide_start_info(is_race_start)

    def show_hide_start_info(self, is_race_start):
        if is_race_start:
            self.gun_label.grid(column=7, row=0, sticky=W)
            self.set_gun_button.grid(column=8, row=0, sticky=W)
        else:
            self.gun_label.grid_forget()
            self.set_gun_button.grid_forget()

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
        self.edit_frame.grid(column=0, row=0, sticky='nwes')
        is_race_start = event.utc_gun is not None
        self.show_hide_start_info(is_race_start)

        self.is_hidden = False

    def save_clip(self):
        self.event.name = self.sv_clip_name.get()
        self.event.utc_from = self.utc_from
        self.event.utc_to = self.utc_to
        self.event.utc_gun = self.utc_gun
        self.on_save_clip(self.event)

    def delete_clip(self):
        self.on_remove_clip(self.event)

    def create_clip(self):
        self.on_create_clip()

    def on_time_change(self, vals):
        utc_from = UTC_TZ.localize(datetime.utcfromtimestamp(vals[0]))
        utc_to = UTC_TZ.localize(datetime.utcfromtimestamp(vals[1]))

        in_changed = self.utc_from != utc_from

        self.utc_from = utc_from
        self.utc_to = utc_to

        self.sv_utc_from_time.set(self.utc_from.strftime("%H:%M:%S"))
        self.sv_utc_to_time.set(self.utc_to.strftime("%H:%M:%S"))

        self.on_in_out_change(self.utc_from, self.utc_to, in_changed)

    def set_utc(self, utc: datetime):
        self.utc_gun = utc
        self.sv_utc_gun.set(utc.strftime("%H:%M:%S"))
