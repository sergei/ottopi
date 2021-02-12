import React from 'react';

class DestInfo extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        ok: false,
        dest: null,
        response: null,
        timer: null,
    };

    componentDidMount() {
        this.updateDestStatus();
        let timer = setInterval(this.updateDestStatus, 5000);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if( this.clearInterval )  // Prevent crash on sign out
            this.clearInterval(this.state.timer);
    }

    updateDestStatus = () => {
        console.log('Fetching destination');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_dest().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, dest: response.body} )
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
        if (val === null) return '';
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
            const direction = this.state.dest.atw_up ? 'up' : 'down;'
            return (
                <div>
                    <div>{this.state.dest.name} {this.toFixed(Math.abs(this.state.dest.atw),1)} degrees {direction} </div>
                    DTW {this.toFixed(this.state.dest.dtw,3)} BTW {this.toFixed(this.state.dest.btw,0)}
                </div>
            );
        }else{
            return (
                <div>Failed to fetch</div>
            );
        }
    }
}

DestInfo.propTypes = {};

export default DestInfo;
