import os
from datetime import datetime, timedelta
from tkinter import Canvas

from tkVideoPlayer import TkinterVideo

STATE_INIT = 'state_init'
STATE_LOADING = 'state_loading'
STATE_LOADED = 'state_loaded'
STATE_STOPPING = 'state_stopping'

EVENT_LOAD_CLIP = 'event_load_clip'
EVENT_SEEK = 'event_seek'
EVENT_CLIP_LOADED = 'event_clip_loaded'
EVENT_CLIP_ENDED = 'event_clip_ended'


class VideoPlayer:
    def __init__(self, top, width=848, height=480, on_clip_utc_change=None):
        self.paused = True
        self.state = STATE_INIT
        self.on_clip_utc_change = on_clip_utc_change
        c = Canvas(top, bg="blue", height=height, width=width)
        c.grid(column=0, row=0, sticky='nwes')
        self.videoplayer = TkinterVideo(master=top, scaled=False)
        self.videoplayer.grid(column=0, row=0, sticky='nwes')
        self.gopro_clips = []
        self.clip_idx = -1
        self.low_res_clip_name = None
        self.videoplayer.bind("<<Loaded>>", self.on_loaded)
        self.videoplayer.bind("<<SecondChanged>>", self.on_second_changed)
        self.videoplayer.bind("<<Ended>>", self.on_ended)
        self.videoplayer.bind("<<FrameGenerated>>", self.on_frame_generated, add=True)

        self.pending_clip_name = None
        self.pending_clip_offset = 0

    def state_machine(self, event, clip_name=None, seek_to_sec=0):
        print(f'{self.state} {event}')

        if self.state == STATE_INIT:
            if event == EVENT_LOAD_CLIP:
                print(f'Loading {clip_name} at {seek_to_sec} sec')
                self.videoplayer.load(clip_name)
                self.videoplayer.seek(seek_to_sec)
                self.videoplayer.play()
                self.state = STATE_LOADING
            else:
                print(f'Ignore event {event}')

        elif self.state == STATE_LOADING:
            if event == EVENT_CLIP_LOADED:
                self.state = STATE_LOADED
            else:
                print(f'Ignore event {event}')
        elif self.state == STATE_LOADED:
            if event == EVENT_LOAD_CLIP:
                self.videoplayer.stop()
                self.pending_clip_name = clip_name
                self.pending_clip_offset = seek_to_sec
                self.state = STATE_STOPPING
            elif event == EVENT_CLIP_ENDED:
                if self.clip_idx < len(self.gopro_clips) - 1:
                    self.clip_idx += 1
                    low_res_name = self.get_low_res_name(self.gopro_clips[self.clip_idx]['name'])
                    print(f'Loading {low_res_name} from the beginning')
                    self.videoplayer.load(low_res_name)
                    self.videoplayer.play()
            elif event == EVENT_SEEK:
                self.videoplayer.seek(seek_to_sec)
            else:
                print(f'Ignore event {event}')
        elif self.state == STATE_STOPPING:
            if event == EVENT_CLIP_ENDED:
                print(f'Loading {self.pending_clip_name} at {self.pending_clip_offset} sec')
                self.videoplayer.load(self.pending_clip_name)
                self.videoplayer.seek(self.pending_clip_offset)
                self.videoplayer.play()
                self.state = STATE_LOADING
            else:
                print(f'Ignore event {event}')

    def play_pause(self):
        if self.videoplayer.is_paused():
            self.paused = False
            self.videoplayer.play()
        else:
            self.paused = True

    def pause(self):
        self.paused = True
        if not self.videoplayer.is_paused():
            self.videoplayer.pause()

    # noinspection PyUnusedLocal
    def on_frame_generated(self, event):
        if self.paused:
            self.videoplayer.pause()

    # noinspection PyUnusedLocal
    def on_second_changed(self, event):
        sec = self.videoplayer.current_duration()
        utc = self.gopro_clips[self.clip_idx]['start_utc'] + timedelta(seconds=sec)
        self.on_clip_utc_change(utc)

    # noinspection PyUnusedLocal
    def on_loaded(self, event):
        print(f'on_loaded')
        self.state_machine(EVENT_CLIP_LOADED)

    # noinspection PyUnusedLocal
    def on_ended(self, event):
        print(f'on_ended')
        self.state_machine(EVENT_CLIP_ENDED)

    def set_clips(self, gopro_clips):
        self.gopro_clips = gopro_clips

    def play_video_at_utc(self, utc: datetime):
        self.clip_idx = -1
        for clip in self.gopro_clips:
            self.clip_idx += 1
            if clip['start_utc'] <= utc <= clip['stop_utc']:
                hi_res_clip_name = clip['name']
                print(f'For utc {utc} found clip {hi_res_clip_name}')
                seek_to_sec = int((utc - clip['start_utc']).total_seconds())
                self.play_clip_at_sec(hi_res_clip_name, seek_to_sec)
                break

    def play_clip_at_sec(self, hi_res_clip_name, seek_to_sec):
        # Build low res file name
        low_res_path_name = self.get_low_res_name(hi_res_clip_name)

        # Check if we need to load new clip
        if low_res_path_name != self.low_res_clip_name:
            self.state_machine(EVENT_LOAD_CLIP, low_res_path_name, seek_to_sec)
        else:
            self.state_machine(EVENT_SEEK, seek_to_sec)

    @staticmethod
    def get_low_res_name(hi_res_clip_name):
        path, name = os.path.split(hi_res_clip_name)
        base, ext = os.path.splitext(name)
        lw_res_base = base.replace('GX', 'GL')
        low_res_path_name = os.path.join(path, lw_res_base + '.LRV')
        return low_res_path_name
