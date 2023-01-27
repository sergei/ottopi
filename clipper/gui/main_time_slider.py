from datetime import datetime
from tkinter import ttk, W, StringVar, DoubleVar

import pytz

from gui.race_info import RaceInfo

UTC_TZ = pytz.UTC


class MainTimeSlider:
    def __init__(self, top, on_change, on_save_race, on_split_race, on_join_race_to_prev, on_delete_race):

        # Callbacks
        self.on_change = on_change
        self.on_save_race = on_save_race
        self.on_split_race = on_split_race
        self.on_join_race_to_prev = on_join_race_to_prev
        self.on_delete_race = on_delete_race

        self.sv_race_name = StringVar()
        self.to_timestamp = None
        self.from_timestamp = None

        self.sv_utc_time = StringVar()
        self.current_value = DoubleVar()

        self.race = None
        self.slider = None
        self.slider_frame = ttk.Frame(top, padding="3 3 12 12")
        self.slider_frame.grid(column=0, row=0, sticky='nwes')

        buttons_frame = ttk.Frame(top, padding="3 3 12 12")
        buttons_frame.grid(column=0, row=1, sticky='nwes')
        ttk.Button(buttons_frame, text="<<", command=lambda: self.on_button_click(-60)).grid(column=0, row=0, sticky=W)
        ttk.Button(buttons_frame, text="<", command=lambda: self.on_button_click(-1)).grid(column=1, row=0, sticky=W)
        ttk.Label(buttons_frame, textvariable=self.sv_utc_time).grid(column=2, row=0, sticky=W)
        ttk.Button(buttons_frame, text=">", command=lambda: self.on_button_click(1)).grid(column=3, row=0, sticky=W)
        ttk.Button(buttons_frame, text=">>", command=lambda: self.on_button_click(60)).grid(column=4, row=0, sticky=W)

        ttk.Entry(buttons_frame, textvariable=self.sv_race_name).grid(column=5, row=0, sticky=W)
        ttk.Button(buttons_frame, text="Save name", command=self.on_save).grid(column=6, row=0, sticky=W)
        ttk.Button(buttons_frame, text="Split", command=self.on_split).grid(column=7, row=0, sticky=W)
        ttk.Button(buttons_frame, text="Join with previous", command=self.on_join).grid(column=8, row=0, sticky=W)
        ttk.Button(buttons_frame, text="Delete", command=self.on_delete).grid(column=9, row=0, sticky=W)

    def on_save(self):
        self.on_save_race(self.race.uuid, self.sv_race_name.get())

    def on_split(self):
        utc = UTC_TZ.localize(datetime.utcfromtimestamp(self.current_value.get()))
        self.on_split_race(self.race.uuid, utc)

    def on_join(self):
        self.on_join_race_to_prev(self.race.uuid)

    def on_delete(self):
        self.on_delete_race(self.race.uuid)

    def on_button_click(self, delta_sec):
        ts = self.current_value.get()
        ts += delta_sec
        ts = max(self.from_timestamp, ts)
        ts = min(self.to_timestamp, ts)
        self.current_value.set(ts)
        self.on_time_change(None)

    def on_time_change(self, event):
        utc = UTC_TZ.localize(datetime.utcfromtimestamp(self.current_value.get()))
        self.sv_utc_time.set(utc.strftime("%m/%d/%Y %H:%M:%S"))
        self.on_change(utc)

    def move_to(self, utc: datetime):
        self.current_value.set(utc.timestamp())
        self.on_time_change(None)

    def set_utc(self, utc: datetime):
        self.current_value.set(utc.timestamp())
        utc = UTC_TZ.localize(datetime.utcfromtimestamp(self.current_value.get()))
        self.sv_utc_time.set(utc.strftime("%m/%d/%Y %H:%M:%S"))

    def show(self, race: RaceInfo):
        self.race = race
        self.sv_race_name.set(race.name)
        self.from_timestamp = race.utc_from.timestamp()
        self.to_timestamp = race.utc_to.timestamp()
        self.slider = ttk.Scale(
            self.slider_frame,
            from_=self.from_timestamp,
            to=self.to_timestamp,
            value=self.from_timestamp,
            orient='horizontal',
            variable=self.current_value,
            command=self.on_time_change,
            length=800
        )
        self.slider.grid(column=0, row=0, sticky=W)
        self.current_value.set(self.from_timestamp)
