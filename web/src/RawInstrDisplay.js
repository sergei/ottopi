import React from 'react';

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
        let timer = setInterval(this.updateInstrumentsStatus, 5000);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if( this.clearInterval )  // Prevent crash on sign out
            this.clearInterval(this.state.timer);
    }

    updateInstrumentsStatus = () => {
        console.log('Fetching raw instruments');
        this.props.swaggerClient
            .then( client => {client.apis.nav.nav_get_raw_instr().then(response => {
                console.log(response)
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
        if (this.state.loading){
            return(<div> Loading ...</div>);
        }else if( this.state.ok ){
            return (
                <div>AWA {this.state.instr.awa} AWS {this.state.instr.aws}</div>
            );
        }else{
            return (
                <div>Failed to fetch</div>
            );
        }
    }
}

RawInstrDisplay.propTypes = {};

export default RawInstrDisplay;
