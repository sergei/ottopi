import React from 'react';

function RoutePoint(props) {
    let wpt = {
        'name': props.name,
        'lat': props.lat,
        'lon': props.lon,
    }
    let active = props.active ? '>' : '';

    console.log("RoutePoint");
    console.log(props);

    return (
        <div>{active} {wpt.name}
            <button onClick={ () => props.selectRoute(props.routeIdx, props.wptIdx)}>GOTO</button>
        </div>
    );
}
export default RoutePoint;

