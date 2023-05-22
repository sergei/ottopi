import csv
import json
import os
import time
from datetime import datetime

from moviepy.video.VideoClip import ImageClip
from moviepy.video.compositing.CompositeVideoClip import CompositeVideoClip
from moviepy.video.compositing.concatenate import concatenate_videoclips
from moviepy.video.io.ImageSequenceClip import ImageSequenceClip
from moviepy.video.io.VideoFileClip import VideoFileClip

from polar_maker import PolarMaker
from summary_maker import SummaryMaker
from overlay_maker import OverlayMaker
from timer_maker import TimerMaker


def get_clip_size(mp4_name):
    clip = VideoFileClip(mp4_name)
    return clip.size[0], clip.size[1]


def make_video(work_dir, base_name, race_events, gopro, polars, ignore_cache):

    # race_events = race_events[0:1]

    width = None
    height = None

    # Determine how to cut GoPro clips
    print(f'Timestamping GOPRO clips ...')
    for evt_idx, evt in enumerate(race_events):
        history = evt['history']
        start_utc = datetime.fromisoformat(history[0]['utc'])
        stop_utc = datetime.fromisoformat(history[-1]['utc'])

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
    polar_width = 400
    timer_height = 128
    overlay_maker = OverlayMaker(work_dir, base_name, width, overlay_height, ignore_cache)
    summary_maker = SummaryMaker(work_dir, base_name, width, height, polars, ignore_cache)
    polar_maker = PolarMaker(work_dir, base_name, polar_width, polars, ignore_cache)
    timer_maker = TimerMaker(work_dir, base_name, timer_height, ignore_cache)
    for evt_idx, evt in enumerate(race_events):
        evt['overlay_images'] = []
        evt['polar_images'] = []
        evt['timer_images'] = []
        file_name = f'chapter_{evt_idx:04d}.png'
        have_wind_data = summary_maker.prepare_data(evt)
        event_title_png = summary_maker.make_chapter_png(have_wind_data, evt, file_name, width, height)
        evt['event_title_png'] = event_title_png

        if have_wind_data:
            polar_maker.set_history(evt['name'], evt['history'])
        gun_utc = evt.get('gun', None)
        for epoch_idx, epoch in enumerate(evt['history']):
            if have_wind_data:
                file_name = f'thumb_{evt_idx:04d}_{epoch_idx:04d}.png'
                thumb_png_name = summary_maker.make_thumbnail(file_name, epoch_idx, epoch, thumb_width, overlay_height)
            else:
                thumb_png_name = None

            file_name = f'polar_{evt_idx:04d}_{epoch_idx:04d}.png'
            if polar_maker.is_available():
                polar_png_name = polar_maker.set_epoch(file_name, epoch_idx)
            else:
                polar_png_name = None
            evt['polar_images'].append(polar_png_name)

            file_name = f'ovl_{evt_idx:04d}_{epoch_idx:04d}.png'
            png_name = overlay_maker.add_epoch(file_name, epoch, thumb_png_name)
            evt['overlay_images'].append(png_name)

            if gun_utc is not None:
                file_name = f'timer_{evt_idx:04d}_{epoch_idx:04d}.png'
                timer_png_name = timer_maker.add_epoch(file_name, gun_utc, epoch)
                evt['timer_images'].append(timer_png_name)

    # Create separate event clips
    max_evt = 999
    video_only = False
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

            if video_only:
                background_clip = concatenate_videoclips(camera_clips)
                clips = [background_clip]
                composite_clip = CompositeVideoClip(clips)
                composite_clip.write_videofile(evt_clip_name)
                # Close unused clips
                composite_clip.close()
                background_clip.close()
                for c in camera_clips:
                    c.close()
                print(f'{evt_clip_name} created')
                evt['composite_clip'] = evt_clip_name

            elif len(camera_clips) > 0:
                title_duration = 4
                event_title_clip = ImageClip(evt['event_title_png'], duration=title_duration)

                background_clip = concatenate_videoclips(camera_clips)
                overlay_clip = ImageSequenceClip(evt['overlay_images'], fps=evt['overlay_fps'])
                overlay_x = 0
                overlay_y = height - overlay_clip.size[1]

                clips = [background_clip,
                         overlay_clip.set_position((overlay_x, overlay_y))
                         ]

                # Optional overlays

                # Polar for tacks and gybes
                if evt['polar_images'][0] is not None:
                    polar_clip = ImageSequenceClip(evt['polar_images'], fps=evt['overlay_fps'])
                    clips.append(polar_clip.set_position(('right', 'top')))

                # Timer for the start
                if len(evt['timer_images']) > 0:
                    timer_clip = ImageSequenceClip(evt['timer_images'], fps=evt['overlay_fps'])
                    clips.append(timer_clip.set_position(('left', 'top')))

                # Title goes the last on top of everything
                clips.append(event_title_clip)
                composite_clip = CompositeVideoClip(clips)

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
    events_csv_name = work_dir + os.sep + base_name + os.sep + f'events.csv'

    print(f'Creating full movie {movie_name} ...')
    clips = []

    with open(description_name, 'wt') as df, open(events_csv_name, 'wt') as csv_file:
        csv_writer = csv.DictWriter(csv_file, delimiter=',', quotechar='"', fieldnames=['url', 'event'])
        csv_writer.writeheader()
        start_time = 0
        for evt_idx, evt in enumerate(race_events):
            if 'composite_clip' in evt:
                clip_name = evt['composite_clip']
                clip = VideoFileClip(clip_name)
                clips.append(clip)
                d = time.strftime('%M:%S', time.gmtime(start_time))
                df.write(f'{str(d)} - {evt_idx+1}. {evt["name"]}\n')
                csv_writer.writerow({'url': f'YOUTUBE_URL?t={int(start_time)}', 'event': evt['name']})

                start_time += clip.duration

        print(f'Created {description_name}')
        print(f'Created {events_csv_name}')

    if os.path.isfile(movie_name) and not ignore_cache:
        print(f'{movie_name} exists, skipped.')
        return movie_name

    movie_clip = concatenate_videoclips(clips)
    movie_clip.write_videofile(movie_name)
    print(f'Created {movie_name}')

    return movie_name
