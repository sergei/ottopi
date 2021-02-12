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
            .then( client => {client.apis.nav.rest_api_get_raw_instr().then(response => {
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

    toFixed = (val, dec) =>{
        if (val === null) return '---';
        if (typeof val !== 'undefined') {
            return val.toFixed(dec);
        }else{
            return '---';
        }
    }

    render() {
        if (this.state.loading){
            return(<div> Loading ...</div>);
        }else if( this.state.ok ){
            return (
                <div>
                    <div>AWA {this.toFixed(this.state.instr.awa,0)} AWS {this.toFixed(this.state.instr.aws,1)}</div>
                    <div>TWA {this.toFixed(this.state.instr.twa,0)} TWS {this.toFixed(this.state.instr.tws,1)}</div>
                    <div>SOG {this.toFixed(this.state.instr.sog,1)} TWS {this.toFixed(this.state.instr.sow,1)}</div>
                    <div>COG {this.toFixed(this.state.instr.cog,0)} HDG {this.toFixed(this.state.instr.hdg,0)}</div>
                </div>
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
