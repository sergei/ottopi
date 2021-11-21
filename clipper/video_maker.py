import os
from datetime import datetime

from moviepy.video.compositing.CompositeVideoClip import CompositeVideoClip
from moviepy.video.compositing.concatenate import concatenate_videoclips
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
    print(f'Timestamping GOPRO clips ...')
    for evt_idx, evt in enumerate(race_events):
        history = evt['history']
        start_utc = datetime.fromisoformat(history[0]['utc'])
        stop_utc = datetime.fromisoformat(history[-1]['utc'])

        # # Fake time interval just for debugging
        # duration = stop_utc - start_utc
        # start_utc = datetime.fromisoformat('2021-11-19T18:05:00').astimezone(pytz.utc)
        # stop_utc = start_utc + duration

        go_pro_clips = gopro.get_clips_for_time_interval(start_utc, stop_utc)
        evt['go_pro_clips'] = go_pro_clips

        if len(go_pro_clips) == 0:
            print(f'No clips were found for {evt["name"]} {start_utc} {stop_utc} ')
        else:
            print(f'{evt["name"]} {start_utc} {stop_utc}')
            for clip in evt['go_pro_clips']:
                print(f'  {clip["name"]} : {clip["in_time"]} - {clip["out_time"]}')
            if width is None:
                width, height = get_clip_size(go_pro_clips[0]['name'])

    if width is None:
        print(f'No GOPRO clips were found for this event')
        return

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
        camera_clips = []
        for go_pro_clip in evt['go_pro_clips']:
            name = go_pro_clip['name']
            in_time = go_pro_clip['in_time']
            out_time = go_pro_clip['out_time']
            camera_clip = VideoFileClip(name).subclip(in_time, out_time)
            camera_clips.append(camera_clip)

        background_clip = concatenate_videoclips(camera_clips)
        overlay_clip = ImageSequenceClip(evt['overlay_images'], fps=1)
        composite_clip = CompositeVideoClip([background_clip, overlay_clip])

        movie_name = work_dir + os.sep + base_name + os.sep + f'overlay_{evt_idx:04d}.mp4'
        composite_clip.write_videofile(movie_name)

        print(f'{movie_name} created')
        break
