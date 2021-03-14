import React from 'react';
import SpeakerView from "../views/SpeakerView";

class Speaker extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        speakerState: null,
        timer: null,
        volumeValue: 50,
        newVolumeValue: 50,
    };

    componentDidMount() {
        this.getSpeakerState();
        this.getNewVolume();
        let timer = setInterval(this.monitorVolumeChange, 2000);
        this.setState({
            timer: timer,
        });
    }

    monitorVolumeChange = () => {
        if (this.state.newVolumeValue !== this.state.volumeValue) {
            this.setNewVolume();
        }
    }

    onVolumeChange = (event, newValue) => {
        console.log("Volume:", newValue)
        this.setState({newVolumeValue: newValue})
    }

    getNewVolume = () => {
        this.props.swaggerClient
            .then( client => {client.apis.speaker.rest_api_get_speaker_volume().then(response => {
                this.setState( {loading:false, ok: true,
                    newVolumeValue: response.body, volumeValue: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    }


    setNewVolume = () => {
        console.log("Set New volume VolumeChange:", this.state.newVolumeValue, this.state.volumeValue)
        const percent = this.state.newVolumeValue;
        this.props.swaggerClient
            .then( client => {client.apis.speaker.rest_api_set_speaker_volume({percent}, {}).then(response => {
                this.setState( {loading:false, ok: true,
                    newVolumeValue: response.body, volumeValue: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    }

    getSpeakerState = () => {
        this.props.swaggerClient
            .then( client => {client.apis.speaker.rest_api_get_speaker_state().then(response => {
                this.setState( {loading:false, ok: true, speakerState: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    }

    postSpeakerState = (speakerState) => {
        this.setState( {loading:true, ok: false} )
        this.props.swaggerClient
            .then( client => {client.apis.speaker.rest_api_set_speaker_state({}, {requestBody: speakerState})
                .then(response => {this.setState( {loading:false, ok: true, speakerState: response.body} )
            }).catch( error => {
                console.log("API error" + error);
                this.setState( {loading:false, ok: false} )
            })
            }).catch( error => {
            console.log("Client error" + error);
            this.setState( {loading:false, ok: false} )
        });
    }


    toggleRoute = () => {
        console.log('toggleRoute');
        let speakerState = {...this.state.speakerState};
        speakerState.route = !speakerState.route;
        console.log(speakerState);
        this.postSpeakerState(speakerState);
    }

    togglePerformance = () => {
        console.log('togglePerformance');
        let speakerState = {...this.state.speakerState};
        speakerState.performance = !speakerState.performance;
        console.log(speakerState);
        this.postSpeakerState(speakerState);
    }


    render() {
        return ( <SpeakerView loading={this.state.loading}
                              speakerState={this.state.speakerState}
                              toggleRoute={this.toggleRoute}
                              togglePerformance={this.togglePerformance}
                              volumeValue = {this.state.newVolumeValue}
                              onVolumeChange = {this.onVolumeChange}

            />);
    }
}

export default Speaker;

