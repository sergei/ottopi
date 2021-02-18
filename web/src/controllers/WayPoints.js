import React, {Component} from 'react';
import WayPointsListView from "../views/WayPointsListView";

class WayPoints extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
    };

    navigateTo = (wpt) => {
        console.log('Navigate to ', wpt);
        this.props.swaggerClient
            .then( client => {
                client.apis.nav.rest_api_goto_wpt({}, {requestBody:wpt})
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
        console.log('Fetching waypoints ');
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
    };

    componentDidMount() {
        this.requestWpts();
    }

    render() {
        return (<WayPointsListView  loading={this.state.loading}  ok={this.state.ok}
                                    navigateTo={this.navigateTo} wpts={this.state.wpts}
            /> );
    }
}

export default WayPoints;
