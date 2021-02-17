import React  from 'react';
import RouteView from "./RouteView";

function RouteListView(props) {
    
        if( props.loading ) {
            return (<div>Loading routes ...</div>)
        }else {
            let routes;
            if( props.ok){
                routes = props.routes.map( (route, i) => (
                        <RouteView {...route} key={i} routeIdx={i} selectRoute={props.selectRoute}/>
                    )
                );
            }else{
                routes = 'Failed to fetch routes';
            }
            return (
                <div>
                    {routes}
                </div>
            )
        }
}

export default RouteListView;
