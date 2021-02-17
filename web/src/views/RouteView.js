import React from 'react';
import RoutePointView from "./RoutePointView";

function RouteView(props) {
    const [open, setOpen] = React.useState(false);

    console.log("RouteView");

    const wpts = props.wpts.map( (wpt, i) => (
            <RoutePointView {...wpt} routeIdx={props.routeIdx} wptIdx={i} selectRoute={props.selectRoute}/>
        )
    );

    if ( open ) {
        return (
            <div>
                <button onClick={ () => setOpen(true) }>Show WPTs</button>
                {props.name}
                <button onClick={ () => props.selectRoute(props.routeIdx, 0)}>Activate</button>
            </div>
        );
    }else{
        return (
            <div>
                <button onClick={ () => setOpen(false) }>Hide WPTs</button>
                {props.name}
                {wpts}
            </div>
        );
    }
}

export default RouteView;
