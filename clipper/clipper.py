import datetime
import os
import re
import string
from tkinter import *
from tkinter import ttk, filedialog

import caffeine as caffeine
from simplekml import Location

from gopro import GoPro
from gui.clip_editor import ClipEditor
from gui.clip_event import ClipEvent
from gui.events_table import EventsTable
from gui.main_time_slider import MainTimeSlider
from gui.map_view import MapView
from gui.race_info import RaceInfo
from gui.video_player import VideoPlayer
from navigator import Navigator
from navigator_listener import NavigationListener
from nmeaparser import NmeaParser
from project import Project, COMMON, GOPRO, NMEA
from race_events_recorder import RaceEventsRecorder
from raw_instr_data import RawInstrData
from video_maker import make_video

SAVE_AS_ENTRY = 'Save As ...'
SAVE_ENTRY = 'Save'
PRODUCE_ENTRY = 'Produce ...'


def get_valid_filename(s):
    s = str(s).strip().replace(' ', '_')
    return re.sub(r'(?u)[^-\w.]', '', s)


class Clipper(NavigationListener):
    races: list[RaceInfo]
    current_race: [RaceInfo]
    instr_data: list[RawInstrData]

    def __init__(self, work_dir):
        super().__init__()
        self.root = Tk()  # Must be the first line
        self.races = []
        self.current_race = None
        self.slider_utc = None
        self.events_table = None
        self.work_dir = work_dir
        self.duration_sec = None
        self.current_utc = None
        self.current_clip_idx = None
        self.gopro = None
        self.menu_file = None
        self.project = Project()
        self.sv_go_pro_dir = StringVar()
        self.sv_nmea_file_name = StringVar()
        self.polar_file_sv = StringVar()
        self.map_view = None
        self.instr_data = []
        self.gen_evt_button = None
        self.main_time_slider = None
        self.clip_editor = None
        self.video_player = None
        self.clip_is_being_edited = False

    def read_project(self):
        self.sv_go_pro_dir.set(self.project.get(GOPRO, 'dir'))
        self.open_clips_dir(self.sv_go_pro_dir.get())
        self.sv_nmea_file_name.set(self.project.get(NMEA, 'dir'))

        self.instr_data = self.project.get(COMMON, 'instr_data')
        self.races = self.project.get(COMMON, 'races')
        if self.races is None:
            self.races = []
            self.current_race = None
        else:
            self.current_race = self.races[0]

        self.update_views()
        self.on_project_change()

    def new_project(self):
        self.project = Project()
        self.read_project()

    def open_project(self):
        file_name = filedialog.askopenfilename()
        if file_name is not None:
            project = Project()
            if project.open(file_name):
                self.project = project
                self.read_project()

    def save_project(self):
        self.project.save()
        self.on_project_change()

    def save_project_as(self):
        file_name = filedialog.asksaveasfilename()
        if file_name is not None:
            self.project.save(file_name)
            self.on_project_change()

    def set_nmea_folder(self):
        file_name = filedialog.askopenfilename()
        if file_name is None:
            return

        self.project.set(NMEA, 'dir', file_name)
        self.on_project_change()
        self.sv_nmea_file_name.set(file_name)

    def read_clips(self):
        initial_dir = self.project.get(GOPRO, 'dir')

        dir_name = filedialog.askdirectory(initialdir=initial_dir)
        if len(dir_name) == 0:
            return

        if self.open_clips_dir(dir_name):
            self.project.set(GOPRO, 'dir', dir_name)
            self.on_project_change()

    def open_clips_dir(self, dir_name):
        self.gopro = GoPro(dir_name, self.work_dir)
        if len(self.gopro.clips) > 0:
            self.current_clip_idx = 0
            self.current_utc = self.gopro.clips[0]['start_utc']
            self.duration_sec = (self.gopro.clips[-1]['stop_utc'] - self.current_utc).total_seconds()
            self.sv_go_pro_dir.set(dir_name)
            self.video_player.set_clips(self.gopro.clips)
            return True

        return False

    def set_polar_name(self):
        file_name = filedialog.askopenfilename()
        if file_name is not None:
            self.project.set(COMMON, 'polar_file', file_name)
            self.polar_file_sv.set(file_name)
            self.on_project_change()

    def on_project_change(self):
        if self.project.json_file is None:
            title = "Untitled"
        else:
            title = self.project.json_file

        if self.project.is_dirty() or self.project.json_file is None:
            title += "*"
            self.menu_file.entryconfigure(SAVE_ENTRY, state=NORMAL)
            self.menu_file.entryconfigure(SAVE_AS_ENTRY, state=NORMAL)
        else:
            self.menu_file.entryconfigure(SAVE_ENTRY, state=DISABLED)
            self.menu_file.entryconfigure(SAVE_AS_ENTRY, state=DISABLED)

        self.menu_file.entryconfigure(PRODUCE_ENTRY, state=NORMAL)

        self.root.title(title)
        if self.sv_go_pro_dir.get() != "" and self.sv_nmea_file_name.get() != "" and self.polar_file_sv != "":
            self.gen_evt_button['state'] = NORMAL

    def start(self):
        video_height = 480
        video_width = 848

        map_height = video_height
        map_width = 800

        # Menu
        self.root.option_add('*tearOff', FALSE)
        menubar = Menu(self.root)
        appmenu = Menu(menubar, name='apple')
        menubar.add_cascade(menu=appmenu)

        self.menu_file = Menu(menubar)
        menubar.add_cascade(menu=self.menu_file, label='File')
        self.menu_file.add_command(label='New Project', command=self.new_project)
        self.menu_file.add_command(label='Open...', command=self.open_project)
        self.menu_file.add_command(label=SAVE_ENTRY, command=self.save_project, state=DISABLED)
        self.menu_file.add_command(label=SAVE_AS_ENTRY, command=self.save_project_as, state=DISABLED)
        self.menu_file.add_command(label=PRODUCE_ENTRY, command=self.produce_video, state=DISABLED)

        windowmenu = Menu(menubar, name='window')
        menubar.add_cascade(menu=windowmenu, label='Window')
        self.root['menu'] = menubar
        self.root.title('Clipper')

        top_frame = ttk.Frame(self.root, padding="3 3 12 12")
        top_frame.grid(column=0, row=0, sticky='nwes')

        # Create high level frames
        files_frame = ttk.Frame(top_frame, padding="3 3 12 12")
        files_frame.grid(column=0, row=0, sticky='nwes')

        displays_frame = ttk.Frame(top_frame)
        displays_frame.grid(column=0, row=1, sticky='nwes')

        events_frame = ttk.Frame(displays_frame)
        events_frame.grid(column=0, row=0, sticky='nwes')

        map_frame = ttk.Frame(displays_frame, width=map_width, height=map_height)
        map_frame.grid(column=1, row=0, sticky='nwes')

        video_frame = ttk.Frame(displays_frame)
        video_frame.grid(column=2, row=0)

        main_time_slider_frame = ttk.Frame(top_frame, padding="3 3 12 12")
        main_time_slider_frame.grid(column=0, row=2, sticky='nwes')

        clip_slider_frame = ttk.Frame(top_frame, padding="3 3 12 12")
        clip_slider_frame.grid(column=0, row=3, sticky='nwes')

        self.root.title("Clipper")
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)

        self.on_project_change()

        # GoPro folder
        clips_dir = self.project.get(GOPRO, 'dir')
        if clips_dir is not None and os.path.isdir(clips_dir):
            self.open_clips_dir(clips_dir)

        ttk.Button(files_frame, text="Open GoPro Folder",
                   command=self.read_clips).grid(column=0, row=0, sticky=W)
        ttk.Label(files_frame, textvariable=self.sv_go_pro_dir).grid(column=0, row=2, sticky=W)

        # NMEA folder
        self.sv_nmea_file_name.set(self.project.get(NMEA, 'dir'))
        ttk.Button(files_frame, text="Open NMEA Folder",
                   command=self.set_nmea_folder).grid(column=1, row=0, sticky=W)
        ttk.Label(files_frame, textvariable=self.sv_nmea_file_name).grid(column=1, row=2, sticky=W)

        # Polar file
        self.polar_file_sv.set(self.project.get(COMMON, 'polar_file'))
        ttk.Button(files_frame, text="Set polar name",
                   command=self.set_polar_name).grid(column=0, row=3, sticky=W)
        ttk.Label(files_frame, textvariable=self.polar_file_sv).grid(column=0, row=4, sticky=W)

        # Polar file
        self.gen_evt_button = ttk.Button(events_frame, text="Generate events", command=self.generate_events,
                                         state=DISABLED)
        self.gen_evt_button.grid(column=0, row=1, sticky=W)

        # Events table
        self.events_table = EventsTable(events_frame,
                                        on_race_selected=self.on_race_selected,
                                        on_event_selected=self.on_event_selected)

        # Map
        self.map_view = MapView(map_frame, map_height, map_height)

        # Main slider
        self.main_time_slider = MainTimeSlider(main_time_slider_frame,
                                               on_change=self.on_main_time_slider_change,
                                               on_save_race=self.on_save_race,
                                               on_split_race=self.on_split_race,
                                               on_join_race_to_prev=self.on_join_race_to_prev,
                                               on_delete_race=self.on_delete_race,
                                               on_play_pause=self.on_play_pause
                                               )

        # Clip editor
        self.clip_editor = ClipEditor(clip_slider_frame,
                                      self.on_in_out_change,
                                      self.on_save_clip,
                                      self.on_remove_clip,
                                      self.on_create_clip)

        self.video_player = VideoPlayer(video_frame, video_width, video_height, self.on_video_utc_change)

        self.root.mainloop()

    def on_play_pause(self):
        self.video_player.play_pause()

    def on_in_out_change(self, utc_in, utc_out, in_changed):
        self.clip_is_being_edited = True
        self.map_view.show_clip_in_out(utc_in, utc_out)
        utc = utc_in if in_changed else utc_out

        ii = self.find_track_point(utc)
        if ii is not None:
            self.map_view.show_boat(ii)

        self.video_player.play_video_at_utc(utc)

        pass

    def on_save_clip(self, event):
        self.clip_is_being_edited = False
        self.map_view.update_clip(event)
        self.events_table.update_event(event)
        for e in self.current_race.events:
            if e.uuid == event.uuid:
                self.current_race.events.remove(e)
                self.current_race.events.append(event)
        self.current_race.events.sort(key=lambda x: x.utc_from)
        self.project.set(COMMON, 'races', self.races)
        self.on_project_change()

    def on_remove_clip(self, event: ClipEvent):
        self.map_view.remove_clip(event)
        self.events_table.remove_event(event)
        for e in self.current_race.events:
            if e.uuid == event.uuid:
                self.current_race.events.remove(e)
        self.project.set(COMMON, 'races', self.races)
        self.on_project_change()

    def on_create_clip(self):
        if self.slider_utc is not None:
            event = ClipEvent('New event',
                              self.slider_utc - datetime.timedelta(seconds=30),
                              self.slider_utc + datetime.timedelta(seconds=30),
                              )
            self.current_race.events.append(event)
            self.current_race.events.sort(key=lambda x: x.utc_from)
            self.project.set(COMMON, 'races', self.races)
            self.update_views()

    def on_video_utc_change(self, utc):
        self.slider_utc = utc
        self.main_time_slider.set_utc(utc)
        ii = self.find_track_point(utc)
        if ii is not None:
            self.map_view.show_boat(ii)

    def on_main_time_slider_change(self, utc):
        self.slider_utc = utc
        ii = self.find_track_point(utc)
        if ii is not None:
            self.map_view.show_boat(ii)

        self.clip_editor.set_utc(utc)
        self.video_player.play_video_at_utc(utc)

    def find_race_by_uuid(self, race_uuid):
        race = None
        for r in self.races:
            if r.uuid == race_uuid:
                race = r
                break
        return race

    def find_race_by_utc(self, utc: datetime):
        for r in self.races:
            if r.utc_from <= utc <= r.utc_to:
                return r

        return None

    def on_race_selected(self, race_uuid):
        race = self.find_race_by_uuid(race_uuid)
        if race is not None and self.current_race != race:
            self.current_race = race
            self.update_views()

    def on_event_selected(self, event: ClipEvent):
        # Check if the event belongs to the current race, otherwise select the one it belongs to
        race = self.find_race_by_utc(event.utc_from)
        race_has_changed = race is not None and self.current_race != race
        if race_has_changed:
            self.current_race = race
            self.update_views()

        self.main_time_slider.move_to(event.utc_from)
        self.clip_editor.show(event)
        self.map_view.show_clip_in_out(event.utc_from, event.utc_to)

    def on_instr_data(self, instr_data: RawInstrData):
        self.instr_data.append(instr_data)

    def generate_events(self):
        print('Starting to generate events')
        navigator = Navigator.get_instance()
        navigator.read_polars(self.project.get(COMMON, 'polar_file'))
        nmea_parser = NmeaParser(navigator, strict_cc=True)
        events_recorder = RaceEventsRecorder(self.work_dir,
                                             self.gopro.start_time_utc, self.gopro.finish_time_utc, self.gopro.clips,
                                             None, None)
        navigator.add_listener(events_recorder)
        navigator.add_listener(self)
        data_dir = os.path.expanduser(self.work_dir)
        navigator.set_data_dir(data_dir)
        nmea_name = self.sv_nmea_file_name.get()

        self.instr_data = []

        print(f'Reading NMEA from {nmea_name}')
        with open(nmea_name, 'r') as f:
            for nmea in f:
                nmea_parser.set_nmea_sentence(nmea)

        events_recorder.finalize()
        events = []
        for e in events_recorder.events:
            if len(e['history']) > 0:
                event = ClipEvent(e['name'], e['history'][0].utc, e['history'][-1].utc)
                events.append(event)

        print(f' {len(events)} events generated')
        if len(events) > 0:
            race = RaceInfo(f'Race {events[0].utc_from.strftime("%Y/%m/%d/ %H:%M:%S")}',
                            self.instr_data[0].utc, self.instr_data[-1].utc, events)
            self.races.append(race)
            self.current_race = self.races[0]

            self.project.set(COMMON, 'races', self.races)
            self.project.set(COMMON, 'instr_data', self.instr_data)

            self.update_views()
            self.on_project_change()

    def update_views(self):
        self.events_table.show_races(self.races)

        if self.current_race is not None:
            self.map_view.show_track(self.current_race.utc_from, self.current_race.utc_to, self.instr_data)
            self.map_view.clear_events()
            self.map_view.show_events(self.current_race.events)

            self.main_time_slider.show(self.current_race)
            self.video_player.play_video_at_utc(self.current_race.utc_from)

    def find_track_point(self, utc: datetime):
        result = None
        for ii in self.instr_data:
            if ii.utc >= utc:
                return ii

        return result

    def locate_event_with_utc(self, utc: datetime):
        for event in self.current_race.events:
            if event.utc_from <= utc <= event.utc_to:
                return event
        return None

    def on_save_race(self, race_uuid: string, race_name):
        self.current_race.name = race_name
        self.update_views()
        self.project.set(COMMON, 'races', self.races)
        self.on_project_change()

    def on_split_race(self, race_uuid: string, utc: datetime):
        old_race = self.find_race_by_uuid(race_uuid)

        # Find where to split events
        split_at_idx = len(old_race.events)
        for idx, event in enumerate(old_race.events):
            if event.utc_from > utc:
                split_at_idx = idx
                break

        # Create new race
        new_events = old_race.events[split_at_idx:-1]
        new_race = RaceInfo(f'Race {utc.strftime("%Y/%m/%d/ %H:%M:%S")}',
                            utc,
                            old_race.utc_to,
                            new_events)
        # Update old race
        old_race.events = old_race.events[0:split_at_idx]
        old_race.utc_to = utc

        race_idx = self.races.index(old_race)
        self.races.insert(race_idx+1, new_race)
        self.project.set(COMMON, 'races', self.races)

        self.update_views()
        self.project.set(COMMON, 'races', self.races)
        self.on_project_change()

    def on_join_race_to_prev(self, race_uuid: string):
        if len(self.races) > 1:
            race = self.find_race_by_uuid(race_uuid)
            race_idx = self.races.index(race)
            if race_idx > 0:
                # Change previous race
                joint_race = self.races[race_idx-1]
                joint_race.events += race.events
                joint_race.utc_to = race.utc_to
                # Delete this race
                self.races.pop(race_idx)

                self.update_views()
                self.project.set(COMMON, 'races', self.races)
                self.on_project_change()

    def on_delete_race(self, race_uuid: string):
        if len(self.races) > 1:
            race = self.find_race_by_uuid(race_uuid)
            race_idx = self.races.index(race)
            self.races.pop(race_idx)
            if race_idx >= len(self.races) - 1:
                race_idx -= 1
            self.current_race = self.races[race_idx]

            self.update_views()
            self.project.set(COMMON, 'races', self.races)
            self.on_project_change()

    def produce_video(self):
        dir_name = filedialog.askdirectory(initialdir='/tmp')
        if len(dir_name) == 0:
            return

        navigator = Navigator.get_instance()
        navigator.read_polars(self.project.get(COMMON, 'polar_file'))
        for race in self.races:
            # Create event's JSON file in old format
            race_events = []
            for event in race.events:

                history = []
                hist_idx = None
                for idx, ii in enumerate(self.instr_data):
                    if event.utc_from <= ii.utc <= event.utc_to:
                        if hist_idx is None:
                            hist_idx = idx
                        history.append(ii.to_dict())

                duration = int((event.utc_to - event.utc_from).total_seconds())
                half_span = duration // 2
                utc = event.utc_from + datetime.timedelta(seconds=half_span)

                race_event = {
                    'name': event.name,
                    'gun': event.utc_gun.isoformat() if event.utc_gun is not None else None,
                    'in': event.utc_from.isoformat(),
                    'out': event.utc_to.isoformat(),
                    'utc': utc.isoformat(),
                    'history': history,
                    'hist_idx': hist_idx,
                    'location': Location(self.instr_data[hist_idx].lat, self.instr_data[hist_idx].lon),
                }
                race_events.append(race_event)

            caffeine.on(display=False)
            make_video(dir_name, get_valid_filename(race.name), race_events, self.gopro, navigator.polars, False)
            caffeine.off()
