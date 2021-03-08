import React from 'react';
import DestInfoView from "../views/DestInfoView";

class DestInfo extends React.Component {

    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        ok: false,
        dest: null,
        timer: null,
    };

    componentDidMount() {
        this.updateDestStatus();
        let timer = setInterval(this.updateDestStatus, 1000);
        this.setState({
            timer: timer,
        });
    }

    componentWillUnmount() {
        if ( typeof clearInterval === "function")
            clearInterval(this.state.timer);
    }

    updateDestStatus = () => {
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_dest().then(response => {
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

    render() {
        return ( <DestInfoView loading={this.state.loading}  ok={this.state.ok}
                               dest={this.state.dest}
        />
        );
    }
}

DestInfo.propTypes = {};

export default DestInfo;
