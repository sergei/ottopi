import React, {Component} from 'react';
import LogFileListView from "../views/LogFileListView";

class LogFiles extends Component {
    // State of this component
    state = {
        loading: true, // will be true when ajax request is running
        result: null,
    };

    requestLogs = () => {
        console.log('Fetching logs');
        this.props.swaggerClient
            .then( client => {client.apis.nav.rest_api_get_logs().then(response => {
                console.log(response)
                this.setState( {loading:false, ok: true, logs: response.body} )
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
        this.requestLogs();
    }

    render() {
        return ( <LogFileListView loading={this.state.loading}  ok={this.state.ok}
                                logs={this.state.logs}
                />);
        }
}

export default LogFiles;
