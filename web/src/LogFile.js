import React from 'react';

function LogFile(props) {
    let href = "log/" + props.log;
    return (
        <div>
            <a href={href}>{props.log}</a>
        </div>
    );
}

export default LogFile;
