import React, {Component} from 'react';
import WayPointsListView from "../views/WayPointsListView";

class WayPoints extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        routeName: '',
        routeWpts: [],
        active_wpt_idx: -1,
    };

    addToRoute = (wpt) => {
        console.log('Adding  ', wpt);
        const routeWpts = this.state.routeWpts.concat(wpt);
        this.setState({
            routeWpts: routeWpts
        })
    };

    removeFromRoute = (idx) => {
        console.log('Removing  ', idx);
        this.state.routeWpts.splice(idx,1);
        this.setState({
            routeWpts: this.state.routeWpts
        })
    };

    navigateTo = (idx) => {
        console.log('Navigate to ', idx);

        const route = {
            name: '',
            active: true,
            active_wpt_idx: idx,
            wpts: this.state.routeWpts,
        }

        this.props.swaggerClient
            .then( client => {
                client.apis.nav.rest_api_select_route({}, {requestBody:route})
                    .then(response => {
                        console.log(response);
                        this.requestWpts();
            }).catch( error => {
                console.log("API error" + error);
            })
            }).catch( error => {
            console.log("Client error" + error);
        });
    };

    requestWpts = () => {
        console.log('Fetching waypoints');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_wpts().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, wpts: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });

        console.log('Fetching current route');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_active_route().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true,
                    routeName: response.body,
                    routeWpts: response.wpts,
                    active_wpt_idx: response.active_wpt_idx,
                } )
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
        this.requestWpts();
    }

    render() {
        return (<WayPointsListView  loading={this.state.loading}  ok={this.state.ok}
                                    navigateTo={this.navigateTo}
                                    addToRoute={this.addToRoute}
                                    removeFromRoute={this.removeFromRoute}
                                    wpts={this.state.wpts}
                                    routeWpts = {this.state.routeWpts}
            /> );
    }
}

export default WayPoints;
