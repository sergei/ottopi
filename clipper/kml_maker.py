import simplekml


def make_kml(kml_file, race_events, instr_data):
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
        point.description = evt['utc']
        point.style = mark_style
        history_pts = evt['history']
        for pt_idx, pt in enumerate(history_pts):
            lng_lat = (pt.lon, pt.lat)
            point = events_folder.newpoint(name='', coords=[lng_lat])
            point.description = f"{pt_idx}<br/>{pt.utc}<br/>"
            point.style = trk_pt_style

    route_folder = kml.newfolder(name="Route")
    for pt_idx, pt in enumerate(instr_data):
        lng_lat = (pt.lon, pt.lat)
        point = route_folder.newpoint(name='', coords=[lng_lat])
        point.description = f"{pt_idx}<br/>{pt.utc}<br/>"
        point.style = trk_pt_style

    kml.save(kml_file)
    print(f'Created {kml_file}')
