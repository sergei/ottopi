import os
from datetime import datetime

import pytz
from moviepy.video.io.ImageSequenceClip import ImageSequenceClip
from moviepy.video.io.VideoFileClip import VideoFileClip

from clipper.gopro import GoPro
from clipper.overlay_maker import OverlayMaker


def get_clip_size(mp4_name):
    clip = VideoFileClip(mp4_name)
    return clip.size[0], clip.size[1]


def make_video(work_dir, base_name, race_events, gopro_dir):
    gopro = GoPro(gopro_dir)

    width = None
    height = None

    # Determine how to cut GoPro clips
    for evt_idx, evt in enumerate(race_events):
        history = evt['history']
        start_utc = datetime.fromisoformat(history[0]['utc'])
        stop_utc = datetime.fromisoformat(history[-1]['utc'])
        duration = stop_utc - start_utc

        start_utc = datetime.fromisoformat('2021-11-19T18:05:00').astimezone(pytz.utc)
        stop_utc = start_utc + duration

        go_pro_clips = gopro.get_clips_for_time_interval(start_utc, stop_utc)
        evt['go_pro_clips'] = go_pro_clips
        if len(go_pro_clips) > 0 and width is None:
            width, height = get_clip_size(go_pro_clips[0]['name'])

    # Create overlay images
    overlay_maker = OverlayMaker(work_dir, base_name, width, 256)
    for evt_idx, evt in enumerate(race_events):
        evt['overlay_images'] = []
        for epoch_idx, epoch in enumerate(evt['history']):
            file_name = f'ovl_{evt_idx:04d}_{epoch_idx:04d}.png'
            png_name = overlay_maker.add_epoch(file_name, epoch)
            evt['overlay_images'].append(png_name)

    # Compose the clip
    for evt_idx, evt in enumerate(race_events):
        overlay_clip = ImageSequenceClip(evt['overlay_images'], fps=1)
        # gopro_clip = VideoFileClip(evt['go_pro_clips'][0]['name']).subclip()

        movie_name = work_dir + os.sep + base_name + os.sep + f'overlay_{evt_idx:04d}.mp4'
        overlay_clip.write_videofile(movie_name)
        print(f'{movie_name} created')
        break
