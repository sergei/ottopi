import React from 'react';

function LogFileView(props) {
    let href = "log/" + props.log;
    return (
        <div>
            <a href={href}>{props.log}</a>
        </div>
    );
}

export default LogFileView;
