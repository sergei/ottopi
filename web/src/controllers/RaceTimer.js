import React, {Component} from 'react';
import RaceTimerView from "../views/RaceTimerView";

class RaceTimer extends Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        ok: false,
        race_timer: null,
        timer: null,
    };

    componentDidMount() {
        this.updateRaceTimerStatus();
        let timer = setInterval(this.updateRaceTimerStatus, 750);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if ( typeof clearInterval === "function")
            clearInterval(this.state.timer);
    }

    start = () => {
        console.log('Start timer');
        this.props.swaggerClient
            .then( client => {
                client.apis.timer.rest_api_timer_start({},{})
                    .then(response => {
                        console.log(response);
                    }).catch( error => {
                    console.log("API error" + error);
                })
            }).catch( error => {
            console.log("Client error" + error);
        });
    }

    stop = () => {
        console.log('Stop timer');
        this.props.swaggerClient
            .then( client => {
                client.apis.timer.rest_api_timer_stop({},{})
                    .then(response => {
                        console.log(response);
                    }).catch( error => {
                    console.log("API error" + error);
                })
            }).catch( error => {
            console.log("Client error" + error);
        });
    }

    sync = () => {
        console.log('Sync timer');
        this.props.swaggerClient
            .then( client => {
                client.apis.timer.rest_api_timer_sync({},{})
                    .then(response => {
                        console.log(response);
                    }).catch( error => {
                    console.log("API error" + error);
                })
            }).catch( error => {
            console.log("Client error" + error);
        });
    }

    updateRaceTimerStatus = () => {
        this.props.swaggerClient
            .then( client => {client.apis.timer.rest_api_timer_get_data().then(response => {
                this.setState( {loading:false, ok: true, race_timer: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    }


    render() {
        return (
            <RaceTimerView {...this.state.race_timer} loading={this.state.loading}  ok={this.state.ok}
                           start={this.start} stop={this.stop} sync={this.sync}
            />
        );
    }
}

export default RaceTimer;
