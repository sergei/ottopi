import React, {Component} from 'react';

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
                client.apis.autopilot.rest_api_steer({degrees},{})
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
        if( this.state.loading ) {
            return (<div>Sending command ...</div>)
        }else {
            let status = '';
            if ( !this.state.ok ){
                status = 'Failed to send command';
            }

            return (
                <div>
                    <div>
                        <button onClick={() => this.turn(-1)}>1 deg left</button>
                        <button onClick={() => this.turn(1)}> 1 deg right</button>
                    </div>
                    <div>
                        <button onClick={() => this.turn(-5)}> 5 deg left</button>
                        <button onClick={() => this.turn(5)}> 5 deg right</button>
                    </div>
                    <div>
                        <button onClick={() => this.turn(-10)}> 10 deg left</button>
                        <button onClick={() => this.turn(10)}> 10 deg right</button>
                    </div>
                    <div>
                        <button onClick={() => this.tack()}>Tack/Gybe</button>
                    </div>
                    <div>{status}</div>
                </div>
            );
        }
    }
}

export default Autopilot;
