import React, {Component} from 'react';
import WayPointsListView from "../views/WayPointsListView";
import RouteListView from "../views/RouteListView";

class Routes extends Component {
    // State of this component
    state = {
        loadingWpts: true,
        loadingActiveRoute: true,
        loadingRoutes: true,
        loadedWpts: false,
        loadedActiveRoute: false,
        loadedRoutes: false,
        routeName: '',
        routeWpts: [],
        activeWptIdx: -1,
        routeIsActive: false,
    };

    toggleActiveRoute = () => {
        console.log('toggleActiveRoute');
        const routeIsActive =  !this.state.routeIsActive;
        if (!routeIsActive){
            this.stopNavigation();
        }
        this.setState({
            routeIsActive: !this.state.routeIsActive
        })
    };

    stopNavigation = () => {
        console.log('Clear destination');
        this.props.swaggerClient
            .then( client => {
                client.apis.routes.rest_api_clear_active_route({},{})
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

    addToRoute = (wpt) => {
        console.log('Adding  ', wpt);
        const routeWpts = this.state.routeWpts.concat(wpt);

        const activeWptIdx = this.state.activeWptIdx === -1 ? 0: this.state.activeWptIdx;
        const route = {
            name: '',
            active: true,
            active_wpt_idx: activeWptIdx,
            wpts: routeWpts,
        }
        this.set_active_route(route);
    };

    removeFromRoute = (idx) => {
        console.log('Removing  ', idx);
        this.state.routeWpts.splice(idx,1);
        const activeWptIdx = this.state.activeWptIdx >= this.state.routeWpts.length ? 0: this.state.activeWptIdx;
        const route = {
            name: '',
            active: true,
            active_wpt_idx: activeWptIdx,
            wpts: this.state.routeWpts,
        }
        this.set_active_route(route);
    };

    navigateTo = (idx) => {
        console.log('Navigate to ', idx);

        const route = {
            name: '',
            active: true,
            active_wpt_idx: idx,
            wpts: this.state.routeWpts,
        }
        this.set_active_route(route);
    };

    selectRoute = (routeIdx, wptIdx) => {
        console.log('Selected route ', routeIdx, wptIdx);
        let route = this.state.routes[routeIdx];
        route.active = true;
        route.active_wpt_idx = wptIdx;
        this.set_active_route(route);
    };

    set_active_route = (route) => {
        this.setState( {loadingWpts:true, loadingRoutes: true, loadingActiveRoute: true} )
        this.props.swaggerClient
            .then(client => {
                client.apis.routes.rest_api_set_active_route({}, {requestBody: route})
                    .then(response => {
                        console.log(response);
                        this.requestWpts();
                        this.requestRoutes();
                    }).catch(error => {
                    console.log("API error" + error);
                })
            }).catch(error => {
            console.log("Client error" + error);
        });
    }

    requestWpts = () => {
        console.log('Fetching waypoints');
        this.setState( {loadingWpts:true, loadedWpts: false} )
        this.props.swaggerClient
            .then( client => {client.apis.routes.rest_api_get_wpts().then(response => {
                console.log(response)
                this.setState( {loadingWpts:false, loadedWpts: true, wpts: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loadingWpts:false, loadedWpts: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loadingWpts:false, loadedWpts: false} )
        });

        console.log('Fetching current route');
        this.setState( {loadingActiveRoute:true, loadedActiveRoute: false} )
        this.props.swaggerClient
            .then( client => {client.apis.routes.rest_api_get_active_route().then(response => {
                console.log(response)
                this.setState( {loadingActiveRoute:false, loadedActiveRoute: true,
                    routeIsActive: response.body.active,
                    routeName: response.body.name,
                    routeWpts: response.body.wpts,
                    activeWptIdx: response.body.active_wpt_idx,
                } )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loadingActiveRoute:false, loadedActiveRoute: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loadingActiveRoute:false, loadedActiveRoute: false} )
        });
    };

    requestRoutes = () => {
        console.log('Fetching routes');
        this.setState( {loadingRoutes:true, loadedRoutes: false} )
        this.props.swaggerClient
            .then( client => {client.apis.routes.rest_api_get_routes().then(response => {
                console.log(response)
                this.setState( {loadingRoutes:false, loadedRoutes: true, routes: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loadingRoutes:false, loadedRoutes: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loadingRoutes:false, loadedRoutes: false} )
        });
    };

    componentDidMount() {
        this.requestWpts();
        this.requestRoutes();
    }

    render() {
        return (<dv>
                <WayPointsListView  loading={this.state.loadingWpts}  ok={this.state.loadedWpts}
                                    loadingActiveRoute = {this.state.loadingActiveRoute}
                                    loadedActiveRoute={this.state.loadedActiveRoute}
                                    routeIsActive={this.state.routeIsActive}
                                    activeWptIdx={this.state.activeWptIdx}
                                    wpts={this.state.wpts}
                                    routeWpts = {this.state.routeWpts}

                    // Methods
                                    navigateTo={this.navigateTo}
                                    addToRoute={this.addToRoute}
                                    removeFromRoute={this.removeFromRoute}
                                    toggleActiveRoute={this.toggleActiveRoute}
                />

                <RouteListView loading={this.state.loadingRoutes}  ok={this.state.loadedRoutes}
                               routes={this.state.routes}

                               selectRoute={this.selectRoute}
                               stopNavigation={this.stopNavigation}
                />

                </dv>
        );
    }
}

export default Routes;
