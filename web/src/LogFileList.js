import React, {Component} from 'react';
import LogFile from "./LogFile";

class LogFileList extends Component {
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
        if( this.state.loading ) {
            return (<div>Loading logs ...</div>)
        }else {
            let logs = this.state.logs.map( (log_name, i) => (
                    <LogFile log={log_name}  key={i} />
                )
            );
            return (
                <div>
                    {logs}
                    <a href="all_logs.zip">Get all files in one ZIP</a>

                </div>
            )
        }
    }
}

export default LogFileList;
