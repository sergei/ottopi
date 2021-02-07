import React, {Component} from 'react';
import WayPoint from "./WayPoint";

class WayPointsList extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        result: null,
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
        if( this.state.loading ) {
            return (<div>Loading WPTs ...</div>)
        }else {
            let wpts;
            if( this.state.ok){
                 wpts = this.state.wpts.map( (wpt, i) => (
                        <WayPoint {...wpt} key={i} navigateTo={this.navigateTo}/>
                    )
                );
            }else{
                wpts = 'Failed to fetch WPTs';
            }
            return (
                <div>
                    {wpts}
                </div>
            )
        }
    }
}

export default WayPointsList;
