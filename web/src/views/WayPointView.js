import React from 'react';

function WayPointView(props) {
    let wpt = {
        'name': props.name,
        'lat': props.lat,
        'lon': props.lon,
    }
    let active = props.active ? '>' : '';
    return (
        <div>{active} {wpt.name}
        <button onClick={ () => props.navigateTo(wpt)}>GOTO</button>
        </div>
    );
}
export default WayPointView;
