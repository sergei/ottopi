import React, {Component} from 'react';
import Route from "./Route";

class RoutesList extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
    };

    selectRoute = (routeIdx, wptIdx) => {
        console.log('Selected route ', routeIdx, wptIdx);
        let route = this.state.routes[routeIdx];
        route.active = true;
        route.active_wpt_idx = wptIdx;
        this.props.swaggerClient
            .then( client => {
                client.apis.nav.rest_api_select_route({}, {requestBody:route})
                    .then(response => {
                        console.log(response);
                        this.requestRoutes();
                    }).catch( error => {
                    console.log("API error" + error);
                })
            }).catch( error => {
            console.log("Client error" + error);
        });
    };


    requestRoutes = () => {
        console.log('Fetching routes');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_routes().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, routes: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    };

    componentDidMount() {
        this.requestRoutes();
    }

    render() {
        if( this.state.loading ) {
            return (<div>Loading routes ...</div>)
        }else {
            let routes;
            if( this.state.ok){
                routes = this.state.routes.map( (route, i) => (
                        <Route {...route} key={i} routeIdx={i} selectRoute={this.selectRoute}/>
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
}

export default RoutesList;
