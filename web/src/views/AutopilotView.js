import React from 'react';

function AutopilotView(props){

        if( props.loading ) {
            return (<div>Sending command ...</div>)
        }else {
            let status = '';
            if ( !props.ok ){
                status = 'Failed to send command';
            }

            return (
                <div>
                    <div>
                        <button onClick={() => props.turn(-1)}>1 deg left</button>
                        <button onClick={() => props.turn(1)}> 1 deg right</button>
                    </div>
                    <div>
                        <button onClick={() => props.turn(-5)}> 5 deg left</button>
                        <button onClick={() => props.turn(5)}> 5 deg right</button>
                    </div>
                    <div>
                        <button onClick={() => props.turn(-10)}> 10 deg left</button>
                        <button onClick={() => props.turn(10)}> 10 deg right</button>
                    </div>
                    <div>
                        <button onClick={() => props.tack()}>Tack/Gybe</button>
                    </div>
                    <div>{status}</div>
                </div>
            );
        }
}

export default AutopilotView;
