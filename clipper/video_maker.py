import os
import time
from datetime import datetime

from moviepy.video.VideoClip import ImageClip
from moviepy.video.compositing.CompositeVideoClip import CompositeVideoClip
from moviepy.video.compositing.concatenate import concatenate_videoclips
from moviepy.video.io.ImageSequenceClip import ImageSequenceClip
from moviepy.video.io.VideoFileClip import VideoFileClip

from summary_maker import SummaryMaker
from overlay_maker import OverlayMaker


def get_clip_size(mp4_name):
    clip = VideoFileClip(mp4_name)
    return clip.size[0], clip.size[1]


def make_video(work_dir, base_name, race_events, gopro, polars, ignore_cache):

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
    overlay_height = 128
    thumb_width = 256
    overlay_maker = OverlayMaker(work_dir, base_name, width, overlay_height, ignore_cache)
    summary_maker = SummaryMaker(work_dir, base_name, width, height, polars, ignore_cache)
    for evt_idx, evt in enumerate(race_events):
        evt['overlay_images'] = []
        file_name = f'chapter_{evt_idx:04d}.png'
        summary_maker.prepare_data(evt)
        event_title_png = summary_maker.make_chapter_png(evt, file_name, width, height)
        evt['event_title_png'] = event_title_png
        for epoch_idx, epoch in enumerate(evt['history']):
            file_name = f'thumb_{evt_idx:04d}_{epoch_idx:04d}.png'
            thumb_png_name = summary_maker.make_thumbnail(file_name, epoch_idx, epoch, thumb_width, overlay_height)

            file_name = f'ovl_{evt_idx:04d}_{epoch_idx:04d}.png'
            png_name = overlay_maker.add_epoch(file_name, epoch, thumb_png_name)
            evt['overlay_images'].append(png_name)

    # Create separate event clips
    max_evt = 999
    for evt_idx, evt in enumerate(race_events):
        evt_clip_name = work_dir + os.sep + base_name + os.sep + f'clip_evt_{evt_idx:04d}.mp4'
        if not os.path.isfile(evt_clip_name) or ignore_cache:
            print(f'Creating {evt_clip_name} ...')

            camera_clips = []

            for go_pro_clip in evt['go_pro_clips']:
                name = go_pro_clip['name']
                in_time = 0 if go_pro_clip['in_time'] is None else go_pro_clip['in_time']
                out_time = go_pro_clip['out_time']
                camera_clip = VideoFileClip(name).subclip(in_time, out_time)
                camera_clips.append(camera_clip)

            if len(camera_clips) > 0:
                title_duration = 4
                event_title_clip = ImageClip(evt['event_title_png'], duration=title_duration)

                background_clip = concatenate_videoclips(camera_clips)
                overlay_clip = ImageSequenceClip(evt['overlay_images'], fps=1)
                overlay_x = 0
                overlay_y = height - overlay_clip.size[1]
                composite_clip = CompositeVideoClip([background_clip,
                                                     overlay_clip.set_position((overlay_x, overlay_y)),
                                                     event_title_clip])

                composite_clip.write_videofile(evt_clip_name)

                # Close unused clips
                composite_clip.close()
                background_clip.close()
                for c in camera_clips:
                    c.close()
                print(f'{evt_clip_name} created')
                evt['composite_clip'] = evt_clip_name
            else:
                print(f'No GOPRO clips found for {evt["name"]} {evt["utc"]} ')
        else:
            print(f'Using cached {evt_clip_name}')
            evt['composite_clip'] = evt_clip_name

        if evt_idx >= max_evt:
            break

    # Create full movie
    movie_name = work_dir + os.sep + base_name + os.sep + f'movie.mp4'

    description_name = work_dir + os.sep + base_name + os.sep + f'description.txt'

    print(f'Creating full movie {movie_name} ...')
    clips = []

    with open(description_name, 'wt') as df:
        start_time = 0
        for evt_idx, evt in enumerate(race_events):
            if 'composite_clip' in evt:
                clip_name = evt['composite_clip']
                clip = VideoFileClip(clip_name)
                clips.append(clip)
                d = time.strftime('%M:%S', time.gmtime(start_time))
                df.write(f'{str(d)} - {evt_idx+1}. {evt["name"]}\n')
                start_time += clip.duration
        print(f'Created {description_name}')

    if os.path.isfile(movie_name) and not ignore_cache:
        print(f'{movie_name} exists, skipped.')
        return movie_name

    movie_clip = concatenate_videoclips(clips)
    movie_clip.write_videofile(movie_name)
    print(f'Created {movie_name}')

    return movie_name
