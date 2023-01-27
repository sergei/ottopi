import os
import tkinter
from datetime import datetime, timedelta

import tkintermapview
from PIL import Image, ImageTk

from gui.clip_event import ClipEvent
from raw_instr_data import RawInstrData


class MapView:
    def __init__(self, top, width=800, height=600):
        self.map_widget = tkintermapview.TkinterMapView(top, width=width, height=height, corner_radius=0)
        self.map_widget.place(relx=0.5, rely=0.5, anchor=tkinter.CENTER)
        self.event_paths = dict()
        self.event_markers = dict()
        self.instr_data = []
        self.boat_marker = None
        self.in_marker = None
        self.out_marker = None
        self.race_path = None
        current_path = os.path.join(os.path.dirname(os.path.abspath(__file__)))
        self.event_center_icon = ImageTk.PhotoImage(
            Image.open(os.path.join(current_path, "assets", "icons8-small-blue-diamond-48.png")).resize((36, 36)))
        self.event_in_icon = ImageTk.PhotoImage(
            Image.open(os.path.join(current_path, "assets", "icons8-kite-shape-red-48.png")).resize((24, 24)))
        self.event_out_icon = ImageTk.PhotoImage(
            Image.open(os.path.join(current_path, "assets", "icons8-kite-shape-green-48.png")).resize((24, 24)))

    def show_track(self, start_time_utc: datetime, finish_time_utc: datetime, instr_data: list[RawInstrData]):
        self.instr_data = instr_data
        position_list = []
        min_lat = 200
        max_lat = -200
        min_lon = 200
        max_lon = -200

        for ii in instr_data:
            if start_time_utc < ii.utc < finish_time_utc:
                position_list.append((ii.lat, ii.lon))
                min_lat = min(ii.lat, min_lat)
                min_lon = min(ii.lon, min_lon)
                max_lat = max(ii.lat, max_lat)
                max_lon = max(ii.lon, max_lon)

        self.map_widget.set_position(instr_data[0].lat, instr_data[0].lon)
        self.map_widget.fit_bounding_box((max_lat, min_lon), (min_lat, max_lon))
        if self.race_path is not None:
            self.race_path.delete()
        self.race_path = self.map_widget.set_path(position_list, width=2)

    def clear_events(self):
        for uuid in self.event_paths:
            self.event_paths[uuid].delete()
        for uuid in self.event_markers:
            self.event_markers[uuid].delete()

    def show_events(self, events: list[ClipEvent]):
        for event in events:
            if event.uuid in self.event_paths:
                self.event_paths[event.uuid].delete()
            if event.uuid in self.event_markers:
                self.event_markers[event.uuid].delete()

            in_event = False
            position_list = []
            for ii in self.instr_data:
                if event.utc_from <= ii.utc <= event.utc_to:
                    if not in_event:
                        in_event = True
                        position_list = []
                    position_list.append((ii.lat, ii.lon))
                else:
                    if in_event:
                        in_event = False
                        path = self.map_widget.set_path(position_list, width=6)
                        self.event_paths[event.uuid] = path
                        self.event_markers[event.uuid] = self.map_widget.set_marker(position_list[0][0],
                                                                                    position_list[0][1],
                                                                                    text=event.name,
                                                                                    icon=self.event_center_icon)

    def update_clip(self, event):
        self.show_events([event])

    def remove_clip(self, event):
        if event.uuid in self.event_paths:
            self.event_paths[event.uuid].delete()
            self.event_paths.pop(event.uuid)

        if event.uuid in self.event_markers:
            self.event_markers[event.uuid].delete()
            self.event_markers.pop(event.uuid)

    def show_boat(self, ii: RawInstrData):
        if self.boat_marker is None:
            self.boat_marker = self.map_widget.set_marker(ii.lat, ii.lon)
        else:
            self.boat_marker.set_position(ii.lat, ii.lon)

    def show_clip_in_out(self, utc_in: datetime, utc_out: datetime):
        utc_in_p = utc_in + timedelta(seconds=1)
        utc_out_p = utc_out + timedelta(seconds=1)
        for ii in self.instr_data:
            if utc_in <= ii.utc < utc_in_p:
                if self.in_marker is None:
                    self.in_marker = self.map_widget.set_marker(ii.lat, ii.lon, icon=self.event_in_icon)
                else:
                    self.in_marker.set_position(ii.lat, ii.lon)
            if utc_out <= ii.utc < utc_out_p:
                if self.out_marker is None:
                    self.out_marker = self.map_widget.set_marker(ii.lat, ii.lon, icon=self.event_out_icon)
                else:
                    self.out_marker.set_position(ii.lat, ii.lon)
