import React from 'react';
import LogFileView from "./LogFileView";

function LogFileListView(props) {
    if( props.loading ) {
        return (<div>Loading logs ...</div>)
    }else {
        let logs;
        if ( props.ok){
            logs = props.logs.map( (log_name, i) => (
                    <LogFileView log={log_name} key={i} />
                )
            );
        }else{
            logs = 'Failed to fetch logs';
        }

        return (
            <div>
                <a href="all_logs.zip">Get all files in one ZIP</a>
                {logs}
            </div>
        )
    }
}

export default LogFileListView;
