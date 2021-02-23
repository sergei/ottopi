import React from 'react';
import RawInstrDisplayView from "../views/RawInstrDisplayView";

class RawInstrDisplay extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        ok: false,
        instr: null,
        response: null,
        timer: null,
    };

    componentDidMount() {
        this.updateInstrumentsStatus();
        let timer = setInterval(this.updateInstrumentsStatus, 1000);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if( this.clearInterval )  // Prevent crash on sign out
            this.clearInterval(this.state.timer);
    }

    updateInstrumentsStatus = () => {
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_raw_instr().then(response => {
                this.setState( {loading:false, ok: true, instr: response.body} )
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
        return ( <RawInstrDisplayView loading={this.state.loading}  ok={this.state.ok}
                                      instr={this.state.instr}
        />);
    }
}

RawInstrDisplay.propTypes = {};

export default RawInstrDisplay;
