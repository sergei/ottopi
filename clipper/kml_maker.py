import simplekml


def make_kml(kml_file, race_events):
    kml = simplekml.Kml()

    trk_pt_style = simplekml.Style()
    trk_pt_style.labelstyle.color = simplekml.Color.red  # Make the text red
    trk_pt_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png'
    trk_pt_style.iconstyle.scale = 0.5

    mark_style = simplekml.Style()
    mark_style.labelstyle.color = simplekml.Color.red  # Make the text red
    mark_style.labelstyle.scale = 1
    mark_style.iconstyle.icon.href = 'http://maps.google.com/mapfiles/kml/paddle/ylw-blank.png'

    route_folder = kml.newfolder(name="Route")

    for evt in race_events:
        lng_lat = (evt['location']['lon'], evt['location']['lat'])
        point = route_folder.newpoint(name=evt['name'], coords=[lng_lat])
        point.description = evt['utc']
        point.style = mark_style
        history_pts = evt['history']
        for pt in history_pts:
            lng_lat = (pt['lon'], pt['lat'])
            point = route_folder.newpoint(name='', coords=[lng_lat])
            point.description = pt['utc'] + '<br/>'
            point.style = trk_pt_style

    kml.save(kml_file)
    print(f'Created {kml_file}')
