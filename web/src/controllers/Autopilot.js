import React, {Component} from 'react';
import DestInfoView from "../views/DestInfoView";
import AutopilotView from "../views/AutopilotView";

class Autopilot extends Component {

    // State of this component
    state = {
        loading: false, // will be true when ajax request is running
        ok: true,
    };

    turn = (degrees) => {
        console.log('Turn ', degrees, ' degrees');
        this.setState( {loading:true, ok: true} )
        this.props.swaggerClient
            .then( client => {
                client.apis.autopilot.rest_api_steer({degrees}, {})
                    .then(response => {
                        this.setState( {loading:false, ok: true} );
                    }).catch( error => {
                    console.log("API error" + error);
                    this.setState( {loading:false, ok: false} );
                })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} );
        });
    };

    tack = () => {
        console.log('Tack or gybe ');
        this.setState( {loading:true, ok: true} )
        this.props.swaggerClient
            .then( client => {
                client.apis.autopilot.rest_api_tack({},{})
                    .then(response => {
                        this.setState( {loading:false, ok: true} );
                    }).catch( error => {
                    console.log("API error" + error);
                    this.setState( {loading:false, ok: false} );

                })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} );

        });
    };

    render() {
        return ( <AutopilotView loading={this.state.loading}  ok={this.state.ok}
                               turn={this.turn} tack={this.tack}
            />
        );
    }
}

export default Autopilot;
