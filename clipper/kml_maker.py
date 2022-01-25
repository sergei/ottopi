import os.path

import simplekml

TIME_FORMAT = "%Y-%m-%dT%H:%M:%SZ"


def make_kml(kml_file, race_events, instr_data, clips):
    kml = simplekml.Kml()

    evt_pt_style = simplekml.Style()
    evt_pt_style.labelstyle.color = simplekml.Color.yellow  # Make the text red
    evt_pt_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png'
    evt_pt_style.iconstyle.scale = 1

    trk_pt_style = simplekml.Style()
    trk_pt_style.labelstyle.color = simplekml.Color.red  # Make the text red
    trk_pt_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png'
    trk_pt_style.iconstyle.scale = 0.5

    mark_style = simplekml.Style()
    mark_style.labelstyle.color = simplekml.Color.red  # Make the text red
    mark_style.labelstyle.scale = 1
    mark_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/paddle/ylw-blank.png'

    events_folder = kml.newfolder(name="Events")

    for evt in race_events:
        lng_lat = (evt['location'].longitude, evt['location'].latitude)
        point = events_folder.newpoint(name=evt['name'], coords=[lng_lat])
        point.description = f"{'='*40}<br/>{evt['utc']}<br/>"
        point.style = mark_style
        point.timestamp.when = evt['utc'].strftime(TIME_FORMAT)

        history_pts = evt['history']
        for pt_idx, pt in enumerate(history_pts):
            lng_lat = (pt.lon, pt.lat)
            point = events_folder.newpoint(name='', coords=[lng_lat])
            point.description = f"{pt_idx}<br/>{pt.utc}<br/>"
            point.style = evt_pt_style
            point.timestamp.when = pt.utc.strftime(TIME_FORMAT)

    route_folder = kml.newfolder(name="Route")
    clip_idx = 0
    for pt_idx, pt in enumerate(instr_data):
        lng_lat = (pt.lon, pt.lat)
        utc = pt.utc

        # Find GoPro clip containing this UTC time
        clip_time = None
        clip_name = None
        while clip_idx < len(clips):
            clip = clips[clip_idx]
            if clip['start_utc'] <= utc <= clip['stop_utc']:
                clip_name = clip['name']
                clip_time = int((utc - clip['start_utc']).total_seconds())
                break
            else:
                clip_idx += 1

        point = route_folder.newpoint(name='', coords=[lng_lat])
        point.description = f"{pt_idx}<br/>{utc}<br/>"
        if clip_time is not None:
            point.description += f"vlc --start-time={clip_time} {clip_name}<br/>"
            minutes = clip_time // 60
            seconds = clip_time % 60
            point.description += f"clip/{os.path.basename(clip_name)}/{minutes:02d}:{seconds:02d}<br/>"
        else:
            # Probably was gap in the clips, reset the clip finder
            clip_idx = 0

        point.style = trk_pt_style
        point.timestamp.when = utc.strftime(TIME_FORMAT)

    kml.save(kml_file)
    print(f'Created {kml_file}')
